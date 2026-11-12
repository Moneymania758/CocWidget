package com.cocwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.util.SizeF
import android.util.TypedValue
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import com.cocwidget.EventsWidget
import com.cocwidget.EventsWidgetConfigActivity
import com.cocwidget.MainActivity
import com.cocwidget.NextWidget
import com.cocwidget.R
import com.cocwidget.SyncActivity
import com.cocwidget.VillageWidget
import com.cocwidget.WidgetConfigActivity
import com.cocwidget.game.Boost
import com.cocwidget.game.Section
import com.cocwidget.game.UpNext
import com.cocwidget.game.Upgrade
import com.cocwidget.events.EventDates
import com.cocwidget.events.EventReminders
import com.cocwidget.events.EventSchedule
import com.cocwidget.game.Village
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.ui.UpNextLabels
import com.cocwidget.store.UpdateChecker
import com.cocwidget.ui.TimeText

object WidgetRenderer {
    private const val REFRESH_REQUEST = 0
    private const val FINISH_REQUEST = 1
    private const val EVENT_REQUEST = 2
    private const val MINUTE_REQUEST = 3
    // Up to this many, a tap on the account chip moves on to the next account. Above it, it opens a list.
    private const val SWAP_MAX_ACCOUNTS = 2
    private const val NARROW_MIN_DP = 180f
    // The longest row, "Legend League" beside "New Tournament in 27d 23:59:56", fits from here.
    private const val WIDE_MIN_DP = 340f
    private const val SHORT_MIN_DP = 40f
    // Header, a row and the Reset chip need about this much.
    private const val TALL_MIN_DP = 130f
    // The small widget's Up next and Glance sizes need about this much height. One row on the Pixel
    // launcher is about 108 dp, too short for them, and two rows about 227 dp.
    private const val NEXT_TALL_MIN_DP = 150f
    // A Pages widget's rows per page by the height it is drawn for, in dp. Header, chips, view
    // buttons and footer take about 190 dp, and a row about 46.
    // Pages: a row is 40 dp (its 34 dp picture and 3 dp above and below). Around the rows sit the
    // 12 dp padding top and bottom, the 34 dp header, the 27 dp status chips and the 38 dp footer
    // with its arrows. The Home / Builder Base strip, the helpers line and the potion chips add theirs
    // only when shown.
    private const val PAGE_ROW_DP = 40f
    private const val PAGE_CHROME_DP = 123f
    private const val PAGE_VIEWS_DP = 27f
    private const val PAGE_HELPERS_DP = 19f
    private const val PAGE_BOOSTS_DP = 24f
    private const val PAGE_MAX_ROWS = 12
    private const val DEFAULT_PER_PAGE = 4
    // The Timeline list's rows travel to the launcher in one go, pictures and all, so both are kept small.
    private const val TIMELINE_MAX_ROWS = 40
    private const val LIST_ICON_PX = 56

    fun update(context: Context, download: () -> String? = EventDates::fromGitHub) {
        // Notifications work without a widget on the home screen, so they come first.
        val accounts = Store.accounts(context)
        val villages = accounts.mapNotNull { tag -> Store.village(context, tag)?.let { tag to it } }.toMap()
        val checkedAt = System.currentTimeMillis()
        for ((tag, village) in villages) {
            val label = if (accounts.size > 1) Store.label(context, tag) else null
            Notifier.notifyFinished(context, tag, village, checkedAt, label)
        }
        scheduleNextFinish(context, villages.values, checkedAt)
        postEvents(context, checkedAt)
        drawWidgets(context, villages)
        // Last, as it's the slow part and nothing above should wait for the network.
        refreshEventDates(context, checkedAt, download)
    }

