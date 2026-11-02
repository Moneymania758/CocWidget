package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Th18ExportTest {
    private val village = TestData.th18Village()

    @Test
    fun readsHeader() {
        assertEquals("#TEST0001", village.tag)
        assertEquals(18, village.townHall)
        assertTrue(village.hasPetHouse)
    }

    @Test
    fun loggerUpgradeIsFoundInTheGuardiansSection() {
        val logger = village.upgrades.single { it.name == "Logger" }
        assertEquals(Section.BUILDERS, logger.section)
        assertEquals(4, logger.toLevel)
        assertEquals(70_789L, logger.workSec)
        assertEquals(950_400L, logger.totalSec)
    }

    // The user saw 5/6 with all six builders busy: three traps, two heroes and the Logger.
    @Test
    fun allSixBuildersAreBusy() {
        assertEquals(6, village.builders)
        assertEquals(6, village.upgrades.count { it.section == Section.BUILDERS })
    }

    @Test
    fun findsEveryRunningUpgradeInItsSection() {
        val bySection = village.upgrades.groupingBy { it.section }.eachCount()
        assertEquals(6, bySection[Section.BUILDERS])
        assertEquals(1, bySection[Section.LAB])
        assertEquals(1, bySection[Section.PETS])
        assertEquals(null, bySection[Section.BUILDER_BASE])
    }

    @Test
    fun helperFieldsDontHideUpgrades() {
        // One of the three Seeking Air Mines carries helper_recurrent, the Goblin helper_timer.
        assertEquals(3, village.upgrades.count { it.name == "Seeking Air Mine" })
        assertEquals(77_936L, village.upgrades.single { it.name == "Goblin" }.workSec)
    }

    @Test
    fun noTimerExceedsItsUpgradeTotal() {
        for (upgrade in village.upgrades) {
            val total = upgrade.totalSec
            assertTrue("${upgrade.name} has no total", total != null)
            assertTrue("${upgrade.name}: ${upgrade.workSec}s left of $total", upgrade.workSec <= total!!)
        }
    }

    @Test
    fun researchPotionSpeedsUpTheLab() {
        // 4,786 s of 24x covers the Goblin's 77,936 s of work: 77,936 / 24 rounded up.
        val goblin = village.upgrades.single { it.name == "Goblin" }
        assertEquals(village.exportedAtMillis + 3_248_000L, village.endMillis(goblin))
    }

    @Test
    fun studySoupIsSlower() {
        // 4,786 s of 4x clears 19,144 s, the remaining 58,792 s run at normal speed.
        val souped = TestData.th18Village(STUDY_SOUP_MULTIPLIER)
        val goblin = souped.upgrades.single { it.name == "Goblin" }
        assertEquals(souped.exportedAtMillis + 63_578_000L, souped.endMillis(goblin))
    }

    @Test
    fun builderPotionStillAppliesToHeroes() {
        // 1,206 s of 10x clears 12,060 s of the Queen's 48,146 s.
        val queen = village.upgrades.single { it.name == "Archer Queen" }
        assertEquals(village.exportedAtMillis + 37_292_000L, village.endMillis(queen))
    }
}
