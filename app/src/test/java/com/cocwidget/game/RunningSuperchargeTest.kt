package com.cocwidget.game

import com.cocwidget.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A building on its final level with a timer is supercharging, not levelling. The widget showed
// "X-Bow → 14" for an X-Bow whose last level is 13.
class RunningSuperchargeTest {
    private val village = TestData.superchargingVillage()

    private fun all(name: String) = village.upgrades.filter { it.name == name }

    @Test
    fun theMortarIsOnItsFirstStage() {
        // supercharge 0 in the export: no stage done. ClashKing: stage 1 is 4d.
        val mortar = all("Mortar").single()
        assertEquals(1, mortar.supercharge)
        assertEquals(18, mortar.toLevel)
        assertEquals(345_600L, mortar.totalSec)
    }

    @Test
    fun xBowsAndMultiArcherTowersAreOnTheirSecondStage() {
        val xBows = all("X-Bow")
        assertEquals(3, xBows.size)
        assertTrue(xBows.all { it.supercharge == 2 && it.toLevel == 13 && it.totalSec == 648_000L })
        val towers = all("Multi-Archer Tower")
        assertEquals(3, towers.size)
        assertTrue(towers.all { it.supercharge == 2 && it.toLevel == 4 && it.totalSec == 604_800L })
    }

    @Test
    fun progressCountsAgainstTheStageTime() {
        val tower = all("Multi-Archer Tower").minBy { it.workSec }
        assertEquals(1f - 75_953f / 604_800, village.progress(tower, village.exportedAtMillis), 0.001f)
    }

    @Test
    fun levelUpgradesAreUntouched() {
        val levels = village.upgrades.filter { it.supercharge == 0 }.associate { it.name to it.toLevel }
        assertEquals(mapOf("Bowler" to 5, "Bat Spell" to 8, "Greedy Raven" to 3), levels)
        assertTrue(TestData.th18Village().upgrades.all { it.supercharge == 0 })
        assertTrue(TestData.village().upgrades.all { it.supercharge == 0 })
    }

    @Test
    fun allSixBuildersAndTheGoblinAreBusy() {
        val builders = village.upgrades.filter { it.section == Section.BUILDERS }
        assertEquals(6, village.builders)
        assertEquals(6, builders.count { !it.extra })
        assertEquals(1, builders.count { it.extra })
    }

    @Test
    fun aMaxedBuilderBaseStillShows() {
        assertTrue(village.hasBuilderBase)
        assertTrue(village.upgrades.none { it.section == Section.BUILDER_BASE })
    }
}
