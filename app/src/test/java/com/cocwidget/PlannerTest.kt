package com.cocwidget

import com.cocwidget.Planner.Group
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    private val day = 86_400L

    private fun Planner.Plan.item(name: String): Planner.Item? =
        groups.flatMap { it.items }.singleOrNull { it.name == name }

    // The TH16 export moved on to what Clash Ninja showed in its Heroes panel: King 62 with 63
    // running (1d 15h 22m left) and the Royal Champion's upgrade to 45 done.
    private fun clashNinjaHeroState(): String {
        val root = JSONObject(TestData.exportJson())
        val heroes = root.getJSONArray("heroes")
        for (i in 0 until heroes.length()) {
            val hero = heroes.getJSONObject(i)
            when (hero.getInt("data")) {
                28000000 -> hero.put("lvl", 62).put("timer", (39 * 60 + 22) * 60L)
                28000004 -> hero.put("lvl", 45).remove("timer")
            }
        }
        return root.toString()
    }

    @Test
    fun heroesMatchClashNinjaDuringHammerJam() {
        val heroes = TestData.plan(clashNinjaHeroState(), eventPercent = 50).group(Group.HEROES)!!
        // Clash Ninja: "Dark Elixir 5.78M". The King's running upgrade is already paid for.
        assertEquals(5_784_000L, heroes.costs["Dark Elixir"])

        // Clash Ninja: King "32 Levels - 2.99M - 2mo 19d", not counting the running upgrade.
        val king = heroes.items.single { it.name == "Barbarian King" }
        assertEquals(2_991_500L, king.costs["Dark Elixir"])
        assertEquals(79 * day, king.seconds - (39 * 60 + 22) * 60L)

        // Clash Ninja: "With 2 builders: 2mo 28d 6h". The Minion Prince's 33 levels in a row are
        // the longest stretch, so two builders can't beat them.
        val longest = heroes.items.maxOf { it.seconds }
        assertEquals(88 * day + 6 * 3_600, maxOf(longest, (heroes.seconds + 1) / 2))
    }

    @Test
    fun countsMatchWhatTheTownHallAllows() {
        // Seven Cannons and eight Archer Towers allowed at TH16, four of each merged into two
        // Ricochet Cannons and two Multi-Archer Towers. Everything allowed is built.
        val plan = TestData.plan()
        assertEquals(3, plan.item("Cannon")!!.levels.size)
        assertEquals(4, plan.item("Archer Tower")!!.levels.size)
        assertEquals(2, plan.item("Ricochet Cannon")!!.levels.size)
        assertTrue(plan.groups.flatMap { it.items }.none { 0 in it.levels })
    }

    @Test
    fun th18MergesUseUpCannonsAndTheEagleArtillery() {
        val plan = TestData.plan(TestData.th18ExportJson())
        // Three Ricochet Cannons and the Multi-Gear Tower take all seven Cannons, and the Town
        // Hall took in the Eagle Artillery at 17.
        assertNull(plan.item("Cannon"))
        assertNull(plan.item("Eagle Artillery"))
        assertEquals(2, plan.item("Archer Tower")!!.levels.size)
        assertEquals(2, plan.item("Wizard Tower")!!.levels.size)
        assertTrue(plan.groups.flatMap { it.items }.none { 0 in it.levels })

        val defences = plan.group(Group.DEFENCES)!!
        assertEquals(0L, defences.seconds)
        assertEquals(1f, defences.done)
    }

    @Test
    fun wallsCostGoldOrElixirAndTakeNoTime() {
        // Eight walls at 16 to take to 17, at 5M each.
        val walls = TestData.plan().group(Group.WALLS)!!
        assertEquals(mapOf("Gold or Elixir" to 40_000_000L), walls.costs)
        assertEquals(0L, walls.seconds)
        assertEquals(5_517f / 5_525, walls.done, 1e-6f)
    }

    @Test
    fun labAndPetTotalsMatchAnIndependentSum() {
        // Summed separately from ClashKing's raw data, running research counted by time left.
        val plan = TestData.plan()
        assertEquals(30_845_304L, plan.group(Group.LAB)!!.seconds)
        assertEquals(32_116_327L, plan.group(Group.PETS)!!.seconds)
    }

    @Test
    fun leavesOutTheBuilderBase() {
        val plan = TestData.plan()
        assertTrue(plan.groups.flatMap { it.items }.all { TestData.gameData.getValue(it.id).village != "builderBase" })
    }

    @Test
    fun rejectsTextThatIsNotAnExport() {
        assertNull(Planner.plan("hello", TestData.gameData, 6))
        assertNull(Planner.plan("{}", TestData.gameData, 6))
    }

    @Test
    fun builderEstimateIsTheLongerOfTheLongestStretchAndTheWorkShared() {
        assertEquals(100L, Planner.Plan(16, 6, emptyList(), 600, 50).builderEstimateSeconds())
        assertEquals(200L, Planner.Plan(16, 6, emptyList(), 600, 200).builderEstimateSeconds())
        // Rounds up.
        assertEquals(151L, Planner.Plan(16, 4, emptyList(), 601, 10).builderEstimateSeconds())
    }

    // A made-up game: Town Hall 1 allows three Towers (max level 2), Town Hall 2 allows level 3
    // and one Big Tower, merged from two level 2 Towers.
    private val tower = 1
    private val bigTower = 2
    private val synthetic = mapOf(
        1000001 to GameItem("Town Hall", "building", "home", IntArray(2), unlocks = listOf(mapOf(tower to 3), mapOf(bigTower to 1))),
        tower to GameItem(
            "Tower", "building", "home",
            times = intArrayOf(10, 20, 40),
            costs = intArrayOf(100, 200, 400),
            townHalls = intArrayOf(1, 1, 2),
            resource = "Gold",
            group = "defences",
        ),
        bigTower to GameItem(
            "Big Tower", "building", "home",
            times = intArrayOf(100),
            costs = intArrayOf(5_000),
            townHalls = intArrayOf(2),
            resource = "Gold",
            group = "defences",
            merge = Merge(1, listOf(tower to 2, tower to 2)),
        ),
    )

    private fun syntheticPlan(townHall: Int, buildings: String, eventPercent: Int = 0) = Planner.plan(
        """{"tag":"#T","timestamp":1,"buildings":[{"data":1000001,"lvl":$townHall},$buildings]}""",
        synthetic,
        builders = 2,
        eventPercent = eventPercent,
    )!!

    @Test
    fun unbuiltCopiesCountFromLevelOne() {
        val tower = syntheticPlan(1, """{"data":1,"lvl":2}""").item("Tower")!!
        assertEquals(listOf(2, 0, 0), tower.levels)
        assertEquals(2 * (10L + 20), tower.seconds)
        assertEquals(mapOf("Gold" to 2 * (100L + 200)), tower.costs)
    }

    @Test
    fun copiesToBeMergedOnlyNeedTheMergeLevel() {
        // One Tower is kept and taken to 3. The lowest two only need 2 for the merge, so the
        // highest is the one kept: 2 x 20 s, where keeping a level 1 would take 80 s.
        val plan = syntheticPlan(2, """{"data":1,"lvl":3},{"data":1,"lvl":1,"cnt":2}""")
        assertEquals(40L, plan.item("Tower")!!.seconds)
        assertEquals(listOf(0), plan.item("Big Tower")!!.levels)
        assertEquals(100L, plan.item("Big Tower")!!.seconds)
    }

    @Test
    fun afterTheMergeOnlyTheKeptCopyIsLeft() {
        val plan = syntheticPlan(2, """{"data":1,"lvl":2},{"data":2,"lvl":1}""")
        assertEquals(listOf(2), plan.item("Tower")!!.levels)
        assertEquals(40L, plan.item("Tower")!!.seconds)
        assertEquals(0L, plan.item("Big Tower")!!.seconds)
    }

    @Test
    fun runningUpgradesCountTimeLeftButNotCost() {
        val tower = syntheticPlan(1, """{"data":1,"lvl":1,"timer":7},{"data":1,"lvl":2,"cnt":2}""").item("Tower")!!
        assertEquals(7L, tower.seconds)
        assertTrue(tower.costs.isEmpty())
    }

    @Test
    fun anEventDiscountsOnlyWhatHasNotStarted() {
        val tower = syntheticPlan(
            1, """{"data":1,"lvl":1,"timer":7},{"data":1,"lvl":0},{"data":1,"lvl":2}""", eventPercent = 50,
        ).item("Tower")!!
        // The running one keeps its 7 s. The unbuilt one costs half: 5 + 10 s and 50 + 100 Gold.
        assertEquals(7L + 15, tower.seconds)
        assertEquals(mapOf("Gold" to 150L), tower.costs)
    }
}
