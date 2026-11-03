package com.cocwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import java.text.DateFormat

object WidgetRenderer {
    private const val REFRESH_REQUEST = 0
    private const val FINISH_REQUEST = 1
    private val GREEN = Color.parseColor("#8EE04A")
    private val ORANGE = Color.parseColor("#FFA640")
    private val DIM = Color.parseColor("#B8A894")

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
        if (widgetIds.isEmpty()) return

        UpdateChecker.checkIfDue(context)
        val shown = widgetIds.associateWith { Store.widgetAccount(context, it) }
        for (tag in shown.values.filterNotNull().distinct()) villages[tag]?.let { Icons.fetchMissing(context, it.upgrades) }
        val now = System.currentTimeMillis()
        for ((widgetId, tag) in shown) {
            manager.updateAppWidget(widgetId, build(context, widgetId, tag, tag?.let(villages::get), now))
        }
        scheduleNext(context, shown.values.distinct().minOf { Countdown.nextRefresh(it?.let(villages::get), now) })
    }

    // Only the redraw alarm. The finish alarm keeps notifications coming without a widget.
    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context, REFRESH_REQUEST))
    }

    private fun build(context: Context, widgetId: Int, tag: String?, village: Village?, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget)
        views.setOnClickPendingIntent(R.id.sync, activityIntent(context, SyncActivity::class.java, 0))
        views.setOnClickPendingIntent(R.id.title, openAppIntent(context, widgetId, tag))
        views.removeAllViews(R.id.rows)

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
        val exported = DateUtils.formatSameDayTime(village.exportedAtMillis, now, DateFormat.MEDIUM, DateFormat.SHORT)
        // A finished upgrade usually means a new one has started in the game that the export can't show.
        val finishedSince = village.upgrades.count { village.endMillis(it) <= now }
        val old = now - village.exportedAtMillis > Countdown.DAY_MS
        val footer = when {
            finishedSince > 0 -> context.getString(R.string.exported_finished_since, exported, finishedSince)
            old -> context.getString(R.string.exported_out_of_date, exported)
            else -> context.getString(R.string.exported_at, exported)
        }
        views.setTextViewText(R.id.footer, footer)
        views.setTextColor(R.id.footer, if (finishedSince > 0 || old) ORANGE else context.getColor(R.color.text_dim))
        showHelpers(context, views, village, now)

        // Rows stay in a plain LinearLayout rather than a scrolling ListView: on One UI the
        // countdowns stop ticking inside a ListView. Sections fold instead, to make room.
        val shown = Shown(widgetId, tag, village, now)
        addSection(context, views, shown, Section.BUILDERS, "BUILDERS", village.builders)
        addSection(context, views, shown, Section.LAB, "LAB", 1)
        if (village.hasPetHouse) addSection(context, views, shown, Section.PETS, "PETS", 1)
        if (village.upgrades.any { it.section == Section.BUILDER_BASE }) {
            addSection(context, views, shown, Section.BUILDER_BASE, "BUILDER BASE", 0)
        }
        return views
    }

    // Ready is orange like an idle builder: a free shift goes to waste.
    private fun showHelpers(context: Context, views: RemoteViews, village: Village, now: Long) {
        val helpers = village.helpers
        if (helpers.apprenticeLevel == 0 && helpers.assistantLevel == 0) return
        views.setViewVisibility(R.id.helpers, View.VISIBLE)
        val readyIn = village.helpersReadyMillis() - now
        if (readyIn > 0) {
            views.setChronometer(R.id.helpers_timer, SystemClock.elapsedRealtime() + readyIn, context.getString(R.string.helpers_ready_in), true)
            views.setChronometerCountDown(R.id.helpers_timer, true)
        } else {
            views.setViewVisibility(R.id.helpers_timer, View.GONE)
            views.setViewVisibility(R.id.helpers_ready, View.VISIBLE)
            views.setTextColor(R.id.helpers_ready, ORANGE)
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
            label.append(" + Goblin", ForegroundColorSpan(GREEN), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val idle = slots - busy
        if (idle > 0) {
            val text = if (section == Section.BUILDERS) "  $idle idle" else "  idle"
            label.append(text, ForegroundColorSpan(ORANGE), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val collapsed = section.name in Store.collapsedSections(context, shown.tag)
        val marker = if (collapsed) "  ▸ ${upgrades.size}" else "  ▾"
        label.append(marker, ForegroundColorSpan(DIM), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

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
        val name = SpannableStringBuilder(upgrade.name)
            .append("  → ${upgrade.toLevel}", RelativeSizeSpan(0.85f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        row.setTextViewText(R.id.name, name)
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

    private fun refreshIntent(context: Context, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, VillageWidget::class.java).setAction(VillageWidget.ACTION_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // The data Uri is unique per widget and section, so each header's intent keeps its own extras.
    private fun toggleIntent(context: Context, shown: Shown, section: Section): PendingIntent = PendingIntent.getBroadcast(
        context,
        100 + section.ordinal,
        Intent(context, VillageWidget::class.java)
            .setAction(VillageWidget.ACTION_TOGGLE_SECTION)
            .setData(Uri.parse("cocwidget://toggle/${shown.widgetId}/${section.name}"))
            .putExtra(VillageWidget.EXTRA_ACCOUNT, shown.tag)
            .putExtra(VillageWidget.EXTRA_SECTION, section.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

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
