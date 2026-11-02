package com.cocwidget

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

// The parser was written against a TH16 export. Later Town Halls add things the export puts in
// sections that export never had, and a user's Logger (a TH18 Guardian) went missing that way.
class NewSectionsTest {
    private fun parse(mutate: (JSONObject) -> Unit): Village {
        val root = JSONObject(TestData.exportJson())
        mutate(root)
        return VillageParser.parse(root.toString(), BUILDER_POTION_MULTIPLIER) { TestData.gameData[it] }!!
    }

    private fun timed(id: Int, lvl: Int, timer: Long) = JSONObject().put("data", id).put("lvl", lvl).put("timer", timer)

    @Test
    fun guardianInASectionTheParserHasNeverSeenIsABuilderJob() {
        val village = parse { it.put("guardians", JSONArray().put(timed(107000008, 1, 100_000))) }
        val logger = village.upgrades.single { it.name == "Logger" }
        assertEquals(Section.BUILDERS, logger.section)
        assertEquals(2, logger.toLevel)
        assertEquals(TestData.gameData[107000008]!!.timeToReach(2), logger.totalSec)
        assertEquals(8, village.upgrades.count { it.section == Section.BUILDERS })
    }

    @Test
    fun unknownSectionsArePlacedByWhatTheGameDataSaysTheyAre() {
        val village = parse {
            // Barbarian is a troop, L.A.S.S.I a pet.
            it.put("something_new", JSONArray().put(timed(4000000, 11, 10)).put(timed(73000000, 6, 10)))
            // The 2 suffix is the Builder Base, as in buildings2 and units2.
            it.put("something_new2", JSONArray().put(timed(1000045, 8, 10)))
        }
        assertEquals(Section.LAB, village.upgrades.single { it.name == "Barbarian" }.section)
        assertEquals(Section.PETS, village.upgrades.single { it.name == "L.A.S.S.I" }.section)
        assertEquals(Section.BUILDER_BASE, village.upgrades.single { it.name == "Multi Mortar" && it.workSec == 10L }.section)
    }

    @Test
    fun timedEntriesNestedInsideABuildingAreFound() {
        // The Crafting Station holds its crafted defences as nested modules.
        val village = parse { root ->
            val buildings = root.getJSONArray("buildings")
            val station = (0 until buildings.length()).map { buildings.getJSONObject(it) }.single { it.optInt("data") == 1000097 }
            val module = timed(102000033, 2, 3_600)
            station.put("types", JSONArray().put(JSONObject().put("data", 103000011).put("modules", JSONArray().put(module))))
        }
        val module = village.upgrades.single { it.workSec == 3_600L }
        assertEquals(Section.BUILDERS, module.section)
        assertEquals("Hot Candle Hitpoints", module.name)
        assertEquals("Hot Candle", module.defence)
        assertEquals(28_800L, module.totalSec)
        assertEquals(3, module.toLevel)
        assertEquals(15, village.upgrades.size)
    }
}
