package com.cocwidget.acceptance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TableRow
import com.cocwidget.GuideHeroesActivity
import com.cocwidget.GuideTownHallActivity
import com.cocwidget.GuideUnitActivity
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.guide.ChartScale
import com.cocwidget.guide.GuideUnit
import com.cocwidget.guide.GuideUnits
import com.cocwidget.plan.PlanFormat
import com.cocwidget.store.Store
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.LevelChart
import com.cocwidget.ui.Skin
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Every hero and piece of equipment on its real page, for players at different points in the game.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HeroChartsTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val units get() = GuideUnits.load(app)
    private val pages get() = units.values.filter { it.kind == "hero" || it.kind == "equipment" }

    private fun unit(name: String) = units.values.first { it.name == name }

    private fun open(unit: GuideUnit) = Screens.open<GuideUnitActivity> { putExtra(GuideScreen.EXTRA_ID, unit.id) }

    private fun texts(unit: GuideUnit) = Screens.texts(Screens.root(open(unit)))

    // A village at this Town Hall with just these levels, by id. Anything else it doesn't have.
    private fun syncVillage(townHall: Int, levels: Map<Int, Int> = emptyMap(), equipment: Map<Int, Int> = emptyMap()) {
        fun section(entries: Map<Int, Int>) = JSONArray(entries.map { (id, level) -> JSONObject().put("data", id).put("lvl", level) })
        val export = JSONObject()
            .put("tag", "#TEST0000")
            .put("timestamp", 1793625123)
            .put("buildings", section(mapOf(TOWN_HALL to townHall) + levels.filterKeys { it !in heroIds }))
            .put("heroes", section(levels.filterKeys { it in heroIds }))
            .put("equipment", section(equipment))
        Store.setSelected(app, Store.saveExport(app, export.toString()))
    }

    private val heroIds get() = units.values.filter { it.kind == "hero" }.map { it.id }.toSet()

    private fun cap(unit: GuideUnit, townHall: Int) = unit.levels[unit.capIndex(townHall)]

    private fun assertNoBrokenText(where: String, texts: List<String>) {
        for (text in texts) {
            for (bad in listOf("null", "NaN", "Infinity", "%1\$", "%2\$", "%d", "%s")) {
                assertFalse("$where shows \"$text\"", text.contains(bad))
            }
        }
    }

    private fun drawCharts(activity: GuideUnitActivity) {
        val canvas = Canvas(Bitmap.createBitmap(1080, 800, Bitmap.Config.ARGB_8888))
        for (chart in Screens.views<LevelChart>(Screens.root(activity))) chart.draw(canvas)
    }

    @Test
    fun everyPageShowsWhatItsDataHasInEverySkin() {
        for (skin in Skin.ALL) {
            Store.setSkin(app, skin.key)
            for (unit in pages) {
                val screen = open(unit)
                val root = Screens.root(screen)
                val texts = Screens.texts(root)
                val where = "${unit.name} in ${skin.key}"
                assertEquals(where, unit.charts.size, Screens.views<LevelChart>(root).size)
                assertEquals(where, unit.levels.size + 1, Screens.views<TableRow>(root).size)
                for (chart in unit.charts) assertTrue("$where: ${chart.title}", texts.contains(chart.title))
                assertNoBrokenText(where, texts)
                drawCharts(screen)
            }
        }
    }

    @Test
    fun everyPageOpensOnItsTopLevelBeforeASync() {
        for (unit in pages) {
            val texts = texts(unit)
            assertTrue(unit.name, texts.contains(app.getString(R.string.guide_level, unit.levels.last())))
            val level = app.getString(R.string.guide_level_short, unit.levels.last())
            for (chart in unit.charts) {
                // More than one line reads out through its legend.
                val readouts = if (chart.series.size == 1) {
                    listOf(app.getString(R.string.guide_level_value, level, ChartScale.value(chart.series.single().values.last()!!, chart.unit)))
                } else {
                    chart.series.map { app.getString(R.string.guide_stat, it.name, ChartScale.value(it.values.last()!!, chart.unit)) }
                }
                for (readout in readouts) assertTrue("${unit.name}: ${chart.title} should read $readout", texts.contains(readout))
            }
        }
    }

    @Test
    fun everyPageWorksAtEveryTownHall() {
        for (townHall in 1..18) {
            syncVillage(townHall)
            for (unit in pages) {
                val texts = texts(unit)
                val where = "${unit.name} at TH$townHall"
                assertNoBrokenText(where, texts)
                val status = if (unit.townHall > townHall) {
                    app.getString(R.string.guide_not_yet, unit.townHall)
                } else {
                    app.getString(R.string.guide_cap_at, cap(unit, townHall), townHall)
                }
                assertTrue("$where should say $status", texts.contains(status))
            }
        }
    }

    // The guide says the same: he stays at level 1 until TH7.
    @Test
    fun theKingOpensAtTownHall4AndWaitsAtLevel1UntilTownHall7() {
        val king = unit("Barbarian King")
        assertEquals(4, king.townHall)
        assertEquals(1, cap(king, 6))
        assertEquals(10, cap(king, 7))
    }

    @Test
    fun aPieceForAHeroNotOpenYetSaysWhenItWill() {
        syncVillage(9)
        assertTrue(texts(unit("Eternal Tome")).contains(app.getString(R.string.guide_not_yet, 11)))
        assertTrue(texts(unit("Royal Gem")).contains(app.getString(R.string.guide_not_yet, 13)))
        assertTrue(texts(unit("Flame Blower")).contains(app.getString(R.string.guide_not_yet, 15)))
    }

    @Test
    fun aCommonPieceCountsAsOwnedOnlyWithItsHeroAndTheBlacksmith() {
        val puppet = unit("Barbarian Puppet")
        val king = unit("Barbarian King")
        val blacksmith = BLACKSMITH to 1
        val atLevel1 = mapOf(puppet.id to 1)
        val yours = app.getString(R.string.guide_you_level, 1, cap(puppet, 9), 9)

        syncVillage(9, mapOf(king.id to 20), atLevel1)
        assertFalse(texts(puppet).contains(yours))
        syncVillage(9, mapOf(blacksmith), atLevel1)
        assertFalse(texts(puppet).contains(yours))
        syncVillage(9, mapOf(king.id to 20, blacksmith), atLevel1)
        assertTrue(texts(puppet).contains(yours))
    }

    @Test
    fun anEpicCountsAsOwnedOnceItIsPastLevel1() {
        val ball = unit("Spiky Ball")
        val king = unit("Barbarian King")
        syncVillage(16, mapOf(king.id to 60, BLACKSMITH to 7), mapOf(ball.id to 2))
        assertTrue(texts(ball).contains(app.getString(R.string.guide_you_level, 2, cap(ball, 16), 16)))
    }

    @Test
    fun aHeroOnItsOwnLevelOpensThere() {
        val queen = unit("Archer Queen")
        syncVillage(12, mapOf(queen.id to 40))
        val texts = texts(queen)
        assertTrue(texts.contains(app.getString(R.string.guide_you_level, 40, cap(queen, 12), 12)))
        assertTrue(texts.contains(app.getString(R.string.guide_level, 40)))
    }

    // A level the data doesn't have, as after a game update before the app catches up.
    @Test
    fun aHeroPastTheDataStillOpens() {
        val king = unit("Barbarian King")
        syncVillage(18, mapOf(king.id to king.levels.last() + 5))
        val screen = open(king)
        val texts = Screens.texts(Screens.root(screen))
        assertTrue(texts.contains(app.getString(R.string.guide_cap_at, cap(king, 18), 18)))
        drawCharts(screen)
    }

    private fun pickAcross(screen: GuideUnitActivity, share: Float) {
        val chart = Screens.views<LevelChart>(Screens.root(screen)).first()
        val y = chart.height / 2f
        val x = chart.width * share
        Screens.touch(chart, MotionEvent.ACTION_DOWN, x, y)
        Screens.touch(chart, MotionEvent.ACTION_UP, x, y)
    }

    @Test
    fun pickingAcrossAHeroChartMovesEveryChartAndTheTable() {
        val king = unit("Barbarian King")
        val screen = open(king)
        pickAcross(screen, 0f)
        val root = Screens.root(screen)
        val texts = Screens.texts(root)
        assertTrue(texts.contains(app.getString(R.string.guide_level, 1)))
        for (chart in king.charts) {
            val value = ChartScale.value(chart.series.first().values.first()!!, chart.unit)
            assertTrue(chart.title, texts.contains(app.getString(R.string.guide_level_value, app.getString(R.string.guide_level_short, 1), value)))
        }
        for (chart in Screens.views<LevelChart>(root)) assertEquals(0, chart.selected)
        val picked = Screens.views<TableRow>(root).filter { it.isSelected }
        assertEquals(1, picked.size)
        assertEquals("1", Screens.texts(picked.single()).first())

        pickAcross(screen, 1f)
        assertTrue(Screens.texts(root).contains(app.getString(R.string.guide_level, 110)))
        for (chart in Screens.views<LevelChart>(root)) assertEquals(109, chart.selected)
    }

    @Test
    fun tappingATableRowMovesTheCharts() {
        val queen = unit("Archer Queen")
        val screen = open(queen)
        val root = Screens.root(screen)
        // The first row is the headers.
        Screens.views<TableRow>(root)[50].performClick()
        assertTrue(Screens.texts(root).contains(app.getString(R.string.guide_level, 50)))
        for (chart in Screens.views<LevelChart>(root)) assertEquals(49, chart.selected)
    }

    @Test
    fun aHeroLevelSaysWhatItCosts() {
        val king = unit("Barbarian King")
        val screen = open(king)
        val root = Screens.root(screen)
        Screens.views<TableRow>(root)[2].performClick()
        val needs = app.getString(R.string.guide_needs_town_hall, 7) +
            " · Dark Elixir ${PlanFormat.amount(TestData.gameData.getValue(king.id).costToReach(2))} · ${PlanFormat.duration(king.times[1])}"
        assertTrue(Screens.texts(root).toString(), Screens.texts(root).contains(needs))

        // The first level comes with the hero, so it has no cost to show.
        Screens.views<TableRow>(root)[1].performClick()
        assertTrue(Screens.texts(root).contains(app.getString(R.string.guide_needs_town_hall, 4)))
    }

    @Test
    fun aPieceLevelSaysOnlyItsTownHall() {
        val screen = open(unit("Barbarian Puppet"))
        val root = Screens.root(screen)
        Screens.views<TableRow>(root)[18].performClick()
        assertTrue(Screens.texts(root).contains(app.getString(R.string.guide_needs_town_hall, 14)))
    }

    @Test
    fun turningThePhoneKeepsThePickedLevel() {
        val king = unit("Barbarian King")
        val intent = android.content.Intent(app, GuideUnitActivity::class.java).putExtra(GuideScreen.EXTRA_ID, king.id)
        val first = Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup()
        Screens.layOut(first.get())
        Screens.views<TableRow>(Screens.root(first.get()))[30].performClick()
        val state = Bundle()
        first.saveInstanceState(state)
        val second = Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup(state).get()
        assertTrue(Screens.texts(Screens.root(second)).contains(app.getString(R.string.guide_level, 30)))
        for (chart in Screens.views<LevelChart>(Screens.root(second))) assertEquals(29, chart.selected)
    }

    @Test
    fun aScreenReaderHearsWhatAChartShows() {
        val root = Screens.root(open(unit("Barbarian King")))
        val descriptions = Screens.views<LevelChart>(root).map { it.contentDescription.toString() }
        assertTrue(descriptions.toString(), descriptions.any { it.startsWith("Health.") && it.contains("13,350") && it.contains("1,445") })
        assertTrue(descriptions.toString(), descriptions.any { it.startsWith("Damage per second.") && it.contains("713") })
    }

    @Test
    fun aTownHallPageReachesAHeroesChartsThroughItsCard() {
        val champion = unit("Royal Champion")
        val townHallPage = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 13) }
        Screens.tappable(Screens.root(townHallPage), champion.name)!!.performClick()
        val card = shadowOf(townHallPage).nextStartedActivity
        assertEquals(GuideHeroesActivity::class.java.name, card.component?.className)
        assertEquals(champion.id, card.getIntExtra(GuideScreen.EXTRA_ID, 0))

        val heroes = Screens.open<GuideHeroesActivity> { putExtra(GuideScreen.EXTRA_ID, champion.id) }
        Screens.tappable(Screens.root(heroes), app.getString(R.string.guide_hero_charts))!!.performClick()
        val charts = shadowOf(heroes).nextStartedActivity
        assertEquals(GuideUnitActivity::class.java.name, charts.component?.className)
        assertEquals(champion.id, charts.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    @Test
    fun aHeroCardListsEveryPieceAndEachOpens() {
        for (hero in units.values.filter { it.kind == "hero" }) {
            val screen = Screens.open<GuideHeroesActivity> { putExtra(GuideScreen.EXTRA_ID, hero.id) }
            val pieces = pages.filter { it.kind == "equipment" && TestData.gameData.getValue(it.id).hero == hero.id }
            assertTrue(hero.name, pieces.isNotEmpty())
            for (piece in pieces) {
                val chip = Screens.tappable(Screens.root(screen), piece.name)
                assertTrue("${hero.name}: ${piece.name}", chip != null)
                chip!!.performClick()
                assertEquals(piece.id, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
            }
        }
    }

    @Test
    fun aHeroPageListsEveryPieceAndEachOpens() {
        for (hero in units.values.filter { it.kind == "hero" }) {
            val screen = open(hero)
            for (piece in pages.filter { it.kind == "equipment" && TestData.gameData.getValue(it.id).hero == hero.id }) {
                val chip = Screens.tappable(Screens.root(screen), piece.name)
                assertTrue("${hero.name}: ${piece.name}", chip != null)
                chip!!.performClick()
                assertEquals(piece.id, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
            }
        }
    }

    private companion object {
        const val TOWN_HALL = 1000001
        const val BLACKSMITH = 1000070
    }
}
