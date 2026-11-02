package com.cocwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
    private const val DAY_MS = 86_400_000L
    private const val MAX_REFRESH_MS = 15 * 60_000L
    private val GREEN = Color.parseColor("#8EE04A")
    private val ORANGE = Color.parseColor("#FFA640")
    private val DIM = Color.parseColor("#B8A894")

    fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, VillageWidget::class.java)
        if (manager.getAppWidgetIds(component).isEmpty()) return

        val village = Store.village(context)
        village?.let { Icons.fetchMissing(context, it.upgrades) }
        val now = System.currentTimeMillis()
        manager.updateAppWidget(component, build(context, village, now))
        scheduleNext(context, nextRefresh(village, now))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    private fun build(context: Context, village: Village?, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget)
        views.setOnClickPendingIntent(R.id.sync, activityIntent(context, SyncActivity::class.java, 0))
        views.setOnClickPendingIntent(R.id.title, activityIntent(context, MainActivity::class.java, 1))
        views.removeAllViews(R.id.rows)

        if (village == null) {
            views.setTextViewText(R.id.title, context.getString(R.string.app_name))
            views.setTextViewText(R.id.town_hall, "")
            views.setViewVisibility(R.id.empty, View.VISIBLE)
            views.setViewVisibility(R.id.footer, View.GONE)
            return views
        }

        views.setViewVisibility(R.id.empty, View.GONE)
        views.setViewVisibility(R.id.footer, View.VISIBLE)
        views.setTextViewText(R.id.title, Store.displayName(context) ?: village.tag)
        views.setTextViewText(R.id.town_hall, if (village.townHall > 0) "TH${village.townHall}" else "")
        val exported = DateUtils.formatSameDayTime(village.exportedAtMillis, now, DateFormat.MEDIUM, DateFormat.SHORT)
        views.setTextViewText(R.id.footer, context.getString(R.string.exported_at, exported))

        // Rows stay in a plain LinearLayout rather than a scrolling ListView: on One UI the
        // countdowns stop ticking inside a ListView. Sections fold instead, to make room.
        addSection(context, views, village, now, Section.BUILDERS, "BUILDERS", village.builders)
        addSection(context, views, village, now, Section.LAB, "LAB", 1)
        if (village.hasPetHouse) addSection(context, views, village, now, Section.PETS, "PETS", 1)
        if (village.upgrades.any { it.section == Section.BUILDER_BASE }) {
            addSection(context, views, village, now, Section.BUILDER_BASE, "BUILDER BASE", 0)
        }
        return views
    }

    private fun addSection(
        context: Context,
        views: RemoteViews,
        village: Village,
        now: Long,
        section: Section,
        title: String,
        slots: Int,
    ) {
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
        val collapsed = section.name in Store.collapsedSections(context)
        val marker = if (collapsed) "  ▸ ${upgrades.size}" else "  ▾"
        label.append(marker, ForegroundColorSpan(DIM), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val header = RemoteViews(context.packageName, R.layout.widget_section)
        header.setTextViewText(R.id.label, label)
        header.setOnClickPendingIntent(R.id.section, toggleIntent(context, section))
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
        if (left <= 0) {
            row.setViewVisibility(R.id.clock, View.GONE)
            row.setViewVisibility(R.id.days, View.GONE)
            row.setViewVisibility(R.id.timer, View.GONE)
            row.setViewVisibility(R.id.done, View.VISIBLE)
            return row
        }

        // Chronometer can't show days, so whole days go in a separate label and the
        // chronometer counts down the rest. A refresh is scheduled for when it reaches zero.
        val days = (left - 1) / DAY_MS
        val rest = left - days * DAY_MS
        row.setTextViewText(R.id.days, "${days}d")
        row.setViewVisibility(R.id.days, if (days > 0) View.VISIBLE else View.GONE)
        row.setChronometer(R.id.timer, SystemClock.elapsedRealtime() + rest, null, true)
        row.setChronometerCountDown(R.id.timer, true)
        row.setViewVisibility(R.id.done, View.GONE)
        return row
    }

    private fun nextRefresh(village: Village?, now: Long): Long {
        var next = now + MAX_REFRESH_MS
        if (village != null) {
            for (upgrade in village.upgrades) {
                val left = village.endMillis(upgrade) - now
                if (left > 0) next = minOf(next, now + left - (left - 1) / DAY_MS * DAY_MS)
            }
            for (boost in listOf(village.builderBoost, village.petBoost)) {
                val end = village.boostEndMillis(boost)
                if (end > now) next = minOf(next, end)
            }
        }
        return maxOf(next, now + 1000)
    }

    private fun scheduleNext(context: Context, at: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = refreshIntent(context)
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, intent)
        }
    }

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, VillageWidget::class.java).setAction(VillageWidget.ACTION_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // One request code per section, so each header's intent keeps its own extra.
    private fun toggleIntent(context: Context, section: Section): PendingIntent = PendingIntent.getBroadcast(
        context,
        100 + section.ordinal,
        Intent(context, VillageWidget::class.java)
            .setAction(VillageWidget.ACTION_TOGGLE_SECTION)
            .putExtra(VillageWidget.EXTRA_SECTION, section.name),
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
