package com.cocwidget.game

import com.cocwidget.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VillageParserTest {
    private val village = TestData.village()

    private fun named(name: String, section: Section) =
        village.upgrades.single { it.name == name && it.section == section && !it.extra }

    @Test
    fun readsExportHeader() {
        assertEquals("#TEST0000", village.tag)
        assertEquals(1_793_625_123_000L, village.exportedAtMillis)
        assertEquals(16, village.townHall)
        assertTrue(village.hasPetHouse)
    }

    @Test
    fun helpersWithNoWorkDayRunningAreReady() {
        assertEquals(3, village.helpers.apprenticeLevel)
        assertEquals(2, village.helpers.assistantLevel)
        assertEquals(village.exportedAtMillis, village.helpersReadyMillis())
        assertTrue(village.upgrades.none { village.isHelped(it, village.exportedAtMillis) })
    }

    @Test
    fun countsBuilderHutsPlusBob() {
        // Five Builder's Huts across three levels, plus B.O.B's Hut.
        assertEquals(6, village.builders)
    }

    @Test
    fun findsEveryRunningUpgradeInItsSection() {
        val bySection = village.upgrades.groupingBy { it.section }.eachCount()
        assertEquals(7, bySection[Section.BUILDERS])
        assertEquals(2, bySection[Section.LAB])
        assertEquals(1, bySection[Section.PETS])
        assertEquals(4, bySection[Section.BUILDER_BASE])
    }

    @Test
    fun targetLevelIsOneAboveCurrent() {
        assertEquals(62, named("Barbarian King", Section.BUILDERS).toLevel)
        assertEquals(2, named("Angry Jelly", Section.PETS).toLevel)
        assertEquals(9, named("Multi Mortar", Section.BUILDER_BASE).toLevel)
    }

    @Test
    fun goblinSlotsAreMarkedExtra() {
        val extras = village.upgrades.filter { it.extra }.map { it.section to it.name }.toSet()
        assertEquals(setOf(Section.BUILDERS to "Bomb Tower", Section.LAB to "P.E.K.K.A"), extras)
    }

    // Buildings store the time to reach a level, everything else the time to leave one.
    // tools/build-coc-data.js normalises both, and these totals break if it stops doing so.
    @Test
    fun totalsAreTheTimeToReachTheTargetLevel() {
        assertEquals(432_000L, named("Bomb Tower", Section.BUILDERS).totalSec)
        assertEquals(345_600L, named("Barbarian King", Section.BUILDERS).totalSec)
        assertEquals(345_600L, named("Giant", Section.LAB).totalSec)
        assertEquals(259_200L, named("Angry Jelly", Section.PETS).totalSec)
        assertEquals(950_400L, named("Multi Mortar", Section.BUILDER_BASE).totalSec)
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
    fun builderPotionFinishesEarly() {
        // 4,125 s of 10x clears 41,250 s of work, the other 38,466 s run at normal speed.
        val storage = named("Elixir Storage", Section.BUILDERS)
        assertEquals(village.exportedAtMillis + 42_591_000L, village.endMillis(storage))
    }

    @Test
    fun builderBiteOnItsOwnRunsAtDouble() {
        val bitten = TestData.village(TestData.exportJson().replace("builder_boost", "builder_consumable"))
        val storage = bitten.upgrades.single { it.name == "Elixir Storage" }
        assertEquals(bitten.exportedAtMillis + 75_591_000L, bitten.endMillis(storage))
    }

    @Test
    fun petPotionSpeedsUpPets() {
        // 533 s of 24x clears 12,792 s of work.
        val jelly = named("Angry Jelly", Section.PETS)
        assertEquals(village.exportedAtMillis + 92_868_000L, village.endMillis(jelly))
    }

    @Test
    fun labIsNotBoosted() {
        val giant = named("Giant", Section.LAB)
        assertEquals(village.exportedAtMillis + giant.workSec * 1000, village.endMillis(giant))
    }

    @Test
    fun unknownIdsStillShow() {
        val parsed = VillageParser.parse(TestData.exportJson()) { null }!!
        val king = parsed.upgrades.first { it.section == Section.BUILDERS && it.workSec == 46_271L }
        assertEquals("#28000000", king.name)
        assertNull(king.totalSec)
    }

    @Test
    fun rejectsTextThatIsNotAnExport() {
        val lookup: (Int) -> GameItem? = { TestData.gameData[it] }
        assertNull(VillageParser.parse("hello", lookup = lookup))
        assertNull(VillageParser.parse("{}", lookup = lookup))
        assertNull(VillageParser.parse("{\"tag\":\"#X\"}", lookup = lookup))
    }
}
