package com.cocwidget.acceptance

import android.view.MotionEvent
import com.cocwidget.GuideActivity
import com.cocwidget.GuideArmyActivity
import com.cocwidget.GuideDefencesActivity
import com.cocwidget.GuideHeroesActivity
import com.cocwidget.GuidePairingsActivity
import com.cocwidget.GuidePetsActivity
import com.cocwidget.GuideTownHallActivity
import com.cocwidget.GuideUnitActivity
import com.cocwidget.TestData
import com.cocwidget.guide.Guide
import com.cocwidget.guide.GuideUnits
import com.cocwidget.store.Store
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.LevelChart
import com.cocwidget.ui.Skin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// What a player sees and taps in the guide, on the real screens with the real data. These stand in
// for most of the clicking on an emulator. How things look still needs a device.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GuideScreensTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val units get() = GuideUnits.load(app)
    private val guide get() = Guide.load(app)
    private val skins = listOf(Skin.WIDGET, Skin.CLEAN, Skin.GAME)

    private fun unitId(name: String) = units.values.first { it.name == name }.id

    private fun openUnit(name: String) = Screens.open<GuideUnitActivity> { putExtra(GuideScreen.EXTRA_ID, unitId(name)) }

    private fun syncTh16() {
        val tag = Store.saveExport(app, TestData.exportJson())
        Store.setSelected(app, tag)
    }

    @Test
    fun everyGuideScreenOpensInEverySkin() {
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            Screens.open<GuideActivity>()
            Screens.open<GuideArmyActivity>()
            Screens.open<GuideHeroesActivity>()
            Screens.open<GuidePetsActivity>()
            Screens.open<GuidePairingsActivity>()
            for (townHall in Guide.FIRST_TOWN_HALL..18) {
                val page = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, townHall) }
                assertTrue("TH$townHall in ${skin.key}", Screens.texts(Screens.root(page)).contains("Town Hall $townHall"))
            }
        }
    }

    @Test
    fun everyTroopAndSpellPageOpensInEverySkinWithItsCharts() {
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            for (unit in units.values) {
                val intent = android.content.Intent(app, GuideUnitActivity::class.java).putExtra(GuideScreen.EXTRA_ID, unit.id)
                val controller = org.robolectric.Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup()
                val root = Screens.root(controller.get())
                val where = "${unit.name} in ${skin.key}"
                assertTrue(where, Screens.texts(root).contains(unit.name))
                val expected = unit.charts.size + unit.subs.sumOf { it.charts.size }
                assertEquals(where, expected, Screens.views<LevelChart>(root).size)
                controller.pause().stop().destroy()
            }
        }
    }

    @Test
    fun everyAttackChipOpensItsOwnPage() {
        for (page in guide.content.townHalls) {
            val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, page.townHall) }
            val root = Screens.root(screen)
            for (strategy in page.strategies) for (name in strategy.army) {
                val where = "TH${page.townHall} ${strategy.name}: $name"
                val chip = Screens.tappable(root, name)
                assertNotNull(where, chip)
                chip!!.performClick()
                val started = shadowOf(screen).nextStartedActivity
                assertEquals(where, GuideUnitActivity::class.java.name, started.component?.className)
                assertEquals(where, unitId(name), started.getIntExtra(GuideScreen.EXTRA_ID, 0))
            }
        }
    }

    @Test
    fun aHeroRowInAnAttackOpensThatHero() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 18) }
        Screens.tappable(Screens.root(screen), "Dragon Duke")!!.performClick()
        val started = shadowOf(screen).nextStartedActivity
        assertEquals(GuideHeroesActivity::class.java.name, started.component?.className)
        assertEquals(guide.facts.heroes.first { it.name == "Dragon Duke" }.id, started.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    @Test
    fun everyChipInTheTroopsAndSpellsListOpensItsPage() {
        val screen = Screens.open<GuideArmyActivity>()
        for (unit in units.values.filter { it.group !in setOf("defence", "trap") }) {
            val chip = Screens.tappable(Screens.root(screen), unit.name)
            assertNotNull(unit.name, chip)
            chip!!.performClick()
            assertEquals(unit.name, unit.id, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
        }
    }

    @Test
    fun everyChipInTheDefencesListOpensItsPage() {
        val screen = Screens.open<GuideDefencesActivity>()
        val defences = units.values.filter { it.group == "defence" || it.group == "trap" }
        assertTrue(defences.size > 25)
        for (unit in defences) {
            val chip = Screens.tappable(Screens.root(screen), unit.name)
            assertNotNull(unit.name, chip)
            chip!!.performClick()
            assertEquals(unit.name, unit.id, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
        }
    }

    @Test
    fun aNewDefenceOnATownHallPageOpensItsPage() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 10) }
        Screens.tappable(Screens.root(screen), "Inferno Tower ×2")!!.performClick()
        assertEquals(unitId("Inferno Tower"), shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    @Test
    fun aDefencePageShowsWhatAnAttackerNeeds() {
        val texts = Screens.texts(Screens.root(openUnit("Multi-Archer Tower")))
        assertTrue(texts.contains("How many, and how high"))
        assertTrue(texts.any { it.startsWith("TH16: 2 × Lv ") })
        assertTrue(texts.contains("Supercharges"))
        assertFalse(texts.contains("From the Multi-Archer Tower"))
        assertFalse(texts.contains("Walks"))
    }

    @Test
    fun aSyncedVillageShowsYourDefenceLevel() {
        syncTh16()
        val texts = Screens.texts(Screens.root(openUnit("Cannon")))
        assertTrue(texts.toString(), texts.any { it.startsWith("You: level ") && it.endsWith(" at TH16") })
    }

    @Test
    fun ruinWitchShowsHerRuinKnights() {
        val root = Screens.root(openUnit("Ruin Witch"))
        val texts = Screens.texts(root)
        assertTrue(texts.contains("Ruin Knight"))
        assertTrue(texts.any { it.contains("Ruin Knights summoned, at most: 10") })
    }

    @Test
    fun aSuperTroopLinksToItsBaseAndBack() {
        val superYeti = openUnit("Super Yeti")
        Screens.tappable(Screens.root(superYeti), "Yeti")!!.performClick()
        assertEquals(unitId("Yeti"), shadowOf(superYeti).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
        val yeti = openUnit("Yeti")
        Screens.tappable(Screens.root(yeti), "Super Yeti")!!.performClick()
        assertEquals(unitId("Super Yeti"), shadowOf(yeti).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    // Picking a level anywhere moves every chart, every readout and the Level card with it.
    @Test
    fun aTapOnAChartMovesTheWholePage() {
        val page = openUnit("Dragon")
        val root = Screens.root(page)
        val charts = Screens.views<LevelChart>(root)
        assertTrue(charts.size >= 2)
        assertTrue(Screens.texts(root).contains("Level 13"))

        val chart = charts.first()
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 0f, 50f)

        assertTrue(charts.all { it.selected == 0 })
        val texts = Screens.texts(root)
        assertTrue(texts.contains("Level 1"))
        assertTrue(texts.contains("Lv 1: 140"))
        assertTrue(texts.contains("Lv 1: 1,900"))
    }

    @Test
    fun aSidewaysDragPicksALevel() {
        val chart = Screens.views<LevelChart>(Screens.root(openUnit("Dragon"))).first()
        val right = chart.width.toFloat()
        assertTrue(right > 0)
        Screens.touch(chart, MotionEvent.ACTION_DOWN, right, 50f)
        Screens.touch(chart, MotionEvent.ACTION_MOVE, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 0f, 50f)
        assertEquals(0, chart.selected)
    }

    // From the emulator test: a scroll that starts on a chart used to change the level.
    @Test
    fun aVerticalScrollFromAChartLeavesTheLevelAlone() {
        val chart = Screens.views<LevelChart>(Screens.root(openUnit("Dragon"))).first()
        val before = chart.selected
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_MOVE, 2f, 200f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 2f, 200f)
        assertEquals(before, chart.selected)
    }

    @Test
    fun aRotationKeepsThePickedLevel() {
        val intent = android.content.Intent(app, GuideUnitActivity::class.java).putExtra(GuideScreen.EXTRA_ID, unitId("Dragon"))
        val controller = org.robolectric.Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup()
        Screens.layOut(controller.get())
        val chart = Screens.views<LevelChart>(Screens.root(controller.get())).first()
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 0f, 50f)
        controller.recreate()
        assertTrue(Screens.texts(Screens.root(controller.get())).contains("Level 1"))
    }

    @Test
    fun aSyncedVillageOpensOnYourLevelAndLocksWhatsAhead() {
        syncTh16()
        val witch = Screens.texts(Screens.root(openUnit("Ruin Witch")))
        assertTrue(witch.contains("Level 2"))
        assertTrue(witch.contains("You: level 2 of 2 at TH16"))
        assertTrue(Screens.texts(Screens.root(openUnit("Meteor Golem"))).contains("Unlocks at TH17"))
    }

    @Test
    fun aSyncedVillageMarksItsTownHall() {
        syncTh16()
        val hub = Screens.root(Screens.open<GuideActivity>())
        assertTrue(Screens.texts(hub).contains("Your Town Hall: TH16"))
        val mine = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 16) }
        assertTrue(Screens.texts(Screens.root(mine)).contains("You"))
        val other = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 15) }
        assertFalse(Screens.texts(Screens.root(other)).contains("You"))
    }

    @Test
    fun bestPetsGivesEachHeroADifferentPetForYourTownHall() {
        syncTh16()
        val texts = Screens.texts(Screens.root(Screens.open<GuidePairingsActivity>()))
        assertTrue(texts.contains("A set for TH16"))
        for (pet in listOf("Unicorn", "Electro Owl", "Spirit Fox", "Angry Jelly", "Frosty", "Phoenix")) {
            assertTrue(pet, texts.contains(pet))
        }
    }

    @Test
    fun theMainScreenOffersTheGuideWithOrWithoutAVillage() {
        val first = Screens.open<com.cocwidget.MainActivity>()
        Screens.tappable(Screens.root(first), "Guide")!!.performClick()
        assertEquals(GuideActivity::class.java.name, shadowOf(first).nextStartedActivity.component?.className)
        syncTh16()
        val synced = Screens.open<com.cocwidget.MainActivity>()
        assertNotNull(Screens.tappable(Screens.root(synced), "Guide"))
    }
}
