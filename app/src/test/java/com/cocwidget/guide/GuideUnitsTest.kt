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

    // ClashKing's first level and every guide read say TH15. One source's count table says TH14.
    @Test
    fun theMonolithOpensAtTownHall15() {
        assertEquals(15, unit("Monolith").townHall)
        assertEquals(15, unit("Monolith").perTownHall.first().townHall)
    }

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
        assertTrue(stats("X-Bow").getValue("Air and ground mode").contains("11.5 tile range"))
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
    fun preferredTargetsAreWords() {
        assertEquals("Heroes", unit("Headhunter").preferredTarget)
        for (entry in units.values) assertFalse(entry.name, entry.preferredTarget.startsWith("["))
        assertEquals("", unit("Multi-Gear Tower").preferredTarget)
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

    @Test
    fun everyHeroHasHealthAndDamageCharts() {
        for (hero in guide.facts.heroes) {
            val entry = units.getValue(hero.id)
            assertEquals(hero.name, "hero", entry.kind)
            assertEquals(hero.name, hero.hitpoints.toDouble(), last(chart(entry.charts, "Health")))
            assertEquals(hero.name, hero.dps.toDouble(), last(chart(entry.charts, "Damage per second")))
        }
    }

    @Test
    fun everyPieceOfEquipmentHasCharts() {
        assertEquals(43, guide.facts.equipment.size)
        for (piece in guide.facts.equipment) {
            val entry = units.getValue(piece.id)
            assertEquals(piece.name, "equipment", entry.kind)
            assertTrue(piece.name, entry.charts.isNotEmpty())
        }
    }

    @Test
    fun heroCapsAndCostsMatchTheTimeToMaxData() {
        for (hero in guide.facts.heroes) {
            val entry = units.getValue(hero.id)
            val item = TestData.gameData.getValue(hero.id)
            for (townHall in entry.townHall..18) assertEquals("${hero.name} TH$townHall", item.maxLevelAt(townHall), entry.levels[entry.capIndex(townHall)])
            for ((index, level) in entry.levels.withIndex().drop(1)) {
                assertEquals("${hero.name} level $level", item.costToReach(level), entry.costs[index])
            }
        }
    }

    // The data lets most epics in at TH8, whichever hero they're for.
    @Test
    fun equipmentCapsFollowTheGameOnceItsHeroHasOpened() {
        for (piece in guide.facts.equipment) {
            val entry = units.getValue(piece.id)
            val item = TestData.gameData.getValue(piece.id)
            val hero = units.getValue(item.hero)
            assertTrue(piece.name, entry.townHall >= hero.townHall)
            assertEquals(piece.name, -1, entry.capIndex(hero.townHall - 1))
            for (townHall in entry.townHall..18) assertEquals("${piece.name} TH$townHall", item.maxLevelAt(townHall), entry.levels[entry.capIndex(townHall)])
        }
    }

    // Supercell's balance table of 22 May 2026. The other source still has the old 2,000 and 2,200.
    @Test
    fun equipmentFollowsTheNewerDamage() {
        val arrow = chart(unit("Giant Arrow").charts, "Ability damage").series.first().values
        assertEquals(listOf(1100.0, 1200.0, 1350.0, 1500.0), listOf(8, 11, 14, 17).map { arrow[it] })
        assertEquals(2150.0, last(chart(unit("Rocket Backpack").charts, "Ability damage")))
    }

    // The other source has these two pieces' ids swapped, so each could show the other's numbers.
    @Test
    fun monolithArrowAndElectroFangsKeepTheirOwnNumbers() {
        assertEquals(27, unit("Monolith Arrow").levels.size)
        assertEquals(1160.0, last(chart(unit("Monolith Arrow").charts, "Extra hero health")))
        assertEquals(18, unit("Electro Fangs").levels.size)
        assertEquals(2400.0, last(chart(unit("Electro Fangs").charts, "Extra hero health")))
    }

    @Test
    fun aPieceWithoutABonusHasNoLineOfZeros() {
        assertEquals(listOf("Lasts"), unit("Eternal Tome").charts.map { it.title })
    }

    // The other source has 2,700 at level 15, but levels 15 to 17 share one ability level.
    @Test
    fun theFlameBlowerHasNoSpikeAtLevel15() {
        val damage = chart(unit("Flame Blower").charts, "Ability damage").series.first().values
        assertEquals(listOf(2250.0, 2250.0, 2250.0), damage.subList(14, 17))
    }

    // A fall is nearly always a typo in a source, and it shows on a chart as a spike.
    @Test
    fun heroAndEquipmentLinesNeverFallAsLevelsRise() {
        val falls = setOf("Time between strikes")
        for (entry in units.values.filter { it.kind == "hero" || it.kind == "equipment" }) {
            for (chart in entry.charts.filter { it.title !in falls }) {
                for (line in chart.series) {
                    val values = line.values.filterNotNull()
                    assertEquals("${entry.name} ${chart.title}", values.sorted(), values)
                }
            }
        }
    }

    @Test
    fun thePiecesThatGetFasterGetFaster() {
        val times = chart(unit("Meteor Staff").charts, "Time between strikes").series.first().values.filterNotNull()
        assertEquals(times.sortedDescending(), times)
    }

    @Test
    fun noLineIsFlatOrEmpty() {
        for (entry in units.values.filter { it.kind == "hero" || it.kind == "equipment" }) {
            for (chart in entry.charts) {
                for (line in chart.series) {
                    val values = line.values.filterNotNull()
                    assertTrue("${entry.name} ${chart.title}", values.toSet().size > 1)
                    assertTrue("${entry.name} ${chart.title}", values.max() > 0)
                }
            }
        }
    }

    // Pages are found by name, so two things sharing one would open the wrong page.
    @Test
    fun noTwoPagesShareAName() {
        val names = units.values.map { it.name }
        assertEquals(names.toSet().size, names.size)
    }

    @Test
    fun aHeroesDamagePerHitIsItsDamagePerSecondTimesItsAttackTime() {
        for (hero in units.values.filter { it.kind == "hero" }) {
            val perSecond = chart(hero.charts, "Damage per second").series.first().values
            val perHit = chart(hero.charts, "Damage per hit").series.first().values
            for (index in hero.levels.indices) {
                assertEquals("${hero.name} level ${hero.levels[index]}", perSecond[index]!! * hero.traits.attackSpeed, perHit[index]!!, 0.01)
            }
        }
    }

    @Test
    fun everyHeroLevelPastTheFirstHasACostAndATime() {
        for (hero in units.values.filter { it.kind == "hero" }) {
            assertEquals(hero.name, 0L, hero.costs.first())
            assertEquals(hero.name, 0L, hero.times.first())
            assertTrue(hero.name, hero.costs.drop(1).all { it > 0 })
            assertTrue(hero.name, hero.times.drop(1).all { it > 0 })
            assertTrue(hero.name, hero.resource.isNotEmpty())
        }
    }

    @Test
    fun everyPieceBelongsToAHeroWithAPage() {
        for (piece in units.values.filter { it.kind == "equipment" }) {
            val hero = units[TestData.gameData.getValue(piece.id).hero]
            assertEquals(piece.name, "hero", hero?.kind)
        }
    }

    @Test
    fun everyPieceHasOurOwnWordsForIt() {
        for (piece in units.values.filter { it.kind == "equipment" }) {
            assertTrue(piece.name, guide.content.equipmentTips.getValue(piece.name).tip.text.isNotBlank())
        }
    }
}
