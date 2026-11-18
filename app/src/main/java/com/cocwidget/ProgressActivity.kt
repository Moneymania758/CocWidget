package com.cocwidget

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.game.GameData
import com.cocwidget.game.GameItem
import com.cocwidget.game.Section
import com.cocwidget.plan.Finished
import com.cocwidget.plan.FinishedUpgrade
import com.cocwidget.plan.PlanFormat
import com.cocwidget.plan.Planner
import com.cocwidget.plan.Progress
import com.cocwidget.plan.ProgressMeasure
import com.cocwidget.plan.ProgressPoint
import com.cocwidget.plan.Week
import com.cocwidget.store.Icons
import com.cocwidget.store.ImportHistory
import com.cocwidget.store.ProgressHistory
import com.cocwidget.store.Store
import com.cocwidget.ui.Pictures
import com.cocwidget.ui.ProgressChart
import com.cocwidget.ui.ProgressText
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Skin
import com.cocwidget.ui.TimeText
import com.cocwidget.ui.WeekChart
import java.time.ZoneId
import kotlin.concurrent.thread

// How far the village has come, from every import kept. Opened from the card on Time to max,
// which already shows where things stand, so this page is about what changed.
class ProgressActivity : Screen() {
    // Every import worked out, and what finished between the one compared with and the latest.
    // [finished] is null when there's nothing earlier to compare with.
    private class Loaded(val points: List<ProgressPoint>, val finished: List<FinishedUpgrade>?)

    // Working out the imports not worked out before, off the main thread.
    internal var loading: Thread? = null
    private var loaded: Loaded? = null
    // The account and export the points are for, so a new import or account works them out again.
    private var loadedFor: String? = null
    // The day picked on the chart, by its import's time, so it survives the page being made again.
    private var pickedAt: Long? = null
    private var measure = ProgressMeasure.DONE
    // The week picked on the weekly charts, by its Monday's day number. Both charts share it.
    private var pickedWeek: Long? = null
    private val pictures = Pictures(this)

    // A rotation makes the page again, so the day picked and what the chart plots are kept.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) return
        pickedAt = savedInstanceState.getLong(STATE_PICKED, -1).takeIf { it >= 0 }
        measure = ProgressMeasure.entries.getOrNull(savedInstanceState.getInt(STATE_MEASURE)) ?: ProgressMeasure.DONE
        pickedWeek = savedInstanceState.getLong(STATE_WEEK, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pickedAt?.let { outState.putLong(STATE_PICKED, it) }
        outState.putInt(STATE_MEASURE, measure.ordinal)
        pickedWeek?.let { outState.putLong(STATE_WEEK, it) }
    }

    override fun build() {
        pictures.clear()
        content.addView(backHeader(R.string.progress_title))
        val current = Store.current(this)
        val items = GameData.all(this)
        // At full price, like every kept import, so they line up.
        val plan = current?.let { Planner.plan(it.json, items, it.village.builders) }
        if (current == null || plan == null) {
            addNothingSynced()
            return
        }
        val latest = Progress.of(plan, current.village.exportedAtMillis)
        val key = current.tag + "@" + latest.exportedAtMillis
        if (key != loadedFor) load(current.tag, current.json, latest.exportedAtMillis, key)

        val all = loaded?.points
        val earlier = all?.let { list ->
            Progress.compareWith(list.map { it.exportedAtMillis }, latest.exportedAtMillis)?.let { at -> list.first { it.exportedAtMillis == at } }
        }
        content.addView(summary(latest, earlier, all != null), ui.matchWidth(16))
        content.addView(ui.card().also { fillChart(it, all) }, ui.matchWidth(12))
        val finished = loaded?.finished
        if (earlier != null && finished != null) content.addView(finishedCard(plan, latest, earlier, finished, items), ui.matchWidth(12))
        if (all != null && all.any { it.since != null }) addWeekCards(Progress.weekly(all, ZoneId.systemDefault()))
        addNote(getString(R.string.progress_notes))
        pictures.load()
    }

