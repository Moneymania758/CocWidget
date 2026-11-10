package com.cocwidget.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.cocwidget.events.EventKind
import com.cocwidget.game.EventDiscount
import com.cocwidget.game.Section
import com.cocwidget.game.Village
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Bedtime
import com.cocwidget.widget.WidgetView
import org.json.JSONException
import org.json.JSONObject

// App-wide settings live in the "village" file. Each account's export and choices live in a
// file of their own, named after the player tag.
object Store {
    private const val KEY_ACCOUNTS = "accounts"
    private const val KEY_SELECTED = "selected_account"
    private const val KEY_WIDGET = "widget_"
    private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    private const val KEY_EVENT_ON = "event_discount_on"
    private const val KEY_REMINDER = "event_reminder_"
    private const val KEY_REMINDER_SINCE = "event_reminder_since_"
    private const val KEY_REMINDERS_STARTED = "event_reminders_started"
    private const val KEY_REMINDERS_NOTIFIED = "event_reminders_notified"
    private const val KEY_EVENT_DATES = "event_dates"
    private const val KEY_LAST_DATES_CHECK = "last_event_dates_check"
    private const val KEY_WHATS_NEW_SEEN = "whats_new_seen"
    private const val KEY_SKIN = "skin"
    private const val KEY_BEDTIME = "bedtime_minute"
    private const val KEY_WAKE = "wake_minute"

    // Per account. Up to 1.9.1 these sat in the app-wide file, for the only account.
    private const val KEY_EXPORT = "export"
    // Answers to a Sync question that's gone. Only still here so an old install moves them out.
    private const val KEY_BUILDER_MULTIPLIER = "builder_multiplier"
    private const val KEY_LAB_MULTIPLIER = "lab_multiplier"
    private const val KEY_NAME = "name"
    private const val KEY_COLLAPSED = "collapsed_sections"
    private const val KEY_NOTIFIED = "notified_upgrades"
    private const val KEY_DISCOUNTED = "discounted_upgrades"
    private const val KEY_UPGRADE_IDS = "upgrade_ids"
    // The big widget's view, by WidgetView name. Newer than any 1.9.1 install, so not moved.
    private const val KEY_WIDGET_VIEW = "widget_view"
    // When the player last cleared finished upgrades. 2.12.1 kept one time for every section,
    // later versions keep one per section, as the widget clears only its view. Newer than any
    // 1.9.1 install, so not moved.
    private const val KEY_CLEARED_AT = "cleared_finished_at"
    private const val KEY_CLEARED_AT_SECTION = "cleared_finished_at_"
    private val ACCOUNT_KEYS = listOf(
        KEY_EXPORT, KEY_BUILDER_MULTIPLIER, KEY_LAB_MULTIPLIER, KEY_NAME, KEY_COLLAPSED,
        KEY_NOTIFIED, KEY_DISCOUNTED, KEY_UPGRADE_IDS,
    )

    // An export without a tag can't be told apart from another, so all of them share this one.
    private const val UNKNOWN_TAG = "?"

    internal const val SHARED_TAG = "#SHARED"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("village", Context.MODE_PRIVATE).also { global ->
            migrate(global) { tag -> account(context, tag) }
        }

    private fun account(context: Context, tag: String): SharedPreferences =
        context.getSharedPreferences(fileName(tag), Context.MODE_PRIVATE)

    internal fun fileName(tag: String) = "account_" + tag.filter { it.isLetterOrDigit() }

    internal fun tagOf(json: String): String = try {
        JSONObject(json).optString("tag").ifEmpty { UNKNOWN_TAG }
    } catch (e: JSONException) {
        UNKNOWN_TAG
    }

    // The export as copied from the game with the player tag swapped out everywhere it appears,
    // for sending to the developer.
    internal fun withoutTag(json: String): String {
        val tag = tagOf(json)
        return if (tag == UNKNOWN_TAG) json else json.replace(tag, SHARED_TAG)
    }

