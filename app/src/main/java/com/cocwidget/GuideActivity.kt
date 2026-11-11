package com.cocwidget

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.guide.Guide
import com.cocwidget.store.Icons
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.Tab

// The way into the guide. It works without a synced village, and offers the player's own Town Hall
// when there is one.
class GuideActivity : GuideScreen() {
    override val tab = Tab.GUIDE

    override fun buildGuide() {
        content.addView(tabHeader(R.string.tab_guide))
        addNote(getString(R.string.guide_intro), 13f)
        if (myTownHall >= Guide.FIRST_TOWN_HALL) {
            content.addView(ui.button(getString(R.string.guide_my_town_hall, myTownHall), null, primary = true) { openTownHall(myTownHall) }, ui.matchWidth(14))
        }
        content.addView(ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_town_halls)))
            // Four to a row, sharing its width, with blanks keeping a short last row in line.
            for ((index, row) in (Guide.FIRST_TOWN_HALL..maxTownHall).chunked(4).withIndex()) {
                addView(LinearLayout(this@GuideActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    for (column in 0 until 4) {
                        val share = ui.weighted(if (column == 0) 0 else 8)
                        addView(row.getOrNull(column)?.let { townHallButton(it) } ?: View(this@GuideActivity), share)
                    }
                }, ui.matchWidth(if (index == 0) 10 else 8))
            }
        }, ui.matchWidth(14))
        content.addView(ui.button(getString(R.string.guide_heroes), null, primary = false) { open(GuideHeroesActivity::class.java) }, ui.matchWidth(14))
        content.addView(ui.button(getString(R.string.guide_pets), null, primary = false) { open(GuidePetsActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_army), null, primary = false) { open(GuideArmyActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_defences), null, primary = false) { open(GuideDefencesActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_pairings), null, primary = false) { open(GuidePairingsActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_rush), null, primary = false) { open(GuideRushActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_glossary), null, primary = false) { open(GuideGlossaryActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_sources), null, primary = false) { open(GuideSourcesActivity::class.java) }, ui.matchWidth(10))
        addNote(getString(R.string.credit))
    }

    // Its picture over its number, marked in the skin's main colour if it's the player's own.
    private fun townHallButton(townHall: Int) = LinearLayout(this).apply {
        val mine = townHall == myTownHall
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = ui.rounded(if (mine) ui.skin.primaryBg else ui.skin.chip, 10f)
        setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6))
        addView(ImageView(this@GuideActivity).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            pictures.add(Icons.townHallPath(townHall), this)
        }, LinearLayout.LayoutParams(ui.dp(56), ui.dp(62)))
        addView(ui.text(getString(R.string.town_hall_chip, townHall), 13f, if (mine) ui.skin.primaryText else ui.skin.text, bold = true), ui.wrap(2))
        ui.pressable(this, 10f) { openTownHall(townHall) }
    }

    private fun openTownHall(townHall: Int) = open(GuideTownHallActivity::class.java) { putExtra(EXTRA_TOWN_HALL, townHall) }
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
