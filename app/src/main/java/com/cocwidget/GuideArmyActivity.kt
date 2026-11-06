package com.cocwidget

import com.cocwidget.guide.GuideUnits
import com.cocwidget.ui.GuideScreen

// Every troop, siege machine and spell, in the order they open.
open class GuideArmyActivity : GuideScreen() {
    protected open val title = R.string.guide_army
    protected open val intro = R.string.guide_army_intro
    protected open val groups = listOf(
        "elixir" to R.string.guide_group_elixir,
        "dark" to R.string.guide_group_dark,
        "super" to R.string.guide_group_super,
        "siege" to R.string.guide_group_siege,
        "spell" to R.string.guide_group_spell,
    )

    override fun buildGuide() {
        content.addView(backHeader(title))
        addNote(getString(intro), 13f)
        val units = GuideUnits.load(this).values.sortedWith(compareBy({ it.townHall }, { it.name }))
        for ((group, title) in groups) {
            content.addView(ui.card().apply {
                addView(sectionTitle(getString(title)))
                addView(ui.flow().apply {
                    for (unit in units.filter { it.group == group }) {
                        addView(namedChip(items[unit.id], unit.name).apply {
                            minimumHeight = ui.dp(40)
                            ui.pressable(this, 8f) { openUnit(unit.id) }
                        })
                    }
                }, ui.matchWidth(10))
            }, ui.matchWidth(14))
        }
        addExtras()
    }

    protected open fun addExtras() = Unit
}

// Every defence and trap, in the same way.
class GuideDefencesActivity : GuideArmyActivity() {
    override val title = R.string.guide_defences
    override val intro = R.string.guide_defences_intro
    override val groups = listOf(
        "defence" to R.string.guide_group_defences,
        "trap" to R.string.guide_group_traps,
    )

    override fun addExtras() {
        val threats = guide.content.threats
        if (threats.isEmpty()) return
        content.addView(ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_group_threats)))
            addView(ui.flow().apply { for (threat in threats) addView(threatChip(threat)) }, ui.matchWidth(10))
        }, ui.matchWidth(14))
    }
}
