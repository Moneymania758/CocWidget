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
    private fun at(name: String, title: String, level: Int): Double? =
        chart(unit(name).charts, title).series.first().values[unit(name).levels.indexOf(level)]

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
    // Only the wiki's data has them, and it has none yet for TH19's level 5, so that stays empty.
    @Test
    fun ruinWitchHasHerRuinKnights() {
        val knight = unit("Ruin Witch").subs.single { it.name == "Ruin Knight" }
        val four = unit("Ruin Witch").levels.indexOf(4)
        assertEquals(2900.0, first(chart(knight.charts, "Health")))
        assertEquals(3800.0, chart(knight.charts, "Health").series.first().values[four])
        assertEquals(240.0, first(chart(knight.charts, "Damage per hit")))
        assertEquals(330.0, chart(knight.charts, "Damage per hit").series.first().values[four])
        assertEquals(990.0, chart(knight.charts, "Damage to walls per second").series.first().values[four])
        assertEquals(null, last(chart(knight.charts, "Health")))
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
        assertEquals(5900.0, at("Dragon Rider", "Health", 6))
        assertEquals(490.0, at("Dragon Rider", "Damage per second", 6))
        assertEquals(580.0, at("Meteor Golem", "Damage per second", 3))
    }

    // Super Yeti showed "None" at levels the other source doesn't list.
    @Test
    fun perHitIsThereAtEveryLevel() {
        // A Builder's Hut really does have no attack at its first levels, so this is for the army.
        for (entry in units.values.filter { it.group !in setOf("defence", "trap") }) {
            val perHit = entry.charts.firstOrNull { it.title == "Damage per hit" } ?: continue
            assertFalse(entry.name, perHit.series.first().values.contains(null))
        }
        assertEquals(550.0, at("Super Yeti", "Damage per hit", 8))
    }

    // It follows ClashKing's damage, so a change only the newer source has carries through.
    @Test
    fun perHitFollowsTheNewerDamage() {
        assertEquals(588.0, at("Dragon Rider", "Damage per hit", 6))
        assertEquals(537.5, at("Dragon", "Damage per hit", 13))
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

    // The wiki data has no TH19 defences yet, so these two come from ClashKing and Supercell's notes.
    @Test
    fun theTh19DefencesHaveTheirPages() {
        val spiky = unit("Spiky Tower")
        assertEquals("defence", spiky.group)
        assertEquals(listOf(19, 19), spiky.townHalls.toList())
        assertEquals(6363.0, last(chart(spiky.charts, "Health")))
        assertEquals(9.0, last(chart(spiky.charts, "Spike damage")))
        val firecracker = unit("Firecracker Tower")
        assertEquals(280.0, last(chart(firecracker.charts, "Damage per second")))
        assertEquals(501.0, last(chart(firecracker.charts, "Damage per hit")))
        assertEquals(385.0, last(chart(firecracker.charts, "Spark damage")))
        assertEquals("1 × Archer Tower level 21, 1 × Scattershot level 7", firecracker.stats.single { it.name == "Merged from" }.value)
    }

    @Test
    fun defenceValuesAreRight() {
        assertEquals(2250.0, last(chart(unit("Cannon").charts, "Health")))
        assertEquals(160.0, last(chart(unit("Cannon").charts, "Damage per second")))
        assertEquals(1400.0, last(chart(unit("Giga Bomb").charts, "Trap damage")))
        assertEquals(5100.0, last(chart(unit("Inferno Tower").charts, "Health")))
        assertEquals(10, unit("Inferno Tower").townHall)
        // 26.5M before TH19 cut it.
        assertEquals(20_000_000L, unit("Inferno Tower").costs.last())
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
        val charge = unit("Inferno Tower").subs.single()
        assertEquals("Supercharges", charge.name)
        assertTrue(charge.partOfUnit)
        assertEquals(2, charge.stats.size)
        assertTrue(charge.stats.first().value.contains("+10 damage per second"))
        assertEquals("For a defence at its final level, from Town Hall 18.", charge.note)
        assertTrue(unit("Cannon").subs.isEmpty())
        // TH19 gave the X-Bow a new level and took its supercharges away.
        assertTrue(unit("X-Bow").subs.isEmpty())
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
        assertEquals(82.0, at("Healer", "Healing per second", 11))
    }

    @Test
    fun theFurnaceDoesNoDamage() {
        assertFalse(unit("Furnace").charts.any { it.title == "Damage per second" })
    }

    @Test
    fun spellsChartWhatTheirLevelsChange() {
        assertEquals(720.0, at("Lightning Spell", "Spell damage", 13))
        assertEquals(190.0, last(chart(unit("Rage Spell").charts, "Damage boost")))
        assertEquals(6.0, last(chart(unit("Freeze Spell").charts, "Lasts")))
        assertEquals(29.0, last(chart(unit("Earthquake Spell").charts, "Damage to buildings")))
        assertEquals(380.0, at("Poison Spell", "Damage per second", 12))
        // Spell effects come only from the wiki's data, which has nothing yet for TH19's level 13.
        assertEquals(null, at("Poison Spell", "Damage per second", 13))
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

    // Squads take members at the player's own levels, so a page has the ability, not health or damage.
    @Test
    fun squadsHaveMembersAndTheirAbility() {
        val squads = units.values.filter { it.kind == "squad" }
        assertEquals(12, squads.size)
        assertTrue(squads.all { it.group == "squad" && it.squadMembers.isNotEmpty() && it.stats.none { stat -> stat.name == "Health" } })
        assertEquals((1..12).toList(), squads.map { it.squadBarracks }.sorted())
        val barch = unit("Barch Squad")
        assertEquals(listOf(4000000 to 15, 4000001 to 15), barch.squadMembers.map { it.id to it.count })
        assertEquals(listOf(null, 6.0, 5.0), chart(barch.charts, "One splash hits at most").series.single().values)
        assertEquals(listOf(8, 19, 19), barch.townHalls.toList())
        // ClashKing has no housing for these two. The notes give 46 and 26, dropping at level 3.
        assertEquals(46, unit("P.E.K.K.A Titan Squad").traits.housing)
        assertEquals(listOf(26.0, 26.0, 24.0), chart(unit("Baby Dragon Squad").charts, "Housing").series.single().values)
        assertEquals(listOf(null, 2.0, 3.0), chart(unit("Healer Squad").charts, "Can't be hurt after landing").series.single().values)
    }

    // Supercell's notes, as neither data source has the spell's effects yet.
    @Test
    fun theHeroicSpellHasTheNotesFigures() {
        assertEquals(listOf(34.0, 36.0, 38.0, 40.0), chart(unit("Heroic Spell").charts, "Damage taken reduced").series.single().values)
        assertEquals(12.75, at("Heroic Spell", "Lasts", 4))
        assertEquals(listOf(17, 17, 18, 19), unit("Heroic Spell").townHalls.toList())
    }

    // TH19 numbered most Super Troops from 1. Super Barbarian 4 is what was 8: same Town Hall, health
    // and damage. Starting at the Barbarian's level 8 left only its top three levels.
    @Test
    fun renumberedSuperTroopsKeepEveryLevel() {
        val barbarian = unit("Super Barbarian")
        assertEquals(listOf(4, 5, 6, 7, 8, 9, 10), barbarian.levels.toList())
        assertEquals(listOf(11, 12, 14, 15, 16, 18, 19), barbarian.townHalls.toList())
        assertEquals(180.0, at("Super Barbarian", "Damage per second", 4))
        assertEquals(8, barbarian.baseLevel)
        // The other source still has the old numbers (its 9 is our 5), and no TH19 level, where the
        // chain is worked out from damage per second like the first hit.
        val chain = chart(unit("Super Wizard").charts, "Damage per hit").series.single { it.name == "Chain" }
        assertEquals(176.0, chain.values.first())
        assertEquals(292.0, chain.values.last())
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
        assertEquals(golem.levels.indexOf(15), golem.capIndex(18))
        assertEquals(golem.levels.lastIndex, golem.capIndex(19))
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
