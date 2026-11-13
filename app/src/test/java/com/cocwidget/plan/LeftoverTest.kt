package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.game.Ids
import com.cocwidget.game.Section
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeftoverTest {
    private val data = TestData.gameData

    private fun export(
        vararg sections: String,
        buildings: Map<Int, Int?> = emptyMap(),
        townHall: Int = 18,
        edit: (JSONObject) -> Unit = {},
    ) = TestData.maxedTh18(*sections, buildings = buildings, townHall = townHall, edit = edit)

    @Test
    fun theRealExportsHaveSomethingLeftToStartEverywhere() {
        assertEquals(emptySet<Section>(), Leftover.of(TestData.exportJson(), data))
        assertEquals(emptySet<Section>(), Leftover.of(TestData.th18ExportJson(), data))
    }

    @Test
    fun maxedPetsInAMaxedPetHouseLeaveNothingToStart() {
        val left = Leftover.of(export("pets", buildings = mapOf(Ids.PET_HOUSE to null)), data)
        assertEquals(setOf(Section.PETS), left)
    }

    // A higher Pet House unlocks more pets, so the pets aren't done while it can go up.
    @Test
    fun maxedPetsUnderAPetHouseThatCanGoUpStillHaveSomethingToStart() {
        val max = data.getValue(Ids.PET_HOUSE).maxLevelAt(18)
        assertFalse(Section.PETS in Leftover.of(export("pets", buildings = mapOf(Ids.PET_HOUSE to max - 1)), data))
    }

    @Test
    fun maxedResearchInAMaxedLabLeavesNothingToStart() {
        val json = export("units", "spells", "siege_machines", buildings = mapOf(Ids.LABORATORY to null))
        assertTrue(Section.LAB in Leftover.of(json, data))
    }

    @Test
    fun aLastLevelStillRunningCountsAsStarted() {
        val json = export("pets", buildings = mapOf(Ids.PET_HOUSE to null)) { root ->
            val pet = root.getJSONArray("pets").getJSONObject(0)
            pet.put("lvl", pet.getInt("lvl") - 1)
            pet.put("timer", 3_600)
        }
        assertTrue(Section.PETS in Leftover.of(json, data))
    }

    @Test
    fun oneLevelNotStartedIsSomethingToStart() {
        val json = export("pets", buildings = mapOf(Ids.PET_HOUSE to null)) { root ->
            val pet = root.getJSONArray("pets").getJSONObject(0)
            pet.put("lvl", pet.getInt("lvl") - 1)
        }
        assertFalse(Section.PETS in Leftover.of(json, data))
    }

    // The data doesn't know a new Town Hall's levels yet, so everything stays as it was.
    @Test
    fun aTownHallNewerThanTheDataIsNeverDone() {
        val top = data.getValue(Ids.TOWN_HALL).levelCount
        assertEquals(emptySet<Section>(), Leftover.of(export("pets", buildings = mapOf(Ids.PET_HOUSE to null), townHall = top + 1), data))
    }

    @Test
    fun somethingThatIsntAnExportIsNeverDone() {
        assertEquals(emptySet<Section>(), Leftover.of("not an export", data))
    }

    private fun item(id: Int, vararg steps: Planner.LevelStep) =
        Planner.Item(id, "Item $id", listOf(1), emptyList(), 10, 0, emptyMap(), steps = steps.toList())

    private fun plan(vararg groups: Pair<Planner.Group, List<Planner.Item>>) =
        Planner.Plan(16, 6, groups.map { (group, items) -> Planner.GroupPlan(group, items, 0, emptyMap(), 0f) }, 0, 0)

    // Walls take no time, so they keep no builder busy.
    @Test
    fun wallsAloneLeaveTheBuildersNothingToStart() {
        val walls = item(1, Planner.LevelStep(17, 300, 0, 0, emptyMap()))
        assertTrue(Section.BUILDERS in Leftover.of(plan(Planner.Group.WALLS to listOf(walls))))
    }

    @Test
    fun aDefenceNotStartedKeepsTheBuildersGoing() {
        val walls = item(1, Planner.LevelStep(17, 300, 0, 0, emptyMap()))
        val cannon = item(2, Planner.LevelStep(21, 2, 1, 86_400, emptyMap()))
        assertFalse(Section.BUILDERS in Leftover.of(plan(Planner.Group.WALLS to listOf(walls), Planner.Group.DEFENCES to listOf(cannon))))
    }

    @Test
    fun aSuperchargeStageNotStartedKeepsTheBuildersGoing() {
        val tower = item(2, Planner.LevelStep(12, 1, 0, 86_400, emptyMap(), supercharge = 1))
        assertFalse(Section.BUILDERS in Leftover.of(plan(Planner.Group.DEFENCES to listOf(tower))))
    }
}
