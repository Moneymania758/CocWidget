package com.cocwidget.game

import com.cocwidget.TestData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

// An export taken while the Clock Tower ran had "clocktower_boost": 1789 and
// "clocktower_cooldown": 80989, which is the boost plus 22 hours.
class ClockTowerTest {
    private fun village(boostSec: Long): Village {
        val root = JSONObject(TestData.exportJson())
        root.getJSONObject("boosts").put("clocktower_boost", boostSec)
        return VillageParser.parse(root.toString()) { TestData.gameData[it] }!!
    }

    @Test
    fun aRunningClockTowerIsTheBuilderBaseBoost() {
        val boost = village(1_789).boostFor(Section.BUILDER_BASE)
        assertEquals(1_789L, boost.seconds)
        assertEquals(CLOCK_TOWER_MULTIPLIER, boost.multiplier)
    }

    @Test
    fun builderBaseUpgradesRunTenTimesFasterWhileItLasts() {
        val village = village(1_789)
        val upgrade = village.upgrades.filter { it.section == Section.BUILDER_BASE }.maxBy { it.workSec }
        // 1,789 seconds at 10x do 17,890 seconds of work, the rest goes at normal speed.
        val realSec = upgrade.workSec - 17_890 + 1_789
        assertEquals(village.exportedAtMillis + realSec * 1000, village.endMillis(upgrade))
    }

    @Test
    fun theHomeVillageIsNotSpedUp() {
        val village = village(1_789)
        val upgrade = village.upgrades.first { it.section == Section.LAB }
        assertEquals(TestData.village().endMillis(upgrade), village.endMillis(upgrade))
    }

    @Test
    fun noBoostWithoutTheField() {
        assertEquals(0L, TestData.village().boostFor(Section.BUILDER_BASE).seconds)
    }
}
