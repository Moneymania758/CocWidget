package com.cocwidget

import com.cocwidget.guide.Fighter
import com.cocwidget.guide.Guide
import com.cocwidget.ui.GuideScreen

// Every pet in the order they unlock, or just one when opened from a Town Hall.
class GuidePetsActivity : GuideScreen() {
    override fun buildGuide() {
        val only = intent.getIntExtra(EXTRA_ID, 0)
        val pets = petsInUnlockOrder().filter { only == 0 || it.id == only }
        val single = pets.singleOrNull().takeIf { only != 0 }
        content.addView(if (single != null) backHeader(single.name) else backHeader(R.string.guide_pets))
        for (pet in pets) content.addView(petCard(pet), ui.matchWidth(14))
        addNote(getString(R.string.guide_picks_note, guide.content.checked))
    }

    private fun petCard(pet: Fighter) = ui.card().apply {
        val item = items[pet.id]
        freshTerms()
        val first = item?.let(Guide::firstTownHall) ?: 0
        val house = item?.requiredLevelFor(1) ?: 0
        addView(titleRow(item, pet.name, getString(R.string.guide_pet_unlock, first, house)))
        val mine = myLevels[pet.id]
        when {
            mine != null -> addView(ui.text(getString(R.string.guide_you_level, mine, item?.maxLevelAt(myTownHall) ?: 0, myTownHall), 14f, ui.skin.accent, bold = true), ui.matchWidth(8))
            myTownHall in 1 until first -> addView(ui.text(getString(R.string.guide_not_yet, first), 14f, ui.skin.dim, bold = true), ui.matchWidth(8))
        }
        guide.content.pets[pet.name]?.let { addView(ui.text(it, 14f).withTerms(), ui.matchWidth(8)) }
        guide.content.petTactics[pet.name]?.let { addTactics(it, opensAt = first) }
        addView(ui.flow().apply {
            addView(ui.chip(moves(pet)))
            addView(ui.chip(hits(pet)))
            addView(ui.chip(getString(R.string.guide_range, Guide.tiles(pet.range))))
            addView(ui.chip(getString(R.string.guide_max_level, item?.levelCount ?: 0)))
        }, ui.matchWidth(10))
        val heroes = guide.heroesFor(pet.name)
        if (heroes.isNotEmpty()) {
            addView(ui.text(getString(R.string.guide_good_with), 13f, bold = true), ui.matchWidth(12))
            addView(ui.flow().apply {
                for (hero in heroes) addView(namedChip(itemNamed(guide.facts.heroes, hero), hero))
            }, ui.matchWidth(4))
        }
    }
}
