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

    @Test
    fun levelRanges() {
        assertEquals("13–14", PlanFormat.levels(listOf(14, 14, 13)))
        assertEquals("14", PlanFormat.levels(listOf(14, 14)))
        assertEquals("0–2", PlanFormat.levels(listOf(2, 0)))
    }
}
