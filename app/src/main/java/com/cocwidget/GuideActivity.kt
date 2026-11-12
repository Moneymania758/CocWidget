package com.cocwidget

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.guide.Guide
import com.cocwidget.store.Icons
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.Tab

// The way into the guide: the player's own Town Hall when there is one, then a tile per part.
// It works without a synced village.
class GuideActivity : GuideScreen() {
    override val tab = Tab.GUIDE

    private class Part(val title: Int, val line: String, val screen: Class<*>, val picture: String?, val icon: Int? = null)

    override fun buildGuide() {
        addTabTop()
        if (myTownHall >= Guide.FIRST_TOWN_HALL) content.addView(myTownHallCard(), ui.matchWidth(12))
        val byName = items.values.filter { it.village == "home" }.associateBy { it.name }
        fun pictureOf(name: String) = byName[name]?.let { Icons.path(it, 1) }
        val parts = listOf(
            Part(
                R.string.guide_town_halls, getString(R.string.guide_tile_town_halls, Guide.FIRST_TOWN_HALL, maxTownHall),
                GuideTownHallsActivity::class.java, Icons.townHallPath(maxOf(myTownHall, Guide.FIRST_TOWN_HALL)),
            ),
            Part(R.string.guide_heroes, getString(R.string.guide_tile_stats), GuideHeroesActivity::class.java, pictureOf("Barbarian King")),
            Part(R.string.guide_pets, getString(R.string.guide_tile_stats), GuidePetsActivity::class.java, pictureOf("L.A.S.S.I")),
            Part(R.string.guide_tile_pairings, getString(R.string.guide_tile_pairings_line), GuidePairingsActivity::class.java, pictureOf("Electro Owl")),
            Part(R.string.guide_army, getString(R.string.guide_tile_stats), GuideArmyActivity::class.java, pictureOf("Barbarian")),
            Part(R.string.guide_defences, getString(R.string.guide_tile_defences), GuideDefencesActivity::class.java, pictureOf("Cannon")),
            Part(R.string.guide_rush, getString(R.string.guide_tile_rush), GuideRushActivity::class.java, pictureOf("Builder's Hut")),
            Part(R.string.guide_glossary, getString(R.string.guide_tile_glossary), GuideGlossaryActivity::class.java, null, R.drawable.ic_book),
        )
        // Two to a row, sharing its width, each row as tall as its taller tile.
        for ((index, row) in parts.chunked(2).withIndex()) {
            content.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                isBaselineAligned = false
                for ((column, part) in row.withIndex()) {
                    addView(tile(part), ui.weighted(if (column == 0) 0 else 8).apply { height = LinearLayout.LayoutParams.MATCH_PARENT })
                }
                if (row.size == 1) addView(View(this@GuideActivity), ui.weighted(8))
            }, ui.matchWidth(if (index == 0) 12 else 8))
        }
        content.addView(sourcesButton(), ui.matchWidth(12))
        addNote(getString(R.string.credit))
    }

    // The player's own Town Hall, the guide's most used page, as the first thing on it.
    private fun myTownHallCard() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ui.rounded(ui.skin.card, 20f, ui.skin.divider, 1f)
        setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14))
        addView(ui.townHallBadge(myTownHall, ui.skin.accent, 72))
        addView(LinearLayout(this@GuideActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.text(getString(R.string.guide_your_town_hall), 12f, ui.skin.dim, bold = true))
            addView(ui.heading(getString(R.string.guide_town_hall_title, myTownHall), 22f, ui.skin.title))
            addView(ui.text(getString(R.string.guide_your_town_hall_line), 13f, ui.skin.dim))
        }, ui.weighted(14))
        addView(ui.icon(R.drawable.ic_chevron, ui.skin.dim, 18))
        ui.pressable(this, 20f) { open(GuideTownHallActivity::class.java) { putExtra(EXTRA_TOWN_HALL, myTownHall) } }
    }

    private fun tile(part: Part) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = ui.rounded(ui.skin.card, 18f, ui.skin.divider, 1f)
        setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
        minimumHeight = ui.dp(104)
        addView(LinearLayout(this@GuideActivity).apply {
            gravity = Gravity.CENTER
            background = ui.rounded(ui.skin.chip, 11f)
            if (part.icon != null) {
                addView(ui.icon(part.icon, ui.skin.accent, 22))
            } else {
                addView(ImageView(this@GuideActivity).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    pictures.add(part.picture, this)
                }, LinearLayout.LayoutParams(ui.dp(32), ui.dp(32)))
            }
        }, LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)))
        addView(View(this@GuideActivity), LinearLayout.LayoutParams(0, 0, 1f))
        addView(ui.text(getString(part.title), 14f, ui.skin.text, bold = true), ui.matchWidth(10))
        addView(ui.text(part.line, 12f, ui.skin.dim), ui.matchWidth(1))
        ui.pressable(this, 18f) { open(part.screen) }
    }

    // A quieter way to the sources, dashed so it reads as a note rather than a part of the guide.
    private fun sourcesButton() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = ui.dp(16).toFloat()
            setStroke(ui.dp(1f), ui.skin.dim, ui.dp(6).toFloat(), ui.dp(4).toFloat())
        }
        minimumHeight = ui.dp(56)
        setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10))
        addView(ui.text(getString(R.string.guide_sources), 14f, ui.skin.text, bold = true))
        addView(ui.text(getString(R.string.guide_intro), 12f, ui.skin.dim), ui.matchWidth(2))
        ui.pressable(this, 16f) { open(GuideSourcesActivity::class.java) }
    }
}