    private fun drawWidgets(context: Context, villages: Map<String, Village>) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, VillageWidget::class.java))
        val nextIds = manager.getAppWidgetIds(ComponentName(context, NextWidget::class.java))
        val eventIds = manager.getAppWidgetIds(ComponentName(context, EventsWidget::class.java))
        if (widgetIds.isEmpty() && nextIds.isEmpty() && eventIds.isEmpty()) return

        UpdateChecker.checkIfDue(context)
        val shown = (widgetIds + nextIds).associateWith { Store.widgetAccount(context, it) }
        for (tag in shown.values.filterNotNull().distinct()) villages[tag]?.let { Icons.fetchMissing(context, it.upgrades) }
        val now = System.currentTimeMillis()
        for (widgetId in widgetIds) {
            val tag = shown[widgetId]
            manager.updateAppWidget(widgetId, bigViews(context, widgetId, tag, tag?.let(villages::get), now))
        }
        for (widgetId in nextIds) {
            val tag = shown[widgetId]
            manager.updateAppWidget(widgetId, nextViews(context, widgetId, tag, tag?.let(villages::get), now))
        }
        val redraws = shown.values.distinct().map { Countdown.nextRefresh(it?.let(villages::get), now) }.toMutableList()
        scheduleTimelines(context, widgetIds, now)
        if (eventIds.isNotEmpty()) {
            val dates = EventDates.current(context)
            for (widgetId in eventIds) {
                // The launcher picks the closest that fits. A narrow widget puts each countdown under
                // its name, and a short one drops the header so a row still fits.
                manager.updateAppWidget(widgetId, RemoteViews(mapOf(
                    SizeF(NARROW_MIN_DP, SHORT_MIN_DP) to buildEvents(context, widgetId, now, dates, narrow = true, short = true),
                    SizeF(WIDE_MIN_DP, SHORT_MIN_DP) to buildEvents(context, widgetId, now, dates, narrow = false, short = true),
                    SizeF(NARROW_MIN_DP, TALL_MIN_DP) to buildEvents(context, widgetId, now, dates, narrow = true, short = false),
                    SizeF(WIDE_MIN_DP, TALL_MIN_DP) to buildEvents(context, widgetId, now, dates, narrow = false, short = false),
                )))
                redraws += Countdown.nextEventRefresh(shownEvents(context, widgetId, now, dates).map { it.target }, now)
            }
        }
        scheduleNext(context, redraws.min())
    }

    // Posts any event reminders due, sets the alarm, then looks for new dates. Does network, so only
    // call this off the main thread.
    fun checkEvents(
        context: Context,
        now: Long = System.currentTimeMillis(),
        download: () -> String? = EventDates::fromGitHub,
    ) {
        postEvents(context, now)
        refreshEventDates(context, now, download)
    }

    // An Events widget needs the dates even with every reminder off. New dates redraw it.
    private fun refreshEventDates(context: Context, now: Long, download: () -> String?) {
        val eventsPlaced = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, EventsWidget::class.java)).isNotEmpty()
        if (!EventReminders.refreshDates(context, now, download, eventsPlaced)) return
        postEvents(context, now)
        if (eventsPlaced) VillageWidget.requestRefresh(context)
    }

    // Has its own alarm, as the finish alarm is cancelled when nothing is upgrading.
    private fun postEvents(context: Context, now: Long) {
        val intent = refreshIntent(context, EVENT_REQUEST)
        val next = EventReminders.check(context, now)
        if (next == null) {
            context.getSystemService(AlarmManager::class.java).cancel(intent)
        } else {
            setAlarm(context, AlarmManager.RTC_WAKEUP, next, intent)
        }
    }

    // A Timeline's list shows minutes as text, so it is redrawn as each minute turns. That has its
    // own alarm, which redraws only the Timelines: no notifications, other widgets or downloads.
    private fun scheduleTimelines(context: Context, widgetIds: IntArray, now: Long) {
        val intent = broadcast(context, MINUTE_REQUEST, Intent(context, VillageWidget::class.java).setAction(VillageWidget.ACTION_TIMELINE_MINUTE))
        if (widgetIds.none { Store.widgetStyle(context, it) == WidgetStyle.TIMELINE }) {
            context.getSystemService(AlarmManager::class.java).cancel(intent)
            return
        }
        scheduleAt(context, Countdown.nextMinute(now), intent)
    }

    fun redrawTimelines(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, VillageWidget::class.java))
        val now = System.currentTimeMillis()
        for (widgetId in widgetIds.filter { Store.widgetStyle(context, it) == WidgetStyle.TIMELINE }) {
            val tag = Store.widgetAccount(context, widgetId)
            manager.updateAppWidget(widgetId, bigViews(context, widgetId, tag, tag?.let { Store.village(context, it) }, now))
        }
        scheduleTimelines(context, widgetIds, now)
    }

    // Only the redraw alarms, and only once no kind of widget is left. The finish alarm keeps
    // notifications coming without a widget.
    fun cancel(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val left = listOf(VillageWidget::class.java, NextWidget::class.java, EventsWidget::class.java).sumOf { manager.getAppWidgetIds(ComponentName(context, it)).size }
        if (left > 0) return
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(refreshIntent(context, REFRESH_REQUEST))
        alarms.cancel(broadcast(context, MINUTE_REQUEST, Intent(context, VillageWidget::class.java).setAction(VillageWidget.ACTION_TIMELINE_MINUTE)))
    }

    // The small widget's sizes. A strip is today's one line. Up next (4x2) adds the three after the
    // next and what is idle. Glance (2x2) is one account's next finish, so a row of them covers
    // several villages.
    enum class NextSize { STRIP, UP_NEXT, GLANCE }

    // Every size at once, and the launcher picks the one that fits.
    internal fun nextViews(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long): RemoteViews = RemoteViews(mapOf(
        SizeF(200f, SHORT_MIN_DP) to buildNext(context, widgetId, tag, village, now, NextSize.STRIP),
        SizeF(110f, NEXT_TALL_MIN_DP) to buildNext(context, widgetId, tag, village, now, NextSize.GLANCE),
        SizeF(250f, NEXT_TALL_MIN_DP) to buildNext(context, widgetId, tag, village, now, NextSize.UP_NEXT),
    ))

    // The small widget: the next upgrade to finish, and what's idle. Every view is set on every
    // update, as the launcher applies it on top of the last one.
    internal fun buildNext(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long, size: NextSize = NextSize.STRIP): RemoteViews {
        if (size != NextSize.STRIP) return buildNextCard(context, widgetId, tag, village, now, size)
        val ink = Ink(context, Store.widgetTheme(context, widgetId))
        val views = RemoteViews(context.packageName, R.layout.widget_next)
        views.setInt(R.id.next_root, "setBackgroundResource", ink.theme.panel)
        views.setInt(R.id.account, "setBackgroundResource", ink.theme.chip)
        views.setInk(R.id.account_name, ink.accent)
        views.setInk(R.id.account_arrow, ink.accent)
        views.setInk(R.id.name, ink.text)
        views.setInk(R.id.days, ink.text)
        views.setInk(R.id.timer, ink.text)
        views.setOnClickPendingIntent(R.id.next_root, openAppIntent(context, widgetId, tag))
        views.setViewVisibility(R.id.days, View.GONE)
        views.setViewVisibility(R.id.timer, View.GONE)
        views.setViewVisibility(R.id.goblin, View.GONE)
        showAccountChip(context, views, widgetId, tag)
        if (tag == null || village == null) {
            views.setViewVisibility(R.id.icon, View.GONE)
            views.setTextViewText(R.id.name, context.getString(R.string.app_name))
            views.setTextViewText(R.id.status, context.getString(R.string.next_no_village))
            views.setInk(R.id.status, ink.dim)
            return views
        }

        val summary = NextFinish.of(village, now)
        val next = summary.next
        if (next == null) {
            views.setViewVisibility(R.id.icon, View.GONE)
            views.setTextViewText(R.id.name, context.getString(R.string.next_nothing))
        } else {
            views.setViewVisibility(R.id.icon, View.VISIBLE)
            showNext(context, views, village, next, now)
        }

        // Anything idle comes first, in orange like the big widget's idle builders.
        views.setTextViewText(R.id.status, UpNextLabels.idleLine(context, summary))
        views.setInk(R.id.status, if (summary.anyIdle) ink.idle else ink.accent)
        return views
    }

    // The next upgrade's picture, name, Goblin and countdown, on any of the small widget's sizes.
    private fun showNext(context: Context, views: RemoteViews, village: Village, next: Upgrade, now: Long) {
        val icon = Icons.load(context, next)
        if (icon != null) views.setImageViewBitmap(R.id.icon, icon) else views.setImageViewResource(R.id.icon, R.drawable.ic_placeholder)
        views.setTextViewText(R.id.name, upgradeName(next))
        // Without the tag, a Goblin upgrade above "Lab idle" reads like a contradiction.
        views.setViewVisibility(R.id.goblin, if (next.extra) View.VISIBLE else View.GONE)
        // A refresh is scheduled for when the chronometer reaches zero (Countdown.nextRefresh).
        val split = Countdown.split(village.endMillis(next) - now)
        views.setTextViewText(R.id.days, "${split.days}d")
        views.setViewVisibility(R.id.days, if (split.days > 0) View.VISIBLE else View.GONE)
        views.setChronometer(R.id.timer, SystemClock.elapsedRealtime() + split.restMs, Countdown.chronometerFormat(split), true)
        views.setChronometerCountDown(R.id.timer, true)
        views.setViewVisibility(R.id.timer, View.VISIBLE)
    }

    // Up next and Glance: a header like the big widget's, with its own open button, so a tap on
    // the account chip can't open the app by accident.
    private fun buildNextCard(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long, size: NextSize): RemoteViews {
        val ink = Ink(context, Store.widgetTheme(context, widgetId))
        val views = RemoteViews(context.packageName, if (size == NextSize.GLANCE) R.layout.widget_next_glance else R.layout.widget_next_upnext)
        views.setInt(R.id.widget_root, "setBackgroundResource", ink.theme.panel)
        views.setInt(R.id.open, "setBackgroundResource", ink.theme.chip)
        views.setTint(R.id.open, "setImageTintList", ink.text)
        views.setOnClickPendingIntent(R.id.open, openAppIntent(context, widgetId, tag))
        for (id in listOf(R.id.title, R.id.town_hall, R.id.name, R.id.days, R.id.timer)) views.setInk(id, ink.text)
        views.setInk(R.id.empty, ink.dim)
        views.setInk(R.id.next_label, ink.dim)
        for (id in listOf(R.id.chip_1, R.id.chip_2, R.id.chip_3)) views.setInt(id, "setBackgroundResource", ink.theme.chip)
        views.removeAllViews(R.id.next_more)
        views.removeAllViews(R.id.boosts)
        views.setViewVisibility(R.id.boosts, View.GONE)
        views.setViewVisibility(R.id.goblin, View.GONE)
        if (tag == null || village == null) {
            views.setTextViewText(R.id.title, context.getString(R.string.app_name))
            showTitleSwitch(context, views, widgetId, tag, ink)
            views.setViewVisibility(R.id.town_hall, View.GONE)
            views.setViewVisibility(R.id.empty, View.VISIBLE)
            views.setViewVisibility(R.id.next_body, View.GONE)
            views.setViewVisibility(R.id.chips, View.GONE)
            views.setViewVisibility(R.id.chip_1, View.GONE)
            return views
        }
        views.setViewVisibility(R.id.empty, View.GONE)
        views.setViewVisibility(R.id.next_body, View.VISIBLE)
        views.setTextViewText(R.id.title, Store.label(context, tag))
        showTitleSwitch(context, views, widgetId, tag, ink)
        showTownHall(context, views, tag, village)
        showChips(context, views, village, now, ink)
        showBoosts(context, views, village, WidgetView.ALL, now, ink)

        val running = village.upgrades.filter { village.endMillis(it) > now }.sortedBy { village.endMillis(it) }
        val next = running.firstOrNull()
        if (next == null) {
            views.setViewVisibility(R.id.icon, View.GONE)
            views.setViewVisibility(R.id.days, View.GONE)
            views.setViewVisibility(R.id.timer, View.GONE)
            views.setTextViewText(R.id.next_label, "")
            views.setTextViewText(R.id.name, context.getString(R.string.next_nothing))
            return views
        }
        views.setViewVisibility(R.id.icon, View.VISIBLE)
        showNext(context, views, village, next, now)
        views.setTextViewText(R.id.next_label, context.getString(R.string.widget_next_at, FinishTime.of(context, village.endMillis(next), now)))
        if (size == NextSize.UP_NEXT) {
            for (upgrade in running.drop(1).take(3)) {
                val more = RemoteViews(context.packageName, R.layout.widget_next_more)
                val icon = Icons.load(context, upgrade)?.let { Bitmap.createScaledBitmap(it, LIST_ICON_PX, LIST_ICON_PX, true) }
                if (icon != null) more.setImageViewBitmap(R.id.more_icon, icon) else more.setImageViewResource(R.id.more_icon, R.drawable.ic_placeholder)
                more.setTextViewText(R.id.more_name, upgrade.name)
                more.setInk(R.id.more_name, ink.text)
                more.setTextViewText(R.id.more_at, FinishTime.of(context, village.endMillis(upgrade), now))
                more.setInk(R.id.more_at, ink.dim)
                views.addView(R.id.next_more, more)
            }
        }
        return views
    }

    // The events this Events widget shows, soonest first. An event with no date yet is left out.
    internal fun shownEvents(context: Context, widgetId: Int, now: Long, dates: EventDates.Dates): List<EventSchedule.Countdown> {
        val hidden = Store.hiddenEvents(context, widgetId)
        return EventSchedule.countdowns(now, dates).filter { it.kind !in hidden }
    }

    // The Events widget: one row per event the player chose, each with a live countdown. Edit picks
    // the events, and Reset shows every one again. [short] leaves out the header and Reset, so a
    // widget too short for them still shows a countdown. Edit is then in the launcher's settings.
    internal fun buildEvents(
        context: Context,
        widgetId: Int,
        now: Long,
        dates: EventDates.Dates,
        narrow: Boolean = false,
        short: Boolean = false,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_events)
        // The same four themes as the other widgets, picked in the launcher's widget settings.
        val ink = Ink(context, Store.widgetTheme(context, widgetId))
        views.setInt(R.id.events_root, "setBackgroundResource", ink.theme.panel)
        views.setInk(R.id.events_title, ink.accent)
        for (id in listOf(R.id.events_edit, R.id.events_reset)) {
            views.setInt(id, "setBackgroundResource", ink.theme.chip)
            views.setInk(id, ink.accent)
        }
        views.setInk(R.id.events_empty, ink.dim)
        // Set both ways: a resize lands on top of the views already on screen, so a tall widget that
        // was short would otherwise keep the header hidden.
        views.setViewVisibility(R.id.events_header, if (short) View.GONE else View.VISIBLE)
        val padding = dp(context, if (short) 8 else 12)
        views.setViewPadding(R.id.events_root, padding, padding, padding, padding)
        views.setViewLayoutMargin(R.id.event_rows, RemoteViews.MARGIN_TOP, if (short) 0f else 8f, TypedValue.COMPLEX_UNIT_DIP)
        views.setOnClickPendingIntent(R.id.events_root, openAppIntent(context, widgetId, null))
        views.setOnClickPendingIntent(R.id.events_edit, PendingIntent.getActivity(
            context,
            4,
            Intent(context, EventsWidgetConfigActivity::class.java)
                .setData(Uri.parse("cocwidget://events/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ))
        views.removeAllViews(R.id.event_rows)

        // Reset counts only what it would bring back on screen, so an event with no date yet doesn't count.
        val hidden = Store.hiddenEvents(context, widgetId)
        val missing = EventSchedule.countdowns(now, dates).count { it.kind in hidden }
        val countdowns = shownEvents(context, widgetId, now, dates)
        if (missing == 0 || (short && countdowns.isNotEmpty())) {
            views.setViewVisibility(R.id.events_reset, View.GONE)
        } else {
            views.setViewVisibility(R.id.events_reset, View.VISIBLE)
            views.setTextViewText(R.id.events_reset, context.resources.getQuantityString(R.plurals.events_widget_reset_hidden, missing, missing))
            views.setOnClickPendingIntent(R.id.events_reset, broadcast(context, 500, Intent(context, VillageWidget::class.java)
                .setAction(VillageWidget.ACTION_SHOW_ALL_EVENTS)
                .setData(Uri.parse("cocwidget://show-all-events/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)))
        }

        if (countdowns.isEmpty()) {
            views.setViewVisibility(R.id.events_empty, View.VISIBLE)
            views.setTextViewText(R.id.events_empty, context.getString(if (missing == 0) R.string.events_widget_no_dates else R.string.events_widget_all_hidden))
            return views
        }
        views.setViewVisibility(R.id.events_empty, View.GONE)
        for (countdown in countdowns) views.addView(R.id.event_rows, eventRow(context, countdown, now, narrow, ink))
        return views
    }

    // Chronometer can't show days, so the days go into its format, and a redraw is due when it
    // reaches zero or drops under an hour (Countdown.nextEventRefresh).
    private fun eventRow(context: Context, countdown: EventSchedule.Countdown, now: Long, narrow: Boolean, ink: Ink): RemoteViews {
        val row = RemoteViews(context.packageName, if (narrow) R.layout.widget_event_row_narrow else R.layout.widget_event_row)
        row.setTextViewText(R.id.event_name, context.getString(countdown.kind.shortLabel))
        val split = Countdown.split(countdown.target - now)
        val days = if (split.days > 0) "${split.days}d " else ""
        val format = context.getString(countdown.label, days + Countdown.hourPrefix(split) + "%s")
        row.setChronometer(R.id.event_timer, SystemClock.elapsedRealtime() + split.restMs, format, true)
        row.setChronometerCountDown(R.id.event_timer, true)
        row.setInt(R.id.event_row_bg, "setBackgroundResource", ink.theme.chip)
        row.setInk(R.id.event_name, ink.text)
        row.setInk(R.id.event_timer, if (countdown.running) ink.ok else ink.dim)
        return row
    }

    // A big widget as the launcher gets it. Pages hands over one layout per height, as how many rows
    // fit a page depends on it, and the launcher picks the one that fits. The others are one layout.
    internal fun bigViews(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long): RemoteViews {
        if (village == null || Store.widgetStyle(context, widgetId) != WidgetStyle.PAGES) return build(context, widgetId, tag, village, now)
        return RemoteViews(pageSizes(village, now).associate { (height, rows) -> SizeF(NARROW_MIN_DP, height) to build(context, widgetId, tag, village, now, rows) })
    }

    // The height each number of rows needs, for this village's header and footer. The launcher
    // picks the tallest that fits, so a page fills the widget without a row cut off.
    internal fun pageSizes(village: Village, now: Long): List<Pair<Float, Int>> {
        var chrome = PAGE_CHROME_DP
        if (Section.BUILDER_BASE in village.sections()) chrome += PAGE_VIEWS_DP
        if (village.helpers.apprenticeLevel > 0 || village.helpers.assistantLevel > 0) chrome += PAGE_HELPERS_DP
        val boosted = village.sections().any { section ->
            listOf(village.boostFor(section), village.snackFor(section)).any { it.seconds > 0 && village.boostEndMillis(it) > now }
        }
        if (boosted) chrome += PAGE_BOOSTS_DP
        return (1..PAGE_MAX_ROWS).map { rows -> chrome + rows * PAGE_ROW_DP to rows }
    }

    // A colour as it is by day and in dark mode. Material You's differ, and the launcher switches
    // the panel behind the text as soon as the phone does, so the text has to switch with it rather
    // than wait for the next redraw. [now] is the one in use, for the few places that can't switch.
    internal class Tone(val day: Int, val night: Int, val now: Int) {
        val dayList: ColorStateList get() = ColorStateList.valueOf(day)
        val nightList: ColorStateList get() = ColorStateList.valueOf(night)
    }

    // The colours one widget is drawn in, read once per draw.
    internal class Ink(context: Context, val theme: WidgetTheme) {
        private val dayContext = withNight(context, false)
        private val nightContext = withNight(context, true)
        private val nowContext = context
        private fun tone(colour: Int) = Tone(dayContext.getColor(colour), nightContext.getColor(colour), nowContext.getColor(colour))

        val text = tone(theme.text)
        val dim = tone(theme.dim)
        val accent = tone(theme.accent)
        val idle = tone(theme.idle)
        val ok = tone(theme.ok)
        val helper = tone(theme.helper)
        val track = tone(theme.track)
        val bar = tone(theme.bar)

        private fun withNight(context: Context, night: Boolean): Context {
            val config = Configuration(context.resources.configuration)
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            return context.createConfigurationContext(config)
        }
    }

    private fun RemoteViews.setInk(id: Int, tone: Tone) = setColorInt(id, "setTextColor", tone.day, tone.night)

    private fun RemoteViews.setTint(id: Int, method: String, tone: Tone) = setColorStateList(id, method, tone.dayList, tone.nightList)

    // A coloured part of one text can't switch with dark mode. Where the colour differs by mode
    // (Material You) the part takes the text's own colour, which does switch, so it stays readable.
    private fun SpannableStringBuilder.appendIn(text: String, tone: Tone): SpannableStringBuilder =
        if (tone.day == tone.night) append(text, ForegroundColorSpan(tone.now), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) else append(text)

    // [perPage] is how many rows a Pages widget shows at once, for the height this is drawn for.
    internal fun build(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long, perPage: Int = DEFAULT_PER_PAGE): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget)
        val style = Store.widgetStyle(context, widgetId)
        val ink = Ink(context, Store.widgetTheme(context, widgetId))
        paint(context, views, ink)
        views.setOnClickPendingIntent(R.id.sync, activityIntent(context, SyncActivity::class.java, 0))
        // Only the open button opens the app, so a tap meant for a fold, a page or the account chip
        // can't open it by accident.
        views.setOnClickPendingIntent(R.id.open, openAppIntent(context, widgetId, tag))
        views.removeAllViews(R.id.rows)
        views.removeAllViews(R.id.boosts)
        // The launcher applies an update on top of the views already on screen, so anything not
        // set here keeps its state from the last update, even one for another account or style.
        // Lines shown only sometimes start hidden on every update.
        for (id in listOf(R.id.helpers, R.id.views, R.id.all_upgrades, R.id.clear, R.id.chips, R.id.hero, R.id.timeline_list, R.id.boosts, R.id.pager)) {
            views.setViewVisibility(id, View.GONE)
        }
        views.setViewVisibility(R.id.rows, View.VISIBLE)
        showTitleSwitch(context, views, widgetId, null, ink)

        val update = UpdateChecker.availableUpdate(context)
        views.setViewVisibility(R.id.update, if (update != null) View.VISIBLE else View.GONE)
        if (update != null) {
            views.setTextViewText(R.id.update, context.getString(R.string.update_available, update))
            val open = Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE))
            views.setOnClickPendingIntent(R.id.update, PendingIntent.getActivity(context, 3, open, PendingIntent.FLAG_IMMUTABLE))
        }

        if (tag == null || village == null) {
            views.setTextViewText(R.id.title, context.getString(R.string.app_name))
            // An account whose export can't be read can still be switched away from.
            showTitleSwitch(context, views, widgetId, tag, ink)
            views.setViewVisibility(R.id.town_hall, View.GONE)
            views.setViewVisibility(R.id.empty, View.VISIBLE)
            views.setViewVisibility(R.id.footer, View.GONE)
            return views
        }

        views.setViewVisibility(R.id.empty, View.GONE)
        views.setViewVisibility(R.id.footer, View.VISIBLE)
        // A launcher can't be trusted to say how tall the widget is, so this never claims how much
        // is cut off. It leaves the footer room for the update link when there is one.
        if (update == null) {
            views.setViewVisibility(R.id.all_upgrades, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.all_upgrades, openAppIntent(context, widgetId, tag))
        }
        views.setTextViewText(R.id.title, Store.label(context, tag))
        showTitleSwitch(context, views, widgetId, tag, ink)
        showTownHall(context, views, tag, village)
        showChips(context, views, village, now, ink)

        val shown = Shown(widgetId, tag, village, now)
        val view = currentView(context, shown)
        showViews(context, views, shown, view, ink)
        val footer = exportLine(context, village, now, view.shows)
        views.setTextViewText(R.id.footer, footer.text)
        views.setInk(R.id.footer, if (footer.stale) ink.idle else ink.dim)
        showClear(context, views, shown, view, ink)
        // The helpers only work in the home village.
        if (Section.BUILDERS in view.shows) showHelpers(context, views, village, now, ink)
        when (style) {
            WidgetStyle.SECTIONS -> showSections(context, views, shown, view, ink)
            WidgetStyle.TIMELINE -> showTimeline(context, views, shown, view, ink)
            WidgetStyle.PAGES -> showPage(context, views, shown, view, ink, perPage)
        }
        return views
    }

    // The theme's colours on everything that is always there.
    private fun paint(context: Context, views: RemoteViews, ink: Ink) {
        val theme = ink.theme
        views.setInt(R.id.widget_root, "setBackgroundResource", theme.panel)
        views.setInt(R.id.open, "setBackgroundResource", theme.chip)
        views.setTint(R.id.open, "setImageTintList", ink.text)
        views.setInk(R.id.title, ink.text)
        views.setInk(R.id.town_hall, ink.text)
        views.setInk(R.id.empty, ink.dim)
        views.setInk(R.id.update, ink.dim)
        views.setInk(R.id.all_upgrades, ink.accent)
        views.setInk(R.id.clear, ink.accent)
        views.setInt(R.id.clear, "setBackgroundResource", theme.chip)
        views.setInk(R.id.helpers_timer, ink.helper)
        for (id in listOf(R.id.chip_1, R.id.chip_2, R.id.chip_3)) views.setInt(id, "setBackgroundResource", theme.chip)
        views.setInt(R.id.hero, "setBackgroundResource", theme.card)
        views.setInk(R.id.hero_label, ink.dim)
        views.setInk(R.id.hero_name, ink.text)
        views.setInk(R.id.hero_days, ink.text)
        views.setTint(R.id.hero_bar, "setProgressTintList", ink.bar)
        views.setTint(R.id.hero_bar, "setProgressBackgroundTintList", ink.track)
        views.setInk(R.id.page_label, ink.text)
    }

    // The Town Hall level in a ring coloured for the account, so widgets for different accounts
    // can be told apart at a glance.
    private fun showTownHall(context: Context, views: RemoteViews, tag: String, village: Village) {
        val hasLevel = village.townHall > 0
        views.setViewVisibility(R.id.town_hall, if (hasLevel) View.VISIBLE else View.GONE)
        if (!hasLevel) return
        views.setTextViewText(R.id.town_hall, village.townHall.toString())
        views.setContentDescription(R.id.town_hall, context.getString(R.string.widget_town_hall, village.townHall))
        views.setInt(R.id.town_hall, "setBackgroundResource", WidgetTheme.ring(Store.accounts(context).indexOf(tag)))
    }

    // What is idle, in the idle colour, or that everything is busy.
    private fun showChips(context: Context, views: RemoteViews, village: Village, now: Long, ink: Ink) {
        val summary = NextFinish.of(village, now)
        val idle = UpNextLabels.idleParts(context, summary)
        val chips = idle.ifEmpty { listOf(context.getString(R.string.widget_all_busy)) }
        views.setViewVisibility(R.id.chips, View.VISIBLE)
        for ((index, id) in listOf(R.id.chip_1, R.id.chip_2, R.id.chip_3).withIndex()) {
            val text = chips.getOrNull(index)
            views.setViewVisibility(id, if (text != null) View.VISIBLE else View.GONE)
            if (text == null) continue
            views.setTextViewText(id, text)
            views.setInk(id, if (idle.isEmpty()) ink.ok else ink.idle)
        }
    }

    private fun showSections(context: Context, views: RemoteViews, shown: Shown, view: WidgetView, ink: Ink) {
        // Rows stay in a plain LinearLayout rather than a scrolling ListView: on One UI the
        // countdowns stop ticking inside a ListView. Sections fold to make room, and what still
        // doesn't fit is cut off at the bottom. The app lists everything.
        for (section in shown.village.sections().filter { it in view.shows }) {
            val title = when (section) {
                Section.BUILDERS -> "BUILDERS"
                Section.LAB -> "LAB"
                Section.PETS -> "PETS"
                Section.BUILDER_BASE -> "BUILDER BASE"
            }
            addSection(context, views, shown, section, title, ink)
        }
    }

    // Every upgrade in finish order, a page at a time. Rows tick, as they sit in a plain layout.
    private fun showPage(context: Context, views: RemoteViews, shown: Shown, view: WidgetView, ink: Ink, perPage: Int) {
        val village = shown.village
        val upgrades = village.upgrades.filter { it.section in view.shows }.sortedBy { village.endMillis(it) }
        val pages = Paging.count(upgrades.size, perPage)
        val page = Paging.clamp(Store.widgetPage(context, shown.widgetId), pages)
        for (upgrade in Paging.slice(upgrades, page, perPage)) views.addView(R.id.rows, row(context, village, upgrade, shown.now, ink))
        showBoosts(context, views, village, view, shown.now, ink)
        if (pages <= 1) return
        views.setViewVisibility(R.id.pager, View.VISIBLE)
        views.setTextViewText(R.id.page_label, context.getString(R.string.widget_page, page + 1, pages))
        showArrow(context, views, shown, R.id.page_up, page - 1, page > 0, ink)
        showArrow(context, views, shown, R.id.page_down, page + 1, page < pages - 1, ink)
    }

    // An arrow that can't go further is dimmed and does nothing.
    private fun showArrow(context: Context, views: RemoteViews, shown: Shown, id: Int, target: Int, enabled: Boolean, ink: Ink) {
        views.setInt(id, "setBackgroundResource", ink.theme.chip)
        views.setTint(id, "setImageTintList", if (enabled) ink.text else ink.dim)
        views.setBoolean(id, "setEnabled", enabled)
        views.setOnClickPendingIntent(id, broadcast(context, 600, Intent(context, VillageWidget::class.java)
            .setAction(VillageWidget.ACTION_SET_PAGE)
            .setData(Uri.parse("cocwidget://page/${shown.widgetId}/$target"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, shown.widgetId)
            .putExtra(VillageWidget.EXTRA_PAGE, target)))
    }

    // Timeline: the next to finish on a card that ticks, then everything else in a list that
    // scrolls. The list holds no countdowns, as they stop inside a scrolling list on One UI. Its
    // times are text, and the widget is redrawn each minute while a Timeline is on the home screen.
    private fun showTimeline(context: Context, views: RemoteViews, shown: Shown, view: WidgetView, ink: Ink) {
        val village = shown.village
        val now = shown.now
        views.setViewVisibility(R.id.rows, View.GONE)
        val items = timelineItems(village, view, now)
        val next = items.filterIsInstance<UpNext.Item.Running>().firstOrNull { it.atMillis > now }
        if (next != null) showHero(context, views, village, next.upgrade, now, ink)
        views.setViewVisibility(R.id.timeline_list, View.VISIBLE)
        val list = RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(2).setHasStableIds(false)
        timelineRows(context, village, items.filter { it !== next }, now, ink).forEachIndexed { index, row -> list.addItem(index.toLong(), row) }
        views.setRemoteAdapter(R.id.timeline_list, list.build())
        showBoosts(context, views, village, view, now, ink)
    }

    // What the Timeline lists, in the app's Up next order, cut down to the widget's view. Potions
    // and snacks show as chips under the list instead.
    internal fun timelineItems(village: Village, view: WidgetView, now: Long): List<UpNext.Item> =
        UpNext.items(village, emptyList(), now, null).filter { item ->
            when (item) {
                is UpNext.Item.Idle -> item.section in view.shows
                is UpNext.Item.Running -> item.upgrade.section in view.shows
                is UpNext.Item.HelpersReady -> Section.BUILDERS in view.shows
                else -> false
            }
        }

    private fun showHero(context: Context, views: RemoteViews, village: Village, upgrade: Upgrade, now: Long, ink: Ink) {
        views.setViewVisibility(R.id.hero, View.VISIBLE)
        val end = village.endMillis(upgrade)
        val icon = Icons.load(context, upgrade)
        if (icon != null) views.setImageViewBitmap(R.id.hero_icon, icon) else views.setImageViewResource(R.id.hero_icon, R.drawable.ic_placeholder)
        views.setTextViewText(R.id.hero_label, context.getString(R.string.widget_next_at, FinishTime.of(context, end, now)))
        views.setTextViewText(R.id.hero_name, upgradeName(upgrade))
        views.setProgressBar(R.id.hero_bar, 1000, (village.progress(upgrade, now) * 1000).toInt(), false)
        // A refresh is scheduled for when the chronometer reaches zero (Countdown.nextRefresh).
        val split = Countdown.split(end - now)
        views.setTextViewText(R.id.hero_days, "${split.days}d")
        views.setViewVisibility(R.id.hero_days, if (split.days > 0) View.VISIBLE else View.GONE)
        views.setChronometer(R.id.hero_timer, SystemClock.elapsedRealtime() + split.restMs, Countdown.chronometerFormat(split), true)
        views.setChronometerCountDown(R.id.hero_timer, true)
        views.setInk(R.id.hero_timer, ink.text)
    }

    // The list's rows, with a heading at each change of group. No row holds a countdown.
    internal fun timelineRows(context: Context, village: Village, items: List<UpNext.Item>, now: Long, ink: Ink): List<RemoteViews> {
        val zone = java.time.ZoneId.systemDefault()
        val rows = mutableListOf<RemoteViews>()
        var group: UpNext.Group? = null
        for (item in items.take(TIMELINE_MAX_ROWS)) {
            val itemGroup = UpNext.groupOf(item.atMillis, now, zone)
            if (itemGroup != group) {
                group = itemGroup
                rows += RemoteViews(context.packageName, R.layout.widget_timeline_label).apply {
                    setTextViewText(R.id.tl_label, context.getString(UpNextLabels.group(itemGroup)))
                    setInk(R.id.tl_label, if (itemGroup == UpNext.Group.NOW) ink.idle else ink.dim)
                }
            }
            rows += timelineRow(context, village, item, now, ink)
        }
        return rows
    }

    private fun timelineRow(context: Context, village: Village, item: UpNext.Item, now: Long, ink: Ink): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_timeline_row)
        row.setInk(R.id.tl_name, ink.text)
        row.setInk(R.id.tl_kind, ink.dim)
        row.setInk(R.id.tl_at, ink.dim)
        row.setViewVisibility(R.id.tl_badge, View.GONE)
        row.setViewVisibility(R.id.tl_icon, View.VISIBLE)
        val waiting = item.atMillis <= now
        row.setTextViewText(R.id.tl_at, if (waiting) "" else FinishTime.of(context, item.atMillis, now))
        row.setTextViewText(R.id.tl_left, if (waiting) "" else Countdown.short(item.atMillis - now))
        row.setInk(R.id.tl_left, if (!waiting && item.atMillis - now < Countdown.HOUR_MS) ink.ok else ink.text)
        when (item) {
            is UpNext.Item.Idle -> {
                row.setViewVisibility(R.id.tl_icon, View.INVISIBLE)
                row.setTextViewText(R.id.tl_name, UpNextLabels.idleTitle(context, item.section, item.count))
                row.setInk(R.id.tl_name, ink.idle)
                row.setTextViewText(R.id.tl_kind, context.getString(UpNextLabels.startHint(item.section)))
                row.setTextViewText(R.id.tl_left, context.getString(R.string.upnext_free))
                row.setInk(R.id.tl_left, ink.idle)
            }
            is UpNext.Item.HelpersReady -> {
                row.setImageViewResource(R.id.tl_icon, R.drawable.ic_face_apprentice)
                row.setTextViewText(R.id.tl_name, context.getString(R.string.helpers_ready))
                row.setInk(R.id.tl_name, if (waiting) ink.idle else ink.helper)
                row.setTextViewText(R.id.tl_kind, helperLevels(context, village))
                if (waiting) {
                    row.setTextViewText(R.id.tl_left, context.getString(R.string.upnext_free))
                    row.setInk(R.id.tl_left, ink.idle)
                }
            }
            is UpNext.Item.Running -> {
                val upgrade = item.upgrade
                val icon = Icons.load(context, upgrade)?.let { Bitmap.createScaledBitmap(it, LIST_ICON_PX, LIST_ICON_PX, true) }
                if (icon != null) row.setImageViewBitmap(R.id.tl_icon, icon) else row.setImageViewResource(R.id.tl_icon, R.drawable.ic_placeholder)
                row.setTextViewText(R.id.tl_name, upgradeName(upgrade))
                val kind = context.getString(UpNextLabels.kind(upgrade.section))
                row.setTextViewText(R.id.tl_kind, if (upgrade.extra) context.getString(R.string.upnext_kind_goblin, kind) else kind)
                when {
                    upgrade.extra -> badge(row, R.drawable.ic_face_goblin, R.drawable.chip_goblin)
                    !waiting && village.isHelped(upgrade, now) -> badge(row, helperFace(upgrade.section), R.drawable.chip_helper)
                }
                if (waiting) {
                    row.setTextViewText(R.id.tl_left, context.getString(R.string.done))
                    row.setInk(R.id.tl_left, ink.accent)
                }
            }
            else -> Unit
        }
        return row
    }

    private fun badge(row: RemoteViews, face: Int, chip: Int) {
        row.setViewVisibility(R.id.tl_badge, View.VISIBLE)
        row.setImageViewResource(R.id.tl_badge, face)
        row.setInt(R.id.tl_badge, "setBackgroundResource", chip)
    }

    private fun helperLevels(context: Context, village: Village): String = buildList {
        if (village.helpers.apprenticeLevel > 0) add(context.getString(R.string.upnext_apprentice, village.helpers.apprenticeLevel))
        if (village.helpers.assistantLevel > 0) add(context.getString(R.string.upnext_assistant, village.helpers.assistantLevel))
    }.joinToString(" · ")

    // Running potions and snacks as chips, for the styles with no section headers to carry them.
    // Study Soup works in the lab and the Pet House alike, so it shows once.
    private fun showBoosts(context: Context, views: RemoteViews, village: Village, view: WidgetView, now: Long, ink: Ink) {
        var soup = false
        for (section in village.sections().filter { it in view.shows }) {
            addBoost(context, views, village, village.boostFor(section), potionIcon(section), now, ink)
            val snackIcon = snackIcon(section)
            if (snackIcon == R.drawable.ic_boost_study_soup && soup) continue
            if (addBoost(context, views, village, village.snackFor(section), snackIcon, now, ink) && snackIcon == R.drawable.ic_boost_study_soup) soup = true
        }
    }

    private fun addBoost(context: Context, views: RemoteViews, village: Village, boost: Boost, icon: Int, now: Long, ink: Ink): Boolean {
        val left = village.boostEndMillis(boost) - now
        if (boost.seconds <= 0 || left <= 0) return false
        val chip = RemoteViews(context.packageName, R.layout.widget_boost)
        chip.setInt(R.id.boost_chip, "setBackgroundResource", if (ink.theme == WidgetTheme.CLASSIC) R.drawable.chip_boost else ink.theme.chipOn)
        chip.setImageViewResource(R.id.boost_icon, icon)
        chip.setChronometer(R.id.boost_timer, SystemClock.elapsedRealtime() + left, "×${boost.multiplier}  %s", true)
        chip.setChronometerCountDown(R.id.boost_timer, true)
        chip.setInk(R.id.boost_timer, ink.text)
        views.addView(R.id.boosts, chip)
        views.setViewVisibility(R.id.boosts, View.VISIBLE)
        return true
    }

    // With two or more accounts the big widget's title is a chip, and a tap moves the widget to
    // another account (switchIntent). With one, it's plain text that opens the app.
    private fun showTitleSwitch(context: Context, views: RemoteViews, widgetId: Int, tag: String?, ink: Ink) {
        val switchable = tag != null && Store.accounts(context).size > 1
        if (!switchable) {
            views.setInt(R.id.title, "setBackgroundResource", 0)
            views.setViewPadding(R.id.title, 0, 0, 0, 0)
            views.setTextViewCompoundDrawablesRelative(R.id.title, 0, 0, 0, 0)
            views.setContentDescription(R.id.title, null)
            views.setOnClickPendingIntent(R.id.title, openAppIntent(context, widgetId, tag))
            return
        }
        val label = Store.label(context, tag!!)
        views.setInt(R.id.title, "setBackgroundResource", ink.theme.chip)
        views.setViewPadding(R.id.title, dp(context, 10), 0, dp(context, 8), 0)
        views.setTextViewText(R.id.title, label)
        views.setTextViewCompoundDrawablesRelative(R.id.title, 0, 0, ink.theme.switchIcon, 0)
        views.setContentDescription(R.id.title, switchDescription(context, label))
        views.setOnClickPendingIntent(R.id.title, switchIntent(context, widgetId))
    }

    // The small widget has no title, so with two or more accounts it gets a chip beside the status line.
    private fun showAccountChip(context: Context, views: RemoteViews, widgetId: Int, tag: String?) {
        val switchable = tag != null && Store.accounts(context).size > 1
        views.setViewVisibility(R.id.account, if (switchable) View.VISIBLE else View.GONE)
        if (!switchable) return
        val label = Store.label(context, tag!!)
        views.setTextViewText(R.id.account_name, label)
        views.setContentDescription(R.id.account, switchDescription(context, label))
        views.setOnClickPendingIntent(R.id.account, switchIntent(context, widgetId))
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    // Without the Builder Base, Home and All would be the same, so there's no choice and it's All.
    // Otherwise the view picked, or before one is picked, the one the old folds match.
    private fun currentView(context: Context, shown: Shown): WidgetView {
        val sections = shown.village.sections().toSet()
        if (Section.BUILDER_BASE !in sections) return WidgetView.ALL
        return Store.widgetView(context, shown.tag)
            ?: WidgetView.matching(Store.collapsedSections(context, shown.tag), sections)
            ?: WidgetView.ALL
    }

    private fun showViews(context: Context, views: RemoteViews, shown: Shown, current: WidgetView, ink: Ink) {
        if (Section.BUILDER_BASE !in shown.village.sections()) return
        views.setViewVisibility(R.id.views, View.VISIBLE)
        val buttons = mapOf(WidgetView.HOME to R.id.view_home, WidgetView.BUILDER_BASE to R.id.view_builder_base, WidgetView.ALL to R.id.view_all)
        for ((view, id) in buttons) {
            val on = view == current
            views.setInt(id, "setBackgroundResource", if (on) ink.theme.chipOn else ink.theme.chip)
            views.setInk(id, if (on) ink.accent else ink.dim)
            views.setOnClickPendingIntent(id, viewIntent(context, shown, view))
        }
    }

    // Clears what has finished in this view only, the same upgrades the footer counts.
    private fun showClear(context: Context, views: RemoteViews, shown: Shown, view: WidgetView, ink: Ink) {
        val village = shown.village
        val finished = village.upgrades.count { it.section in view.shows && village.endMillis(it) <= shown.now }
        if (finished == 0) return
        views.setViewVisibility(R.id.clear, View.VISIBLE)
        views.setTextViewText(R.id.clear, context.resources.getQuantityString(R.plurals.widget_clear_finished, finished, finished))
        views.setInk(R.id.clear, ink.accent)
        // Clears up to the time this count was drawn, so a tap hides no more than the button says.
        views.setOnClickPendingIntent(R.id.clear, broadcast(context, 400, accountIntent(context, shown, VillageWidget.ACTION_CLEAR_FINISHED, "clear", view.name)
            .putExtra(VillageWidget.EXTRA_VIEW, view.name)
            .putExtra(VillageWidget.EXTRA_AT, shown.now)))
    }

    // Ready is orange like an idle builder: a free shift goes to waste.
    private fun showHelpers(context: Context, views: RemoteViews, village: Village, now: Long, ink: Ink) {
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
            views.setInk(R.id.helpers_ready, ink.idle)
        }
    }

    class ExportLine(val text: String, val stale: Boolean)

    // When the export was taken, and whether it's time for a new one. A finished upgrade usually
    // means a new one has started in the game that the export can't show.
    // [sections] limits the finished count to what's on screen.
    fun exportLine(context: Context, village: Village, now: Long, sections: Set<Section> = Section.entries.toSet()): ExportLine {
        val exported = TimeText.exported(village.exportedAtMillis, now)
        val finishedSince = village.upgrades.count { it.section in sections && village.endMillis(it) <= now }
        val old = now - village.exportedAtMillis > Countdown.DAY_MS
        val text = when {
            finishedSince > 0 -> context.getString(R.string.exported_finished_since, exported, finishedSince)
            old -> context.getString(R.string.exported_out_of_date, exported)
            else -> context.getString(R.string.exported_at, exported)
        }
        return ExportLine(text, finishedSince > 0 || old)
    }

    // What one widget is drawing.
    private class Shown(val widgetId: Int, val tag: String, val village: Village, val now: Long)

    private fun addSection(
        context: Context,
        views: RemoteViews,
        shown: Shown,
        section: Section,
        title: String,
        ink: Ink,
    ) {
        val village = shown.village
        val now = shown.now
        val upgrades = village.upgrades.filter { it.section == section }.sortedBy { village.endMillis(it) }
        val running = upgrades.filter { village.endMillis(it) > now }
        val busy = village.busyIn(section, now)
        val slots = village.slotsFor(section)

        val label = SpannableStringBuilder(title)
        if (section == Section.BUILDERS) label.append(" $busy/$slots")
        if (running.any { it.extra }) {
            label.appendIn(" + Goblin", ink.ok)
        }
        val idle = village.idleIn(section, now)
        if (idle > 0) {
            val text = if (section == Section.BUILDERS) "  $idle idle" else "  idle"
            label.appendIn(text, ink.idle)
        }
        val collapsed = section.name in Store.collapsedSections(context, shown.tag)
        val marker = if (collapsed) "  ▸ ${upgrades.size}" else "  ▾"
        label.appendIn(marker, ink.dim)

        val header = RemoteViews(context.packageName, R.layout.widget_section)
        header.setTextViewText(R.id.label, label)
        header.setInk(R.id.label, ink.accent)
        header.setOnClickPendingIntent(R.id.section, toggleIntent(context, shown, section))
        showBoost(header, R.id.badge_chip, R.id.badge_icon, R.id.badge, potionIcon(section), village, village.boostFor(section), now, ink)
        showBoost(header, R.id.badge_snack_chip, R.id.badge_snack_icon, R.id.badge_snack, snackIcon(section), village, village.snackFor(section), now, ink)
        views.addView(R.id.rows, header)
        if (collapsed) return
        for (upgrade in upgrades) views.addView(R.id.rows, row(context, village, upgrade, now, ink))
    }

    // The picture on a section's chips. Multipliers alone don't say which item is running.
    internal fun potionIcon(section: Section): Int = when (section) {
        Section.BUILDERS -> R.drawable.ic_boost_builder_potion
        Section.LAB -> R.drawable.ic_boost_research_potion
        Section.PETS -> R.drawable.ic_boost_pet_potion
        Section.BUILDER_BASE -> R.drawable.ic_boost_clock_tower_potion
    }

    internal fun snackIcon(section: Section): Int =
        if (section == Section.BUILDERS) R.drawable.ic_boost_builder_bite else R.drawable.ic_boost_study_soup

    private fun showBoost(header: RemoteViews, chip: Int, iconView: Int, timer: Int, icon: Int, village: Village, boost: Boost, now: Long, ink: Ink) {
        val left = village.boostEndMillis(boost) - now
        if (boost.seconds > 0 && left > 0) {
            header.setInt(chip, "setBackgroundResource", if (ink.theme == WidgetTheme.CLASSIC) R.drawable.chip_boost else ink.theme.chipOn)
            header.setImageViewResource(iconView, icon)
            header.setChronometer(timer, SystemClock.elapsedRealtime() + left, "×${boost.multiplier}  %s", true)
            header.setChronometerCountDown(timer, true)
            header.setInk(timer, ink.text)
            header.setViewVisibility(chip, View.VISIBLE)
        } else {
            header.setViewVisibility(chip, View.GONE)
        }
    }

    // Who is working on a row. Only the builders and the lab have a helper.
    internal fun helperFace(section: Section): Int =
        if (section == Section.LAB) R.drawable.ic_face_assistant else R.drawable.ic_face_apprentice

    private fun row(context: Context, village: Village, upgrade: Upgrade, now: Long, ink: Ink): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_row)
        row.setTextViewText(R.id.name, upgradeName(upgrade))
        row.setInk(R.id.name, ink.text)
        row.setInk(R.id.days, ink.text)
        row.setInk(R.id.finish, ink.dim)
        row.setInk(R.id.done, ink.accent)
        row.setViewVisibility(R.id.goblin, if (upgrade.extra) View.VISIBLE else View.GONE)
        row.setProgressBar(R.id.bar, 1000, (village.progress(upgrade, now) * 1000).toInt(), false)
        row.setTint(R.id.bar, "setProgressTintList", ink.bar)
        row.setTint(R.id.bar, "setProgressBackgroundTintList", ink.track)

        val icon = Icons.load(context, upgrade)
        if (icon != null) row.setImageViewBitmap(R.id.icon, icon) else row.setImageViewResource(R.id.icon, R.drawable.ic_placeholder)

        val end = village.endMillis(upgrade)
        val left = end - now
        row.setImageViewResource(R.id.helper, helperFace(upgrade.section))
        row.setViewVisibility(R.id.helper, if (left > 0 && village.isHelped(upgrade, now)) View.VISIBLE else View.GONE)
        if (left <= 0) {
            row.setViewVisibility(R.id.finish, View.GONE)
            row.setViewVisibility(R.id.clock, View.GONE)
            row.setViewVisibility(R.id.days, View.GONE)
            row.setViewVisibility(R.id.timer, View.GONE)
            row.setViewVisibility(R.id.done, View.VISIBLE)
            return row
        }

        row.setViewVisibility(R.id.finish, View.VISIBLE)
        row.setTextViewText(R.id.finish, FinishTime.of(context, end, now))
        // A refresh is scheduled for when the chronometer reaches zero or drops under an hour
        // (Countdown.nextRefresh).
        val split = Countdown.split(left)
        row.setTextViewText(R.id.days, "${split.days}d")
        row.setViewVisibility(R.id.days, if (split.days > 0) View.VISIBLE else View.GONE)
        row.setChronometer(R.id.timer, SystemClock.elapsedRealtime() + split.restMs, Countdown.chronometerFormat(split), true)
        row.setChronometerCountDown(R.id.timer, true)
        row.setInk(R.id.timer, ink.text)
        row.setViewVisibility(R.id.done, View.GONE)
        return row
    }

    // "Cannon → 12", or "X-Bow ⚡2" for a supercharge (the row ellipsizes, so the stage stays short).
    internal fun upgradeName(upgrade: Upgrade): CharSequence = SpannableStringBuilder(upgrade.name)
        .append(if (upgrade.supercharge > 0) "  ⚡${upgrade.supercharge}" else "  → ${upgrade.toLevel}", RelativeSizeSpan(0.85f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // Redraws don't wake the phone. A sleeping screen doesn't need fresh numbers.
    private fun scheduleNext(context: Context, at: Long) {
        scheduleAt(context, at, refreshIntent(context, REFRESH_REQUEST))
    }

    private fun scheduleAt(context: Context, at: Long, intent: PendingIntent) = setAlarm(context, AlarmManager.RTC, at, intent)

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
            .putExtra(VillageWidget.EXTRA_SECTION, section.name)
            .putExtra(VillageWidget.EXTRA_VIEW, currentView(context, shown).name))

    private fun viewIntent(context: Context, shown: Shown, view: WidgetView): PendingIntent =
        broadcast(context, 200 + view.ordinal, accountIntent(context, shown, VillageWidget.ACTION_SET_VIEW, "view", view.name)
            .putExtra(VillageWidget.EXTRA_VIEW, view.name))

    // With two accounts a tap swaps them, through VillageWidget, which draws every widget. With more,
    // it opens the list of accounts, so any of them is one pick away. The data Uri keeps each
    // widget's id.
    private fun switchIntent(context: Context, widgetId: Int): PendingIntent {
        if (!picksFromList(context)) {
            return broadcast(context, 300, Intent(context, VillageWidget::class.java)
                .setAction(VillageWidget.ACTION_SWITCH_ACCOUNT)
                .setData(Uri.parse("cocwidget://switch/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }
        return PendingIntent.getActivity(
            context,
            5,
            Intent(context, WidgetConfigActivity::class.java)
                .setData(Uri.parse("cocwidget://pick/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun picksFromList(context: Context) = Store.accounts(context).size > SWAP_MAX_ACCOUNTS

    private fun switchDescription(context: Context, label: String): String =
        context.getString(if (picksFromList(context)) R.string.widget_pick_account else R.string.widget_switch_account, label)

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
