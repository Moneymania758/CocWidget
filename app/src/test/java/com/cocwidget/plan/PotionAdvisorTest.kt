package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.game.BUILDER_POTION_MULTIPLIER
import com.cocwidget.game.Boost
import com.cocwidget.game.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PotionAdvisorTest {
    private val now = 1_800_000_000_000L
    private val hour = PotionAdvisor.HOUR_SEC
    private val day = 24 * hour

    private fun advise(vararg work: Long, slots: Int = 3) = PotionAdvisor.advise(
        TestData.villageOf(now, *work.map { TestData.upgrade(it) }.toTypedArray()),
        Section.BUILDERS, slots, BUILDER_POTION_MULTIPLIER, now,
    )

    @Test
    fun fullyUsedWhenEverySlotStaysBusy() {
        val advice = advise(day, day, 11 * hour)
        assertTrue(advice.full)
        assertEquals(3 * 9 * hour, advice.savedSec)
    }

    @Test
    fun anIdleBuilderWastesItsShare() {
        val advice = advise(day, day)
        assertFalse(advice.full)
        assertEquals(1, advice.idle)
        assertEquals(2 * 9 * hour, advice.savedSec)
        assertEquals(3 * 9 * hour, advice.possibleSec)
    }

    @Test
    fun anUpgradeFinishingInsideTheHourSavesLess() {
        // 2h of work at 10x takes 12 minutes, saving 1h 48m rather than 9h.
        val advice = advise(day, day, 2 * hour)
        assertFalse(advice.full)
        assertEquals(12 * 60L, advice.finishing.single().second)
        assertEquals(2 * 9 * hour + 2 * hour - 12 * 60, advice.savedSec)
    }

    @Test
    fun aRunningBoostIsReportedInstead() {
        val village = TestData.villageOf(now, TestData.upgrade(day), builderBoost = Boost(1800, BUILDER_POTION_MULTIPLIER))
        val advice = PotionAdvisor.advise(village, Section.BUILDERS, 6, BUILDER_POTION_MULTIPLIER, now)
        assertEquals(1800L, advice.boostLeftSec)
        assertFalse(advice.full)
    }

    @Test
    fun theGoblinsUpgradeIsNotABuilder() {
        val village = TestData.villageOf(now, TestData.upgrade(day), TestData.upgrade(day, extra = true))
        assertEquals(1, PotionAdvisor.advise(village, Section.BUILDERS, 2, BUILDER_POTION_MULTIPLIER, now).idle)
    }
}
