package com.cocwidget.guide

import com.cocwidget.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GuideUnitsTest {
    private val units = GuideUnits.parse(File("src/main/assets/guide_units.json").readText())
    private val guide = Guide.parse(
        File("src/main/assets/guide_content.json").readText(),
        File("src/main/assets/guide_facts.json").readText(),
    )

    private fun unit(name: String) = units.values.first { it.name == name }
    private fun chart(owner: List<UnitChart>, title: String) = owner.first { it.title == title }
    private fun first(chart: UnitChart) = chart.series.first().values.first()
    private fun last(chart: UnitChart) = chart.series.first().values.last()

    @Test
    fun everyUnitInTheGuideCanBeOpened() {
        for (entry in guide.facts.army) assertNotNull(entry.name, units[entry.id])
        for (page in guide.content.townHalls) for (strategy in page.strategies) for (name in strategy.army) {
            assertNotNull("TH${page.townHall} ${strategy.name}: $name", guide.unit(name)?.let { units[it.id] })
        }
    }

    @Test
    fun everyLineHasAValueSlotForEveryLevel() {
        for (entry in units.values) {
            assertEquals(entry.name, entry.levels.size, entry.townHalls.size)
            val charts = entry.charts + entry.subs.flatMap { it.charts }
            for (chart in charts) {
                assertTrue("${entry.name} ${chart.title}", chart.series.size in 1..3)
                for (line in chart.series) {
                    assertEquals("${entry.name} ${chart.title}", entry.levels.size, line.values.size)
                    assertTrue("${entry.name} ${chart.title}", line.values.filterNotNull().all { it >= 0 })
                }
            }
        }
    }

    @Test
    fun everyUnitHasSomethingToShow() {
        for (entry in units.values) assertTrue(entry.name, entry.charts.isNotEmpty() || entry.stats.isNotEmpty())
    }

    @Test
    fun townHallsNeverFallAsLevelsRiseAndStartWhereTheUnitOpens() {
        for (entry in units.values) {
            assertEquals(entry.name, entry.townHall, entry.townHalls.first())
            assertEquals(entry.name, entry.townHalls.toList(), entry.townHalls.sorted())
        }
    }

    @Test
    fun unlocksAgreeWithTheGuide() {
        for (entry in guide.facts.army) assertEquals(entry.name, entry.townHall, units.getValue(entry.id).townHall)
    }

    // The Ruin Knight's figures after October 2026's nerf, as the wiki's patch notes give them.
    @Test
    fun ruinWitchHasHerRuinKnights() {
        val knight = unit("Ruin Witch").subs.single { it.name == "Ruin Knight" }
        assertEquals(2900.0, first(chart(knight.charts, "Health")))
        assertEquals(3800.0, last(chart(knight.charts, "Health")))
        assertEquals(240.0, first(chart(knight.charts, "Damage per hit")))
        assertEquals(330.0, last(chart(knight.charts, "Damage per hit")))
        assertEquals(990.0, last(chart(knight.charts, "Damage to walls per second")))
        assertEquals(18, knight.traits?.housing)
        assertTrue(knight.stats.any { it.name.startsWith("Ruin Knights summoned") && it.value == "10" })
    }

    @Test
    fun golemitesAreAFifthOfTheGolem() {
        val golem = unit("Golem")
        val golemite = golem.subs.single { it.name == "Golemite" }
        assertEquals(last(chart(golem.charts, "Health"))!! / 5, last(chart(golemite.charts, "Health"))!!, 0.01)
        assertEquals(last(chart(golem.charts, "Damage per second"))!! / 5, last(chart(golemite.charts, "Damage per second"))!!, 0.01)
    }

    @Test
    fun subUnitsWithoutDataSaySo() {
        for (name in listOf("Yeti", "Meteor Golem", "Druid", "Super Witch")) {
            assertTrue(name, unit(name).subs.single().note.isNotEmpty())
        }
    }

    // ClashKing's numbers win where the sources disagree. Dragon Rider had its July nerf there.
    @Test
    fun baseStatsComeFromTheNewerSource() {
        assertEquals(5900.0, last(chart(unit("Dragon Rider").charts, "Health")))
        assertEquals(490.0, last(chart(unit("Dragon Rider").charts, "Damage per second")))
        assertEquals(580.0, last(chart(unit("Meteor Golem").charts, "Damage per second")))
    }

    @Test
    fun perHitIsDroppedWhereTheSourcesDisagreeOnDamage() {
        val perHit = chart(unit("Dragon Rider").charts, "Damage per hit")
        assertEquals(null, perHit.series.first().values.last())
    }

    @Test
    fun healersHealRatherThanDamage() {
        val healer = unit("Healer")
        assertFalse(healer.charts.any { it.title == "Damage per second" })
        assertEquals(82.0, chart(healer.charts, "Healing per second").series.first().values.last())
    }

    @Test
    fun theFurnaceDoesNoDamage() {
        assertFalse(unit("Furnace").charts.any { it.title == "Damage per second" })
    }

    @Test
    fun spellsChartWhatTheirLevelsChange() {
        assertEquals(720.0, last(chart(unit("Lightning Spell").charts, "Spell damage")))
        assertEquals(190.0, last(chart(unit("Rage Spell").charts, "Damage boost")))
        assertEquals(6.0, last(chart(unit("Freeze Spell").charts, "Lasts")))
        assertEquals(29.0, last(chart(unit("Earthquake Spell").charts, "Damage to buildings")))
        assertEquals(380.0, last(chart(unit("Poison Spell").charts, "Damage per second")))
    }

    @Test
    fun aValueThatNeverChangesIsAFactNotAChart() {
        val rider = unit("Root Rider")
        assertFalse(rider.charts.any { it.title == "Damage to walls per second" })
        assertTrue(rider.stats.any { it.name == "Damage to walls per second" && it.value == "10,000" })
    }

    @Test
    fun superTroopsLinkToTheirBaseAndStartWhereItAllows() {
        val superYeti = unit("Super Yeti")
        assertEquals(unit("Yeti").id, superYeti.baseId)
        assertEquals(superYeti.baseLevel, superYeti.levels.first())
        assertTrue(superYeti.costs.isEmpty())
        assertTrue(superYeti.boostCost > 0)
        assertTrue(unit("Yeti").costs.isNotEmpty())
    }

    @Test
    fun researchCostsAreForReachingEachLevel() {
        val dragon = unit("Dragon")
        assertEquals(0L, dragon.costs.first())
        assertEquals(1_000_000L, dragon.costs[1])
        assertEquals(64_800L, dragon.times[1])
    }

    @Test
    fun capIsTheHighestLevelATownHallAllows() {
        val golem = unit("Golem")
        assertEquals(-1, golem.capIndex(7))
        assertEquals(1, golem.capIndex(8))
        assertEquals(golem.levels.lastIndex, golem.capIndex(18))
    }

    @Test
    fun exportsGiveTroopSpellAndSiegeLevels() {
        val levels = Guide.levels(TestData.th18ExportJson())
        for (name in listOf("Dragon", "Lightning Spell", "Battle Blimp")) assertTrue(name, (levels[unit(name).id] ?: 0) > 0)
    }

    @Test
    fun axisTicksAreRoundAndCoverTheTop() {
        assertEquals(listOf(0.0, 200.0, 400.0, 600.0), ChartScale.ticks(530.0))
        assertEquals(listOf(0.0, 10000.0, 20000.0), ChartScale.ticks(18500.0))
        for (top in listOf(0.7, 3.0, 26.0, 430.0, 3075.0, 18500.0)) {
            val ticks = ChartScale.ticks(top)
            assertTrue("$top: $ticks", ticks.last() >= top)
            assertTrue("$top: $ticks", ticks.size in 2..6)
        }
    }

    @Test
    fun axisLabelsShortenFromTenThousand() {
        assertEquals("5000", ChartScale.axisLabel(5000.0, 9000.0, ""))
        assertEquals("5K", ChartScale.axisLabel(5000.0, 20000.0, ""))
        assertEquals("20%", ChartScale.axisLabel(20.0, 40.0, "%"))
        assertEquals("2.5", ChartScale.axisLabel(2.5, 5.0, " tiles"))
    }

    @Test
    fun aTouchPicksTheNearestLevel() {
        assertEquals(0, ChartScale.nearest(-50f, 0f, 100f, 5))
        assertEquals(4, ChartScale.nearest(500f, 0f, 100f, 5))
        assertEquals(2, ChartScale.nearest(52f, 0f, 100f, 5))
        assertEquals(0, ChartScale.nearest(50f, 0f, 100f, 1))
    }

    @Test
    fun levelLabelsSpreadOutWhenTheyWontFit() {
        assertEquals(1, ChartScale.labelStep(5, 300f, 30f))
        assertEquals(5, ChartScale.labelStep(15, 100f, 25f))
    }
}
