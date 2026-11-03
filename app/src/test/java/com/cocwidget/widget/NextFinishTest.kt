package com.cocwidget.widget

import com.cocwidget.TestData
import com.cocwidget.TestData.upgrade
import com.cocwidget.TestData.villageOf
import com.cocwidget.game.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextFinishTest {
    private val now = 1_800_000_000_000L

    @Test
    fun soonestRunningUpgradeIsNextWhateverItsSection() {
        val village = villageOf(
            now,
            upgrade(7_200, name = "Cannon"),
            upgrade(3_600, Section.PETS, name = "Poison Lizard"),
            upgrade(5_400, Section.LAB, name = "Giant"),
        )
        assertEquals("Poison Lizard", NextFinish.of(village, now).next?.name)
    }

    @Test
    fun finishedUpgradesAreSkipped() {
        val village = villageOf(now - 2_000_000, upgrade(1_000, name = "Done"), upgrade(9_000, name = "Running"))
        assertEquals("Running", NextFinish.of(village, now).next?.name)
    }

    @Test
    fun nothingRunningMeansNoNextAndEverythingIdle() {
        val summary = NextFinish.of(villageOf(now), now)
        assertNull(summary.next)
        assertEquals(6, summary.idleBuilders)
        assertTrue(summary.labIdle)
        assertTrue(summary.petsIdle)
    }

    @Test
    fun allSlotsBusyIsNotIdle() {
        val builders = (1..6).map { upgrade(3_600L * it, name = "Building $it") }
        val village = villageOf(
            now,
            *builders.toTypedArray(),
            upgrade(3_600, Section.LAB),
            upgrade(3_600, Section.PETS),
        )
        val summary = NextFinish.of(village, now)
        assertEquals(0, summary.idleBuilders)
        assertFalse(summary.anyIdle)
    }

    // The Goblin builds on top of the usual builders, so it doesn't fill a slot.
    @Test
    fun goblinSlotDoesNotCountAsBusy() {
        val village = villageOf(now, upgrade(3_600, extra = true), upgrade(3_600))
        assertEquals(5, NextFinish.of(village, now).idleBuilders)
    }

    @Test
    fun builderBaseIsNextButNeverIdle() {
        val village = villageOf(now, upgrade(60, Section.BUILDER_BASE, name = "Battle Machine"))
        val summary = NextFinish.of(village, now)
        assertEquals("Battle Machine", summary.next?.name)
        assertEquals(6, summary.idleBuilders)
    }

    // Deval's TH18 export: six builders on supercharges plus the Goblin, research and a pet.
    @Test
    fun realExportWithEveryBuilderBusy() {
        val village = TestData.superchargingVillage()
        val summary = NextFinish.of(village, village.exportedAtMillis)
        assertEquals(6, summary.builders)
        assertEquals(0, summary.idleBuilders)
        assertFalse(summary.labIdle)
        assertFalse(summary.petsIdle)
        val soonest = village.upgrades.minBy { village.endMillis(it) }
        assertEquals(soonest, summary.next)
    }
}
