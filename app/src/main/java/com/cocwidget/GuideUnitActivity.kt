package com.cocwidget

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.cocwidget.guide.ChartScale
import com.cocwidget.guide.GuideUnit
import com.cocwidget.guide.GuideUnits
import com.cocwidget.guide.SubUnit
import com.cocwidget.guide.UnitChart
import com.cocwidget.guide.UnitStat
import com.cocwidget.guide.UnitTraits
import com.cocwidget.plan.PlanFormat
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.LevelChart
import java.text.NumberFormat

// One troop, siege machine or spell: how to use it, and how it grows level by level. Picking a level on
// any chart moves every chart and the table with it.
class GuideUnitActivity : GuideScreen() {
    private val units by lazy { GuideUnits.load(this) }
    private val unit: GuideUnit? by lazy { units[intent.getIntExtra(EXTRA_ID, 0)] }

    // -1 until the page has picked a starting level.
    private var selected = -1
    private val onSelect = mutableListOf<(Int) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getInt(KEY_SELECTED, -1) ?: -1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED, selected)
    }

    override fun buildGuide() {
        onSelect.clear()
        val unit = this.unit
        if (unit == null) {
            finish()
            return
        }
        val owned = myLevel(unit)
        if (selected !in unit.levels.indices) selected = startingIndex(unit, owned)
        val cap = if (myTownHall > 0) unit.capIndex(myTownHall) else -1
        // Out of reach only means something once the unit has opened for this village.
        val reach = if (cap >= 0) cap else -1
        val mine = owned?.let(unit::indexOf) ?: -1

        content.addView(backHeader(unit.name))
        content.addView(aboutCard(unit, owned, cap), ui.matchWidth(12))
        linkCard(unit)?.let { content.addView(it, ui.matchWidth(10)) }
        perTownHallCard(unit)?.let { content.addView(it, ui.matchWidth(10)) }
        content.addView(selectedCard(unit), ui.matchWidth(14))
        for (chart in unit.charts) content.addView(chartCard(unit, chart, reach, mine), ui.matchWidth(10))
        for (sub in unit.subs) addSubUnit(unit, sub, reach, mine)
        tableCard(unit)?.let { content.addView(it, ui.matchWidth(14)) }
        addNote(getString(R.string.guide_tips_note, guide.content.checked))
        addNote(getString(R.string.guide_unit_sources))
        select(selected)
    }

    private fun select(index: Int) {
        selected = index
        onSelect.forEach { it(index) }
    }

    // A Super Troop has no level of its own in an export, so it follows its base troop's.
    private fun myLevel(unit: GuideUnit): Int? {
        val level = myLevels[if (unit.baseId != 0) unit.baseId else unit.id] ?: return null
        return level.takeIf { unit.indexOf(it) >= 0 }
    }

    private fun startingIndex(unit: GuideUnit, owned: Int?): Int = when {
        owned != null -> unit.indexOf(owned)
        myTownHall > 0 && unit.capIndex(myTownHall) >= 0 -> unit.capIndex(myTownHall)
        else -> unit.levels.lastIndex
    }

    private fun aboutCard(unit: GuideUnit, owned: Int?, cap: Int) = ui.card().apply {
        val item = items[unit.id]
        addView(titleRow(item, unit.name, getString(R.string.guide_opens_at, unit.townHall)))
        val status = when {
            myTownHall == 0 -> null
            cap < 0 -> getString(R.string.guide_not_yet, unit.townHall)
            owned != null -> getString(R.string.guide_you_level, owned, unit.levels[cap], myTownHall)
            else -> getString(R.string.guide_cap_at, unit.levels[cap], myTownHall)
        }
        status?.let { addView(ui.text(it, 14f, ui.skin.accent, bold = true), ui.matchWidth(8)) }
        val defence = unit.kind == "defence" || unit.kind == "trap"
        val tactics = if (defence) guide.content.defenceTactics[unit.name] else guide.content.unitTactics[unit.name]
        tactics?.let {
            addView(ui.text(it.role, 14f).withTerms(), ui.matchWidth(8))
            if (it.targets.isNotEmpty()) addView(ui.text(getString(R.string.guide_goes_for, it.targets), 13f, bold = true).withTerms(), ui.matchWidth(6))
            if (viewTownHall > 0 && it.allTips.any { tip -> tip.isLaterThan(viewTownHall) || tip.isPast(viewTownHall) }) {
                addView(ui.text(getString(R.string.guide_tips_for, viewTownHall), 12f, ui.skin.dim), ui.matchWidth(6))
            }
            addTactics(it, defence, unit.townHall)
        }
        val chips = traitChips(unit.traits, unit)
        addView(ui.flow().apply {
            for (chip in chips) addView(ui.chip(chip).withTerms())
            for (stat in unit.stats) addView(ui.chip(getString(R.string.guide_stat, stat.name, stat.value)).withTerms())
        }, ui.matchWidth(10))
    }

    private fun traitChips(traits: UnitTraits, unit: GuideUnit?) = buildList {
        if (traits.housing > 0) add(getString(R.string.guide_housing, traits.housing))
        if (unit != null) {
            if (unit.isTroop) add(getString(if (unit.flying) R.string.guide_flying else R.string.guide_ground))
            when {
                unit.hitsNote.isNotEmpty() -> add(unit.hitsNote)
                unit.hitsAir && unit.hitsGround -> add(getString(R.string.guide_hits_both))
                unit.hitsAir -> add(getString(R.string.guide_hits_air))
                unit.hitsGround -> add(getString(R.string.guide_hits_ground))
            }
        }
        if (traits.targets.isNotEmpty()) add(getString(R.string.guide_targets, traits.targets))
        if (traits.range > 0) add(getString(R.string.guide_range, ChartScale.number(traits.range)))
        if (traits.speed > 0) add(getString(R.string.guide_speed, ChartScale.number(traits.speed)))
        if (traits.attackSpeed > 0) add(getString(R.string.guide_attack_speed, ChartScale.number(traits.attackSpeed)))
        if (traits.damageType.isNotEmpty()) add(getString(R.string.guide_damage_type, traits.damageType))
        // A troop's page says what it goes for above these, in words.
        if (unit != null && unit.preferredTarget.isNotEmpty() && guide.content.unitTactics[unit.name]?.targets.isNullOrEmpty()) {
            add(getString(R.string.guide_prefers, unit.preferredTarget))
        }
        if (unit != null && unit.ability.isNotEmpty()) add(unit.ability)
    }

    // How many a Town Hall allows and how high they go, for a defence or trap.
    private fun perTownHallCard(unit: GuideUnit): View? {
        if (unit.perTownHall.isEmpty()) return null
        // The last entry at or below the player's Town Hall is the one that applies to them.
        val mine = unit.perTownHall.lastOrNull { it.townHall <= myTownHall }
        return ui.card().apply {
            addView(ui.heading(getString(R.string.guide_by_town_hall), 15f))
            addView(ui.flow(4).apply {
                for (entry in unit.perTownHall) {
                    val colours = if (entry === mine) ui.skin.primaryBg to ui.skin.primaryText else ui.skin.chip to ui.skin.text
                    addView(ui.chip(getString(R.string.guide_by_town_hall_chip, entry.townHall, entry.count, unit.levels[entry.cap - 1]), colours.first, colours.second))
                }
            }, ui.matchWidth(8))
        }
    }

    // From a Super Troop to its base troop and the other way, with what boosting one takes.
    private fun linkCard(unit: GuideUnit): View? {
        val base = units[unit.baseId]
        val supers = units.values.filter { it.baseId == unit.id }
        if (base == null && supers.isEmpty()) return null
        return ui.card().apply {
            if (base != null) {
                addView(ui.text(getString(R.string.guide_super_of, base.name, unit.baseLevel), 14f))
                if (unit.boostCost > 0) {
                    val cost = PlanFormat.amount(unit.boostCost)
                    addView(ui.text(getString(R.string.guide_boost, cost, unit.boostResource, PlanFormat.duration(unit.boostSeconds)), 13f, ui.skin.dim), ui.matchWidth(4))
                }
            }
            addView(ui.flow().apply {
                for (linked in listOfNotNull(base) + supers) addView(unitChip(linked.id, linked.name))
            }, ui.matchWidth(if (base != null) 8 else 0))
        }
    }

    // What the picked level needs, above the charts it applies to.
    private fun selectedCard(unit: GuideUnit) = ui.card().apply {
        val title = ui.heading("", 20f)
        val needs = ui.text("", 14f, ui.skin.dim)
        addView(title)
        addView(needs, ui.matchWidth(2))
        addView(ui.text(getString(R.string.guide_pick_level), 13f, ui.skin.dim), ui.matchWidth(6))
        onSelect += { index ->
            title.text = getString(R.string.guide_level, unit.levels[index])
            val parts = mutableListOf(getString(R.string.guide_needs_town_hall, unit.townHalls[index]))
            unit.costs.getOrNull(index)?.takeIf { it > 0 }?.let { parts += "${unit.resource} ${PlanFormat.amount(it)}" }
            unit.times.getOrNull(index)?.takeIf { it > 0 }?.let { parts += PlanFormat.duration(it) }
            needs.text = parts.joinToString(" · ")
        }
    }

    private fun chartCard(unit: GuideUnit, chart: UnitChart, reach: Int, mine: Int) = ui.card().apply {
        val readout = ui.text("", 14f, bold = true).apply { gravity = Gravity.END }
        addView(LinearLayout(this@GuideUnitActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ui.heading(chart.title, 15f), ui.weighted())
            addView(readout, ui.wrap().apply { marginStart = ui.dp(8) })
        })
        val view = LevelChart(this@GuideUnitActivity, chart, unit.levels, reach, mine) { select(it) }
        view.contentDescription = describe(unit, chart)
        addView(view, ui.matchWidth(8))

        // More than one line always gets a legend, and it carries each line's value at the level.
        val legend = chart.series.takeIf { it.size > 1 }?.let { lines ->
            val texts = lines.map { ui.text("", 13f) }
            addView(ui.flow(10).apply {
                for ((index, text) in texts.withIndex()) {
                    addView(LinearLayout(this@GuideUnitActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(View(this@GuideUnitActivity).apply {
                            background = GradientDrawable().apply {
                                setColor(LevelChart.COLOURS[index % LevelChart.COLOURS.size])
                                cornerRadius = ui.dp(2).toFloat()
                            }
                        }, LinearLayout.LayoutParams(ui.dp(14), ui.dp(4)))
                        addView(text, ui.wrap().apply { marginStart = ui.dp(6) })
                    })
                }
            }, ui.matchWidth(8))
            texts
        }
        onSelect += { index ->
            view.selected = index
            val level = getString(R.string.guide_level_short, unit.levels[index])
            if (legend == null) {
                val value = chart.series.first().values[index]
                readout.text = getString(R.string.guide_level_value, level, display(value, chart))
            } else {
                readout.text = level
                for ((line, text) in chart.series.zip(legend)) text.text = getString(R.string.guide_stat, line.name, display(line.values[index], chart))
            }
        }
    }

    private fun display(value: Double?, chart: UnitChart) = if (value == null) getString(R.string.guide_no_value) else ChartScale.value(value, chart.unit)

    // What the chart shows for a screen reader, which can also read every level from the table.
    private fun describe(unit: GuideUnit, chart: UnitChart) = chart.series.joinToString(". ", prefix = "${chart.title}. ") { line ->
        val first = line.values.indexOfFirst { it != null }
        val last = line.values.indexOfLast { it != null }
        val range = getString(
            R.string.guide_chart_range,
            display(line.values[first], chart), unit.levels[first], display(line.values[last], chart), unit.levels[last],
        )
        if (chart.series.size > 1) "${line.name}: $range" else range
    }

    private fun addSubUnit(unit: GuideUnit, sub: SubUnit, reach: Int, mine: Int) {
        content.addView(ui.card().apply {
            val icon = if (sub.icon.isNotEmpty()) picture(sub.icon) else picture(items[unit.id])
            addView(LinearLayout(this@GuideUnitActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(icon)
                addView(LinearLayout(this@GuideUnitActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(ui.heading(sub.name, 17f))
                    if (!sub.partOfUnit) addView(ui.text(getString(R.string.guide_comes_from, unit.name), 13f, ui.skin.dim), ui.matchWidth(1))
                }, ui.weighted(10))
            })
            if (sub.note.isNotEmpty()) addView(ui.text(sub.note, 13f, ui.skin.accent).withTerms(), ui.matchWidth(8))
            val chips = sub.traits?.let { traitChips(it, null) }.orEmpty() + sub.stats.map(::statText)
            if (chips.isNotEmpty()) addView(ui.flow().apply { for (chip in chips) addView(ui.chip(chip)) }, ui.matchWidth(10))
        }, ui.matchWidth(18))
        for (chart in sub.charts) content.addView(chartCard(unit, chart, reach, mine), ui.matchWidth(10))
    }

    private fun statText(stat: UnitStat) = getString(R.string.guide_stat, stat.name, stat.value)

    // Every level in full. It scrolls sideways, so large text still fits, and a tap picks a level.
    private fun tableCard(unit: GuideUnit): View? {
        val columns = unit.charts.filter { it.series.size == 1 }.take(TABLE_CHARTS)
        return ui.card().apply {
            addView(ui.heading(getString(R.string.guide_all_levels), 15f))
            val table = TableLayout(this@GuideUnitActivity)
            val headers = mutableListOf(getString(R.string.guide_col_level), getString(R.string.guide_col_town_hall))
            headers += columns.map { it.title }
            if (unit.costs.isNotEmpty()) {
                headers += getString(R.string.guide_col_cost, unit.resource)
                headers += getString(R.string.guide_col_time)
            }
            table.addView(TableRow(this@GuideUnitActivity).apply { for (header in headers) addView(cell(header, ui.skin.dim, true)) })
            val rows = unit.levels.indices.map { index ->
                TableRow(this@GuideUnitActivity).apply {
                    addView(cell(unit.levels[index].toString(), ui.skin.text, true))
                    addView(cell(unit.townHalls[index].toString(), ui.skin.text, false))
                    for (chart in columns) addView(cell(display(chart.series.first().values[index], chart), ui.skin.text, false))
                    if (unit.costs.isNotEmpty()) {
                        // A troop's first level comes free, while a defence's first level is built.
                        addView(cell(if (unit.costs[index] == 0L) getString(R.string.guide_no_value) else NumberFormat.getIntegerInstance().format(unit.costs[index]), ui.skin.text, false))
                        addView(cell(if (unit.times[index] == 0L) getString(R.string.guide_no_value) else PlanFormat.duration(unit.times[index]), ui.skin.text, false))
                    }
                    ui.pressable(this, 4f) { select(index) }
                }
            }
            rows.forEach(table::addView)
            onSelect += { picked ->
                for ((index, row) in rows.withIndex()) {
                    row.background = if (index == picked) ui.rounded(ui.skin.chip, 4f) else null
                    row.isSelected = index == picked
                }
            }
            addView(HorizontalScrollView(this@GuideUnitActivity).apply {
                // Wider tables run past the card, so keep the bar showing to say they scroll.
                isHorizontalScrollBarEnabled = true
                isScrollbarFadingEnabled = false
                // The bar goes under the last row, not over it. One UI draws it about 10 dp up from
                // the edge, so the room has to be more than the bar's own thickness.
                scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
                setPadding(0, 0, 0, ui.dp(TABLE_BAR_ROOM))
                addView(table)
            }, ui.matchWidth(8))
        }
    }

    private fun cell(value: String, colour: Int, bold: Boolean) = ui.text(value, 13f, colour, bold).apply {
        setPadding(ui.dp(6), ui.dp(6), ui.dp(10), ui.dp(6))
        (this as TextView).maxLines = 2
    }

    companion object {
        private const val KEY_SELECTED = "selected"
        // The table stays readable with this many stat columns. The charts have the rest.
        private const val TABLE_CHARTS = 3
        private const val TABLE_BAR_ROOM = 20
    }
}
