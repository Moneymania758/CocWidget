package com.cocwidget.plan

import com.cocwidget.TestData
import com.cocwidget.plan.Planner.Group
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// What finished between two imports, from the levels in each.
class FinishedTest {
    private val armyCamp = 1000000
    private val cannon = 1000008
    private val wall = 1000010
    private val bombTower = 1000032
    private val queen = 28000001
    private val builderBaseWall = 1000033

    private fun entry(id: Int, level: Int, count: Int = 1, stage: Int? = null) = JSONObject().put("data", id).put("lvl", level).apply {
        if (count > 1) put("cnt", count)
        if (stage != null) put("supercharge", stage)
    }

    private fun export(buildings: List<JSONObject> = emptyList(), heroes: List<JSONObject> = emptyList(), builderBase: List<JSONObject> = emptyList()) =
        JSONObject().put("tag", "#TEST").put("timestamp", 1)
            .put("buildings", JSONArray(buildings)).put("heroes", JSONArray(heroes)).put("buildings2", JSONArray(builderBase))
            .toString()

    private fun between(before: String, after: String) = Finished.between(before, after, TestData.gameData)!!

    @Test
    fun aHerosLevelsAreOneRow() {
        val found = between(export(heroes = listOf(entry(queen, 93))), export(heroes = listOf(entry(queen, 95))))
        assertEquals(listOf(FinishedUpgrade(queen, "Archer Queen", Group.HEROES, 93, 95, 1)), found)
    }

    @Test
    fun copiesThatMovedTogetherAreCountedAndTheRestLeftOut() {
        val before = export(listOf(entry(cannon, 20, 7), entry(wall, 16, 250)))
        val after = export(listOf(entry(cannon, 21, 2), entry(cannon, 20, 5), entry(wall, 17, 25), entry(wall, 16, 225)))
        assertEquals(
            listOf(
                FinishedUpgrade(cannon, "Cannon", Group.DEFENCES, 20, 21, 2),
                FinishedUpgrade(wall, "Wall", Group.WALLS, 16, 17, 25),
            ),
            between(before, after),
        )
    }

    // Matched highest with highest, so the copy left over is the one built since.
    @Test
    fun aNewCopyIsBuilt() {
        val found = between(export(listOf(entry(armyCamp, 12, 4))), export(listOf(entry(armyCamp, 12, 4), entry(armyCamp, 3))))
        assertEquals(listOf(FinishedUpgrade(armyCamp, "Army Camp", Group.ARMY, 0, 3, 1)), found)
        // Placed but not built yet.
        assertTrue(between(export(listOf(entry(armyCamp, 12, 4))), export(listOf(entry(armyCamp, 12, 4), entry(armyCamp, 0)))).isEmpty())
    }

    @Test
    fun aSuperchargeKeepsItsLevel() {
        val found = between(export(listOf(entry(bombTower, 13, stage = 1))), export(listOf(entry(bombTower, 13, stage = 2))))
        val row = found.single()
        assertTrue(row.supercharge)
        assertEquals(listOf(13, 13, 1, 2), listOf(row.from, row.to, row.fromStage, row.toStage))
    }

    @Test
    fun theBuilderBaseAndUnchangedItemsAreLeftOut() {
        val before = export(listOf(entry(cannon, 20, 7)), builderBase = listOf(entry(builderBaseWall, 9, 100)))
        val after = export(listOf(entry(cannon, 20, 7)), builderBase = listOf(entry(builderBaseWall, 10, 100)))
        assertTrue(between(before, after).isEmpty())
        assertNull(Finished.between("not an export", after, TestData.gameData))
    }

    // Groups in Time to max's order, so the page reads the same way.
    @Test
    fun rowsComeInThePlannersGroupOrder() {
        val before = export(listOf(entry(wall, 16), entry(cannon, 20)), listOf(entry(queen, 93)))
        val after = export(listOf(entry(wall, 17), entry(cannon, 21)), listOf(entry(queen, 94)))
        assertEquals(listOf(Group.DEFENCES, Group.WALLS, Group.HEROES), between(before, after).map { it.group })
    }

    @Test
    fun theRealExportAgainstItselfFinishesNothing() {
        assertTrue(between(TestData.th18ExportJson(), TestData.th18ExportJson()).isEmpty())
    }
}
