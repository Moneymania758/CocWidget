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
        assertEquals(12, guide.unit("Super Yeti")?.townHall)
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
            for (line in listOf(tactics.role) + tactics.use + tactics.beware) {
                banned.find(line)?.let { problems += "$name: ${it.value} in \"$line\"" }
            }
        }
        assertEquals(emptyList<String>(), problems)
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