    // Moves the single account kept by 1.9.1 and earlier into its own file. The account's file
    // is written before the old keys go, so a crash in between only means doing it again. The
    // widget and the app can both get here first after an update, hence the lock.
    @Synchronized
    @SuppressLint("ApplySharedPref")
    internal fun migrate(global: SharedPreferences, account: (String) -> SharedPreferences) {
        val json = global.getString(KEY_EXPORT, null) ?: return
        val tag = tagOf(json)
        val editor = account(tag).edit()
        for ((key, value) in global.all.filterKeys { it in ACCOUNT_KEYS }) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
        val moved = global.edit()
        ACCOUNT_KEYS.forEach(moved::remove)
        moved.putString(KEY_ACCOUNTS, added(accountList(global), tag).joinToString("\n"))
            .putString(KEY_SELECTED, tag)
            .commit()
    }

    private fun accountList(global: SharedPreferences): List<String> =
        global.getString(KEY_ACCOUNTS, null).orEmpty().split("\n").filter { it.isNotEmpty() }

    internal fun added(accounts: List<String>, tag: String) = if (tag in accounts) accounts else accounts + tag

    // A widget shows the account picked for it. One placed before there was a choice, or whose
    // account was removed, shows the first account synced.
    internal fun resolve(picked: String?, accounts: List<String>): String? =
        picked?.takeIf { it in accounts } ?: accounts.firstOrNull()

    // Player tags in the order they were first synced.
    fun accounts(context: Context): List<String> = accountList(prefs(context))

    fun hasExport(context: Context): Boolean = accounts(context).isNotEmpty()

    // The account the app's screens show: the last one synced or picked.
    fun selected(context: Context): String? = resolve(prefs(context).getString(KEY_SELECTED, null), accounts(context))

