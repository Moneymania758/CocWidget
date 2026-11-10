package com.cocwidget

import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import com.cocwidget.guide.Guide
import com.cocwidget.guide.MergeNeed
import com.cocwidget.guide.RushBand
import com.cocwidget.guide.RushGuide
import com.cocwidget.guide.Tip
import com.cocwidget.plan.PlanFormat
import com.cocwidget.ui.GuideScreen

// How to rush: the rules, what to upgrade in order, advice for the Town Hall being viewed, and what
// the next one needs merged. The merges and the heroes' longest upgrades come from the game data, so
// they follow a new Town Hall without new text.
class GuideRushActivity : GuideScreen() {
    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_rush))
        val rush = guide.content.rushing ?: return
        val here = viewTownHall
        if (maxTownHall > rush.writtenFor) addNote(getString(R.string.guide_rush_stale, maxTownHall), 13f)
        content.addView(ui.text(rush.about, 14f, ui.skin.dim).withTerms(), ui.matchWidth(10).apply { marginStart = ui.dp(4) })
        content.addView(bulletCard(R.string.guide_rush_careful, rush.careful), ui.matchWidth(14))
        content.addView(bulletCard(R.string.guide_rush_rules, rush.rules), ui.matchWidth(14))
        content.addView(orderCard(rush, here), ui.matchWidth(14))
        content.addView(bulletCard(R.string.guide_rush_upgrade_now, rush.upgradeNow), ui.matchWidth(14))
        for (band in rush.bandsFor(here)) content.addView(bandCard(rush, band, here), ui.matchWidth(14))
        mergeCard(here)?.let { content.addView(it, ui.matchWidth(14)) }
        content.addView(ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_rush_equipment)))
            addTipLines(rush.equipment, here)
        }, ui.matchWidth(14))
        heroCard(rush, here)?.let { content.addView(it, ui.matchWidth(14)) }
        content.addView(ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_rush_items)))
            addTipLines(rush.items, here)
        }, ui.matchWidth(14))
        creditCard(rush)?.let { content.addView(it, ui.matchWidth(14)) }
    }

    private fun bulletCard(title: Int, lines: List<String>) = ui.card().apply {
        freshTerms()
        addView(sectionTitle(getString(title)))
        for ((index, line) in lines.withIndex()) {
            addView(ui.text(getString(R.string.guide_tip, line), 14f).withTerms(), ui.matchWidth(if (index == 0) 8 else 4))
        }
    }

    private fun orderCard(rush: RushGuide, here: Int) = ui.card().apply {
        freshTerms()
        addView(sectionTitle(getString(R.string.guide_rush_order)))
        for ((index, tip) in Guide.tipsAt(rush.order, here).withIndex()) {
            val colour = if (here > 0 && tip.isLaterThan(here)) ui.skin.dim else ui.skin.text
            addView(ui.text(getString(R.string.guide_step, index + 1, tipText(tip, here)), 14f, colour).withTerms(), ui.matchWidth(if (index == 0) 8 else 4))
        }
    }

    // Lines that apply now first, later ones dimmed and labelled, past ones left out.
    private fun LinearLayout.addTipLines(tips: List<Tip>, here: Int) {
        freshTerms()
        for ((index, tip) in Guide.tipsAt(tips, here).withIndex()) {
            val colour = if (here > 0 && tip.isLaterThan(here)) ui.skin.dim else ui.skin.text
            addView(ui.text(getString(R.string.guide_tip, tipText(tip, here)), 14f, colour).withTerms(), ui.matchWidth(if (index == 0) 8 else 4))
        }
    }

    private fun bandCard(rush: RushGuide, band: RushBand, here: Int) = ui.card().apply {
        val title = when {
            here > 0 -> getString(R.string.guide_rush_at, here)
            band.top && maxTownHall > rush.writtenFor -> getString(R.string.guide_rush_band_up, rush.writtenFor)
            band.top -> getString(R.string.guide_rush_band_one, rush.writtenFor)
            band.from == band.to -> getString(R.string.guide_rush_band_one, band.from)
            else -> getString(R.string.guide_rush_band, band.from, band.to)
        }
        addView(sectionTitle(title))
        addTipLines(band.lines, here)
    }

    // What the next Town Hall takes in or adds as merged defences. With no Town Hall known, every
    // Town Hall that has any.
    private fun mergeCard(here: Int): LinearLayout? {
        val next = if (here > 0) listOf(here + 1) else (Guide.FIRST_TOWN_HALL + 1..maxTownHall).toList()
        val needs = next.associateWith { Guide.mergesFor(it, items) }.filterValues { it.isNotEmpty() }
        if (needs.isEmpty()) return null
        return ui.card().apply {
            for ((index, entry) in needs.entries.withIndex()) {
                addView(sectionTitle(getString(R.string.guide_rush_before, entry.key)), ui.matchWidth(if (index == 0) 0 else 14))
                for (need in entry.value) addView(mergeRow(entry.key, need), ui.matchWidth(8))
            }
        }
    }

    private fun mergeRow(townHall: Int, need: MergeNeed): LinearLayout {
        val from = need.sources.groupingBy { it }.eachCount().entries.joinToString(getString(R.string.guide_rush_and)) { (source, count) ->
            getString(R.string.guide_rush_source, withQuantity(source.first.name, count), source.second)
        }
        // The Town Hall's own merge is shown as the building it takes in, which is what needs the work.
        val shown = if (need.intoTownHall) need.sources.firstOrNull()?.first ?: need.item else need.item
        val line = when {
            need.intoTownHall -> getString(R.string.guide_rush_takes_in, townHall, need.sources.firstOrNull()?.second ?: 0)
            need.quantity > 1 -> getString(R.string.guide_rush_made_from_each, from)
            else -> getString(R.string.guide_rush_made_from, from)
        }
        val page = unitId(shown.name)
        return titleRow(shown, withQuantity(shown.name, if (need.intoTownHall) 1 else need.quantity), line, 36).apply {
            if (page > 0) ui.pressable(this, 8f) { openUnit(page) }
        }
    }

    private fun withQuantity(name: String, quantity: Int) = if (quantity > 1) getString(R.string.guide_quantity, name, quantity) else name

    // Where each hero's longest upgrades start, and with a synced village, how far off each is.
    private fun heroCard(rush: RushGuide, here: Int): LinearLayout? {
        val heroes = guide.facts.heroes.mapNotNull { fighter -> items[fighter.id]?.let { fighter to it } }
        val longest = Guide.longestHeroUpgrade(heroes.map { it.second })
        val stretches = heroes.mapNotNull { (fighter, item) -> Guide.stretch(fighter.id, item, fighter.name, longest)?.let { Triple(fighter, item, it) } }
        if (stretches.isEmpty()) return null
        val days = (longest / DAY).toInt()
        val mine = here > 0 && here == myTownHall
        val progress = if (mine) {
            stretches.mapNotNull { (fighter, item, stretch) ->
                myLevels[fighter.id]?.takeIf { it > 0 }?.let { fighter.id to Guide.progress(item, it, here, stretch) }
            }.toMap()
        } else {
            emptyMap()
        }
        // The hero to push next: still something to do at this Town Hall, and the least of it.
        val closest = progress.filterValues { it.left > 0 && !it.maxedHere }.minByOrNull { it.value.left }?.key
        return ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_rush_heroes, days)))
            addTipLines(rush.heroes, here)
            for ((fighter, item, stretch) in stretches) {
                val facts = resources.getQuantityString(R.plurals.guide_rush_stretch, stretch.count, stretch.count, stretch.firstLevel, stretch.townHall)
                val level = myLevels[fighter.id]
                val mineNow = progress[fighter.id]
                val line = when {
                    mineNow == null || level == null -> facts
                    mineNow.maxedHere -> getString(R.string.guide_rush_yours_maxed, facts, level, here)
                    mineNow.left == 0L -> getString(R.string.guide_rush_yours_there, facts, level)
                    mineNow.someLocked -> getString(R.string.guide_rush_yours_to_go_locked, facts, level, PlanFormat.duration(mineNow.left))
                    else -> getString(R.string.guide_rush_yours_to_go, facts, level, PlanFormat.duration(mineNow.left))
                }
                addView(titleRow(item, fighter.name, line, 36).apply {
                    if (fighter.id == closest) addView(ui.chip(getString(R.string.guide_rush_closest), ui.skin.primaryBg, ui.skin.primaryText))
                    ui.pressable(this, 8f) { open(GuideHeroesActivity::class.java) { putExtra(EXTRA_ID, fighter.id) } }
                }, ui.matchWidth(10))
            }
            val books = stretches.sumOf { it.third.count }
            addView(ui.text(resources.getQuantityString(R.plurals.guide_rush_books, books, books, days), 13f, ui.skin.dim), ui.matchWidth(10))
        }
    }

    private fun creditCard(rush: RushGuide): LinearLayout? {
        val source = rush.sources.firstNotNullOfOrNull { key -> guide.content.sources.firstOrNull { it.key == key } } ?: return null
        return ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_rush_where)))
            addView(ui.text(getString(R.string.guide_rush_credit, source.name, source.date), 14f, ui.skin.dim), ui.matchWidth(8))
            addView(ui.button(getString(R.string.guide_rush_read), null, primary = false) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)))
            }, ui.matchWidth(10))
        }
    }

    private companion object {
        const val DAY = 86_400L
    }
}
