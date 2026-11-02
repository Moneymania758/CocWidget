package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanFormatTest {
    @Test
    fun durationsShowTheTwoLargestUnits() {
        assertEquals("88d 6h", PlanFormat.duration(88 * 86_400L + 6 * 3_600 + 59))
        assertEquals("3d", PlanFormat.duration(3 * 86_400L))
        assertEquals("6h 30m", PlanFormat.duration(6 * 3_600L + 30 * 60))
        assertEquals("45m", PlanFormat.duration(45 * 60L))
        // Under a minute still shows something is left.
        assertEquals("1m", PlanFormat.duration(20))
        assertEquals("0m", PlanFormat.duration(0))
    }

    @Test
    fun amountsMatchClashNinja() {
        assertEquals("5.78M", PlanFormat.amount(5_784_000))
        assertEquals("2.99M", PlanFormat.amount(2_991_500))
        assertEquals("56.5K", PlanFormat.amount(56_500))
        assertEquals("40M", PlanFormat.amount(40_000_000))
        assertEquals("1.2B", PlanFormat.amount(1_200_000_000))
        assertEquals("950", PlanFormat.amount(950))
    }

    @Test
    fun costsListGoldFirst() {
        val costs = mapOf("Dark Elixir" to 2_790_000L, "Elixir" to 358_700_000L, "Gold" to 65_200_000L)
        assertEquals("Gold 65.2M · Elixir 358.7M · Dark Elixir 2.79M", PlanFormat.costs(costs))
    }

    private fun describe(chips: List<PlanFormat.LevelChip>) = chips.joinToString(" ") {
        "${it.level}x${it.count}" + (if (it.running) "R" else "") + (if (it.maxed) "M" else "")
    }

    @Test
    fun levelChipsGroupCopiesOnTheSameLevelLowestFirst() {
        // Five Builder's Huts at TH16: three on 2, one on 3, one on 4, max 6.
        assertEquals("2x3 3x1 4x1", describe(PlanFormat.levelChips(listOf(4, 3, 2, 2, 2), emptyList(), 6)))
    }

    @Test
    fun levelChipsMarkMaxedAndRunningCopies() {
        // Two Bomb Towers: one maxed at 11, one on 10 upgrading to 11.
        assertEquals("10x1R 11x1M", describe(PlanFormat.levelChips(listOf(11, 10), listOf(10), 11)))
        // A running copy on a level that other copies also sit on gets its own chip.
        assertEquals("16x3 16x1R 17x2M", describe(PlanFormat.levelChips(listOf(17, 17, 16, 16, 16, 16), listOf(16), 17)))
    }

    @Test
    fun levelChipsShowUnbuiltCopies() {
        assertEquals("0x2 3x1", describe(PlanFormat.levelChips(listOf(3, 0, 0), emptyList(), 5)))
    }
}
