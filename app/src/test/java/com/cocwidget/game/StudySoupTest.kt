package com.cocwidget.game

import com.cocwidget.TestData
import com.cocwidget.plan.PotionAdvisor
import com.cocwidget.widget.Countdown
import org.junit.Assert.assertEquals
import org.junit.Test

// Study Soup shows in the export as lab_consumable, with no lab_boost.
class StudySoupTest {
    private val now = 1_800_000_000_000L

    private fun parse(boosts: String) = VillageParser.parse(
        """{"tag":"#TEST","timestamp":1800000000,"buildings":[{"data":1000015,"lvl":3,"timer":100000}],"units":[{"data":4000000,"lvl":3,"timer":100000}],"pets":[{"data":73000000,"lvl":1,"timer":100000}],"boosts":{$boosts}}""",
    ) { TestData.gameData[it] }!!

    private fun Village.upgradeIn(section: Section) = upgrades.single { it.section == section }

    @Test
    fun aSoupOnItsOwnRunsResearchAndPetsAtFourTimes() {
        val village = parse(""""lab_consumable":3600""")
        val expected = now + (3600 + 100_000L - 4 * 3600) * 1000
        assertEquals(expected, village.endMillis(village.upgradeIn(Section.LAB)))
        assertEquals(expected, village.endMillis(village.upgradeIn(Section.PETS)))
    }

    @Test
    fun aSoupLeavesBuildersAlone() {
        val village = parse(""""lab_consumable":3600""")
        assertEquals(now + 100_000_000, village.endMillis(village.upgradeIn(Section.BUILDERS)))
        assertEquals(0L, village.snackFor(Section.BUILDERS).seconds)
        assertEquals(0L, village.snackFor(Section.BUILDER_BASE).seconds)
    }

    @Test
    fun aBiteLeavesResearchAlone() {
        val village = parse(""""builder_consumable":3600""")
        assertEquals(now + 100_000_000, village.endMillis(village.upgradeIn(Section.LAB)))
    }

    @Test
    fun aPotionAndASoupAddUpToTwentySeven() {
        val pace = Pace(Boost(3600, RESEARCH_POTION_MULTIPLIER), snack = Boost(3600, STUDY_SOUP_MULTIPLIER))
        assertEquals(27L * 3600, pace.workDoneIn(3600))
    }

    @Test
    fun aBiteAndASoupWithNoPotionsAreBothCounted() {
        val village = parse(""""builder_consumable":3594,"lab_consumable":3592""")
        assertEquals(now + (100_000L - 3594) * 1000, village.endMillis(village.upgradeIn(Section.BUILDERS)))
        assertEquals(now + (100_000L - 3 * 3592) * 1000, village.endMillis(village.upgradeIn(Section.LAB)))
        assertEquals(now + (100_000L - 3 * 3592) * 1000, village.endMillis(village.upgradeIn(Section.PETS)))
    }

    @Test
    fun theBoostFieldsAreAlwaysPotions() {
        val village = parse(""""builder_boost":3600,"lab_boost":3600,"pet_boost":3600""")
        assertEquals(BUILDER_POTION_MULTIPLIER, village.boostFor(Section.BUILDERS).multiplier)
        assertEquals(RESEARCH_POTION_MULTIPLIER, village.boostFor(Section.LAB).multiplier)
        assertEquals(PET_POTION_MULTIPLIER, village.boostFor(Section.PETS).multiplier)
    }

    @Test
    fun anExportWithoutOneHasNoSoup() {
        assertEquals(0L, parse(""""lab_boost":3600""").labSnack.seconds)
    }

    @Test
    fun theWidgetRedrawsWhenTheSoupEnds() {
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 1_000_000), labSnack = Boost(60, STUDY_SOUP_MULTIPLIER))
        assertEquals(now + 60_000, Countdown.nextRefresh(village, now))
    }

    @Test
    fun potionAdviceSaysASoupIsRunning() {
        val village = TestData.villageOf(now, TestData.upgrade(100_000, section = Section.LAB), labSnack = Boost(600, STUDY_SOUP_MULTIPLIER))
        val advice = PotionAdvisor.advise(village, Section.LAB, 2, RESEARCH_POTION_MULTIPLIER, now)
        assertEquals(600L, advice.boostLeftSec)
    }
}
