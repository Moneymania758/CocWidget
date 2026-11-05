package com.cocwidget

import android.widget.LinearLayout
import com.cocwidget.guide.Guide
import com.cocwidget.guide.HeroGuide
import com.cocwidget.guide.PetPick
import com.cocwidget.ui.GuideScreen

// Which pet with which hero, and a full set that uses each pet once.
class GuidePairingsActivity : GuideScreen() {
    // Pets start at TH14, so a village below that sees the set for the top Town Hall.
    private val setTownHall get() = if (myTownHall >= PET_HOUSE_TOWN_HALL) myTownHall else maxTownHall

    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_pairings))
        addNote(getString(R.string.guide_pairings_intro, guide.content.checked), 13f)
        content.addView(setCard(), ui.matchWidth(12))
        for (fighter in guide.facts.heroes) {
            guide.heroGuide(fighter.name)?.let { content.addView(heroCard(it), ui.matchWidth(14)) }
        }
    }

    private fun setCard() = ui.card().apply {
        addView(sectionTitle(getString(R.string.guide_set_title, setTownHall)))
        addView(ui.text(getString(R.string.guide_set_detail), 13f, ui.skin.dim), ui.matchWidth(4))
        for ((hero, pet) in guide.petSet(setTownHall, items)) {
            addView(titleRow(itemNamed(guide.facts.heroes, hero), hero, pet ?: getString(R.string.guide_set_spare)), ui.matchWidth(8))
        }
    }

    private fun heroCard(hero: HeroGuide) = ui.card().apply {
        addView(titleRow(itemNamed(guide.facts.heroes, hero.hero), hero.hero, ""))
        for ((index, pick) in hero.picks.withIndex()) addView(pickRow(pick, index == 0), ui.matchWidth(10))
        for (pick in hero.avoid) {
            addView(ui.text(getString(R.string.guide_avoid, pick.pet, pick.why), 14f, ui.skin.accent), ui.matchWidth(10))
        }
        addView(ui.text(hero.split, 13f, ui.skin.dim), ui.matchWidth(10))
    }

    private fun pickRow(pick: PetPick, first: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        val item = itemNamed(guide.facts.pets, pick.pet)
        val unlocks = item?.let(Guide::firstTownHall) ?: 0
        addView(picture(item, 36))
        addView(LinearLayout(this@GuidePairingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            val title = if (first) getString(R.string.guide_first_pick, pick.pet) else pick.pet
            addView(ui.text(title, 14f, bold = true))
            addView(ui.text(pick.why, 13f, ui.skin.dim))
            val locked = when {
                myTownHall == 0 -> getString(R.string.guide_from_town_hall, unlocks)
                myTownHall < unlocks -> getString(R.string.guide_not_yet, unlocks)
                else -> ""
            }
            if (locked.isNotEmpty()) addView(ui.text(locked, 12f, ui.skin.dim))
        }, ui.weighted(10))
    }

    companion object {
        private const val PET_HOUSE_TOWN_HALL = 14
    }
}
