package com.cocwidget

import android.widget.LinearLayout
import com.cocwidget.guide.Equipment
import com.cocwidget.guide.Fighter
import com.cocwidget.guide.Guide
import com.cocwidget.ui.GuideScreen
import java.text.NumberFormat

// Every hero, or just one when opened from a Town Hall.
class GuideHeroesActivity : GuideScreen() {
    override fun buildGuide() {
        val only = intent.getIntExtra(EXTRA_ID, 0)
        val heroes = guide.facts.heroes.filter { only == 0 || it.id == only }
        val single = heroes.singleOrNull().takeIf { only != 0 }
        content.addView(if (single != null) backHeader(single.name) else backHeader(R.string.guide_heroes))
        for (hero in heroes) content.addView(heroCard(hero), ui.matchWidth(14))
        if (single != null) {
            content.addView(ui.button(getString(R.string.guide_pairings), null, primary = false) { open(GuidePairingsActivity::class.java) }, ui.matchWidth(14))
        }
        addNote(getString(R.string.guide_picks_note, guide.content.checked))
    }

    private fun heroCard(hero: Fighter) = ui.card().apply {
        freshTerms()
        val item = items[hero.id]
        val first = item?.let(Guide::firstTownHall) ?: 0
        val hall = item?.requiredLevelFor(1) ?: 0
        addView(titleRow(item, hero.name, getString(R.string.guide_hero_unlock, first, hall)))
        val mine = myLevels[hero.id]
        if (mine != null) {
            val cap = item?.maxLevelAt(myTownHall) ?: 0
            addView(ui.text(getString(R.string.guide_you_level, mine, cap, myTownHall), 14f, ui.skin.accent, bold = true), ui.matchWidth(8))
        }
        val written = guide.heroGuide(hero.name)
        written?.let { addView(ui.text(it.role, 14f).withTerms(), ui.matchWidth(8)) }
        guide.content.heroTactics[hero.name]?.let { addTactics(it, opensAt = first) }
        addView(ui.flow().apply {
            addView(ui.chip(moves(hero)))
            addView(ui.chip(hits(hero)))
            addView(ui.chip(getString(R.string.guide_range, Guide.tiles(hero.range))))
            addView(ui.chip(item?.resource.orEmpty()))
        }, ui.matchWidth(10))
        val number = NumberFormat.getIntegerInstance()
        addView(ui.text(getString(R.string.guide_top_stats, item?.levelCount ?: 0, number.format(hero.hitpoints), number.format(hero.dps)), 13f, ui.skin.dim), ui.matchWidth(8))
        addView(ui.button(getString(R.string.guide_hero_charts), null, primary = false) { openUnit(hero.id) }, ui.matchWidth(8))
        addView(ui.text(getString(R.string.guide_caps), 13f, bold = true), ui.matchWidth(10))
        addView(ui.flow(4).apply {
            var last = 0
            for (townHall in 1..maxTownHall) {
                val cap = item?.maxLevelAt(townHall) ?: 0
                if (cap != last) {
                    val colours = if (townHall == myTownHall) ui.skin.primaryBg to ui.skin.primaryText else ui.skin.chip to ui.skin.text
                    addView(ui.chip(getString(R.string.guide_cap_chip, townHall, cap), colours.first, colours.second))
                    last = cap
                }
            }
        }, ui.matchWidth(4))
        written?.let { guideFor ->
            addView(ui.text(getString(R.string.guide_best_pets), 13f, bold = true), ui.matchWidth(12))
            addView(ui.flow().apply {
                for (pick in guideFor.picks) addView(namedChip(itemNamed(guide.facts.pets, pick.pet), pick.pet))
            }, ui.matchWidth(4))
            val pieces = guide.facts.equipment.filter { items[it.id]?.hero == hero.id }
            val usual = guideFor.gear.mapNotNull { name -> pieces.firstOrNull { it.name == name } }
            addView(ui.text(getString(R.string.guide_gear), 13f, bold = true), ui.matchWidth(12))
            addView(ui.text(getString(R.string.guide_gear_from), 13f, ui.skin.dim), ui.matchWidth(2))
            for (piece in usual) addView(gearRow(piece), ui.matchWidth(8))
            val others = pieces - usual.toSet()
            if (others.isNotEmpty()) {
                addView(ui.text(getString(R.string.guide_gear_other), 13f, bold = true), ui.matchWidth(14))
                for (piece in others) addView(gearRow(piece), ui.matchWidth(8))
            }
            addSources(pieces.flatMap { guide.content.equipmentTips[it.name]?.sources.orEmpty() }.distinct())
        }
    }

    // A piece and what it's for. A tap opens its charts.
    private fun gearRow(piece: Equipment) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(unitChip(piece.id, piece.name), ui.wrap())
        guide.content.equipmentTips[piece.name]?.let { written ->
            val locked = viewTownHall > 0 && written.tip.isLaterThan(viewTownHall)
            addView(ui.text(tipText(written.tip, viewTownHall), 14f, if (locked) ui.skin.dim else ui.skin.text).withTerms(), ui.matchWidth(3))
        }
    }
}
