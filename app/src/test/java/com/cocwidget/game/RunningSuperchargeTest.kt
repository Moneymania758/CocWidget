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

    // The export is from before TH19, with three X-Bows and three Multi-Archer Towers on their
    // second stage. TH19 gave both a new level, took their supercharges away and cancelled the
    // running ones, so nothing is running on them, and no "X-Bow → 14" either.
    @Test
    fun superchargesTheUpdateCancelledShowNothingRunning() {
        assertTrue(all("X-Bow").isEmpty())
        assertTrue(all("Multi-Archer Tower").isEmpty())
        val plan = TestData.plan(TestData.superchargingExportJson())
        val xBow = plan.groups.flatMap { it.items }.single { it.name == "X-Bow" }
        assertTrue(xBow.running.isEmpty())
        // X-Bow 14 needs TH19.
        assertEquals(13, xBow.maxLevel)
    }

    @Test
    fun progressCountsAgainstTheStageTime() {
        val mortar = all("Mortar").single()
        assertEquals(1f - 305_769f / 345_600, village.progress(mortar, village.exportedAtMillis), 0.001f)
    }

    @Test
    fun levelUpgradesAreUntouched() {
        val levels = village.upgrades.filter { it.supercharge == 0 }.associate { it.name to it.toLevel }
        assertEquals(mapOf("Bowler" to 5, "Bat Spell" to 8, "Greedy Raven" to 3), levels)
        assertTrue(TestData.th18Village().upgrades.all { it.supercharge == 0 })
        assertTrue(TestData.village().upgrades.all { it.supercharge == 0 })
    }

    @Test
    fun theCancelledSuperchargesFreeTheirBuilders() {
        // Before TH19 all six builders and the Goblin were busy. Only the Mortar is left.
        val builders = village.upgrades.filter { it.section == Section.BUILDERS }
        assertEquals(6, village.builders)
        assertEquals(listOf("Mortar"), builders.map { it.name })
    }

    @Test
    fun aMaxedBuilderBaseStillShows() {
        assertTrue(village.hasBuilderBase)
        assertTrue(village.upgrades.none { it.section == Section.BUILDER_BASE })
    }
}
