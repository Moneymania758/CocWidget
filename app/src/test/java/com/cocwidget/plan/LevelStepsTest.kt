package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.plan.Planner.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// What's left one level at a time adds up to the item's totals.
class LevelStepsTest {
    private fun Planner.Plan.items() = groups.flatMap { it.items }

    // A running copy's level is already paid, so it adds no cost.
    private fun stepCosts(item: Planner.Item): Map<String, Long> {
        val total = HashMap<String, Long>()
        for (step in item.steps) for ((resource, cost) in step.costs) total.merge(resource, cost * (step.copies - step.running), Long::plus)
        return total.filterValues { it > 0 }
    }

    private fun assertStepsAddUp(plan: Planner.Plan) {
        for (item in plan.items()) {
            assertEquals(item.name, item.costs, stepCosts(item))
            // With at most one copy running per level, time adds up exactly too: a running copy
            // counts its time left, the rest the whole level.
            if (item.steps.all { it.running <= 1 }) {
                assertEquals(item.name, item.seconds, item.steps.sumOf { it.seconds * (it.copies - it.running) + it.runningLeft })
            }
            for (step in item.steps) assertEquals(item.name, step.running > 0, step.runningLeft > 0)
            assertEquals(item.name, item.steps.sortedWith(compareBy({ it.level }, { it.supercharge })).map { it.level to it.supercharge }, item.steps.map { it.level to it.supercharge })
        }
    }

    @Test
    fun stepsAddUpToEachItem() = assertStepsAddUp(TestData.plan())

    @Test
    fun stepsAddUpDuringAnEvent() = assertStepsAddUp(TestData.plan(eventPercent = 50))

    @Test
    fun stepsAddUpWithMergesAndSupercharges() {
        assertStepsAddUp(TestData.plan(TestData.th18ExportJson()))
        val supercharging = TestData.plan(TestData.superchargingExportJson())
        assertStepsAddUp(supercharging)
        val charges = supercharging.group(Group.DEFENCES)!!.items.flatMap { it.steps }.filter { it.supercharge > 0 }
        assertTrue(charges.isNotEmpty())
        assertTrue("a running supercharge is marked", charges.any { it.running > 0 })
    }

    @Test
    fun everyItemWithWorkLeftHasSteps() {
        for (item in TestData.plan().items()) {
            if (item.seconds > 0 || item.costs.isNotEmpty()) assertTrue(item.name, item.steps.isNotEmpty())
            for (step in item.steps) assertTrue(item.name, step.copies in 1..item.levels.size && step.running in 0..step.copies)
        }
    }

    @Test
    fun aRunningLevelIsCountedOnce() {
        val plan = TestData.plan()
        for (item in plan.items()) {
            val running = item.steps.filter { it.supercharge == 0 }.sumOf { it.running }
            assertEquals(item.name, item.running.size, running)
        }
    }
}
