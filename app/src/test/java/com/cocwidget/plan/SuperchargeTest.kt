package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.plan.Planner.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Supercharges and hero equipment, from ClashKing's data and the TH18 export.
class SuperchargeTest {
    private val th18 by lazy { TestData.plan(TestData.th18ExportJson()) }

    private fun Planner.Plan.item(name: String): Planner.Item =
        groups.flatMap { it.items }.single { it.name == name }

    @Test
    fun bombTowersCountTheStagesLeft() {
        // Two Bomb Towers on their final level 13: one with no stage done, one with stage 1 done.
        // ClashKing: stage 1 is 2d 12h and 6M Gold, stage 2 is 4d and 3M Gold.
        val bombTower = th18.item("Bomb Tower")
        assertEquals(3, bombTower.supercharges)
        assertEquals(216_000L + 345_600 + 345_600, bombTower.seconds)
        assertEquals(12_000_000L, bombTower.costs["Gold"])
    }

    @Test
    fun noSuperchargesBelowTheHighestTownHall() {
        val th16 = TestData.plan()
        assertTrue(th16.groups.flatMap { it.items }.all { it.supercharges == 0 })
    }

    @Test
    fun bedtimeOffersTheNextStage() {
        val steps = Planner.nextSteps(TestData.th18ExportJson(), TestData.gameData)!!.filter { it.name == "Bomb Tower" }
        assertEquals(setOf(1, 2), steps.map { it.supercharge }.toSet())
        assertEquals(345_600L, steps.single { it.supercharge == 2 }.seconds)
    }

    @Test
    fun equipmentIsOreLeftToMax() {
        val equipment = th18.group(Group.EQUIPMENT)!!
        assertEquals(0L, equipment.seconds)
        for (item in equipment.items) {
            assertTrue(item.name, item.costs.keys.all { it.endsWith("Ore") })
        }
    }

    @Test
    fun epicsAtLevelOneAreLeftOut() {
        // Ben's TH16 export lists 42 pieces. He doesn't own every one, and the ones at level 1
        // include event epics like Stick Horse and Heroic Torch.
        val plan = TestData.plan()
        val names = plan.group(Group.EQUIPMENT)!!.items.map { it.name }
        assertTrue("Stick Horse" !in names)
        assertTrue("Heroic Torch" !in names)
        // Common pieces come from the Blacksmith, so one at level 1 still counts.
        assertTrue("Hog Rider Puppet" in names)
        assertEquals(6, plan.unknownEpics)
    }

    @Test
    fun theEventDoesNotDiscountEquipment() {
        val normal = th18.group(Group.EQUIPMENT)!!.costs
        val event = TestData.plan(TestData.th18ExportJson(), eventPercent = 50).group(Group.EQUIPMENT)!!.costs
        assertEquals(normal, event)
    }
}
