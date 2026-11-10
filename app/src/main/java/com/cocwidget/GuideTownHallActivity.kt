package com.cocwidget

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.game.Ids
import com.cocwidget.guide.Fighter
import com.cocwidget.guide.Guide
import com.cocwidget.guide.GuideUnits
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

    override val viewTownHall: Int get() = townHall

    override fun buildGuide() {
        content.addView(backHeader(getString(R.string.guide_town_hall_title, townHall)).apply {
            if (townHall == myTownHall) addView(ui.chip(getString(R.string.guide_you), ui.skin.primaryBg, ui.skin.primaryText))
        })
        val page = guide.townHall(townHall)
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ui.townHall(townHall, pictures), LinearLayout.LayoutParams(ui.dp(104), ui.dp(112)))
            page?.summary?.let { addView(ui.text(it, 14f, ui.skin.dim).withTerms(), ui.weighted(10)) }
        }, ui.matchWidth(10))
        content.addView(upgradeCard(), ui.matchWidth(12))
        content.addView(newCard(), ui.matchWidth(14))
        upgradeFirstCard()?.let { content.addView(it, ui.matchWidth(14)) }
        threatCard()?.let { content.addView(it, ui.matchWidth(14)) }
        fighterCard(R.string.guide_heroes, guide.facts.heroes, GuideHeroesActivity::class.java)?.let { content.addView(it, ui.matchWidth(14)) }
        fighterCard(R.string.guide_pets, petsInUnlockOrder(), GuidePetsActivity::class.java)?.let { content.addView(it, ui.matchWidth(14)) }
        content.addView(sectionTitle(getString(R.string.guide_strategies)), ui.matchWidth(18).apply { marginStart = ui.dp(4) })
        if (page == null) {
            addNote(getString(R.string.guide_no_strategies), 14f)
        } else {
            if (page.note.isNotEmpty()) addTermNote(page.note, 13f)
            addNote(getString(R.string.guide_tap_unit), 13f)
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
        val pages = GuideUnits.load(this@GuideTownHallActivity).values.associate { it.name to it.id }
        addView(ui.flow().apply {
            for (addition in additions.filter { it.first }) {
                val label = withQuantity(addition.item.name, addition.quantity)
                // Defences and traps have their own pages. Other buildings don't.
                val page = pages[addition.item.name]
                addView(if (page != null) unitChip(page, label) else namedChip(addition.item, label))
            }
            for (unit in units) addView(unitChip(unit.id, unit.name))
        }, ui.matchWidth(8))
        val more = additions.filter { !it.first }
        if (more.isNotEmpty()) {
            addView(ui.text(getString(R.string.guide_also, more.joinToString(", ") { withQuantity(it.item.name, it.quantity) }), 13f, ui.skin.dim), ui.matchWidth(8))
        }
    }

    // What to spend builders and the lab on after reaching this Town Hall.
    private fun upgradeFirstCard() = guide.content.upgradeFirst[townHall]?.let { advice ->
        ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_upgrade_first)))
            for ((index, line) in advice.lines.withIndex()) {
                addView(ui.text(getString(R.string.guide_step, index + 1, line), 14f).withTerms(), ui.matchWidth(if (index == 0) 8 else 4))
            }
            addSources(advice.sources)
            if (guide.content.rushing != null) {
                addView(ui.button(getString(R.string.guide_rush_instead), null, primary = false) {
                    open(GuideRushActivity::class.java) { putExtra(EXTRA_VIEW_TOWN_HALL, townHall) }
                }, ui.matchWidth(10))
            }
        }
    }

    // What guards the Town Hall itself here.
    private fun threatCard() = guide.content.threats.filter { it.isAt(townHall) }.takeIf { it.isNotEmpty() }?.let { threats ->
        ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_threats_here)))
            addView(ui.flow().apply { for (threat in threats) addView(threatChip(threat)) }, ui.matchWidth(8))
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

    private fun addTermNote(text: String, sizeSp: Float) {
        content.addView(ui.text(text, sizeSp, ui.skin.dim).withTerms(), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
    }

    private fun strategyCard(strategy: Strategy) = ui.card().apply {
        freshTerms()
        val (label, colour) = when (strategy.level) {
            "easy" -> R.string.guide_level_easy to ui.skin.green
            "medium" -> R.string.guide_level_medium to ui.skin.accent
            else -> R.string.guide_level_hard to ui.skin.dim
        }
        addView(LinearLayout(this@GuideTownHallActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(ui.heading(strategy.name, 17f).withTerms(), ui.weighted())
            addView(ui.chip(getString(label), colour = colour), ui.wrap().apply { marginStart = ui.dp(8) })
        })
        addView(ui.text(strategy.about, 14f, ui.skin.dim).withTerms(), ui.matchWidth(6))
        addView(ui.flow().apply {
            for (name in strategy.army) addView(unitChip(guide.unit(name)?.id ?: 0, armyLabel(strategy, name)))
        }, ui.matchWidth(10))
        armyFill(strategy)?.let { addView(ui.text(it, 12f, ui.skin.dim), ui.matchWidth(6)) }
        for ((index, step) in strategy.steps.withIndex()) {
            addView(ui.text(getString(R.string.guide_step, index + 1, step), 14f).withTerms(), ui.matchWidth(if (index == 0) 10 else 4))
        }
        for (setup in strategy.heroes) {
            val detail = listOfNotNull(setup.pet.takeIf { it.isNotEmpty() }) + setup.gear
            val heroId = guide.facts.heroes.firstOrNull { it.name == setup.hero }?.id ?: 0
            addView(titleRow(items[heroId], setup.hero, detail.joinToString(", "), 32).apply {
                ui.pressable(this, 8f) { open(GuideHeroesActivity::class.java) { putExtra(EXTRA_ID, heroId) } }
            }, ui.matchWidth(8))
        }
        val names = strategy.sources.mapNotNull { key -> guide.content.sources.firstOrNull { it.key == key }?.name }
        addView(ui.text(getString(R.string.guide_from, names.joinToString(", ")), 12f, ui.skin.dim), ui.matchWidth(10))
    }

    private fun armyLabel(strategy: Strategy, name: String) =
        strategy.counts[name]?.let { getString(R.string.guide_quantity, name, it) } ?: name

    // How full the example army is, so a player can tell how to adjust it.
    private fun armyFill(strategy: Strategy): String? {
        val size = guide.content.capacities[townHall] ?: return null
        if (strategy.counts.isEmpty()) return null
        val units = GuideUnits.load(this).values
        val (army, spells) = Guide.armyHousing(
            strategy,
            units.associate { it.name to it.traits.housing },
            units.associate { it.name to it.kind },
        )
        return getString(R.string.guide_army_fill, army, size.army, spells, size.spells)
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