// Every Town Hall, four to a row, the player's own marked.
class GuideTownHallsActivity : GuideScreen() {
    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_town_halls))
        content.addView(ui.card().apply {
            // Four to a row, sharing its width, with blanks keeping a short last row in line.
            for ((index, row) in (Guide.FIRST_TOWN_HALL..maxTownHall).chunked(4).withIndex()) {
                addView(LinearLayout(this@GuideTownHallsActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    for (column in 0 until 4) {
                        val share = ui.weighted(if (column == 0) 0 else 8)
                        addView(row.getOrNull(column)?.let { townHallButton(it) } ?: View(this@GuideTownHallsActivity), share)
                    }
                }, ui.matchWidth(if (index == 0) 0 else 8))
            }
        }, ui.matchWidth(14))
    }

    // Its picture over its number, marked in the skin's main colour if it's the player's own.
    private fun townHallButton(townHall: Int) = LinearLayout(this).apply {
        val mine = townHall == myTownHall
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = ui.rounded(if (mine) ui.skin.primaryBg else ui.skin.chip, 10f)
        setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6))
        addView(ImageView(this@GuideTownHallsActivity).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            pictures.add(Icons.townHallPath(townHall), this)
        }, LinearLayout.LayoutParams(ui.dp(56), ui.dp(62)))
        addView(ui.text(getString(R.string.town_hall_chip, townHall), 13f, if (mine) ui.skin.primaryText else ui.skin.text, bold = true), ui.wrap(2))
        ui.pressable(this, 10f) { open(GuideTownHallActivity::class.java) { putExtra(EXTRA_TOWN_HALL, townHall) } }
    }
}

// Every guide the picks and tips came from. A tap opens it in the browser.
class GuideSourcesActivity : GuideScreen() {
    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_sources))
        addNote(getString(R.string.guide_sources_detail, guide.content.checked), 13f)
        content.addView(ui.card(0).apply {
            for ((index, source) in guide.content.sources.withIndex()) {
                if (index > 0) addView(ui.divider())
                addView(ui.row().apply {
                    addView(ui.text(getString(R.string.guide_source_row, source.name, source.date), 14f, ui.skin.accent))
                    ui.pressable(this, 0f) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                })
            }
        }, ui.matchWidth(14))
    }
}
