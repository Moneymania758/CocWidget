package com.cocwidget.game

import com.cocwidget.TestData
import com.cocwidget.plan.PotionAdvisor
import com.cocwidget.widget.Countdown
import org.junit.Assert.assertEquals
import org.junit.Test

// Times from an export with a Builder Potion and a Builder Bite both running.
class BuilderBiteTest {
    private val now = 1_800_000_000_000L
    private val potion = Boost(7191, BUILDER_POTION_MULTIPLIER)
    private val bite = Boost(3574, BUILDER_BITE_MULTIPLIER)

    private fun parse(boosts: String) = VillageParser.parse(
        """{"tag":"#TEST","timestamp":1800000000,"buildings":[{"data":1000015,"lvl":3,"timer":100000}],"units":[{"data":4000000,"lvl":3,"timer":100000}],"boosts":{$boosts}}""",
        BUILDER_POTION_MULTIPLIER,
    ) { TestData.gameData[it] }!!

    @Test
    fun theyAddUpWhileBothRun() {
        val both = Pace(potion, snack = bite)
        assertEquals(11L * 3574, both.workDoneIn(3574))
        assertEquals(11L * 3574 + 10 * (7191 - 3574), both.workDoneIn(7191))
    }

    @Test
    fun theBiteBringsALongUpgradeForwardByItsLength() {
        assertEquals(3574L, Pace(potion).realSecondsFor(100_000) - Pace(potion, snack = bite).realSecondsFor(100_000))
    }

    @Test
    fun aBiteWithNoPotionIsDoubleSpeed() {
        assertEquals(200L, Pace(Boost.NONE, snack = bite).workDoneIn(100))
    }

    @Test
    fun readFromTheExportForBuildersOnly() {
        val village = parse(""""builder_boost":7191,"builder_consumable":3574""")
        assertEquals(3574L, village.snackFor(Section.BUILDERS).seconds)
        assertEquals(BUILDER_BITE_MULTIPLIER, village.snackFor(Section.BUILDERS).multiplier)
        assertEquals(0L, village.snackFor(Section.LAB).seconds)
        val builder = village.upgrades.single { it.section == Section.BUILDERS }
        val lab = village.upgrades.single { it.section == Section.LAB }
        assertEquals(now + (7191 + 100_000 - (11 * 3574 + 10 * 3617)) * 1000L, village.endMillis(builder))
        assertEquals(now + 100_000_000, village.endMillis(lab))
    }

    @Test
    fun anExportWithoutOneHasNoBite() {
        assertEquals(0L, parse(""""builder_boost":7191""").builderSnack.seconds)
    }

    @Test
    fun theWidgetRedrawsWhenTheBiteEnds() {
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 1_000_000), builderSnack = Boost(60, BUILDER_BITE_MULTIPLIER))
        assertEquals(now + 60_000, Countdown.nextRefresh(village, now))
    }

    @Test
    fun potionAdviceSaysABiteIsRunning() {
        val village = TestData.villageOf(now, TestData.upgrade(100_000), builderSnack = Boost(600, BUILDER_BITE_MULTIPLIER))
        val advice = PotionAdvisor.advise(village, Section.BUILDERS, 6, BUILDER_POTION_MULTIPLIER, now)
        assertEquals(600L, advice.boostLeftSec)
    }
}
