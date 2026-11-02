package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDiscountTest {
    private val t0 = 1_800_000_000_000L
    private val hour = 3_600L

    // A same-level Air Defense upgrade, 4 days at full price.
    private fun airDefense(workSec: Long) = Upgrade(
        section = Section.BUILDERS,
        name = "Air Defense",
        kind = "building",
        village = "home",
        toLevel = 16,
        workSec = workSec,
        totalSec = 96 * hour,
        extra = false,
    )

    private fun village(at: Long, vararg upgrades: Upgrade) = TestData.villageOf(at, *upgrades)

    private fun halved(village: Village, tracked: EventDiscount.Tracked) =
        EventDiscount.apply(village, tracked.ids, tracked.discounted).upgrades.map { it.totalSec == 48 * hour }

    @Test
    fun newUpgradeDuringTheEventIsHalved() {
        val before = village(t0, airDefense(80 * hour))
        val first = EventDiscount.track(null, emptyList(), emptySet(), before, eventOn = true)

        // An hour later the old one has 79 h left and a new one started at half time.
        val after = village(t0 + hour * 1000, airDefense(79 * hour), airDefense(47 * hour))
        val tracked = EventDiscount.track(before, first.ids, first.discounted, after, eventOn = true)
        assertEquals(listOf(false, true), halved(after, tracked))
    }

    @Test
    fun aNewDiscountedUpgradeCanFinishBeforeAnOldOne() {
        // The new one has less time left than the old one, so finishing order can't identify them.
        val before = village(t0, airDefense(60 * hour))
        val first = EventDiscount.track(null, emptyList(), emptySet(), before, eventOn = true)

        val after = village(t0 + hour * 1000, airDefense(20 * hour), airDefense(59 * hour))
        val tracked = EventDiscount.track(before, first.ids, first.discounted, after, eventOn = true)
        assertEquals(listOf(true, false), halved(after, tracked))
    }

    @Test
    fun staysDiscountedAfterTheSwitchIsTurnedOff() {
        val before = village(t0, airDefense(47 * hour))
        val start = EventDiscount.track(village(t0 - hour * 1000), emptyList(), emptySet(), before, eventOn = true)
        assertEquals(listOf(true), halved(before, start))

        val later = village(t0 + 10 * hour * 1000, airDefense(37 * hour))
        val tracked = EventDiscount.track(before, start.ids, start.discounted, later, eventOn = false)
        assertEquals(listOf(true), halved(later, tracked))
    }

    @Test
    fun switchOffMeansNothingNewIsHalved() {
        val before = village(t0)
        val after = village(t0 + hour * 1000, airDefense(47 * hour))
        val tracked = EventDiscount.track(before, emptyList(), emptySet(), after, eventOn = false)
        assertTrue(tracked.discounted.isEmpty())
    }

    @Test
    fun firstSyncTreatsEverythingAsAlreadyRunning() {
        val tracked = EventDiscount.track(null, emptyList(), emptySet(), village(t0, airDefense(10 * hour)), eventOn = true)
        assertTrue(tracked.discounted.isEmpty())
    }

    @Test
    fun moreTimeLeftThanHalfCantBeDiscounted() {
        // Switch left on after the event: a full-price upgrade has more than half its time left.
        val before = village(t0)
        val after = village(t0 + hour * 1000, airDefense(90 * hour))
        val tracked = EventDiscount.track(before, emptyList(), emptySet(), after, eventOn = true)
        assertTrue(tracked.discounted.isEmpty())
    }

    @Test
    fun upgradingFromVersionThreeWithoutIdsStillWorks() {
        // 1.3 saved no ids, so the previous export's ids are missing.
        val before = village(t0, airDefense(80 * hour))
        val after = village(t0 + hour * 1000, airDefense(79 * hour), airDefense(47 * hour))
        val tracked = EventDiscount.track(before, emptyList(), emptySet(), after, eventOn = true)
        assertEquals(listOf(false, true), halved(after, tracked))
    }
}
