package com.cocwidget

import org.json.JSONException
import org.json.JSONObject

// Time and resources left to max everything the current Town Hall allows, worked out from a
// village export and the bundled game data. As in Clash Ninja's "Remaining" panel, a running
// upgrade counts its time left but not its cost, which is already paid. Times are at normal
// speed: no potions, helpers or Goblins.
object Planner {
    private const val TOWN_HALL = 1000001
    private val SECTIONS = listOf("buildings", "traps", "heroes", "guardians", "units", "spells", "siege_machines", "pets")

    enum class Group(val key: String, val usesBuilders: Boolean) {
        DEFENCES("defences", true),
        TRAPS("traps", true),
        ARMY("army", true),
        RESOURCES("resources", true),
        WALLS("walls", true),
        HEROES("heroes", true),
        GUARDIANS("guardians", true),
        LAB("lab", false),
        PETS("pets", false),
    }

    // One building, trap, hero, troop, spell or pet type. [levels] holds each copy's current level,
    // highest first, with 0 for one the Town Hall allows that isn't built yet.
    class Item(
        val id: Int,
        val name: String,
        val levels: List<Int>,
        val maxLevel: Int,
        val seconds: Long,
        val costs: Map<String, Long>,
    )

    // [done] is the share of the group's upgrade time already done, or of its levels for walls,
    // which take no time.
    class GroupPlan(val group: Group, val items: List<Item>, val seconds: Long, val costs: Map<String, Long>, val done: Float)

    class Plan(
        val townHall: Int,
        val builders: Int,
        val groups: List<GroupPlan>,
        // All builder work, and the longest stretch one copy needs in order (a hero's levels
        // can't run side by side).
        val builderSeconds: Long,
        val longestSeconds: Long,
    ) {
        fun group(group: Group): GroupPlan? = groups.firstOrNull { it.group == group }

        // How long the builders take if none is ever idle: a lower bound.
        fun builderEstimateSeconds(): Long =
            maxOf(longestSeconds, if (builders > 0) (builderSeconds + builders - 1) / builders else builderSeconds)
    }

    private class Copy(val level: Int, val timer: Long) {
        var target = 0
    }

    // Returns null when [text] isn't a village export. [eventPercent] comes off the time and cost
    // of every upgrade not started yet, as during Hammer Jam.
    fun plan(text: String, items: Map<Int, GameItem>, builders: Int, eventPercent: Int = 0): Plan? {
        val root = try {
            JSONObject(text.trim())
        } catch (e: JSONException) {
            return null
        }
        if (!root.has("tag") || !root.has("timestamp")) return null

        var townHall = 0
        val copies = HashMap<Int, MutableList<Copy>>()
        for (section in SECTIONS) {
            val entries = root.optJSONArray(section) ?: continue
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                val id = entry.optInt("data")
                if (id == TOWN_HALL) townHall = entry.optInt("lvl")
                // Only home village items have a group, so this also skips the Builder Base.
                if (items[id]?.group.isNullOrEmpty()) continue
                repeat(entry.optInt("cnt", 1)) {
                    copies.getOrPut(id) { mutableListOf() } += Copy(entry.optInt("lvl"), entry.optLong("timer"))
                }
            }
        }

