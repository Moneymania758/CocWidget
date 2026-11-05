package com.cocwidget

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.game.Ids
import com.cocwidget.guide.Fighter
import com.cocwidget.guide.Guide
import com.cocwidget.guide.Strategy
import com.cocwidget.plan.PlanFormat
import com.cocwidget.ui.GuideScreen

// One Town Hall: what the upgrade costs and adds, the hero and pet caps, and how people attack.
class GuideTownHallActivity : GuideScreen() {
    private var townHall = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        townHall = savedInstanceState?.getInt(EXTRA_TOWN_HALL) ?: intent.getIntExtra(EXTRA_TOWN_HALL, Guide.FIRST_TOWN_HALL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_TOWN_HALL, townHall)
    }

    override fun buildGuide() {
        content.addView(backHeader(getString(R.string.guide_town_hall_title, townHall)).apply {
            if (townHall == myTownHall) addView(ui.chip(getString(R.string.guide_you), colour = ui.skin.accent))
        })
        val page = guide.townHall(townHall)
        page?.summary?.let { addNote(it, 14f) }
        content.addView(upgradeCard(), ui.matchWidth(12))
        content.addView(newCard(), ui.matchWidth(14))
        fighterCard(R.string.guide_heroes, guide.facts.heroes, GuideHeroesActivity::class.java)?.let { content.addView(it, ui.matchWidth(14)) }
        fighterCard(R.string.guide_pets, guide.facts.pets, GuidePetsActivity::class.java)?.let { content.addView(it, ui.matchWidth(14)) }
        content.addView(sectionTitle(getString(R.string.guide_strategies)), ui.matchWidth(18).apply { marginStart = ui.dp(4) })
        if (page == null) {
            addNote(getString(R.string.guide_no_strategies), 14f)
        } else {
            if (page.note.isNotEmpty()) addNote(page.note, 13f)
            for (strategy in page.strategies) content.addView(strategyCard(strategy), ui.matchWidth(10))
            addNote(getString(R.string.guide_opinion, guide.content.checked))
        }
        content.addView(neighbours(), ui.matchWidth(14))
    }

    private fun upgradeCard() = ui.card().apply {
        val hall = items[Ids.TOWN_HALL]
        addView(sectionTitle(getString(R.string.guide_upgrade)))
        addView(ui.flow().apply {
            addView(ui.costChip(hall?.resource.orEmpty(), PlanFormat.amount(hall?.costToReach(townHall) ?: 0)))
            addView(ui.chip(PlanFormat.duration(hall?.secondsToReach(townHall) ?: 0)))
        }, ui.matchWidth(8))
        val buildings = listOf(
            R.string.guide_hero_hall to Ids.HERO_HALL,
            R.string.guide_pet_house to Ids.PET_HOUSE,
            R.string.guide_laboratory to Ids.LABORATORY,
            R.string.guide_blacksmith to Ids.BLACKSMITH,
        ).mapNotNull { (name, id) -> items[id]?.maxLevelAt(townHall)?.takeIf { it > 0 }?.let { getString(name, it) } }
        if (buildings.isNotEmpty()) {
            addView(ui.flow().apply { for (building in buildings) addView(ui.chip(building)) }, ui.matchWidth(6))
        }
    }

    private fun newCard() = ui.card().apply {
        addView(sectionTitle(getString(R.string.guide_new, townHall)))
        val additions = Guide.additions(townHall, items)
        // Super Troops all open together at TH11, and only as boosts of troops already listed.
        val units = guide.unitsOpeningAt(townHall).filter { it.kind != "super" }
        addView(ui.flow().apply {
            for (addition in additions.filter { it.first }) {
                addView(namedChip(addition.item, withQuantity(addition.item.name, addition.quantity)))
            }
            for (unit in units) addView(namedChip(items[unit.id], unit.name))
        }, ui.matchWidth(8))
        val more = additions.filter { !it.first }
        if (more.isNotEmpty()) {
            addView(ui.text(getString(R.string.guide_also, more.joinToString(", ") { withQuantity(it.item.name, it.quantity) }), 13f, ui.skin.dim), ui.matchWidth(8))
        }
    }

    private fun withQuantity(name: String, quantity: Int) = if (quantity > 1) getString(R.string.guide_quantity, name, quantity) else name

    // Each hero or pet this Town Hall has, with its cap, or null when it has none. A tap opens its
    // full entry.
    private fun fighterCard(title: Int, fighters: List<Fighter>, screen: Class<*>) =
        fighters.filter { (items[it.id]?.maxLevelAt(townHall) ?: 0) > 0 }.takeIf { it.isNotEmpty() }?.let { available ->
            ui.card().apply {
                addView(sectionTitle(getString(title)))
                for (fighter in available) {
                    val item = items[fighter.id]
                    val cap = item?.maxLevelAt(townHall) ?: 0
                    val mine = myLevels[fighter.id]?.takeIf { townHall == myTownHall }
                    val line = if (mine != null) getString(R.string.guide_level_of, mine, cap) else getString(R.string.guide_cap, cap)
                    addView(titleRow(item, fighter.name, line).apply {
                        ui.pressable(this, 8f) { open(screen) { putExtra(EXTRA_ID, fighter.id) } }
                    }, ui.matchWidth(10))
                }
            }
        }

    private fun strategyCard(strategy: Strategy) = ui.card().apply {
        val (label, colour) = when (strategy.level) {
            "easy" -> R.string.guide_level_easy to ui.skin.green
            "medium" -> R.string.guide_level_medium to ui.skin.accent
            else -> R.string.guide_level_hard to ui.skin.dim
        }
        addView(LinearLayout(this@GuideTownHallActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(ui.heading(strategy.name, 17f), ui.weighted())
            addView(ui.chip(getString(label), colour = colour), ui.wrap().apply { marginStart = ui.dp(8) })
        })
        addView(ui.text(strategy.about, 14f, ui.skin.dim), ui.matchWidth(6))
        addView(ui.flow().apply {
            for (name in strategy.army) addView(namedChip(guide.unit(name)?.let { items[it.id] }, name))
        }, ui.matchWidth(10))
        for ((index, step) in strategy.steps.withIndex()) {
            addView(ui.text(getString(R.string.guide_step, index + 1, step), 14f), ui.matchWidth(if (index == 0) 10 else 4))
        }
        for (setup in strategy.heroes) {
            val detail = listOfNotNull(setup.pet.takeIf { it.isNotEmpty() }) + setup.gear
            addView(titleRow(itemNamed(guide.facts.heroes, setup.hero), setup.hero, detail.joinToString(", "), 32), ui.matchWidth(8))
        }
        val names = strategy.sources.mapNotNull { key -> guide.content.sources.firstOrNull { it.key == key }?.name }
        addView(ui.text(getString(R.string.guide_from, names.joinToString(", ")), 12f, ui.skin.dim), ui.matchWidth(10))
    }

    private fun neighbours() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        if (townHall > Guide.FIRST_TOWN_HALL) addView(step(townHall - 1, R.string.guide_previous), ui.weighted())
        if (townHall < maxTownHall) addView(step(townHall + 1, R.string.guide_next), ui.weighted(if (townHall > Guide.FIRST_TOWN_HALL) 8 else 0))
    }

    private fun step(target: Int, label: Int) = ui.button(getString(label, target), null, primary = false) {
        townHall = target
        show()
        findViewById<ScrollView>(R.id.scroll).scrollTo(0, 0)
    }
}
