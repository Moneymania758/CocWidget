package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.game.Section
import com.cocwidget.plan.Planner.Group
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BedtimeTest {
    private val LABORATORY = 1000007
    private val zone = ZoneId.of("Europe/London")
    private val night = Bedtime.Sleep(23 * 60, 7 * 60)

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun step(name: String, hours: Long) = Planner.Step(1, name, Group.DEFENCES, 2, 1, hours * 3600, emptyMap())

    @Test
    fun sleepWindowCrossingMidnight() {
        assertTrue(Bedtime.asleep(at(3, 23), night, zone))
        assertTrue(Bedtime.asleep(at(4, 3), night, zone))
        assertFalse(Bedtime.asleep(at(4, 7), night, zone))
        assertFalse(Bedtime.asleep(at(3, 22, 59), night, zone))
    }

    @Test
    fun sleepWindowWithinOneDay() {
        val late = Bedtime.Sleep(60, 9 * 60)
        assertFalse(Bedtime.asleep(at(3, 23, 30), late, zone))
        assertTrue(Bedtime.asleep(at(4, 1), late, zone))
        assertTrue(Bedtime.asleep(at(4, 8, 59), late, zone))
    }

    @Test
    fun aSlotFreeingOvernightStartsAtWakeUp() {
        assertEquals(at(4, 7), Bedtime.startAt(at(4, 3, 10), night, zone))
        assertEquals(at(4, 7), Bedtime.startAt(at(3, 23, 30), night, zone))
        assertEquals(at(3, 21, 40), Bedtime.startAt(at(3, 21, 40), night, zone))
    }

    @Test
    fun optionsSayWhichFinishWhileAwake() {
        // Started at 22:00: 8h lands at 06:00 (asleep), 10h at 08:00 (awake), 30m at 22:30.
        val options = Bedtime.options(listOf(step("Eight", 8), step("Ten", 10), Planner.Step(2, "Short", Group.TRAPS, 1, 1, 1800, emptyMap())), at(3, 22), night, zone)
        assertEquals(listOf("Short", "Eight", "Ten"), options.map { it.step.name })
        assertEquals(listOf(true, false, true), options.map { it.awake })
    }

    @Test
    fun freeNowWhenABuilderIsIdle() {
        val now = at(3, 20)
        val village = TestData.villageOf(now, TestData.upgrade(3600), TestData.upgrade(7200))
        assertEquals(now, Bedtime.freeAt(village, Section.BUILDERS, 6, now))
        assertEquals(now + 3_600_000, Bedtime.freeAt(village, Section.BUILDERS, 2, now))
    }

    @Test
    fun theGoblinsSlotDoesNotCount() {
        val now = at(3, 20)
        val village = TestData.villageOf(now, TestData.upgrade(3600), TestData.upgrade(600, extra = true))
        assertEquals(now + 3_600_000, Bedtime.freeAt(village, Section.BUILDERS, 1, now))
    }

    @Test
    fun researchWaitsForTheLab() {
        val root = JSONObject(TestData.exportJson())
        val buildings = root.getJSONArray("buildings")
        for (i in 0 until buildings.length()) {
            val building = buildings.getJSONObject(i)
            if (building.getInt("data") == LABORATORY) building.put("lvl", 5)
        }
        val lab = { json: String -> Planner.nextSteps(json, TestData.gameData)!!.filter { it.group == Group.LAB } }
        val full = lab(TestData.exportJson())
        val low = lab(root.toString())
        assertTrue(low.size < full.size)
        for (step in low) assertTrue(step.name, TestData.gameData.getValue(step.id).requiredLevelFor(step.toLevel) <= 5)
    }

    @Test
    fun nextStepsSkipRunningCopiesAndWalls() {
        val steps = Planner.nextSteps(TestData.exportJson(), TestData.gameData)!!
        assertTrue(steps.none { it.group == Group.WALLS })
        assertTrue(steps.all { it.seconds > 0 && it.count > 0 })
        // A one-copy item being upgraded in the export (a hero, a troop in the lab) isn't offered.
        val plan = TestData.plan()
        val running = plan.groups.flatMap { it.items }.filter { it.running.isNotEmpty() && it.levels.size == 1 }
        assertTrue(running.isNotEmpty())
        for (item in running) assertTrue(item.name, steps.none { it.id == item.id })
    }
}
