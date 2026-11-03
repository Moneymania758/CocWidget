package com.cocwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import com.cocwidget.MainActivity
import com.cocwidget.NextWidget
import com.cocwidget.R
import com.cocwidget.SyncActivity
import com.cocwidget.VillageWidget
import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.store.UpdateChecker
import com.cocwidget.ui.TimeText

object WidgetRenderer {
    private const val REFRESH_REQUEST = 0
    private const val FINISH_REQUEST = 1

    fun update(context: Context) {
        // Notifications work without a widget on the home screen, so they come first.
        val accounts = Store.accounts(context)
        val villages = accounts.mapNotNull { tag -> Store.village(context, tag)?.let { tag to it } }.toMap()
        val checkedAt = System.currentTimeMillis()
        for ((tag, village) in villages) {
            val label = if (accounts.size > 1) Store.label(context, tag) else null
            Notifier.notifyFinished(context, tag, village, checkedAt, label)
        }
        scheduleNextFinish(context, villages.values, checkedAt)

        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, VillageWidget::class.java))
        val nextIds = manager.getAppWidgetIds(ComponentName(context, NextWidget::class.java))
        if (widgetIds.isEmpty() && nextIds.isEmpty()) return

        UpdateChecker.checkIfDue(context)
        val shown = (widgetIds + nextIds).associateWith { Store.widgetAccount(context, it) }
        for (tag in shown.values.filterNotNull().distinct()) villages[tag]?.let { Icons.fetchMissing(context, it.upgrades) }
        val now = System.currentTimeMillis()
        for (widgetId in widgetIds) {
            val tag = shown[widgetId]
            manager.updateAppWidget(widgetId, build(context, widgetId, tag, tag?.let(villages::get), now))
        }
        for (widgetId in nextIds) {
            val tag = shown[widgetId]
            manager.updateAppWidget(widgetId, buildNext(context, widgetId, tag, tag?.let(villages::get), now))
        }
        scheduleNext(context, shown.values.distinct().minOf { Countdown.nextRefresh(it?.let(villages::get), now) })
    }

    // Only the redraw alarm, and only once neither kind of widget is left. The finish alarm
    // keeps notifications coming without a widget.
    fun cancel(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val left = listOf(VillageWidget::class.java, NextWidget::class.java).sumOf { manager.getAppWidgetIds(ComponentName(context, it)).size }
        if (left > 0) return
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context, REFRESH_REQUEST))
    }

    // The small widget: the next upgrade to finish, and what's idle. Every view is set on every
    // update, as the launcher applies it on top of the last one.
    private fun buildNext(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next)
        views.setOnClickPendingIntent(R.id.next_root, openAppIntent(context, widgetId, tag))
        views.setViewVisibility(R.id.days, View.GONE)
        views.setViewVisibility(R.id.timer, View.GONE)
        if (tag == null || village == null) {
            views.setViewVisibility(R.id.icon, View.GONE)
            views.setTextViewText(R.id.name, context.getString(R.string.app_name))
            views.setTextViewText(R.id.status, context.getString(R.string.next_no_village))
            views.setTextColor(R.id.status, context.getColor(R.color.text_dim))
            return views
        }

        val summary = NextFinish.of(village, now)
        val next = summary.next
        if (next == null) {
            views.setViewVisibility(R.id.icon, View.GONE)
            views.setTextViewText(R.id.name, context.getString(R.string.next_nothing))
        } else {
            views.setViewVisibility(R.id.icon, View.VISIBLE)
            val icon = Icons.load(context, next)
            if (icon != null) views.setImageViewBitmap(R.id.icon, icon) else views.setImageViewResource(R.id.icon, R.drawable.ic_placeholder)
            views.setTextViewText(R.id.name, upgradeName(next))
            // A refresh is scheduled for when the chronometer reaches zero (Countdown.nextRefresh).
            val split = Countdown.split(village.endMillis(next) - now)
            views.setTextViewText(R.id.days, "${split.days}d")
            views.setViewVisibility(R.id.days, if (split.days > 0) View.VISIBLE else View.GONE)
            views.setChronometer(R.id.timer, SystemClock.elapsedRealtime() + split.restMs, null, true)
            views.setChronometerCountDown(R.id.timer, true)
            views.setViewVisibility(R.id.timer, View.VISIBLE)
        }

        // Anything idle comes first, in orange like the big widget's idle builders.
        val status = if (summary.anyIdle) {
            buildList {
                if (summary.idleBuilders > 0) add(context.resources.getQuantityString(R.plurals.next_builders_idle, summary.idleBuilders, summary.idleBuilders))
                if (summary.labIdle) add(context.getString(R.string.next_lab_idle))
                if (summary.petsIdle) add(context.getString(R.string.next_pets_idle))
            }.joinToString(" · ")
        } else {
            context.resources.getQuantityString(R.plurals.next_all_busy, summary.builders, summary.builders)
        }
        views.setTextViewText(R.id.status, status)
        views.setTextColor(R.id.status, context.getColor(if (summary.anyIdle) R.color.idle else R.color.gold))
        return views
    }

    private fun build(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget)
        views.setOnClickPendingIntent(R.id.sync, activityIntent(context, SyncActivity::class.java, 0))
        views.setOnClickPendingIntent(R.id.title, openAppIntent(context, widgetId, tag))
        views.removeAllViews(R.id.rows)
        // The launcher applies an update on top of the views already on screen, so anything not
        // set here keeps its state from the last update, even one for another account. Lines
        // shown only sometimes start hidden on every update.
        views.setViewVisibility(R.id.helpers, View.GONE)
        views.setViewVisibility(R.id.views, View.GONE)

        val update = UpdateChecker.availableUpdate(context)
        views.setViewVisibility(R.id.update, if (update != null) View.VISIBLE else View.GONE)
        if (update != null) {
            views.setTextViewText(R.id.update, context.getString(R.string.update_available, update))
            val open = Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE))
            views.setOnClickPendingIntent(R.id.update, PendingIntent.getActivity(context, 3, open, PendingIntent.FLAG_IMMUTABLE))
        }

        if (tag == null || village == null) {
            views.setTextViewText(R.id.title, context.getString(R.string.app_name))
            views.setTextViewText(R.id.town_hall, "")
            views.setViewVisibility(R.id.empty, View.VISIBLE)
            views.setViewVisibility(R.id.footer, View.GONE)
            return views
        }

        views.setViewVisibility(R.id.empty, View.GONE)
        views.setViewVisibility(R.id.footer, View.VISIBLE)
        views.setTextViewText(R.id.title, Store.label(context, tag))
        views.setTextViewText(R.id.town_hall, if (village.townHall > 0) "TH${village.townHall}" else "")
        val exported = TimeText.exported(village.exportedAtMillis, now)
        // A finished upgrade usually means a new one has started in the game that the export can't show.
        val finishedSince = village.upgrades.count { village.endMillis(it) <= now }
        val old = now - village.exportedAtMillis > Countdown.DAY_MS
        val footer = when {
            finishedSince > 0 -> context.getString(R.string.exported_finished_since, exported, finishedSince)
            old -> context.getString(R.string.exported_out_of_date, exported)
            else -> context.getString(R.string.exported_at, exported)
        }
        views.setTextViewText(R.id.footer, footer)
        views.setTextColor(R.id.footer, context.getColor(if (finishedSince > 0 || old) R.color.idle else R.color.text_dim))
        showHelpers(context, views, village, now)

        // Rows stay in a plain LinearLayout rather than a scrolling ListView: on One UI the
        // countdowns stop ticking inside a ListView. Sections fold instead, to make room.
        val shown = Shown(widgetId, tag, village, now)
        val sections = buildSet {
            add(Section.BUILDERS)
            add(Section.LAB)
            if (village.hasPetHouse) add(Section.PETS)
            if (village.hasBuilderBase || village.upgrades.any { it.section == Section.BUILDER_BASE }) add(Section.BUILDER_BASE)
        }
        showViews(context, views, shown, sections)
        addSection(context, views, shown, Section.BUILDERS, "BUILDERS", village.builders)
        addSection(context, views, shown, Section.LAB, "LAB", 1)
        if (Section.PETS in sections) addSection(context, views, shown, Section.PETS, "PETS", 1)
        if (Section.BUILDER_BASE in sections) addSection(context, views, shown, Section.BUILDER_BASE, "BUILDER BASE", 0)
        return views
    }

    // Without the Builder Base on the widget, Home and All would be the same, so no buttons.
    private fun showViews(context: Context, views: RemoteViews, shown: Shown, sections: Set<Section>) {
        if (Section.BUILDER_BASE !in sections) return
        views.setViewVisibility(R.id.views, View.VISIBLE)
        val current = WidgetView.matching(Store.collapsedSections(context, shown.tag), sections)
        val buttons = mapOf(WidgetView.HOME to R.id.view_home, WidgetView.BUILDER_BASE to R.id.view_builder_base, WidgetView.ALL to R.id.view_all)
        for ((view, id) in buttons) {
            val on = view == current
            views.setInt(id, "setBackgroundResource", if (on) R.drawable.chip_view_on else R.drawable.chip_view)
            views.setTextColor(id, context.getColor(if (on) R.color.gold else R.color.text_dim))
            views.setOnClickPendingIntent(id, viewIntent(context, shown, view))
        }
    }

    // Ready is orange like an idle builder: a free shift goes to waste.
    private fun showHelpers(context: Context, views: RemoteViews, village: Village, now: Long) {
        val helpers = village.helpers
        if (helpers.apprenticeLevel == 0 && helpers.assistantLevel == 0) return
        views.setViewVisibility(R.id.helpers, View.VISIBLE)
        val readyIn = village.helpersReadyMillis() - now
        views.setViewVisibility(R.id.helpers_timer, if (readyIn > 0) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.helpers_ready, if (readyIn > 0) View.GONE else View.VISIBLE)
        if (readyIn > 0) {
            views.setChronometer(R.id.helpers_timer, SystemClock.elapsedRealtime() + readyIn, context.getString(R.string.helpers_ready_in), true)
            views.setChronometerCountDown(R.id.helpers_timer, true)
        } else {
            views.setTextColor(R.id.helpers_ready, context.getColor(R.color.idle))
        }
    }

    // What one widget is drawing.
    private class Shown(val widgetId: Int, val tag: String, val village: Village, val now: Long)

    private fun addSection(
        context: Context,
        views: RemoteViews,
        shown: Shown,
        section: Section,
        title: String,
        slots: Int,
    ) {
        val village = shown.village
        val now = shown.now
        val upgrades = village.upgrades.filter { it.section == section }.sortedBy { village.endMillis(it) }
        val running = upgrades.filter { village.endMillis(it) > now }
        val busy = running.count { !it.extra }

        val label = SpannableStringBuilder(title)
        if (section == Section.BUILDERS) label.append(" $busy/$slots")
        if (running.any { it.extra }) {
            label.append(" + Goblin", ForegroundColorSpan(context.getColor(R.color.goblin)), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // The Builder Base has no slot count, so it reads idle only when nothing runs there.
        val idle = if (section == Section.BUILDER_BASE && running.isEmpty()) 1 else slots - busy
        if (idle > 0) {
            val text = if (section == Section.BUILDERS) "  $idle idle" else "  idle"
            label.append(text, ForegroundColorSpan(context.getColor(R.color.idle)), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val collapsed = section.name in Store.collapsedSections(context, shown.tag)
        val marker = if (collapsed) "  ▸ ${upgrades.size}" else "  ▾"
        label.append(marker, ForegroundColorSpan(context.getColor(R.color.fold_marker)), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val header = RemoteViews(context.packageName, R.layout.widget_section)
        header.setTextViewText(R.id.label, label)
        header.setOnClickPendingIntent(R.id.section, toggleIntent(context, shown, section))
        val boost = village.boostFor(section)
        val boostLeft = village.boostEndMillis(boost) - now
        if (boost.seconds > 0 && boostLeft > 0) {
            header.setChronometer(R.id.badge, SystemClock.elapsedRealtime() + boostLeft, "×${boost.multiplier}  %s", true)
            header.setChronometerCountDown(R.id.badge, true)
            header.setViewVisibility(R.id.badge, View.VISIBLE)
        } else {
            header.setViewVisibility(R.id.badge, View.GONE)
        }
        views.addView(R.id.rows, header)
        if (collapsed) return
        for (upgrade in upgrades) views.addView(R.id.rows, row(context, village, upgrade, now))
    }

    private fun row(context: Context, village: Village, upgrade: Upgrade, now: Long): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_row)
        row.setTextViewText(R.id.name, upgradeName(upgrade))
        row.setViewVisibility(R.id.goblin, if (upgrade.extra) View.VISIBLE else View.GONE)
        row.setProgressBar(R.id.bar, 1000, (village.progress(upgrade, now) * 1000).toInt(), false)

        val icon = Icons.load(context, upgrade)
        if (icon != null) row.setImageViewBitmap(R.id.icon, icon) else row.setImageViewResource(R.id.icon, R.drawable.ic_placeholder)

        val left = village.endMillis(upgrade) - now
        row.setViewVisibility(R.id.helper, if (left > 0 && village.isHelped(upgrade, now)) View.VISIBLE else View.GONE)
        if (left <= 0) {
            row.setViewVisibility(R.id.clock, View.GONE)
            row.setViewVisibility(R.id.days, View.GONE)
            row.setViewVisibility(R.id.timer, View.GONE)
            row.setViewVisibility(R.id.done, View.VISIBLE)
            return row
        }

        // A refresh is scheduled for when the chronometer reaches zero (Countdown.nextRefresh).
        val split = Countdown.split(left)
        row.setTextViewText(R.id.days, "${split.days}d")
        row.setViewVisibility(R.id.days, if (split.days > 0) View.VISIBLE else View.GONE)
        row.setChronometer(R.id.timer, SystemClock.elapsedRealtime() + split.restMs, null, true)
        row.setChronometerCountDown(R.id.timer, true)
        row.setViewVisibility(R.id.done, View.GONE)
        return row
    }

    // "Cannon → 12", or "X-Bow ⚡2" for a supercharge (the row ellipsizes, so the stage stays short).
    private fun upgradeName(upgrade: Upgrade): CharSequence = SpannableStringBuilder(upgrade.name)
        .append(if (upgrade.supercharge > 0) "  ⚡${upgrade.supercharge}" else "  → ${upgrade.toLevel}", RelativeSizeSpan(0.85f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // Redraws don't wake the phone. A sleeping screen doesn't need fresh numbers.
    private fun scheduleNext(context: Context, at: Long) {
        setAlarm(context, AlarmManager.RTC, at, refreshIntent(context, REFRESH_REQUEST))
    }

    // Wakes the phone when the next upgrade finishes, so its notification arrives on time.
    private fun scheduleNextFinish(context: Context, villages: Collection<Village>, now: Long) {
        val intent = refreshIntent(context, FINISH_REQUEST)
        val next = villages.flatMap { village -> village.upgrades.map { village.endMillis(it) } }.filter { it > now }.minOrNull()
        if (next == null) {
            context.getSystemService(AlarmManager::class.java).cancel(intent)
            return
        }
        setAlarm(context, AlarmManager.RTC_WAKEUP, next, intent)
    }

    private fun setAlarm(context: Context, type: Int, at: Long, intent: PendingIntent) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(type, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(type, at, intent)
        }
    }

    private fun refreshIntent(context: Context, requestCode: Int): PendingIntent =
        broadcast(context, requestCode, Intent(context, VillageWidget::class.java).setAction(VillageWidget.ACTION_REFRESH))

    // The data Uri is unique per widget and section, so each header's intent keeps its own extras.
    private fun toggleIntent(context: Context, shown: Shown, section: Section): PendingIntent =
        broadcast(context, 100 + section.ordinal, accountIntent(context, shown, VillageWidget.ACTION_TOGGLE_SECTION, "toggle", section.name)
            .putExtra(VillageWidget.EXTRA_SECTION, section.name))

    private fun viewIntent(context: Context, shown: Shown, view: WidgetView): PendingIntent =
        broadcast(context, 200 + view.ordinal, accountIntent(context, shown, VillageWidget.ACTION_SET_VIEW, "view", view.name)
            .putExtra(VillageWidget.EXTRA_VIEW, view.name))

    // A tap on one widget's control, for its account.
    private fun accountIntent(context: Context, shown: Shown, action: String, kind: String, name: String): Intent =
        Intent(context, VillageWidget::class.java)
            .setAction(action)
            .setData(Uri.parse("cocwidget://$kind/${shown.widgetId}/$name"))
            .putExtra(VillageWidget.EXTRA_ACCOUNT, shown.tag)

    private fun broadcast(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    // Opens the app on the widget's account. A data Uri per widget keeps each widget's extra.
    private fun openAppIntent(context: Context, widgetId: Int, tag: String?): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java)
            .setData(Uri.parse("cocwidget://open/$widgetId"))
            .putExtra(MainActivity.EXTRA_ACCOUNT, tag)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun activityIntent(context: Context, activity: Class<*>, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
