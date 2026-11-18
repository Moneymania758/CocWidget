package com.cocwidget.acceptance

import com.cocwidget.BedtimeActivity
import com.cocwidget.PlannerActivity
import com.cocwidget.R
import com.cocwidget.SettingsActivity
import com.cocwidget.TestData
import com.cocwidget.events.EventSchedule
import com.cocwidget.plan.PlanFormat
import com.cocwidget.plan.Planner
import com.cocwidget.plan.Planner.Group
import com.cocwidget.store.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

// The Gold Pass boost per account on Time to max, and Hammer Jam's half price from the event
// dates, now there's no switch for it.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoldPassTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun texts(screen: android.app.Activity) = Screens.texts(Screens.root(screen))

    @Test
    fun theBoostCutsBuilderAndLabUpgradesAfterAnyEvent() {
        val prices = Planner.Prices(goldPassPercent = 20)
        assertEquals(80, prices.percentFor(Group.HEROES))
        assertEquals(80, prices.percentFor(Group.DEFENCES))
        assertEquals(80, prices.percentFor(Group.LAB))
        assertEquals(100, prices.percentFor(Group.PETS))
        assertEquals(100, prices.percentFor(Group.EQUIPMENT))
        val both = Planner.Prices(eventPercent = 50, goldPassPercent = 20)
        assertEquals(40, both.percentFor(Group.LAB))
        assertEquals(50, both.percentFor(Group.PETS))
    }

    @Test
    fun aPlanAtTwentyPercentTakesAFifthLessTimeAndCost() {
        val full = TestData.plan()
        val boosted = Planner.plan(TestData.exportJson(), TestData.gameData, 6, goldPassPercent = 20)!!
        for (group in listOf(Group.DEFENCES, Group.LAB)) {
            val fullSeconds = full.group(group)!!.items.sumOf { it.steps.filter { s -> s.running == 0 }.sumOf { s -> s.seconds * s.copies } }
            val boostedSeconds = boosted.group(group)!!.items.sumOf { it.steps.filter { s -> s.running == 0 }.sumOf { s -> s.seconds * s.copies } }
            assertEquals(fullSeconds * 0.8, boostedSeconds.toDouble(), fullSeconds * 0.001)
            val gold = full.group(group)!!.costs.values.sum()
            assertEquals(gold * 0.8, boosted.group(group)!!.costs.values.sum().toDouble(), gold * 0.001)
        }
        assertEquals(full.group(Group.PETS)!!.seconds, boosted.group(Group.PETS)!!.seconds)
    }

    @Test
    fun eachAccountKeepsItsBoostUntilTheSeasonEnds() {
        Store.setGoldPass(app, "#ONE", 20, at("2026-11-20T12:00:00Z"))
        assertEquals(20, Store.goldPass(app, "#ONE", at("2026-11-30T23:00:00Z")))
        assertEquals(20, Store.goldPass(app, "#ONE", at("2026-12-01T07:59:00Z")))
        assertEquals(0, Store.goldPass(app, "#ONE", at("2026-12-01T08:00:00Z")))
        assertEquals(0, Store.goldPass(app, "#TWO", at("2026-11-20T12:00:00Z")))
        // Set in the hours before the 1st's reset, it belongs to the season then ending.
        assertEquals(at("2026-11-01T08:00:00Z"), EventSchedule.seasonStart(at("2026-12-01T07:00:00Z")))
    }

    @Test
    fun pickingTwentyPercentOnTimeToMaxShortensIt() {
        val tag = Store.saveExport(app, TestData.exportJson())
        Store.setSelected(app, tag)
        val full = PlanFormat.duration(TestData.plan(builders = TestData.village().builders).builderEstimateSeconds())
        val screen = Screens.open<PlannerActivity>()
        assertTrue(texts(screen).contains(full))

        Screens.tappable(Screens.root(screen), "20%")!!.performClick()
        assertEquals(20, Store.goldPass(app, tag))
        val boosted = Planner.plan(TestData.exportJson(), TestData.gameData, TestData.village().builders, goldPassPercent = 20)!!
        val shown = texts(screen)
        assertTrue(shown.contains(PlanFormat.duration(boosted.builderEstimateSeconds())))
        assertTrue(shown.contains(app.getString(R.string.planner_gold_pass, 20)))
        assertTrue(Screens.views<android.widget.TextView>(Screens.root(screen)).single { it.text == "20%" }.isSelected)

        // Bedtime prices the same way and says so.
        assertTrue(texts(Screens.open<BedtimeActivity>()).any { it.contains(app.getString(R.string.planner_gold_pass, 20)) })
    }

    // The TH16 export on 20 Nov, then an hour later with the Barbarian King's next level started
    // since and [left] of it to go. Nothing else runs.
    private fun kingStarted(left: Double): Pair<String, String> {
        val king = 28000000
        fun export(seconds: Long, timer: Long?): String {
            val root = org.json.JSONObject(TestData.exportJson()).put("timestamp", seconds)
            root.remove("boosts")
            for (section in root.keys().asSequence().toList()) {
                val list = root.optJSONArray(section) ?: continue
                for (i in 0 until list.length()) list.optJSONObject(i)?.remove("timer")
            }
            val heroes = root.getJSONArray("heroes")
            for (i in 0 until heroes.length()) {
                val hero = heroes.getJSONObject(i)
                if (hero.getInt("data") == king && timer != null) hero.put("timer", timer)
            }
            return root.toString()
        }
        // After Hammer Jam, so only the boost applies.
        val start = at("2026-11-20T12:00:00Z") / 1000
        val total = TestData.gameData.getValue(king).secondsToReach(62)
        return export(start, null) to export(start + 3_600, (total * left).toLong())
    }

    private fun timeOf(json: String) = org.json.JSONObject(json).getLong("timestamp")

    private fun kingTotal(tag: String) = Store.village(app, tag)!!.upgrades.single { it.name == "Barbarian King" }.totalSec!!

    // Started with the boost, the King's bar runs over 80% of the full time.
    @Test
    fun anUpgradeStartedWithTheBoostGetsAShorterBar() {
        val (before, after) = kingStarted(0.7)
        val tag = Store.saveExport(app, before)
        Store.setGoldPass(app, tag, 20, timeOf(after) * 1000)
        Store.saveExport(app, after)
        val full = TestData.gameData.getValue(28000000).secondsToReach(62)
        assertEquals(full * 80 / 100, kingTotal(tag))
    }

    // Saved before the boost, a discounted upgrade was kept by id alone, for Hammer Jam's half.
    @Test
    fun aDiscountSavedByAnOlderVersionIsStillHalf() {
        val (before, after) = kingStarted(0.4)
        val tag = Store.saveExport(app, before)
        Store.saveExport(app, after)
        val prefs = app.getSharedPreferences(Store.fileName(tag), android.content.Context.MODE_PRIVATE)
        val id = prefs.getString("upgrade_ids", "")!!.lines().first()
        prefs.edit().putStringSet("discounted_upgrades", setOf(id)).commit()
        assertEquals(TestData.gameData.getValue(28000000).secondsToReach(62) / 2, kingTotal(tag))
    }

    @Test
    fun hammerJamIsOnOnlyBetweenItsDates() {
        assertTrue(Store.eventOn(app, at("2026-11-10T12:00:00Z")))
        assertFalse(Store.eventOn(app, at("2026-11-16T08:00:00Z")))
        assertFalse(Store.eventOn(app, at("2026-10-31T12:00:00Z")))
    }

    @Test
    fun settingsHasNoEventSwitch() {
        assertFalse(texts(Screens.open<SettingsActivity>()).any { it.contains("50% off") })
    }
}
