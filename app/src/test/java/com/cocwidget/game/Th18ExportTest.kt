package com.cocwidget.game

import com.cocwidget.TestData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // A maxed Builder Base runs nothing, but the widget still shows it so its view buttons appear.
    @Test
    fun builderBaseWithNothingRunningIsStillThere() {
        assertTrue(village.upgrades.none { it.section == Section.BUILDER_BASE })
        assertTrue(village.hasBuilderBase)
    }

    @Test
    fun noBuilderBaseBeforeTheBoatIsFixed() {
        val root = JSONObject(TestData.th18ExportJson())
        root.remove("buildings2")
        val village = VillageParser.parse(root.toString()) { TestData.gameData[it] }!!
        assertFalse(village.hasBuilderBase)
        assertFalse(village.withUpgrades(emptyList()).hasBuilderBase)
        assertTrue(TestData.th18Village().withUpgrades(emptyList()).hasBuilderBase)
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
    fun readsHelpers() {
        assertEquals(8, village.helpers.apprenticeLevel)
        assertEquals(12, village.helpers.assistantLevel)
        assertEquals(village.exportedAtMillis + 71_792_000L, village.helpersReadyMillis())
    }

    @Test
    fun readsWhichUpgradesHelpersAreOn() {
        assertEquals(3_063L, village.upgrades.single { it.name == "Goblin" }.helperSec)
        assertEquals(listOf(20_392L), village.upgrades.filter { it.helperRepeats }.map { it.workSec })
        // The Logger carries helper_timer 0: no shift running on it.
        assertEquals(0L, village.upgrades.single { it.name == "Logger" }.helperSec)
    }

    @Test
    fun helperChipShowsWhileTheShiftRunsOrTheAssignmentRepeats() {
        val goblin = village.upgrades.single { it.name == "Goblin" }
        val shiftEnd = village.exportedAtMillis + 3_063_000L
        assertTrue(village.isHelped(goblin, shiftEnd - 1))
        assertFalse(village.isHelped(goblin, shiftEnd))

        val repeating = village.upgrades.single { it.helperRepeats }
        assertTrue(village.isHelped(repeating, shiftEnd + 86_400_000L))
        assertFalse(village.isHelped(village.upgrades.single { it.name == "Logger" }, village.exportedAtMillis))
    }

    @Test
    fun researchPotionAndLabAssistantAddUp() {
        // During the Assistant's 3,063 s shift research runs at 24 + 12 = 36x, which clears the
        // Goblin's 77,936 s of work in 2,165 s. The potion alone would take 3,248 s.
        val goblin = village.upgrades.single { it.name == "Goblin" }
        assertEquals(village.exportedAtMillis + 2_165_000L, village.endMillis(goblin))
    }

    @Test
    fun studySoupIsSlower() {
        // 3,063 s at 4 + 12 = 16x clears 49,008 s, the soup's last 1,723 s at 4x clear 6,892 s,
        // and the remaining 22,036 s run at normal speed.
        val souped = TestData.th18Village(TestData.th18ExportJson().replace("lab_boost", "lab_consumable"))
        val goblin = souped.upgrades.single { it.name == "Goblin" }
        assertEquals(souped.exportedAtMillis + 26_822_000L, souped.endMillis(goblin))
    }

    @Test
    fun builderPotionStillAppliesToHeroes() {
        // 1,206 s of 10x clears 12,060 s of the Queen's 48,146 s.
        val queen = village.upgrades.single { it.name == "Archer Queen" }
        assertEquals(village.exportedAtMillis + 37_292_000L, village.endMillis(queen))
    }
}
