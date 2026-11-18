package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.game.Section
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner.Group
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

// What happened between two imports in a row, and how the weeks add it up.
class BetweenTest {
    private val day = 86_400L
    private val king = 28000000
    private val queen = 28000001
    private val items = TestData.gameData

    // The TH16 export at [seconds] with nothing running and no potions or helpers, so only
    // what a test adds is busy.
    private fun export(seconds: Long, edit: (JSONObject) -> Unit = {}): String {
        val root = JSONObject(TestData.exportJson()).put("timestamp", seconds)
        root.remove("boosts")
        for (section in root.keys().asSequence().toList()) {
            val list = root.optJSONArray(section) ?: continue
            for (i in 0 until list.length()) list.optJSONObject(i)?.remove("timer")
        }
        edit(root)
        return root.toString()
    }

    private fun hero(root: JSONObject, id: Int, level: Int, timer: Long? = null) {
        val heroes = root.getJSONArray("heroes")
        for (i in 0 until heroes.length()) {
            val hero = heroes.getJSONObject(i)
            if (hero.getInt("data") != id) continue
            hero.put("lvl", level)
            if (timer != null) hero.put("timer", timer)
        }
    }

    private val builders = VillageParser.parse(TestData.exportJson()) { items[it] }!!.builders
    private val start = 1_800_000_000L

    @Test
    fun withNothingRunningEverySlotSitsIdle() {
        val between = Betweens.of(export(start), export(start + day), items)!!
        assertEquals(builders * day, between.idleSeconds.getValue(Section.BUILDERS))
        assertEquals(day, between.idleSeconds.getValue(Section.LAB))
        assertEquals(day, between.idleSeconds.getValue(Section.PETS))
        assertEquals(mapOf(Section.BUILDERS to 0, Section.LAB to 0, Section.PETS to 0), between.levels)
        assertEquals(start * 1000, between.previousAt)
    }

    // Started and finished between the two, so each level took its full time.
    @Test
    fun levelsDoneInBetweenCountAndKeepABuilderBusy() {
        val span = 30 * day
        val between = Betweens.of(export(start) { hero(it, queen, 93) }, export(start + span) { hero(it, queen, 95) }, items)!!
        assertEquals(2, between.levels.getValue(Section.BUILDERS))
        val busy = items.getValue(queen).secondsToReach(94) + items.getValue(queen).secondsToReach(95)
        assertEquals(builders * span - busy, between.idleSeconds.getValue(Section.BUILDERS))
    }

    // Running then with an hour left: only that hour was spent between the two.
    @Test
    fun anUpgradeRunningThenCountsWhatWasLeft() {
        val between = Betweens.of(export(start) { hero(it, king, 61, 3_600) }, export(start + day) { hero(it, king, 62) }, items)!!
        assertEquals(1, between.levels.getValue(Section.BUILDERS))
        assertEquals(builders * day - 3_600, between.idleSeconds.getValue(Section.BUILDERS))
    }

    // Running now and not then: busy for as long as it has gone.
    @Test
    fun anUpgradeStartedSinceCountsWhatHasPassed() {
        val total = items.getValue(king).secondsToReach(62)
        val between = Betweens.of(export(start), export(start + day) { hero(it, king, 61, total - 7_200) }, items)!!
        assertEquals(builders * day - 7_200, between.idleSeconds.getValue(Section.BUILDERS))
        assertEquals(0, between.levels.getValue(Section.BUILDERS))
    }

    // Still running across both: busy the whole time, counted once.
    @Test
    fun anUpgradeRunningInBothIsCountedOnce() {
        val between = Betweens.of(export(start) { hero(it, king, 61, 3 * day) }, export(start + day) { hero(it, king, 61, 2 * day) }, items)!!
        assertEquals((builders - 1) * day, between.idleSeconds.getValue(Section.BUILDERS))
    }

    @Test
    fun theSameMomentOrNotAnExportGivesNothing() {
        assertNull(Betweens.of(export(start), export(start), items))
        assertNull(Betweens.of("not an export", export(start), items))
    }

    private fun point(at: Long, since: Between?) = ProgressPoint(at, 16, 0.5f, mapOf(Group.HEROES to 0.5f), 1000, since)

    private fun utc(text: String) = java.time.Instant.parse(text).toEpochMilli()

    // Idle time shared by how much of the gap fell in each week, levels in the later import's week.
    @Test
    fun weeksShareIdleTimeByTheGap() {
        val previous = utc("2026-11-09T12:00:00Z")
        val latest = utc("2026-11-18T12:00:00Z")
        val since = Between(previous, mapOf(Section.BUILDERS to 4), mapOf(Section.BUILDERS to 9_000L))
        val weeks = Progress.weekly(listOf(point(previous, null), point(latest, since)), ZoneOffset.UTC, 3)
        assertEquals(listOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16)), weeks.map { it.start })
        assertEquals(listOf(false, true, true), weeks.map { it.covered })
        assertEquals(6_500L, weeks[1].idleSeconds.getValue(Section.BUILDERS))
        assertEquals(2_500L, weeks[2].idleSeconds.getValue(Section.BUILDERS))
        assertTrue(weeks[1].levels.isEmpty())
        assertEquals(4, weeks[2].levels.getValue(Section.BUILDERS))
        assertFalse(weeks[0].covered)
    }

    @Test
    fun barLinesStartAtNothing() {
        assertEquals(listOf(0, 1), Progress.barTicks(0f))
        assertEquals(listOf(0, 5, 10, 15), Progress.barTicks(12f))
        assertEquals(listOf(0, 20, 40, 60, 80), Progress.barTicks(71f))
    }

    @Test
    fun theFiguresAreKeptWithThePoint() {
        val since = Between(1, mapOf(Section.BUILDERS to 3, Section.LAB to 1), mapOf(Section.BUILDERS to 7_200L, Section.LAB to 0L))
        val points = listOf(point(1, null), point(2, since))
        assertEquals(points, Progress.parse(Progress.toJson(points, 46), 46).values.sortedBy { it.exportedAtMillis })
    }
}