        addUnbuilt(items, townHall, copies)
        for ((id, list) in copies) {
            val max = items.getValue(id).maxLevelAt(townHall)
            for (copy in list) copy.target = maxOf(copy.level, if (copy.target > 0) minOf(copy.target, max) else max)
        }
        return summarise(items, townHall, builders, copies, 100 - eventPercent)
    }

    // The Town Hall's unlocks give how many of each building and trap it allows. Merged buildings
    // use up others: two max Cannons make a Ricochet Cannon, and the Town Hall takes in the Eagle
    // Artillery at 17. Copies still to be merged only need the level the merge asks for.
    private fun addUnbuilt(items: Map<Int, GameItem>, townHall: Int, copies: HashMap<Int, MutableList<Copy>>) {
        val allowed = HashMap<Int, Int>()
        items[TOWN_HALL]?.unlocks?.take(townHall)?.forEach { level ->
            for ((id, quantity) in level) allowed.merge(id, quantity, Int::plus)
        }
        val usedUp = HashMap<Int, Int>()
        val mergeLevel = HashMap<Int, Int>()
        for ((id, item) in items) {
            val merge = item.merge ?: continue
            val merges = if (id == TOWN_HALL) (if (townHall >= merge.level) 1 else 0) else allowed[id] ?: 0
            for ((source, level) in merge.sources) {
                usedUp.merge(source, merges, Int::plus)
                mergeLevel[source] = level
            }
        }

        for ((id, quantity) in allowed) {
            val item = items[id] ?: continue
            if (item.group.isEmpty()) continue
            val list = copies.getOrPut(id) { mutableListOf() }
            val keep = quantity - (usedUp[id] ?: 0)
            repeat(keep - list.size) { list += Copy(0, 0) }
            // Keeping the highest copies and merging the lowest costs least, because the lowest
            // then only need the merge level.
            list.sortByDescending { it.level }
            for (copy in list.takeLast(list.size - keep)) copy.target = mergeLevel[id] ?: 0
        }
    }

    private fun summarise(
        items: Map<Int, GameItem>,
        townHall: Int,
        builders: Int,
        copies: Map<Int, List<Copy>>,
        pricePercent: Int,
    ): Plan {
        var builderSeconds = 0L
        var longestSeconds = 0L
        val byGroup = HashMap<Group, MutableList<Item>>()
        val doneByGroup = HashMap<Group, LongArray>()

        for ((id, list) in copies) {
            // A building merged away entirely, like every Cannon at TH18.
            if (list.isEmpty()) continue
            val item = items.getValue(id)
            val group = Group.entries.firstOrNull { it.key == item.group } ?: continue
            val resource = if (item.altResource.isNotEmpty()) "${item.resource} or ${item.altResource}" else item.resource
            var seconds = 0L
            var cost = 0L
            val done = doneByGroup.getOrPut(group) { LongArray(2) }

            for (copy in list) {
                var copySeconds = 0L
                for (level in copy.level + 1..copy.target) {
                    if (level == copy.level + 1 && copy.timer > 0) {
                        copySeconds += copy.timer
                        continue
                    }
                    copySeconds += item.secondsToReach(level) * pricePercent / 100
                    cost += item.costToReach(level) * pricePercent / 100
                }
                seconds += copySeconds
                if (group.usesBuilders) {
                    builderSeconds += copySeconds
                    longestSeconds = maxOf(longestSeconds, copySeconds)
                }

                if (group == Group.WALLS) {
                    done[0] += minOf(copy.level, copy.target).toLong()
                    done[1] += copy.target.toLong()
                } else {
                    val running = if (copy.timer > 0) maxOf(0L, item.secondsToReach(copy.level + 1) - copy.timer) else 0L
                    done[0] += (1..copy.level).sumOf { item.secondsToReach(it) } + running
                    done[1] += (1..copy.target).sumOf { item.secondsToReach(it) }
                }
            }

            byGroup.getOrPut(group) { mutableListOf() } += Item(
                id = id,
                name = item.name,
                levels = list.map { it.level }.sortedDescending(),
                maxLevel = item.maxLevelAt(townHall),
                seconds = seconds,
                costs = if (cost > 0) mapOf(resource to cost) else emptyMap(),
            )
        }

        val groups = Group.entries.mapNotNull { group ->
            val list = byGroup[group] ?: return@mapNotNull null
            val costs = HashMap<String, Long>()
            for (item in list) for ((resource, cost) in item.costs) costs.merge(resource, cost, Long::plus)
            val done = doneByGroup.getValue(group)
            GroupPlan(
                group = group,
                items = list.sortedWith(compareByDescending<Item> { it.seconds }.thenBy { it.name }),
                seconds = list.sumOf { it.seconds },
                costs = costs,
                done = if (done[1] > 0) done[0].toFloat() / done[1] else 1f,
            )
        }
        return Plan(townHall, builders, groups, builderSeconds, longestSeconds)
    }
}
