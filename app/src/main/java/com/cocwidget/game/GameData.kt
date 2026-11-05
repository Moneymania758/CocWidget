package com.cocwidget.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Extra stages on a building's final [level], open only at the highest Town Hall. Stage n takes
// times[n - 1] seconds and costs[n - 1] of [resource].
class Supercharge(val level: Int, val resource: String, val times: IntArray, val costs: IntArray)

// A building merged from others from [level] on. [sources] lists each building used up, as
// (id, level it must have reached).
class Merge(val level: Int, val sources: List<Pair<Int, Int>>)

class GameItem(
    val name: String,
    val kind: String,
    val village: String,
    private val times: IntArray,
    private val costs: IntArray = IntArray(0),
    private val townHalls: IntArray = IntArray(0),
    val resource: String = "",
    // Walls can also be paid for in this resource.
    val altResource: String = "",
    // The planner group, empty for items the planner leaves out.
    val group: String = "",
    val merge: Merge? = null,
    // Town Hall only: unlocks[n - 1] is what level n adds, as id to quantity.
    val unlocks: List<Map<Int, Int>> = emptyList(),
    // Crafted defence modules only: the defence the module belongs to.
    val defence: String = "",
    // The building whose level some levels also need (the Lab for research), or 0 for none.
    val requires: Int = 0,
    private val requiredLevels: IntArray = IntArray(0),
    // Other resources a level also costs, such as Glowy Ore for equipment.
    private val extraCosts: Map<String, IntArray> = emptyMap(),
    val supercharge: Supercharge? = null,
    // Equipment only: the hero it belongs to, and whether it's an epic (from events, not the
    // Blacksmith).
    val hero: Int = 0,
    val epic: Boolean = false,
) {
    // [resource] first, then the others a level costs, leaving out ones it doesn't need.
    fun costsToReach(level: Int): Map<String, Long> = buildMap {
        costToReach(level).takeIf { it > 0 }?.let { put(resource, it) }
        for ((name, costs) in extraCosts) costs.getOrNull(level - 1)?.takeIf { it > 0 }?.let { put(name, it.toLong()) }
    }

    // The level of [requires] that [level] needs, 0 when it needs none.
    fun requiredLevelFor(level: Int): Int = requiredLevels.getOrNull(level - 1) ?: 0

    // Seconds to upgrade to [level], or null when the bundled data doesn't know it.
    fun timeToReach(level: Int): Long? = times.getOrNull(level - 1)?.takeIf { it > 0 }?.toLong()

    // As timeToReach, but 0 for an instant upgrade.
    fun secondsToReach(level: Int): Long = times.getOrNull(level - 1)?.toLong() ?: 0

    fun costToReach(level: Int): Long = costs.getOrNull(level - 1)?.toLong() ?: 0

    val levelCount: Int get() = townHalls.size

    // The highest level [townHall] allows.
    fun maxLevelAt(townHall: Int): Int = townHalls.indices.lastOrNull { townHalls[it] <= townHall }?.plus(1) ?: 0
}

// Export "data" ids the app needs by name.
object Ids {
    const val TOWN_HALL = 1000001
    const val BUILDER_HUT = 1000015
    const val BOB_HUT = 1000064
    const val PET_HOUSE = 1000068
    const val LABORATORY = 1000007
    const val BLACKSMITH = 1000070
    const val HERO_HALL = 1000071
    const val BUILDERS_APPRENTICE = 93000000
    const val LAB_ASSISTANT = 93000001
}

// Names, upgrade times and planner data keyed by the export's "data" id. Built by
// tools/build-coc-data.js.
object GameData {
    @Volatile
    private var items: Map<Int, GameItem>? = null

    fun get(context: Context, id: Int): GameItem? = all(context)[id]

    fun all(context: Context): Map<Int, GameItem> {
        items?.let { return it }
        val text = context.assets.open("coc_data.json").bufferedReader().use { it.readText() }
        return parse(text).also { items = it }
    }

    fun parse(text: String): Map<Int, GameItem> {
        val root = JSONObject(text)
        val map = HashMap<Int, GameItem>(root.length())
        for (key in root.keys()) {
            val entry = root.getJSONObject(key)
            map[key.toInt()] = GameItem(
                name = entry.getString("n"),
                kind = entry.getString("k"),
                village = entry.optString("v"),
                times = entry.getJSONArray("t").toIntArray(),
                costs = entry.optJSONArray("c").toIntArray(),
                townHalls = entry.optJSONArray("h").toIntArray(),
                resource = entry.optString("r"),
                altResource = entry.optString("a"),
                group = entry.optString("g"),
                merge = entry.optJSONObject("m")?.let(::merge),
                unlocks = entry.optJSONArray("u")?.let(::unlocks).orEmpty(),
                defence = entry.optString("d"),
                requires = entry.optInt("b"),
                requiredLevels = entry.optJSONArray("q").toIntArray(),
                extraCosts = entry.optJSONObject("x")?.let { x -> x.keys().asSequence().associateWith { x.getJSONArray(it).toIntArray() } }.orEmpty(),
                supercharge = entry.optJSONObject("s")?.let {
                    Supercharge(it.getInt("l"), it.getString("r"), it.getJSONArray("t").toIntArray(), it.getJSONArray("c").toIntArray())
                },
                hero = entry.optInt("o"),
                epic = entry.optInt("e") == 1,
            )
        }
        return map
    }

    private fun merge(json: JSONObject): Merge {
        val sources = json.getJSONArray("s")
        return Merge(json.getInt("l"), (0 until sources.length()).map {
            val source = sources.getJSONArray(it)
            source.getInt(0) to source.getInt(1)
        })
    }

    private fun unlocks(levels: JSONArray): List<Map<Int, Int>> = (0 until levels.length()).map { level ->
        val pairs = levels.getJSONArray(level)
        (0 until pairs.length()).associate { pairs.getJSONArray(it).let { pair -> pair.getInt(0) to pair.getInt(1) } }
    }

    private fun JSONArray?.toIntArray(): IntArray = if (this == null) IntArray(0) else IntArray(length()) { optInt(it) }
}
