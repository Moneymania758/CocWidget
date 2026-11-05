package com.cocwidget

import com.cocwidget.guide.GuideUnits
import com.cocwidget.ui.GuideScreen

// Every troop, siege machine and spell, in the order they open.
class GuideArmyActivity : GuideScreen() {
    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_army))
        addNote(getString(R.string.guide_army_intro), 13f)
        val units = GuideUnits.load(this).values.sortedWith(compareBy({ it.townHall }, { it.name }))
        val groups = listOf(
            "elixir" to R.string.guide_group_elixir,
            "dark" to R.string.guide_group_dark,
            "super" to R.string.guide_group_super,
            "siege" to R.string.guide_group_siege,
            "spell" to R.string.guide_group_spell,
        )
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
    }
}
