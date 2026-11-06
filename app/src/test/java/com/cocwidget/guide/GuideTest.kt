package com.cocwidget.guide

import com.cocwidget.TestData
import com.cocwidget.game.Ids
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GuideTest {
    private val items = TestData.gameData
    private val guide = Guide.parse(
        File("src/main/assets/guide_content.json").readText(),
        File("src/main/assets/guide_facts.json").readText(),
    )

    private fun cap(name: String, townHall: Int): Int {
        val fighter = (guide.facts.heroes + guide.facts.pets).first { it.name == name }
        return items.getValue(fighter.id).maxLevelAt(townHall)
    }

    // From Clash Ninja's max levels table, an independent check on the game data.
    @Test
    fun heroCapsMatchClashNinja() {
        val expected = mapOf(
            7 to listOf(10, 0, 0, 0, 0, 0),
            11 to listOf(50, 50, 30, 20, 0, 0),
            13 to listOf(75, 75, 50, 50, 25, 0),
            15 to listOf(90, 90, 70, 65, 40, 10),
            16 to listOf(95, 95, 80, 70, 45, 15),
            18 to listOf(110, 110, 95, 85, 55, 25),
        )
        val heroes = listOf("Barbarian King", "Archer Queen", "Minion Prince", "Grand Warden", "Royal Champion", "Dragon Duke")
        for ((townHall, caps) in expected) assertEquals("TH$townHall", caps, heroes.map { cap(it, townHall) })
    }

    @Test
    fun petCapsMatchClashNinja() {
        assertEquals(10, cap("L.A.S.S.I", 14))
        assertEquals(15, cap("L.A.S.S.I", 15))
        assertEquals(0, cap("Frosty", 14))
        assertEquals(10, cap("Diggy", 17))
        assertEquals(15, cap("Diggy", 18))
        assertEquals(0, cap("Sneezy", 16))
        assertEquals(10, cap("Greedy Raven", 18))
    }

    @Test
    fun townHallBuildingLevelsMatchClashNinja() {
        assertEquals(12, items.getValue(Ids.HERO_HALL).maxLevelAt(18))
        assertEquals(7, items.getValue(Ids.HERO_HALL).maxLevelAt(13))
        assertEquals(8, items.getValue(Ids.PET_HOUSE).maxLevelAt(15))
        assertEquals(0, items.getValue(Ids.PET_HOUSE).maxLevelAt(13))
    }

    @Test
    fun armyUnlocksComeFromTheirBuildings() {
        val expected = mapOf(
            "Dragon" to 7, "Witch" to 9, "Electro Dragon" to 11, "Yeti" to 12, "Dragon Rider" to 13,
            "Root Rider" to 15, "Battle Blimp" to 12, "Lightning Spell" to 5, "Totem Spell" to 16,
        )
        for ((name, townHall) in expected) assertEquals(name, townHall, guide.unit(name)?.townHall)
    }

    @Test
    fun superTroopsWaitForTownHall11AndTheirBaseTroop() {
        assertEquals(11, guide.unit("Super Archer")?.townHall)
        // Super Yeti needs Yeti level 3, which arrives at TH13.
        assertEquals(13, guide.unit("Super Yeti")?.townHall)
        assertEquals(13, guide.unit("Super Hog Rider")?.townHall)
        assertEquals(12, guide.unit("Super Giant")?.townHall)
        assertTrue(guide.facts.army.filter { it.kind == "super" }.all { it.townHall >= 11 })
    }

    @Test
    fun seasonalUnitsAreLeftOut() {
        for (name in listOf("Pumpkin Barbarian", "Santa's Surprise", "Elephant Rider")) assertNull(name, guide.unit(name))
        assertEquals(1, guide.facts.army.count { it.name == "Meteor Golem" })
    }

    @Test
    fun everyUnitHeroAndPetHasTactics() {
        val content = guide.content
        assertEquals(guide.facts.army.map { it.name }.toSet(), content.unitTactics.keys)
        assertEquals(guide.facts.heroes.map { it.name }.toSet(), content.heroTactics.keys)
        assertEquals(guide.facts.pets.map { it.name }.toSet(), content.petTactics.keys)
        val units = GuideUnits.parse(File("src/main/assets/guide_units.json").readText()).values
        assertEquals(units.filter { it.kind == "defence" || it.kind == "trap" }.map { it.name }.toSet(), content.defenceTactics.keys)
        for ((name, tactics) in content.unitTactics + content.defenceTactics) assertTrue(name, tactics.role.isNotBlank())
        for ((name, tactics) in content.unitTactics + content.defenceTactics + content.heroTactics + content.petTactics) {
            assertTrue(name, tactics.use.isNotEmpty() && tactics.beware.isNotEmpty())
        }
    }

    @Test
    fun tacticsNameRealUnitsAndSources() {
        val problems = mutableListOf<String>()
        val sources = guide.content.sources.map { it.key }.toSet()
        for ((name, tactics) in guide.content.unitTactics + guide.content.defenceTactics + guide.content.heroTactics + guide.content.petTactics) {
            for (partner in tactics.partners) {
                if (guide.unit(partner) == null) problems += "$name: no unit called $partner"
                if (partner == name) problems += "$name works with itself"
            }
            for (key in tactics.sources) if (key !in sources) problems += "$name: unknown source $key"
        }
        assertEquals(emptyList<String>(), problems)
    }

    // British spelling for our own words, though the game's Air Defense keeps its name.
    @Test
    fun tacticsFollowTheHouseStyle() {
        val banned = Regex("[\u2013\u2014]|\\bdefense|\\bcolor|behavior|favorite|armor|\\b(?!(?:size|sizes|prize|prizes|seize)\\b)\\w+iz(e|ed|es|ing|ation)\\b")
        val problems = mutableListOf<String>()
        for ((name, tactics) in guide.content.unitTactics + guide.content.defenceTactics + guide.content.heroTactics + guide.content.petTactics) {
            for (line in listOf(tactics.role, tactics.targets) + tactics.allTips.map { it.text }) {
                banned.find(line)?.let { problems += "$name: ${it.value} in \"$line\"" }
                siteNames.find(line)?.let { problems += "$name names a website in \"$line\"" }
            }
        }
        val other = guide.content.threats.flatMap { threat -> listOf(threat.tactics.role) + threat.tactics.allTips.map { it.text } } +
            guide.content.equipmentTips.values.map { it.tip.text } +
            guide.content.upgradeFirst.values.flatMap { it.lines }
        for (line in other) {
            banned.find(line)?.let { problems += "${it.value} in \"$line\"" }
            siteNames.find(line)?.let { problems += "A website in \"$line\"" }
        }
        assertEquals(emptyList<String>(), problems)
    }

    private val siteNames = Regex("Base Drop|AllClash|Clash Ninja|Blueprint|Trophy Coach|1337wiki|COC Base Guide|Skycoach|EZ Arena|GameRant")
    private val units = GuideUnits.parse(File("src/main/assets/guide_units.json").readText())
    private fun opens(name: String) = units.values.first { it.name == name }.townHall

    // Advice for a Town Hall has to be something that player can do, so a line naming something
    // that opens later has to say when it applies.
    @Test
    fun linesNamingSomethingLaterSayWhenTheyApply() {
        val problems = mutableListOf<String>()
        val heroes = guide.facts.heroes.associate { it.name to cap2(it.id) }
        val pets = guide.facts.pets.associate { it.name to cap2(it.id) }
        val infernos = opens("Inferno Tower")
        val allNames = (units.values.associate { it.name to it.townHall } + heroes + pets + mapOf("Inferno" to infernos))
            .toList().sortedByDescending { it.first.length }
        val check = { entry: String, entryOpens: Int, tactics: Tactics ->
            for (tip in tactics.allTips) {
                // "Until Sneezy opens" is fine when the line stops there. The guide starts at TH3.
                if (tip.to > 0) continue
                val floor = maxOf(entryOpens, tip.from, Guide.FIRST_TOWN_HALL)
                // Longest first, so a Wizard Tower isn't also read as a Wizard.
                var rest = tip.text
                val named = mutableListOf<Pair<String, Int>>()
                for ((name, townHall) in allNames) {
                    val pattern = Regex("\\b${Regex.escape(name)}s?\\b")
                    if (pattern.containsMatchIn(rest)) {
                        if (name != entry) named += name to townHall
                        rest = pattern.replace(rest, " ")
                    }
                }
                // A line can offer alternatives, so it only needs one that's open.
                if (named.isNotEmpty() && named.minOf { it.second } > floor) {
                    problems += "$entry names ${named.map { it.first }} with no from: ${tip.text}"
                }
            }
        }
        for ((name, tactics) in guide.content.unitTactics + guide.content.defenceTactics) check(name, opens(name), tactics)
        for ((name, tactics) in guide.content.heroTactics) check(name, heroes.getValue(name), tactics)
        for ((name, tactics) in guide.content.petTactics) check(name, pets.getValue(name), tactics)
        assertEquals(emptyList<String>(), problems)
    }

    private fun cap2(id: Int) = Guide.firstTownHall(items.getValue(id))

    @Test
    fun tipsShowWhatAppliesFirstAndDropWhatHasPassed() {
        val always = Tip("always")
        val early = Tip("early", to = 8)
        val later = Tip("later", from = 9)
        val latest = Tip("latest", from = 12)
        val tips = listOf(latest, later, early, always)
        assertEquals(listOf("early", "always", "later", "latest"), Guide.tipsAt(tips, 5).map { it.text })
        assertEquals(listOf("later", "always", "latest"), Guide.tipsAt(tips, 10).map { it.text })
        assertEquals(listOf("latest", "later", "always"), Guide.tipsAt(tips, 18).map { it.text })
    }

    @Test
    fun withNoTownHallKnownEveryTipShowsAsWritten() {
        val tips = listOf(Tip("later", from = 9), Tip("early", to = 8), Tip("always"))
        assertEquals(listOf("later", "early", "always"), Guide.tipsAt(tips, 0).map { it.text })
    }

    @Test
    fun everyTroopAndSiegeMachineSaysWhatItTargets() {
        val needed = units.values.filter { it.kind in setOf("troop", "super", "siege") }.map { it.name }
        assertEquals(emptyList<String>(), needed.filter { guide.content.unitTactics[it]?.targets.isNullOrBlank() })
    }

    @Test
    fun threatsCoverTheTownHallsTheyShouldAndAreComplete() {
        val threats = guide.content.threats
        assertEquals(3, threats.count { it.kind == "guardian" })
        for (name in listOf("Smasher", "Longshot", "Logger")) assertEquals(name, 18, guide.threat(name)?.from)
        // A Town Hall has its own weapon from TH12 until TH18 replaces it with Guardians.
        for (townHall in 12..17) assertTrue("TH$townHall", threats.any { it.kind == "weapon" && it.isAt(townHall) })
        for (townHall in 3..11) assertTrue("TH$townHall", threats.none { it.isAt(townHall) })
        assertTrue(threats.none { it.kind == "weapon" && it.isAt(18) })
        val sources = guide.content.sources.map { it.key }.toSet()
        for (threat in threats) {
            assertTrue(threat.name, threat.tactics.role.isNotBlank() && threat.tactics.use.isNotEmpty() && threat.tactics.beware.isNotEmpty())
            assertEquals(threat.name, emptyList<String>(), threat.tactics.sources.filter { it !in sources })
            for (partner in threat.tactics.partners) assertNotNull("${threat.name}: $partner", guide.unit(partner))
        }
    }

    @Test
    fun everyPieceOfEquipmentSaysWhatItsFor() {
        assertEquals(emptyList<String>(), guide.facts.equipment.map { it.name }.filter { guide.content.equipmentTips[it]?.tip?.text.isNullOrBlank() })
        assertEquals(guide.facts.equipment.map { it.name }.toSet(), guide.content.equipmentTips.keys)
    }

    @Test
    fun everyTownHallSaysWhatToUpgradeFirst() {
        for (townHall in Guide.FIRST_TOWN_HALL..18) {
            assertTrue("TH$townHall", guide.content.upgradeFirst[townHall]?.lines?.size in 2..4)
        }
    }

    // Counts have to be trainable, or the plan isn't one anybody can follow.
    @Test
    fun everyArmyHasCountsThatFitItsTownHall() {
        val problems = mutableListOf<String>()
        val housing = units.values.associate { it.name to it.traits.housing }
        val kinds = units.values.associate { it.name to it.kind }
        for (page in guide.content.townHalls) {
            val size = guide.content.capacities[page.townHall]
            if (size == null) {
                problems += "TH${page.townHall}: no capacity"
                continue
            }
            for (strategy in page.strategies) {
                val where = "TH${page.townHall} ${strategy.name}"
                if (strategy.counts.keys != strategy.army.toSet()) problems += "$where: counts don't match the army"
                if (strategy.counts.values.any { it < 1 }) problems += "$where: a count under 1"
                if (strategy.army.count { kinds[it] == "siege" } > 1) problems += "$where: more than one Siege Machine"
                if (strategy.army.count { kinds[it] == "super" } > 2) problems += "$where: more than two Super Troop types"
                val (army, spells) = Guide.armyHousing(strategy, housing, kinds)
                if (army > size.army) problems += "$where: army $army over ${size.army}"
                if (spells > size.spells) problems += "$where: spells $spells over ${size.spells}"
                if (army < size.army * 0.8) problems += "$where: army only $army of ${size.army}"
            }
        }
        assertEquals(emptyList<String>(), problems)
    }

    // What a finished base has left follows from how many it may build and what each merge uses.
    @Test
    fun mergedDefenceCountsAddUp() {
        fun count(name: String, townHall: Int) = units.values.first { it.name == name }.perTownHall.last { it.townHall <= townHall }.count
        val ricochet = { townHall: Int -> count("Ricochet Cannon", townHall) }
        val multiArcher = { townHall: Int -> count("Multi-Archer Tower", townHall) }
        assertEquals(3, count("Cannon", 16) - 2 * ricochet(16))
        assertEquals(0, count("Cannon", 17) - 2 * ricochet(17) - count("Multi-Gear Tower", 17))
        assertEquals(4, count("Archer Tower", 16) - 2 * multiArcher(16))
        assertEquals(2, count("Archer Tower", 17) - 2 * multiArcher(17) - count("Multi-Gear Tower", 17))
        assertEquals(2, count("Wizard Tower", 18) - 2 * count("Super Wizard Tower", 18))
    }

    @Test
    fun everySourceListedIsUsed() {
        val used = (guide.content.unitTactics + guide.content.defenceTactics + guide.content.heroTactics + guide.content.petTactics).values.flatMap { it.sources } +
            guide.content.threats.flatMap { it.tactics.sources } +
            guide.content.equipmentTips.values.flatMap { it.sources } +
            guide.content.upgradeFirst.values.flatMap { it.sources } +
            guide.glossary.terms.flatMap { it.sources } +
            guide.content.townHalls.flatMap { page -> page.strategies.flatMap { it.sources } }
        val pairingsAndPets = setOf("A", "P", "G", "N", "C")
        assertEquals(emptyList<String>(), guide.content.sources.map { it.key }.filter { it !in used && it !in pairingsAndPets })
        assertEquals(guide.content.sources.size, guide.content.sources.map { it.url }.toSet().size)
    }

    // Anything the guide recommends has to exist, and be available at the Town Hall it's listed for.
    @Test
    fun everyStrategyIsPossibleAtItsTownHall() {
        val problems = mutableListOf<String>()
        val pieces = guide.facts.equipment.associateBy { it.name }
        for (page in guide.content.townHalls) {
            for (strategy in page.strategies) {
                val where = "TH${page.townHall} ${strategy.name}"
                for (name in strategy.army) {
                    val unit = guide.unit(name)
                    if (unit == null) problems += "$where: no unit called $name"
                    else if (unit.townHall > page.townHall) problems += "$where: $name opens at TH${unit.townHall}"
                }
                for (setup in strategy.heroes) {
                    if (cap(setup.hero, page.townHall) == 0) problems += "$where: no ${setup.hero} yet"
                    if (setup.pet.isNotEmpty() && cap(setup.pet, page.townHall) == 0) problems += "$where: no ${setup.pet} yet"
                    val heroId = guide.facts.heroes.first { it.name == setup.hero }.id
                    for (gear in setup.gear) {
                        val piece = pieces[gear]
                        if (piece == null) problems += "$where: no equipment called $gear"
                        else if (items.getValue(piece.id).hero != heroId) problems += "$where: $gear isn't for ${setup.hero}"
                    }
                }
                val pets = strategy.heroes.map { it.pet }.filter { it.isNotEmpty() }
                if (pets.size != pets.toSet().size) problems += "$where: a pet is used twice"
                for (key in strategy.sources) if (guide.content.sources.none { it.key == key }) problems += "$where: unknown source $key"
                if (strategy.level !in setOf("easy", "medium", "hard")) problems += "$where: unknown level ${strategy.level}"
                if (strategy.steps.isEmpty() || strategy.army.isEmpty() || strategy.sources.isEmpty()) problems += "$where: incomplete"
            }
        }
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun everyTownHallFromThreeHasStrategies() {
        for (townHall in Guide.FIRST_TOWN_HALL..18) {
            assertTrue("TH$townHall", guide.townHall(townHall)?.strategies?.isNotEmpty() == true)
        }
        assertNull(guide.townHall(2))
    }

    @Test
    fun everyHeroAndPetIsCovered() {
        assertEquals(guide.facts.heroes.map { it.name }.toSet(), guide.content.heroes.map { it.hero }.toSet())
        assertEquals(guide.facts.pets.map { it.name }.toSet(), guide.content.pets.keys)
    }

    @Test
    fun pairingsNameRealPetsAndGear() {
        val pets = guide.facts.pets.map { it.name }.toSet()
        for (hero in guide.content.heroes) {
            val heroId = guide.facts.heroes.first { it.name == hero.hero }.id
            for (pick in hero.picks + hero.avoid) assertTrue("${hero.hero}: ${pick.pet}", pick.pet in pets)
            assertEquals(hero.hero, emptyList<String>(), hero.picks.map { it.pet }.filter { it in hero.avoid.map { avoid -> avoid.pet } })
            for (gear in hero.gear) {
                val piece = guide.facts.equipment.firstOrNull { it.name == gear }
                assertNotNull("${hero.hero}: $gear", piece)
                assertEquals("${hero.hero}: $gear", heroId, items.getValue(piece!!.id).hero)
            }
        }
    }

    @Test
    fun petSetUsesEachPetOnceAndOnlyWhatsOpen() {
        for (townHall in 14..18) {
            val set = guide.petSet(townHall, items)
            val pets = set.values.filterNotNull()
            assertEquals("TH$townHall", pets.size, pets.toSet().size)
            for (pet in pets) assertTrue("TH$townHall $pet", cap(pet, townHall) > 0)
            for (hero in set.keys) assertTrue("TH$townHall $hero", cap(hero, townHall) > 0)
        }
    }

    @Test
    fun petSetAtTheTopGivesEveryHeroAPetAndAvoidsSneezyOnTheDuke() {
        val set = guide.petSet(18, items)
        assertEquals(6, set.size)
        assertFalse(set.values.contains(null))
        assertTrue(set["Dragon Duke"] != "Sneezy")
    }

    @Test
    fun petSetHasNoPetsBeforeThePetHouse() {
        assertTrue(guide.petSet(13, items).values.all { it == null })
    }

    @Test
    fun readsHeroAndPetLevelsFromAnExport() {
        val levels = Guide.levels(TestData.th18ExportJson())
        val king = guide.facts.heroes.first { it.name == "Barbarian King" }.id
        assertTrue((levels[king] ?: 0) > 0)
        assertTrue(guide.facts.pets.any { (levels[it.id] ?: 0) > 0 })
    }

    @Test
    fun townHallAdditionsTellNewFromMore() {
        val th14 = Guide.additions(14, items)
        assertTrue(th14.any { it.item.name == "Pet House" && it.first })
        assertTrue(th14.any { it.item.name == "Giant Bomb" && !it.first })
        assertTrue(Guide.additions(10, items).any { it.item.name == "Inferno Tower" && it.quantity == 2 && it.first })
    }

    @Test
    fun rangesReadAsTiles() {
        assertEquals("1", Guide.tiles(100))
        assertEquals("4.5", Guide.tiles(450))
        assertEquals("1.25", Guide.tiles(125))
    }
}
