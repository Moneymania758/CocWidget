package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class BoostTest {
    @Test
    fun noBoostRunsAtNormalSpeed() {
        assertEquals(100L, Boost.NONE.realSecondsFor(100))
        assertEquals(100L, Boost.NONE.workDoneIn(100))
    }

    @Test
    fun workThatFitsInsideTheBoost() {
        val potion = Boost(3600, 10)
        assertEquals(3600L, potion.realSecondsFor(36_000))
        assertEquals(100L, potion.realSecondsFor(1000))
        // Rounds up, so a timer never shows done a second early.
        assertEquals(101L, potion.realSecondsFor(1001))
    }

    @Test
    fun workThatOutlastsTheBoost() {
        // 100 s at 10x clears 1,000 s, the remaining 500 s run at normal speed.
        assertEquals(600L, Boost(100, 10).realSecondsFor(1500))
    }

    @Test
    fun workDoneSlowsWhenTheBoostEnds() {
        val potion = Boost(100, 10)
        assertEquals(500L, potion.workDoneIn(50))
        assertEquals(1050L, potion.workDoneIn(150))
    }
}
