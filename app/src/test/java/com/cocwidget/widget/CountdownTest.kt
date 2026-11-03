package com.cocwidget.widget

import com.cocwidget.TestData
import com.cocwidget.game.Boost
import com.cocwidget.game.Helpers
import com.cocwidget.widget.Countdown.DAY_MS
import com.cocwidget.widget.Countdown.MAX_REFRESH_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTest {
    private val now = 1_800_000_000_000L

    @Test
    fun underADayHasNoDays() {
        val split = Countdown.split(5_000)
        assertEquals(0L, split.days)
        assertEquals(5_000L, split.restMs)
    }

    @Test
    fun exactlyOneDayShowsAsTwentyFourHoursNotOneDayAndZero() {
        // "1d 0:00:00" would start the chronometer at zero and count into negatives.
        val split = Countdown.split(DAY_MS)
        assertEquals(0L, split.days)
        assertEquals(DAY_MS, split.restMs)
    }

    @Test
    fun justOverADay() {
        val split = Countdown.split(DAY_MS + 1)
        assertEquals(1L, split.days)
        assertEquals(1L, split.restMs)
    }

    @Test
    fun severalDays() {
        val fiveHours = 5 * 3_600_000L
        val split = Countdown.split(2 * DAY_MS + fiveHours)
        assertEquals(2L, split.days)
        assertEquals(fiveHours, split.restMs)
    }

    @Test
    fun withNothingRunningRefreshesOnTheRegularInterval() {
        assertEquals(now + MAX_REFRESH_MS, Countdown.nextRefresh(null, now))
    }

    @Test
    fun refreshesWhenADayRollsOver() {
        // 2 days and 10 seconds left: the chronometer hits zero in 10 seconds.
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 2 * 86_400 + 10))
        assertEquals(now + 10_000, Countdown.nextRefresh(village, now))
    }

    @Test
    fun refreshesWhenABoostEnds() {
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 1_000_000), builderBoost = Boost(60, 10))
        assertEquals(now + 60_000, Countdown.nextRefresh(village, now))
    }

    @Test
    fun refreshesWhenAHelperShiftEnds() {
        val helped = TestData.upgrade(workSec = 1_000_000, helperSec = 90)
        val village = TestData.villageOf(now, helped, helpers = Helpers(8, 12, 0))
        assertEquals(now + 90_000, Countdown.nextRefresh(village, now))
    }

    @Test
    fun refreshesWhenHelpersBecomeReady() {
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 1_000_000), helpers = Helpers(8, 12, 60))
        assertEquals(now + 60_000, Countdown.nextRefresh(village, now))
    }

    @Test
    fun neverSchedulesSoonerThanASecond() {
        // Exported 9.8 s ago with 10 s of work, so it finishes in 200 ms.
        val village = TestData.villageOf(now - 9_800, TestData.upgrade(workSec = 10))
        assertEquals(now + 1_000, Countdown.nextRefresh(village, now))
    }
}
