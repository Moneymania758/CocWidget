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
    fun theClockShowsDaysHoursAndMinutes() {
        assertEquals("3d 04:12:55", Countdown.clock(3 * DAY_MS + (4 * 3600 + 12 * 60 + 55) * 1000L))
        assertEquals("04:12:55", Countdown.clock((4 * 3600 + 12 * 60 + 55) * 1000L))
        assertEquals("12:55", Countdown.clock((12 * 60 + 55) * 1000L))
    }

    @Test
    fun theClockRoundsUpSoItNeverReadsZeroWhileRunning() {
        assertEquals("00:01", Countdown.clock(1))
        assertEquals("00:00", Countdown.clock(0))
        assertEquals("00:00", Countdown.clock(-5000))
    }

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

    @Test
    fun withDaysInFrontTheHourComesBackUnderAnHour() {
        // Chronometer shows 30:00 for half an hour, so "2d 30:00" would read as 30 hours.
        assertEquals("0:", Countdown.hourPrefix(Countdown.split(2 * DAY_MS + 30 * 60_000)))
        assertEquals("0:%s", Countdown.chronometerFormat(Countdown.split(2 * DAY_MS + 30 * 60_000)))
        assertEquals("", Countdown.hourPrefix(Countdown.split(2 * DAY_MS + 2 * 3_600_000)))
        // Under a day there are no days in front, and 30:00 reads right.
        assertEquals("", Countdown.hourPrefix(Countdown.split(30 * 60_000)))
        assertEquals(null, Countdown.chronometerFormat(Countdown.split(30 * 60_000)))
    }

    @Test
    fun refreshesWhenADayCountdownDropsUnderAnHour() {
        // 2 days, 1 hour and 10 seconds left: under an hour in just over 10 seconds.
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 2 * 86_400 + 3_600 + 10))
        assertEquals(now + 10_001, Countdown.nextRefresh(village, now))
        assertEquals(now + 10_001, Countdown.nextEventRefresh(listOf(now + 2 * DAY_MS + 3_600_000 + 10_000), now))
    }

    // The Timeline's list, where nothing ticks, rounds up so a row reads 1m until it ends.
    @Test
    fun theShortFormShowsTheTwoBiggestUnits() {
        assertEquals("2d 4h", Countdown.short(2 * DAY_MS + 4 * 3_600_000L + 59_000))
        assertEquals("13h 49m", Countdown.short((13 * 60 + 48) * 60_000L + 1))
        assertEquals("42m", Countdown.short(42 * 60_000L))
        assertEquals("1m", Countdown.short(1))
        assertEquals("0m", Countdown.short(0))
    }

    @Test
    fun theNextMinuteIsTheNextTurnOfTheClock() {
        val minute = 60_000L
        val start = now - Math.floorMod(now, minute)
        assertEquals(start + minute, Countdown.nextMinute(start))
        assertEquals(start + minute, Countdown.nextMinute(start + 59_999))
    }
}
