package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class PaceTest {
    @Test
    fun noBoostRunsAtNormalSpeed() {
        assertEquals(100L, Pace(Boost.NONE).realSecondsFor(100))
        assertEquals(100L, Pace(Boost.NONE).workDoneIn(100))
    }

    @Test
    fun workThatFitsInsideTheBoost() {
        val potion = Pace(Boost(3600, 10))
        assertEquals(3600L, potion.realSecondsFor(36_000))
        assertEquals(100L, potion.realSecondsFor(1000))
        // Rounds up, so a timer never shows done a second early.
        assertEquals(101L, potion.realSecondsFor(1001))
    }

    @Test
    fun workThatOutlastsTheBoost() {
        // 100 s at 10x clears 1,000 s, the remaining 500 s run at normal speed.
        assertEquals(600L, Pace(Boost(100, 10)).realSecondsFor(1500))
    }

    @Test
    fun workDoneSlowsWhenTheBoostEnds() {
        val potion = Pace(Boost(100, 10))
        assertEquals(500L, potion.workDoneIn(50))
        assertEquals(1050L, potion.workDoneIn(150))
    }

    @Test
    fun aHelperAddsItsLevelToNormalSpeed() {
        // A level 8 Apprentice's hour runs at 1 + 8 = 9x, clearing 32,400 s. The rest is normal speed.
        val shift = Pace(Boost.NONE, helperSec = 3600, helperLevel = 8)
        assertEquals(21_200L, shift.realSecondsFor(50_000))
        assertEquals(32_400L + 100, shift.workDoneIn(3700))
    }

    @Test
    fun aHelperAddsItsLevelToABoost() {
        // 1,000 s at 10 + 8 = 18x, then 2,600 s at 9x, then normal speed.
        val both = Pace(Boost(1000, 10), helperSec = 3600, helperLevel = 8)
        assertEquals(27_000L, both.workDoneIn(2000))
        assertEquals(3700L, both.realSecondsFor(18_000 + 23_400 + 100))
    }

    @Test
    fun aShiftEndingBeforeTheBoost() {
        // 600 s at 24 + 12 = 36x, then the potion's 24x alone.
        val both = Pace(Boost(4000, 24), helperSec = 600, helperLevel = 12)
        assertEquals(21_600L + 24 * 400, both.workDoneIn(1000))
        assertEquals(1000L, both.realSecondsFor(21_600 + 24 * 400))
    }

    @Test
    fun aShiftWithNoHelperLevelDoesNothing() {
        // A shift on a section no helper works for.
        assertEquals(100L, Pace(Boost.NONE, helperSec = 3600, helperLevel = 0).realSecondsFor(100))
    }
}
