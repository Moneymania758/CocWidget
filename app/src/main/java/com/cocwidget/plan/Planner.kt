package com.cocwidget.plan

import com.cocwidget.game.GameItem
import com.cocwidget.game.Ids
import com.cocwidget.game.VillageParser

// Time and resources left to max everything the current Town Hall allows, worked out from a
// village export and the bundled game data. As in Clash Ninja's "Remaining" panel, a running
// upgrade counts its time left but not its cost, which is already paid. Times are at normal
// speed: no potions, helpers or Goblins. At the highest Town Hall, buildings on their final level
// also count the supercharge stages left.
object Planner {
    private val SECTIONS = listOf("buildings", "traps", "heroes", "guardians", "units", "spells", "siege_machines", "pets", "equipment")

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
        // Upgrades are instant and paid in ore, and events don't discount them.
        EQUIPMENT("equipment", false);

        companion object {
            // The group named in the game data, or null for items the planner leaves out.
            fun of(key: String): Group? = entries.firstOrNull { it.key == key }
        }
    }

    // One building, trap, hero, troop, spell or pet type. [levels] holds each copy's current level,
    // highest first, with 0 for one the Town Hall allows that isn't built yet. [running] holds the
    // current levels of the copies being upgraded. [supercharges] is the supercharge stages left
    // across all copies. [steps] is what's left one level at a time, lowest first.
    class Item(
        val id: Int,
        val name: String,
        val levels: List<Int>,
        val running: List<Int>,
        val maxLevel: Int,
        val seconds: Long,
        val costs: Map<String, Long>,
        val supercharges: Int = 0,
        val steps: List<LevelStep> = emptyList(),
    )

    // One level still to reach. [copies] is how many copies need it, [running] how many of those are
    // on it now, and [runningLeft] the longest time one of those has left. [seconds] and [costs] are
    // for one copy not started yet, at the planner's price. A running copy's level is already paid.
    // A supercharge stage has [supercharge] set to its number, and [level] is the level it's on.
    class LevelStep(
        val level: Int,
        val copies: Int,
        val running: Int,
        val seconds: Long,
        val costs: Map<String, Long>,
        val supercharge: Int = 0,
        val runningLeft: Long = 0,
    )

    // [done] is the share of the group's upgrade time already done, or of its levels for walls and
    // equipment, which take no time.
    class GroupPlan(val group: Group, val items: List<Item>, val seconds: Long, val costs: Map<String, Long>, val done: Float)

    class Plan(
        val townHall: Int,
        val builders: Int,
        val groups: List<GroupPlan>,
        // All builder work, and the longest stretch one copy needs in order (a hero's levels
        // can't run side by side).
        val builderSeconds: Long,
        val longestSeconds: Long,
        // Whose levels make up that longest stretch.
        val longestName: String = "",
        // Epic equipment at level 1, left out as the export doesn't say whether it's owned.
        val unknownEpics: Int = 0,
    ) {
        fun group(group: Group): GroupPlan? = groups.firstOrNull { it.group == group }

        // How long the builders take if none is ever idle: a lower bound.
        fun builderEstimateSeconds(): Long =
            maxOf(longestSeconds, if (builders > 0) (builderSeconds + builders - 1) / builders else builderSeconds)
    }

    // [supercharge] is the supercharge stages done. A running upgrade on a copy already at its
    // target level is its next supercharge stage.
    private class Copy(val level: Int, val timer: Long, val supercharge: Int = 0) {
        var target = 0
    }

    // The next level for copies of one item on the same level, for the bedtime planner. A
    // supercharge stage has [supercharge] set to its number, and [toLevel] is the level it's on.
    class Step(
        val id: Int,
        val name: String,
        val group: Group,
        val toLevel: Int,
        val count: Int,
        val seconds: Long,
        val costs: Map<String, Long>,
        val supercharge: Int = 0,
    )

    // Returns null when [text] isn't a village export. [eventPercent] comes off the time and cost
    // of every upgrade not started yet, as during Hammer Jam. Supercharges stay at full price.
    fun plan(text: String, items: Map<Int, GameItem>, builders: Int, eventPercent: Int = 0): Plan? {
        val export = read(text, items) ?: return null
        return summarise(items, export, builders, 100 - eventPercent)
    }

    // What could start next: each copy's next level, leaving out copies being upgraded, instant
    // levels (walls), which keep nobody busy, and levels the Lab, Pet House or Hero Hall isn't
    // high enough for yet.
    fun nextSteps(text: String, items: Map<Int, GameItem>, eventPercent: Int = 0): List<Step>? {
        val export = read(text, items) ?: return null
        val pricePercent = 100 - eventPercent
        val steps = ArrayList<Step>()
        for ((id, list) in export.copies) {
            val item = items.getValue(id)
            val group = Group.of(item.group) ?: continue
            val waiting = list.filter { it.timer == 0L && it.level < it.target }
            for ((level, same) in waiting.groupBy { it.level }) {
                if ((export.built[item.requires] ?: 0) < item.requiredLevelFor(level + 1)) continue
                val seconds = at(item.secondsToReach(level + 1), pricePercent)
                if (seconds == 0L) continue
                steps += Step(id, item.name, group, level + 1, same.size, seconds, levelCosts(item, level + 1, pricePercent))
            }
            // Copies on their final level with a supercharge stage to start.
            val charging = list.filter { it.timer == 0L && it.level == it.target && !stagesLeft(item, it, export).isEmpty() }
            for ((stage, same) in charging.groupBy { it.supercharge + 1 }) {
                val supercharge = item.supercharge ?: continue
                steps += Step(
                    id, item.name, group, supercharge.level, same.size,
                    supercharge.times[stage - 1].toLong(),
                    mapOf(supercharge.resource to supercharge.costs[stage - 1].toLong()),
                    stage,
                )
            }
        }
        return steps
    }

    // [copies] holds every copy with the level it should reach, and [built] the highest level of
    // each building in the export.
    // [unknownEpics] is the epic equipment at level 1 left out, as it may not be owned.
    private class Export(
        val townHall: Int,
        val copies: HashMap<Int, MutableList<Copy>>,
        val built: Map<Int, Int>,
        val topTownHall: Boolean,
        val unknownEpics: Int,
    )

    // Returns null when [text] isn't a village export.
    private fun read(text: String, items: Map<Int, GameItem>): Export? {
        val root = VillageParser.root(text) ?: return null
        var townHall = 0
        val copies = HashMap<Int, MutableList<Copy>>()
        val built = HashMap<Int, Int>()
        var unknownEpics = 0
        // Heroes and the Blacksmith come before equipment, so [built] has them by then.
        for (section in SECTIONS) {
            val entries = root.optJSONArray(section) ?: continue
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                val id = entry.optInt("data")
                if (id == Ids.TOWN_HALL) townHall = entry.optInt("lvl")
                built.merge(id, entry.optInt("lvl"), ::maxOf)
                // Only home village items have a group, so this also skips the Builder Base.
                val item = items[id]
                if (item == null || item.group.isEmpty()) continue
                if (item.group == Group.EQUIPMENT.key && !owned(item, entry.optInt("lvl"), built)) {
                    if (item.epic) unknownEpics++
                    continue
                }
                repeat(entry.optInt("cnt", 1)) {
                    copies.getOrPut(id) { mutableListOf() } += Copy(entry.optInt("lvl"), entry.optLong("timer"), entry.optInt("supercharge"))
                }
            }
        }

        addUnbuilt(items, townHall, copies)
        for ((id, list) in copies) {
            val max = items.getValue(id).maxLevelAt(townHall)
            for (copy in list) copy.target = maxOf(copy.level, if (copy.target > 0) minOf(copy.target, max) else max)
        }
        val top = items[Ids.TOWN_HALL]?.levelCount ?: 0
        return Export(townHall, copies, built, top > 0 && townHall >= top, unknownEpics)
    }

    // Exports list every piece of equipment, owned or not, the ones not owned at level 1. The
    // Blacksmith unlocks common pieces, so those are owned once it and the hero are there. Epics
    // come from events, so one at level 1 can't be told apart from one not owned.
    fun owned(item: GameItem, level: Int, built: Map<Int, Int>): Boolean =
        if (item.epic) {
            level > 1
        } else {
            (built[item.requires] ?: 0) >= item.requiredLevelFor(1) && (built[item.hero] ?: 0) > 0
        }

    // The supercharge stages [copy] has left: only at the highest Town Hall, on a building whose
    // final level is the copy's target.
    private fun stagesLeft(item: GameItem, copy: Copy, export: Export): IntRange {
        val supercharge = item.supercharge ?: return IntRange.EMPTY
        if (!export.topTownHall || copy.target != supercharge.level) return IntRange.EMPTY
        return copy.supercharge + 1..supercharge.times.size
    }

    // What reaching [level] costs, under the planner's resource names ("Gold or Elixir" for walls).
    private fun levelCosts(item: GameItem, level: Int, pricePercent: Int): Map<String, Long> {
        val percent = if (item.group == Group.EQUIPMENT.key) 100 else pricePercent
        return item.costsToReach(level).entries.associate { (resource, cost) ->
            val name = if (resource == item.resource && item.altResource.isNotEmpty()) "$resource or ${item.altResource}" else resource
            name to at(cost, percent)
        }
    }

    // [value] at [percent] of full price, rounded down.
    private fun at(value: Long, percent: Int): Long = value * percent / 100

    // The Town Hall's unlocks give how many of each building and trap it allows. Merged buildings
    // use up others: two max Cannons make a Ricochet Cannon, and the Town Hall takes in the Eagle
    // Artillery at 17. Copies still to be merged only need the level the merge asks for.
    private fun addUnbuilt(items: Map<Int, GameItem>, townHall: Int, copies: HashMap<Int, MutableList<Copy>>) {
        val allowed = HashMap<Int, Int>()
        items[Ids.TOWN_HALL]?.unlocks?.take(townHall)?.forEach { level ->
            for ((id, quantity) in level) allowed.merge(id, quantity, Int::plus)
        }
        val usedUp = HashMap<Int, Int>()
        val mergeLevel = HashMap<Int, Int>()
        for ((id, item) in items) {
            val merge = item.merge ?: continue
            val merges = if (id == Ids.TOWN_HALL) (if (townHall >= merge.level) 1 else 0) else allowed[id] ?: 0
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

    private fun summarise(items: Map<Int, GameItem>, export: Export, builders: Int, pricePercent: Int): Plan {
        val townHall = export.townHall
        var builderSeconds = 0L
        var longestSeconds = 0L
        var longestName = ""
        val byGroup = HashMap<Group, MutableList<Item>>()
        val doneByGroup = HashMap<Group, LongArray>()

        for ((id, list) in export.copies) {
            // A building merged away entirely, like every Cannon at TH18.
            if (list.isEmpty()) continue
            val item = items.getValue(id)
            val group = Group.of(item.group) ?: continue
            var seconds = 0L
            val costs = HashMap<String, Long>()
            var supercharges = 0
            val done = doneByGroup.getOrPut(group) { LongArray(2) }
            // By (level, supercharge stage): copies needing it, how many are on it now, and the
            // longest time one of those has left.
            val stepCounts = sortedMapOf<Pair<Int, Int>, LongArray>(compareBy({ it.first }, { it.second }))
            fun countStep(level: Int, stage: Int, running: Boolean, timer: Long) {
                val count = stepCounts.getOrPut(level to stage) { LongArray(3) }
                count[0]++
                if (running) {
                    count[1]++
                    count[2] = maxOf(count[2], timer)
                }
            }

            for (copy in list) {
                var copySeconds = 0L
                for (level in copy.level + 1..copy.target) {
                    val running = level == copy.level + 1 && copy.timer > 0
                    countStep(level, 0, running, copy.timer)
                    if (running) {
                        copySeconds += copy.timer
                        continue
                    }
                    copySeconds += at(item.secondsToReach(level), pricePercent)
                    for ((resource, cost) in levelCosts(item, level, pricePercent)) costs.merge(resource, cost, Long::plus)
                }
                val stages = stagesLeft(item, copy, export)
                val supercharge = item.supercharge
                if (supercharge != null && !stages.isEmpty()) {
                    supercharges += stages.count()
                    for (stage in stages) {
                        val running = stage == stages.first && copy.level == copy.target && copy.timer > 0
                        countStep(copy.target, stage, running, copy.timer)
                        if (running) {
                            copySeconds += copy.timer
                            continue
                        }
                        copySeconds += supercharge.times[stage - 1].toLong()
                        costs.merge(supercharge.resource, supercharge.costs[stage - 1].toLong(), Long::plus)
                    }
                }
                seconds += copySeconds
                if (group.usesBuilders) {
                    builderSeconds += copySeconds
                    if (copySeconds > longestSeconds) {
                        longestSeconds = copySeconds
                        longestName = item.name
                    }
                }

                if (group == Group.WALLS || group == Group.EQUIPMENT) {
                    done[0] += minOf(copy.level, copy.target).toLong()
                    done[1] += copy.target.toLong()
                } else {
                    val running = if (copy.timer > 0 && copy.level < copy.target) maxOf(0L, item.secondsToReach(copy.level + 1) - copy.timer) else 0L
                    done[0] += (1..copy.level).sumOf { item.secondsToReach(it) } + running
                    done[1] += (1..copy.target).sumOf { item.secondsToReach(it) }
                    if (supercharge != null && export.topTownHall && copy.target == supercharge.level) {
                        done[0] += (1..copy.supercharge).sumOf { supercharge.times[it - 1].toLong() }
                        done[1] += supercharge.times.sum().toLong()
                    }
                }
            }

            byGroup.getOrPut(group) { mutableListOf() } += Item(
                id = id,
                name = item.name,
                levels = list.map { it.level }.sortedDescending(),
                // A copy at its target with a timer is on a supercharge, not a new level.
                running = list.filter { it.timer > 0 && it.level < it.target }.map { it.level }.sorted(),
                maxLevel = item.maxLevelAt(townHall),
                seconds = seconds,
                costs = costs.filterValues { it > 0 },
                supercharges = supercharges,
                steps = stepCounts.map { (key, count) ->
                    val (level, stage) = key
                    val copies = count[0].toInt()
                    val running = count[1].toInt()
                    val supercharge = item.supercharge
                    if (stage > 0 && supercharge != null) {
                        val costs = mapOf(supercharge.resource to supercharge.costs[stage - 1].toLong())
                        LevelStep(level, copies, running, supercharge.times[stage - 1].toLong(), costs, stage, count[2])
                    } else {
                        val costs = levelCosts(item, level, pricePercent).filterValues { it > 0 }
                        LevelStep(level, copies, running, at(item.secondsToReach(level), pricePercent), costs, runningLeft = count[2])
                    }
                },
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
        return Plan(townHall, builders, groups, builderSeconds, longestSeconds, longestName, export.unknownEpics)
    }
}