    private fun load(tag: String, json: String, latestAt: Long, key: String) {
        // A new import or account starts on its last day. The first load keeps a day kept over a
        // rotation.
        if (loadedFor != null) {
            pickedAt = null
            pickedWeek = null
        }
        loaded = null
        loadedFor = key
        val app = applicationContext
        loading = thread {
            val result = runCatching {
                val points = ProgressHistory.all(app, tag)
                val at = Progress.compareWith(points.map { it.exportedAtMillis }, latestAt)
                val before = Store.history(app, tag).firstOrNull { it.exportedAtMillis == at }?.let(ImportHistory::read)
                Loaded(points, before?.let { Finished.between(it, json, GameData.all(app)) })
            }.getOrDefault(Loaded(emptyList(), null))
            runOnUiThread {
                if (isDestroyed || loadedFor != key) return@runOnUiThread
                loaded = result
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
        addView(ui.text(maxLine(latest, earlier?.takeIf { it.townHall == latest.townHall }), 13f, ui.skin.dim), ui.matchWidth(12))
    }

    // "Time to max 412d → 398d, 14d closer", against [then] at the same Town Hall.
    private fun maxLine(latest: ProgressPoint, then: ProgressPoint?): String {
        val now = PlanFormat.duration(latest.maxSeconds)
        if (then == null) return getString(R.string.progress_max, now)
        val was = PlanFormat.duration(then.maxSeconds)
        return when {
            was == now -> getString(R.string.progress_max_same, now)
            then.maxSeconds > latest.maxSeconds -> getString(R.string.progress_max_closer, was, now, PlanFormat.duration(then.maxSeconds - latest.maxSeconds))
            else -> getString(R.string.progress_max_further, was, now)
        }
    }

    // Refilled in place when the switch changes what it plots, so the page doesn't scroll.
    private fun fillChart(card: LinearLayout, all: List<ProgressPoint>?) {
        card.removeAllViews()
        card.addView(ui.heading(getString(R.string.progress_chart_title), headingSize()))
        if (all == null) {
            card.addView(ui.text(getString(R.string.progress_loading), 14f, ui.skin.dim), ui.matchWidth(8))
            return
        }
        val daily = Progress.daily(all, ZoneId.systemDefault())
        if (daily.size < 2) {
            card.addView(ui.text(getString(R.string.progress_one_day), 14f, ui.skin.dim), ui.matchWidth(8))
            return
        }
        card.addView(measureSwitch { chosen ->
            measure = chosen
            fillChart(card, all)
        }, ui.matchWidth(10))
        val picked = daily.indexOfFirst { it.exportedAtMillis == pickedAt }.takeIf { it >= 0 } ?: daily.lastIndex
        val readout = ui.text(pickedText(daily[picked]), 13f, ui.skin.dim)
        card.addView(readout, ui.matchWidth(10))
        // A finger on the chart picks a day, and the line above it names that import.
        lateinit var chart: ProgressChart
        chart = ProgressChart(this, daily, measure, ui.skin == Skin.LIGHT) { index ->
            pickedAt = daily[index].exportedAtMillis
            chart.selected = index
            readout.text = pickedText(daily[index])
        }
        val first = daily.first()
        val last = daily.last()
        chart.contentDescription = getString(
            if (measure == ProgressMeasure.DONE) R.string.progress_chart_description else R.string.progress_chart_description_max,
            value(first), TimeText.day(first.exportedAtMillis),
            value(last), TimeText.day(last.exportedAtMillis),
        )
        chart.selected = picked
        card.addView(chart, ui.matchWidth(8))
    }

    // The share done or the time to max, side by side, the one shown raised.
    private fun measureSwitch(onPick: (ProgressMeasure) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        background = ui.rounded(ui.skin.chip, 14f)
        setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
        for ((index, choice) in ProgressMeasure.entries.withIndex()) {
            val chosen = choice == measure
            val label = getString(if (choice == ProgressMeasure.DONE) R.string.progress_show_done else R.string.progress_show_max)
            addView(ui.text(label, 14f, if (chosen) ui.skin.text else ui.skin.dim, bold = chosen).apply {
                gravity = Gravity.CENTER
                minHeight = ui.dp(40)
                maxLines = 1
                if (chosen) background = ui.rounded(ui.skin.card, 10f)
                isSelected = chosen
                ui.pressable(this, 10f) { if (!chosen) onPick(choice) }
            }, ui.weighted(if (index == 0) 0 else 4))
        }
    }

    private fun value(point: ProgressPoint) =
        if (measure == ProgressMeasure.DONE) ProgressText.percent(point.done) else PlanFormat.duration(point.maxSeconds)

    private fun pickedText(point: ProgressPoint): String {
        val shown = if (measure == ProgressMeasure.DONE) value(point) else getString(R.string.progress_to_max, value(point))
        return getString(R.string.progress_picked, TimeText.day(point.exportedAtMillis), point.townHall, shown)
    }

    // Each upgrade done between [earlier] and the latest import, under its group with how far the
    // group moved. Groups that didn't move are left out.
    private fun finishedCard(
        plan: Planner.Plan,
        latest: ProgressPoint,
        earlier: ProgressPoint,
        finished: List<FinishedUpgrade>,
        items: Map<Int, GameItem>,
    ) = ui.card(0).apply {
        addView(ui.heading(getString(R.string.progress_finished, TimeText.day(earlier.exportedAtMillis)), headingSize()).apply {
            setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(4))
        })
        if (finished.isEmpty()) {
            addView(ui.text(getString(R.string.progress_finished_none), 14f, ui.skin.dim).apply {
                setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(16))
            })
            return@apply
        }
        // A share against another Town Hall's target says nothing, so changes need the same one.
        val sameTownHall = earlier.townHall == latest.townHall
        for ((group, rows) in finished.groupBy { it.group }) {
            val then = earlier.groups[group]?.takeIf { sameTownHall }
            val now = plan.group(group)?.done
            val change = if (then != null && now != null) ProgressText.change(now, then) else null
            val up = then != null && now != null && now > then
            addView(LinearLayout(this@ProgressActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(4))
                addView(ui.text(getString(PlannerActivity.titleOf(group)), 13f, ui.skin.dim, bold = true), ui.weighted())
                if (change != null) addView(ui.text(change, 13f, if (up) ui.skin.green else ui.skin.dim, bold = true))
            })
            for (row in rows) addView(finishedRow(row, items[row.id]))
        }
        addView(View(this@ProgressActivity), LinearLayout.LayoutParams(1, ui.dp(10)))
    }

    private fun finishedRow(row: FinishedUpgrade, game: GameItem?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(16), ui.dp(6), ui.dp(16), ui.dp(6))
        val picture = ui.picture()
        pictures.add(game?.let { Icons.path(it, row.to) }, picture)
        addView(picture, LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)))
        val name = if (row.copies > 1) getString(R.string.progress_copies, row.name, row.copies) else row.name
        addView(ui.text(name, 15f), ui.weighted(12))
        val levels = when {
            row.supercharge -> getString(R.string.progress_supercharge, row.fromStage, row.toStage)
            row.from == 0 -> getString(R.string.progress_built, row.to)
            else -> getString(R.string.progress_levels, row.from, row.to)
        }
        addView(ui.text(levels, 14f, ui.skin.accent, bold = true).apply { gravity = Gravity.END }, ui.wrap().apply { marginStart = ui.dp(10) })
    }

    // Upgrades finished and idle time, a bar a week. Picking a week on either picks it on both.
    private fun addWeekCards(weeks: List<Week>) {
        val start = weeks.indexOfFirst { it.start.toEpochDay() == pickedWeek }.takeIf { it >= 0 } ?: weeks.lastIndex
        val upgradesLine = ui.text(levelsText(weeks[start]), 13f, ui.skin.dim)
        val idleLine = ui.text(idleText(weeks[start]), 13f, ui.skin.dim)
        val charts = ArrayList<WeekChart>()
        val pick = { index: Int ->
            pickedWeek = weeks[index].start.toEpochDay()
            for (chart in charts) chart.selected = index
            upgradesLine.text = levelsText(weeks[index])
            idleLine.text = idleText(weeks[index])
        }
        val light = ui.skin == Skin.LIGHT
        val upgrades = weeks.map { week -> if (week.covered) week.levels.values.sum().toFloat() else null }
        val idle = weeks.map { week -> if (week.covered) (week.idleSeconds[Section.BUILDERS] ?: 0) / 3_600f else null }
        charts += WeekChart(this, weeks, upgrades, { "$it" }, light, pick).apply {
            contentDescription = getString(R.string.progress_upgrades_description, describe(weeks, upgrades) { "${it.toInt()}" })
        }
        charts += WeekChart(this, weeks, idle, { getString(R.string.progress_hours, it) }, light, pick).apply {
            contentDescription = getString(R.string.progress_idle_description, describe(weeks, idle) { PlanFormat.duration((it * 3_600).toLong()) })
        }
        for (chart in charts) chart.selected = start
        content.addView(weekCard(R.string.progress_upgrades_title, upgradesLine, charts[0]), ui.matchWidth(12))
        content.addView(weekCard(R.string.progress_idle_title, idleLine, charts[1]), ui.matchWidth(12))
    }

    private fun weekCard(title: Int, line: TextView, chart: WeekChart) = ui.card().apply {
        addView(ui.heading(getString(title), headingSize()))
        addView(line, ui.matchWidth(8))
        addView(chart, ui.matchWidth(8))
    }

    // "Week of 9 Nov · Builders 7 · Lab 4 · Pets 1"
    private fun levelsText(week: Week): String = weekLine(week) { section -> "${week.levels[section] ?: 0}" }

    // "Week of 9 Nov · Builders 14h · Lab 3h · Pets none"
    private fun idleText(week: Week): String = weekLine(week) { section ->
        val seconds = week.idleSeconds[section] ?: 0
        if (seconds < 60) getString(R.string.progress_idle_none) else PlanFormat.duration(seconds)
    }

    private fun weekLine(week: Week, value: (Section) -> String): String {
        val day = TimeText.day(week.start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        if (!week.covered) return getString(R.string.progress_week_none, day)
        val parts = week.idleSeconds.keys.sortedBy { it.ordinal }.map { section -> getString(sectionName(section)) + " " + value(section) }
        return (listOf(getString(R.string.progress_week, day)) + parts).joinToString(" · ")
    }

    private fun sectionName(section: Section) = when (section) {
        Section.LAB -> R.string.progress_section_lab
        Section.PETS -> R.string.progress_section_pets
        else -> R.string.progress_section_builders
    }

    // "2 Nov 5, 9 Nov 12", leaving out weeks no imports span.
    private fun describe(weeks: List<Week>, values: List<Float?>, format: (Float) -> String) =
        weeks.indices.mapNotNull { index ->
            val value = values[index] ?: return@mapNotNull null
            TimeText.day(weeks[index].start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) + " " + format(value)
        }.joinToString(", ")

    private fun headingSize() = if (ui.skin.headingFont != null) 18f else 15f

    private companion object {
        const val STATE_PICKED = "picked_at"
        const val STATE_MEASURE = "measure"
        const val STATE_WEEK = "week"
    }
}