    fun setSelected(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_SELECTED, tag).apply()
    }

    fun widgetAccount(context: Context, widgetId: Int): String? =
        resolve(prefs(context).getString(KEY_WIDGET + widgetId, null), accounts(context))

    fun setWidgetAccount(context: Context, widgetId: Int, tag: String) {
        prefs(context).edit().putString(KEY_WIDGET + widgetId, tag).apply()
    }

    // A tap on a widget's account chip moves that widget, and only that one, to the next account.
    fun switchWidgetAccount(context: Context, widgetId: Int) {
        val accounts = accounts(context)
        nextAccount(widgetAccount(context, widgetId), accounts)?.let { setWidgetAccount(context, widgetId, it) }
    }

    // The account after this one, back to the first after the last. Null when there's nothing to switch to.
    internal fun nextAccount(current: String?, accounts: List<String>): String? {
        if (accounts.size < 2) return null
        return accounts[(accounts.indexOf(current) + 1) % accounts.size]
    }

    fun forgetWidgets(context: Context, widgetIds: IntArray) {
        val editor = prefs(context).edit()
        for (id in widgetIds) editor.remove(KEY_WIDGET + id)
        editor.apply()
    }

    @SuppressLint("ApplySharedPref")
    fun removeAccount(context: Context, tag: String) {
        val global = prefs(context)
        global.edit().putString(KEY_ACCOUNTS, (accountList(global) - tag).joinToString("\n")).apply()
        // Cleared first, so an instance still held in memory doesn't bring the old values back.
        account(context, tag).edit().clear().commit()
        context.deleteSharedPreferences(fileName(tag))
    }

    // Files the export under its player tag, adding the account if it's new, and shows it on the
    // app's screens. Also matches upgrades to the last sync's, to spot new ones for the event
    // discount.
    fun saveExport(context: Context, json: String): String {
        val tag = tagOf(json)
        val previous = rawVillage(context, tag)
        val current = VillageParser.parse(context, json)
        val tracked = current?.let {
            EventDiscount.track(previous, upgradeIds(context, tag), discountedUpgrades(context, tag), it, eventOn(context))
        }
        account(context, tag).edit()
            .putString(KEY_EXPORT, json)
            .putString(KEY_UPGRADE_IDS, tracked?.ids.orEmpty().joinToString("\n"))
            .putStringSet(KEY_DISCOUNTED, tracked?.discounted.orEmpty())
            .remove(KEY_CLEARED_AT)
            .apply { Section.entries.forEach { remove(KEY_CLEARED_AT_SECTION + it.name) } }
            .apply()
        val global = prefs(context)
        global.edit()
            .putString(KEY_ACCOUNTS, added(accountList(global), tag).joinToString("\n"))
            .putString(KEY_SELECTED, tag)
            .apply()
        return tag
    }

    // The selected account's village and its export as copied from the game, or null before the
    // first sync.
    class Current(val tag: String, val village: Village, val json: String)

    fun current(context: Context): Current? {
        val tag = selected(context) ?: return null
        val village = village(context, tag) ?: return null
        val json = exportJson(context, tag) ?: return null
        return Current(tag, village, json)
    }

    // Without the upgrades cleared since the last sync. The discount goes on first, as it matches
    // upgrades by their place in the export.
    fun village(context: Context, tag: String): Village? {
        val village = rawVillage(context, tag) ?: return null
        val discounted = EventDiscount.apply(village, upgradeIds(context, tag), discountedUpgrades(context, tag))
        val prefs = account(context, tag)
        val clearedAt = Section.entries.associateWith { maxOf(prefs.getLong(KEY_CLEARED_AT, 0), prefs.getLong(KEY_CLEARED_AT_SECTION + it.name, 0)) }
        return if (clearedAt.values.any { it > 0 }) discounted.withoutFinishedBy { clearedAt.getValue(it) } else discounted
    }

    // Hides what has finished by [now] in [sections] until the next sync, which brings back the
    // full export. The export itself is kept as copied, so the planner still sees it.
    fun clearFinished(context: Context, tag: String, now: Long, sections: Set<Section> = Section.entries.toSet()) {
        val editor = account(context, tag).edit()
        for (section in sections) editor.putLong(KEY_CLEARED_AT_SECTION + section.name, now)
        editor.apply()
    }

    // The last synced export as copied from the game, for the planner.
    fun exportJson(context: Context, tag: String): String? = account(context, tag).getString(KEY_EXPORT, null)

    private fun rawVillage(context: Context, tag: String): Village? {
        val json = exportJson(context, tag) ?: return null
        return VillageParser.parse(context, json)
    }

    private fun upgradeIds(context: Context, tag: String): List<String> =
        account(context, tag).getString(KEY_UPGRADE_IDS, null)?.takeIf { it.isNotEmpty() }?.split("\n").orEmpty()

    private fun discountedUpgrades(context: Context, tag: String): Set<String> =
        account(context, tag).getStringSet(KEY_DISCOUNTED, emptySet()).orEmpty()

    fun eventOn(context: Context): Boolean = prefs(context).getBoolean(KEY_EVENT_ON, false)

    fun setEventOn(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_EVENT_ON, on).apply()
    }

    fun eventReminderOn(context: Context, kind: EventKind): Boolean =
        prefs(context).getBoolean(KEY_REMINDER + kind.key, kind.onByDefault)

    // Switching one on notes the time, so it doesn't announce what has already started.
    fun setEventReminderOn(context: Context, kind: EventKind, on: Boolean, now: Long) {
        prefs(context).edit().putBoolean(KEY_REMINDER + kind.key, on).apply {
            if (on) putLong(KEY_REMINDER_SINCE + kind.key, now)
        }.apply()
    }

    // Notes the first check, so an update or a new install stays quiet about what already began.
    fun startEventReminders(context: Context, now: Long) {
        if (!prefs(context).contains(KEY_REMINDERS_STARTED)) prefs(context).edit().putLong(KEY_REMINDERS_STARTED, now).apply()
    }

    // Reminders from this time or earlier stay quiet.
    fun eventReminderSince(context: Context, kind: EventKind): Long = maxOf(
        prefs(context).getLong(KEY_REMINDERS_STARTED, 0),
        prefs(context).getLong(KEY_REMINDER_SINCE + kind.key, 0),
    )

    fun notifiedReminders(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_REMINDERS_NOTIFIED, emptySet()).orEmpty()

    fun setNotifiedReminders(context: Context, ids: Set<String>) {
        prefs(context).edit().putStringSet(KEY_REMINDERS_NOTIFIED, ids).apply()
    }

    fun eventDates(context: Context): String? = prefs(context).getString(KEY_EVENT_DATES, null)

    fun setEventDates(context: Context, text: String) {
        prefs(context).edit().putString(KEY_EVENT_DATES, text).apply()
    }

    fun lastEventDatesCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_DATES_CHECK, 0)

    fun setLastEventDatesCheck(context: Context, now: Long) {
        prefs(context).edit().putLong(KEY_LAST_DATES_CHECK, now).apply()
    }

    // What the event switch takes off upgrades not started yet, 0 with it off.
    fun eventPercent(context: Context): Int = if (eventOn(context)) EventDiscount.PERCENT else 0

    fun collapsedSections(context: Context, tag: String): Set<String> =
        account(context, tag).getStringSet(KEY_COLLAPSED, emptySet()).orEmpty()

    fun toggleSection(context: Context, tag: String, section: String) {
        val collapsed = collapsedSections(context, tag).toMutableSet()
        if (!collapsed.remove(section)) collapsed += section
        account(context, tag).edit().putStringSet(KEY_COLLAPSED, collapsed).apply()
    }

    fun setCollapsedSections(context: Context, tag: String, sections: Set<String>) {
        account(context, tag).edit().putStringSet(KEY_COLLAPSED, sections).apply()
    }

    // Null until a view is picked on 2.12.2 or later.
    fun widgetView(context: Context, tag: String): WidgetView? {
        val name = account(context, tag).getString(KEY_WIDGET_VIEW, null)
        return WidgetView.entries.firstOrNull { it.name == name }
    }

    // Picking a view also unfolds its sections, so the tap always shows something.
    fun setWidgetView(context: Context, tag: String, view: WidgetView) {
        val collapsed = collapsedSections(context, tag) - view.shows.map { it.name }.toSet()
        account(context, tag).edit().putString(KEY_WIDGET_VIEW, view.name).putStringSet(KEY_COLLAPSED, collapsed).apply()
    }

    fun lastUpdateCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_UPDATE_CHECK, 0)

    fun setLastUpdateCheck(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_LAST_UPDATE_CHECK, millis).apply()
    }

    fun latestVersion(context: Context): String? = prefs(context).getString(KEY_LATEST_VERSION, null)

    fun setLatestVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_LATEST_VERSION, version).apply()
    }

    fun notifiedUpgrades(context: Context, tag: String): Set<String> =
        account(context, tag).getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()

    fun setNotifiedUpgrades(context: Context, tag: String, keys: Set<String>) {
        account(context, tag).edit().putStringSet(KEY_NOTIFIED, keys).apply()
    }

    fun askedNotifications(context: Context): Boolean = prefs(context).getBoolean(KEY_ASKED_NOTIFICATIONS, false)

    fun setAskedNotifications(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
    }

    fun displayName(context: Context, tag: String): String? =
        account(context, tag).getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }

    fun setDisplayName(context: Context, tag: String, name: String) {
        account(context, tag).edit().putString(KEY_NAME, name).apply()
    }

    // The account's name if one was set, otherwise its player tag.
    fun label(context: Context, tag: String): String = displayName(context, tag) ?: tag

    // The version whose What's new card was last dismissed.
    fun whatsNewSeen(context: Context): String? = prefs(context).getString(KEY_WHATS_NEW_SEEN, null)

    fun setWhatsNewSeen(context: Context, version: String) {
        prefs(context).edit().putString(KEY_WHATS_NEW_SEEN, version).apply()
    }

    // The app's chosen look, by Skin key.
    fun skin(context: Context): String? = prefs(context).getString(KEY_SKIN, null)

    fun setSkin(context: Context, key: String) {
        prefs(context).edit().putString(KEY_SKIN, key).apply()
    }

    // The same for every account: it's the player who sleeps. 23:00 to 07:00 until changed.
    fun sleep(context: Context) = Bedtime.Sleep(
        prefs(context).getInt(KEY_BEDTIME, 23 * 60),
        prefs(context).getInt(KEY_WAKE, 7 * 60),
    )

    fun setSleep(context: Context, sleep: Bedtime.Sleep) {
        prefs(context).edit().putInt(KEY_BEDTIME, sleep.bedMinute).putInt(KEY_WAKE, sleep.wakeMinute).apply()
    }
}
