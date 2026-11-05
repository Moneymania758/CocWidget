package com.cocwidget

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import com.cocwidget.guide.Guide
import com.cocwidget.ui.GuideScreen

// The way into the guide. It works without a synced village, and offers the player's own Town Hall
// when there is one.
class GuideActivity : GuideScreen() {
    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_open))
        addNote(getString(R.string.guide_intro), 13f)
        if (myTownHall >= Guide.FIRST_TOWN_HALL) {
            content.addView(ui.button(getString(R.string.guide_my_town_hall, myTownHall), null, primary = true) { openTownHall(myTownHall) }, ui.matchWidth(14))
        }
        content.addView(ui.card().apply {
            addView(sectionTitle(getString(R.string.guide_town_halls)))
            addView(ui.flow(8).apply {
                for (townHall in Guide.FIRST_TOWN_HALL..maxTownHall) {
                    val mine = townHall == myTownHall
                    addView(ui.chip(
                        getString(R.string.town_hall_chip, townHall),
                        if (mine) ui.skin.primaryBg else ui.skin.chip,
                        if (mine) ui.skin.primaryText else ui.skin.text,
                    ).apply {
                        minHeight = ui.dp(44)
                        minWidth = ui.dp(56)
                        gravity = Gravity.CENTER
                        ui.pressable(this, 8f) { openTownHall(townHall) }
                    })
                }
            }, ui.matchWidth(10))
        }, ui.matchWidth(14))
        content.addView(ui.button(getString(R.string.guide_heroes), null, primary = false) { open(GuideHeroesActivity::class.java) }, ui.matchWidth(14))
        content.addView(ui.button(getString(R.string.guide_pets), null, primary = false) { open(GuidePetsActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.button(getString(R.string.guide_pairings), null, primary = false) { open(GuidePairingsActivity::class.java) }, ui.matchWidth(10))
        content.addView(ui.card(0).apply {
            addView(ui.row().apply {
                addView(sectionTitle(getString(R.string.guide_sources)))
                addView(ui.text(getString(R.string.guide_sources_detail, guide.content.checked), 13f, ui.skin.dim), ui.matchWidth(4))
            })
            for (source in guide.content.sources) {
                addView(ui.divider())
                addView(ui.row().apply {
                    addView(ui.text(getString(R.string.guide_source_row, source.name, source.date), 14f, ui.skin.accent))
                    ui.pressable(this, 0f) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                })
            }
        }, ui.matchWidth(14))
        addNote(getString(R.string.credit))
    }

    private fun openTownHall(townHall: Int) = open(GuideTownHallActivity::class.java) { putExtra(EXTRA_TOWN_HALL, townHall) }
}
