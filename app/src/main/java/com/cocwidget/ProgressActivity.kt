package com.cocwidget

import android.view.Gravity
import android.widget.LinearLayout
import com.cocwidget.game.GameData
import com.cocwidget.plan.PlanFormat
import com.cocwidget.plan.Planner
import com.cocwidget.plan.Progress
import com.cocwidget.plan.ProgressPoint
import com.cocwidget.store.ProgressHistory
import com.cocwidget.store.Store
import com.cocwidget.ui.ProgressChart
import com.cocwidget.ui.ProgressText
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Skin
import com.cocwidget.ui.TimeText
import java.time.ZoneId
import kotlin.concurrent.thread

// How far the village has come, from every import kept. Opened from the card on Time to max.
class ProgressActivity : Screen() {
    // Working out the imports not worked out before, off the main thread.
    internal var loading: Thread? = null
    private var points: List<ProgressPoint>? = null
    // The account and export the points are for, so a new import or account works them out again.
    private var loadedFor: String? = null
    private var picked = -1

    override fun build() {
        content.addView(backHeader(R.string.progress_title))
        val current = Store.current(this)
        // At full price, like every kept import, so they line up.
        val plan = current?.let { Planner.plan(it.json, GameData.all(this), it.village.builders) }
        if (current == null || plan == null) {
            addNothingSynced()
            return
        }
        val latest = Progress.of(plan, current.village.exportedAtMillis)
        val key = current.tag + "@" + latest.exportedAtMillis
        if (key != loadedFor) load(current.tag, key)

        val all = points
        val earlier = all?.let { list ->
            Progress.compareWith(list.map { it.exportedAtMillis }, latest.exportedAtMillis)?.let { at -> list.first { it.exportedAtMillis == at } }
        }
        content.addView(summary(latest, earlier, all != null), ui.matchWidth(16))
        content.addView(chartCard(all), ui.matchWidth(12))
        content.addView(groupsCard(plan, earlier?.takeIf { it.townHall == latest.townHall }), ui.matchWidth(12))
        addNote(getString(R.string.progress_notes))
    }

    private fun load(tag: String, key: String) {
        points = null
        loadedFor = key
        picked = -1
        val app = applicationContext
        loading = thread {
            val loaded = runCatching { ProgressHistory.all(app, tag) }.getOrDefault(emptyList())
            runOnUiThread {
                if (isDestroyed || loadedFor != key) return@runOnUiThread
                points = loaded
                show()
            }
        }
    }

    private fun summary(latest: ProgressPoint, earlier: ProgressPoint?, loaded: Boolean) = ui.card().apply {
        addView(ui.text(getString(R.string.progress_label, latest.townHall), 13f, ui.skin.dim))
        addView(ui.heading(ProgressText.percent(latest.done), 36f), ui.matchWidth(2))
        if (loaded) {
            val colour = if (ProgressText.wentUp(latest, earlier)) ui.skin.green else ui.skin.dim
            addView(ui.text(ProgressText.since(this@ProgressActivity, latest, earlier), 14f, colour, bold = colour == ui.skin.green), ui.matchWidth(4))
        }
        val max = PlanFormat.duration(latest.maxSeconds)
        val then = earlier?.takeIf { it.townHall == latest.townHall }
        val line = if (then == null) {
            getString(R.string.progress_max, max)
        } else {
            getString(R.string.progress_max_was, max, PlanFormat.duration(then.maxSeconds), TimeText.day(then.exportedAtMillis))
        }
        addView(ui.text(line, 13f, ui.skin.dim), ui.matchWidth(12))
    }

    private fun chartCard(all: List<ProgressPoint>?) = ui.card().apply {
        addView(ui.heading(getString(R.string.progress_chart_title), if (ui.skin.headingFont != null) 18f else 15f))
        if (all == null) {
            addView(ui.text(getString(R.string.progress_loading), 14f, ui.skin.dim), ui.matchWidth(8))
            return@apply
        }
        val daily = Progress.daily(all, ZoneId.systemDefault())
        if (daily.size < 2) {
            addView(ui.text(getString(R.string.progress_one_day), 14f, ui.skin.dim), ui.matchWidth(8))
            return@apply
        }
        if (picked !in daily.indices) picked = daily.lastIndex
        val readout = ui.text(pickedText(daily[picked]), 13f, ui.skin.dim)
        addView(readout, ui.matchWidth(8))
        // A finger on the chart picks a day, and the line above it names that import.
        lateinit var chart: ProgressChart
        chart = ProgressChart(this@ProgressActivity, daily, ui.skin == Skin.LIGHT) { index ->
            picked = index
            chart.selected = index
            readout.text = pickedText(daily[index])
        }
        val first = daily.first()
        val last = daily.last()
        chart.contentDescription = getString(
            R.string.progress_chart_description,
            ProgressText.percent(first.done), TimeText.day(first.exportedAtMillis),
            ProgressText.percent(last.done), TimeText.day(last.exportedAtMillis),
        )
        chart.selected = picked
        addView(chart, ui.matchWidth(8))
    }

    private fun pickedText(point: ProgressPoint) =
        getString(R.string.progress_picked, TimeText.day(point.exportedAtMillis), point.townHall, ProgressText.percent(point.done))

    // Each group as Time to max shows it, with how far it moved since [earlier].
    private fun groupsCard(plan: Planner.Plan, earlier: ProgressPoint?) = ui.card(0).apply {
        for ((index, group) in plan.groups.withIndex()) {
            if (index > 0) addView(ui.divider())
            addView(LinearLayout(this@ProgressActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12))
                addView(ui.text(getString(PlannerActivity.titleOf(group.group)), 15f, bold = true), ui.weighted())
                val then = earlier?.groups?.get(group.group)
                val change = then?.let { ProgressText.change(group.done, it) }
                if (change != null) addView(ui.text(change, 13f, if (group.done > then) ui.skin.green else ui.skin.dim), ui.wrap().apply { marginEnd = ui.dp(10) })
                addView(ui.text(ProgressText.percent(group.done), 14f, if (group.done >= 1f) ui.skin.green else ui.skin.accent, bold = true).apply {
                    gravity = Gravity.END
                }, LinearLayout.LayoutParams(ui.dp(60), LinearLayout.LayoutParams.WRAP_CONTENT))
            })
        }
    }
}
