package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.plan.Planner.Group
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

// What one import says about progress, and how imports are picked and grouped for the card and chart.
class ProgressTest {
    private val day = 86_400_000L
    private val queen = 28000001
    private val wall = 1000010

    // The TH16 export with one entry's level changed.
    private fun withLevel(section: String, id: Int, level: Int, json: String = TestData.exportJson()): String {
        val root = JSONObject(json)
        val entries = root.getJSONArray(section)
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            if (entry.optInt("data") == id) {
                entry.put("lvl", level)
                break
            }
        }
        return root.toString()
    }

    private fun point(at: Long, townHall: Int = 16, done: Float = 0.5f) =
        ProgressPoint(at, townHall, done, mapOf(Group.HEROES to done), 1000)

    @Test
    fun timeDoneWeighsEveryTimedGroupAndLeavesOutWalls() {
        val plan = TestData.plan()
        val timed = plan.groups.filter { it.group != Group.WALLS && it.group != Group.EQUIPMENT }
        val done = plan.timeDone()
        assertTrue(done > timed.minOf { it.done } && done < timed.maxOf { it.done })

        // A wall level takes no time: the walls row moves, the overall share doesn't.
        val walled = TestData.plan(withLevel("buildings", wall, 17))
        assertEquals(done, walled.timeDone())
        assertTrue(walled.group(Group.WALLS)!!.done > plan.group(Group.WALLS)!!.done)

        // A hero level does.
        assertTrue(TestData.plan(withLevel("heroes", queen, 94)).timeDone() < done)
    }

    // Walls and equipment count levels, not seconds, so adding them in would mix the two.
    @Test
    fun wallsAndEquipmentAreLeftOutOfTheOverallShare() {
        fun group(group: Group, done: Long, all: Long) = Planner.GroupPlan(group, emptyList(), 0, emptyMap(), done.toFloat() / all, done, all)
        val plan = Planner.Plan(
            townHall = 16, builders = 6,
            groups = listOf(group(Group.HEROES, 30, 100), group(Group.LAB, 70, 100), group(Group.WALLS, 0, 1000), group(Group.EQUIPMENT, 0, 1000)),
            builderSeconds = 0, longestSeconds = 0,
        )
        assertEquals(0.5f, plan.timeDone())
    }

    @Test
    fun aPointIsTheImportsOwnTownHallAtFullPrice() {
        val point = Progress.point(TestData.exportJson(), TestData.gameData)!!
        val plan = TestData.plan(builders = TestData.village().builders)
        assertEquals(1793625123000, point.exportedAtMillis)
        assertEquals(16, point.townHall)
        assertEquals(plan.timeDone(), point.done)
        assertEquals(plan.groups.associate { it.group to it.done }, point.groups)
        assertEquals(plan.builderEstimateSeconds(), point.maxSeconds)
        assertNull(Progress.point("not an export", TestData.gameData))
    }

    @Test
    fun theCardComparesWithTheFirstImportInTheLastMonthOrElseTheOneBefore() {
        val now = 100 * day
        assertEquals(now - 20 * day, Progress.compareWith(listOf(now - 40 * day, now - 20 * day, now - 5 * day, now), now))
        // Nothing that recent: the newest earlier one.
        assertEquals(now - 40 * day, Progress.compareWith(listOf(now - 50 * day, now - 40 * day, now), now))
        assertNull(Progress.compareWith(listOf(now), now))
        assertNull(Progress.compareWith(emptyList(), now))
    }

    @Test
    fun aDayOfImportsIsOnePointTheLastOne() {
        val morning = LocalDate.of(2026, 11, 10).atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val points = listOf(point(morning + 12 * 3_600_000L), point(morning), point(morning + 3_600_000L), point(morning + day))
        assertEquals(listOf(morning + 12 * 3_600_000L, morning + day), Progress.daily(points, ZoneOffset.UTC).map { it.exportedAtMillis })
        // Five hours ahead, the 20:00 import falls after midnight, on the next day.
        assertEquals(listOf(morning + 3_600_000L, morning + day), Progress.daily(points, ZoneOffset.ofHours(5)).map { it.exportedAtMillis })
    }

    @Test
    fun eachTownHallIsItsOwnStretch() {
        val points = listOf(point(1, 16), point(2, 16), point(3, 17), point(4, 17), point(5, 18))
        assertEquals(listOf(listOf(1L, 2L), listOf(3L, 4L), listOf(5L)), Progress.stretches(points).map { run -> run.map { it.exportedAtMillis } })
    }

    @Test
    fun theChartsLinesHugTheLine() {
        assertEquals(listOf(78, 80, 82), Progress.ticks(0.78f, 0.81f))
        assertEquals(listOf(50, 52), Progress.ticks(0.5f, 0.5f))
        assertEquals(listOf(98, 100), Progress.ticks(1f, 1f))
        assertEquals(listOf(60, 65, 70, 75, 80), Progress.ticks(0.62f, 0.78f))
        assertEquals(listOf(0, 20, 40, 60, 80, 100), Progress.ticks(0f, 1f))
    }

    @Test
    fun keptPointsComeBackOnlyForTheSameVersion() {
        val points = listOf(point(1, 16, 0.25f), point(2, 17, 0.75f))
        val text = Progress.toJson(points, 46)
        assertEquals(points, Progress.parse(text, 46).values.sortedBy { it.exportedAtMillis })
        assertTrue(Progress.parse(text, 47).isEmpty())
        assertTrue(Progress.parse("broken", 46).isEmpty())
        assertTrue(Progress.parse(null, 46).isEmpty())
    }
}
