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

    // Super Yeti showed "None" at levels the other source doesn't list.
    @Test
    fun perHitIsThereAtEveryLevel() {
        // A Builder's Hut really does have no attack at its first levels, so this is for the army.
        for (entry in units.values.filter { it.group !in setOf("defence", "trap") }) {
            val perHit = entry.charts.firstOrNull { it.title == "Damage per hit" } ?: continue
            assertFalse(entry.name, perHit.series.first().values.contains(null))
        }
        assertEquals(550.0, last(chart(unit("Super Yeti").charts, "Damage per hit")))
    }

    // It follows ClashKing's damage, so a change only the newer source has carries through.
    @Test
    fun perHitFollowsTheNewerDamage() {
        assertEquals(588.0, last(chart(unit("Dragon Rider").charts, "Damage per hit")))
        assertEquals(537.5, last(chart(unit("Dragon").charts, "Damage per hit")))
    }

    // Sky Wagon's Balloons sat exactly under its Baby Dragons, so a line in its legend couldn't be seen.
    @Test
    fun everyLineOnAChartCanBeSeen() {
        for (entry in units.values) {
            for (chart in entry.charts + entry.subs.flatMap { it.charts }) {
                val where = "${entry.name} ${chart.title}"
                val lines = chart.series.map { it.values }
                assertEquals(where, lines.size, lines.toSet().size)
                if (lines.size > 1) for (line in lines) assertTrue(where, line.filterNotNull().toSet().size > 1)
            }
        }
    }

    @Test
    fun theStoneSlammerSplitsItsDamageByDistance() {
        val slammer = unit("Stone Slammer")
        assertEquals(listOf("Outer", "Inner"), chart(slammer.charts, "Damage when destroyed").series.map { it.name })
        assertFalse(slammer.stats.any { it.name.startsWith("Damage when destroyed") })
    }

    // Every defence and trap the game lists, apart from the Crafting Station, which has no levels.
    @Test
    fun everyDefenceAndTrapHasAPage() {
        val items = TestData.gameData.values
        val expected = items.filter { it.kind == "trap" && it.village == "home" }.map { it.name }.toSet()
        assertEquals(expected, units.values.filter { it.group == "trap" }.map { it.name }.toSet())
        for (name in listOf("Cannon", "X-Bow", "Inferno Tower", "Eagle Artillery", "Monolith", "Multi-Archer Tower", "Ricochet Cannon", "Firespitter", "Revenge Tower", "Super Wizard Tower", "Spell Tower", "Air Sweeper", "Builder's Hut")) {
            assertEquals(name, "defence", unit(name).group)
        }
        assertFalse(units.values.any { it.name == "Crafting Station" })
    }

    @Test
    fun defenceValuesAreRight() {
        assertEquals(2250.0, last(chart(unit("Cannon").charts, "Health")))
        assertEquals(160.0, last(chart(unit("Cannon").charts, "Damage per second")))
        assertEquals(1400.0, last(chart(unit("Giga Bomb").charts, "Trap damage")))
        assertEquals(5100.0, last(chart(unit("Inferno Tower").charts, "Health")))
        assertEquals(10, unit("Inferno Tower").townHall)
        assertEquals(26_500_000L, unit("Inferno Tower").costs.last())
    }

    // The two sources mean different things by its damage, and the peak is what matters when attacking.
    @Test
    fun theInfernoTowerShowsItsPeakAndItsMultiTargetDamage() {
        val damage = chart(unit("Inferno Tower").charts, "Damage per second")
        assertEquals(3300.0, damage.series.first { it.name == "Single, at its peak" }.values.last())
        assertEquals(155.0, damage.series.first { it.name == "Multi-target" }.values.last())
    }

    @Test
    fun aTownHallsCountAndCapFollowTheGame() {
        val tesla = unit("Multi-Archer Tower").perTownHall
        assertEquals(listOf(16, 17, 18), tesla.map { it.townHall })
        assertEquals(listOf(2, 3, 3), tesla.map { it.count })
        for (entry in units.values) {
            for (row in entry.perTownHall) assertTrue(entry.name, row.cap in 1..entry.levels.size && row.count > 0)
            if (entry.group == "defence" || entry.group == "trap") assertTrue(entry.name, entry.perTownHall.isNotEmpty())
        }
    }

    @Test
    fun defencesSayWhatMakesThemDifferent() {
        val stats = { name: String -> unit(name).stats.associate { it.name to it.value } }
        assertEquals("200 housing deployed", stats("Eagle Artillery")["Wakes up after"])
        assertEquals("7 tiles", stats("Eagle Artillery")["Can't hit closer than"])
        assertTrue(stats("Multi-Archer Tower").getValue("Merged from").contains("Archer Tower level 21"))
        assertTrue(stats("X-Bow").getValue("Air and ground mode").contains("11.5 tiles range"))
        assertTrue(stats("Cannon").getValue("Gear up").contains("needs level 7"))
        assertEquals("From level 1: +60% damage, lasts 18s, recharges in 70s", stats("Spell Tower")["Rage"])
    }

    @Test
    fun superchargesBelongToTheDefence() {
        val charge = unit("X-Bow").subs.single()
        assertEquals("Supercharges", charge.name)
        assertTrue(charge.partOfUnit)
        assertEquals(2, charge.stats.size)
        assertTrue(charge.stats.first().value.contains("+10 damage per second"))
        assertTrue(unit("Cannon").subs.isEmpty())
    }

    @Test
    fun onlyTroopsMove() {
        assertFalse(unit("Cannon").isTroop)
        assertFalse(unit("Lightning Spell").isTroop)
        assertTrue(unit("Dragon").isTroop)
        assertTrue(unit("Battle Blimp").isTroop)
    }

    @Test
    fun aFlatLineBesideMovingOnesIsAFact() {
        val barrels = unit("Sky Wagon").subs.single()
        assertTrue(barrels.stats.any { it.name.contains("balloons") && it.value == "1" })
        assertEquals(listOf("Minions"), barrels.charts.first { it.title.startsWith("Troops per barrel") }.series.map { it.name })
    }

    // A scroll starting on a chart used to change the level, as a level was picked on touching down.
    @Test
    fun aVerticalScrollFromAChartPicksNothing() {
        val touch = ChartTouch(slop = 10f)
        touch.down(100f, 100f)
        assertFalse(touch.move(104f, 60f))
        assertFalse(touch.move(106f, 20f))
        assertFalse(touch.up(106f, 20f))
    }

    @Test
    fun aSidewaysDragPicksAsItGoes() {
        val touch = ChartTouch(slop = 10f)
        touch.down(100f, 100f)
        assertFalse(touch.move(105f, 100f))
        assertTrue(touch.move(130f, 104f))
        assertTrue(touch.move(100f, 140f))
        assertFalse(touch.up(100f, 140f))
    }

    @Test
    fun aTapPicksWhereItLands() {
        val touch = ChartTouch(slop = 10f)
        touch.down(100f, 100f)
        assertFalse(touch.move(103f, 102f))
        assertTrue(touch.up(103f, 102f))
    }

    @Test
    fun aSlowDiagonalThatEndsFarAwayIsNotATap() {
        val touch = ChartTouch(slop = 10f)
        touch.down(100f, 100f)
        assertFalse(touch.move(100f, 200f))
        assertFalse(touch.up(100f, 200f))
    }

    // Inferno Dragon's "91" sat on the next line's dot, and looked like that line's value.
    @Test
    fun labelsGiveWayRatherThanSitOnAnotherLine() {
        assertTrue(ChartScale.endLabelsClear(listOf(100f), 14f, 9f))
        assertTrue(ChartScale.endLabelsClear(listOf(200f, 100f), 14f, 9f))
        assertFalse(ChartScale.endLabelsClear(listOf(200f, 190f, 20f), 14f, 9f))
        assertFalse(ChartScale.endLabelsClear(listOf(200f, 200f), 14f, 9f))
        // The labels clear each other here, but the lower one sits right on the higher line's dot.
        assertFalse(ChartScale.endLabelsClear(listOf(200f, 180f), 14f, 9f))
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
        assertEquals("0", ChartScale.axisLabel(0.0, 20000.0, ""))
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
