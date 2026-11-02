package com.cocwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

const val BUILDER_POTION_MULTIPLIER = 10
// Community figure, not confirmed by Supercell.
const val BUILDER_BITE_MULTIPLIER = 2
const val PET_POTION_MULTIPLIER = 24
const val RESEARCH_POTION_MULTIPLIER = 24
const val STUDY_SOUP_MULTIPLIER = 4

enum class Section { BUILDERS, LAB, PETS, BUILDER_BASE }

data class Upgrade(
    val section: Section,
    val name: String,
    val kind: String,
    val village: String,
    val toLevel: Int,
    // Upgrade time left at export, before any potion speed-up.
    val workSec: Long,
    val totalSec: Long?,
    // Set on the slot run by the event Goblin builder or researcher.
    val extra: Boolean,
)

// A potion running for [seconds] more real seconds, making upgrades go [multiplier] times faster.
class Boost(val seconds: Long, val multiplier: Int) {
    fun realSecondsFor(work: Long): Long {
        val boostedWork = seconds * multiplier
        return if (work <= boostedWork) (work + multiplier - 1) / multiplier else seconds + (work - boostedWork)
    }

    fun workDoneIn(elapsed: Long): Long = multiplier * minOf(elapsed, seconds) + maxOf(0L, elapsed - seconds)

    companion object {
        val NONE = Boost(0, 1)
    }
}

class Village(
    val tag: String,
    val exportedAtMillis: Long,
    val townHall: Int,
    val builders: Int,
    val hasPetHouse: Boolean,
    val upgrades: List<Upgrade>,
    val builderBoost: Boost,
    val labBoost: Boost,
    val petBoost: Boost,
) {
    fun boostFor(section: Section): Boost = when (section) {
        Section.BUILDERS -> builderBoost
        Section.LAB -> labBoost
        Section.PETS -> petBoost
        else -> Boost.NONE
    }

    fun boostEndMillis(boost: Boost): Long = exportedAtMillis + boost.seconds * 1000

    fun withUpgrades(upgrades: List<Upgrade>) =
        Village(tag, exportedAtMillis, townHall, builders, hasPetHouse, upgrades, builderBoost, labBoost, petBoost)

    fun endMillis(upgrade: Upgrade): Long =
        exportedAtMillis + boostFor(upgrade.section).realSecondsFor(upgrade.workSec) * 1000

    fun progress(upgrade: Upgrade, nowMillis: Long): Float {
        val total = upgrade.totalSec ?: return 0f
        val elapsed = ((nowMillis - exportedAtMillis) / 1000).coerceAtLeast(0)
        val left = (upgrade.workSec - boostFor(upgrade.section).workDoneIn(elapsed)).coerceAtLeast(0)
        return (1f - left.toFloat() / total).coerceIn(0f, 1f)
    }
}

object VillageParser {
    private const val TOWN_HALL = 1000001
    private const val BUILDER_HUT = 1000015
    private const val BOB_HUT = 1000064
    private const val PET_HOUSE = 1000068

    private val SECTIONS = mapOf(
        "buildings" to Section.BUILDERS,
        "traps" to Section.BUILDERS,
        "heroes" to Section.BUILDERS,
        "units" to Section.LAB,
        "spells" to Section.LAB,
        "siege_machines" to Section.LAB,
        "pets" to Section.PETS,
        "buildings2" to Section.BUILDER_BASE,
        "traps2" to Section.BUILDER_BASE,
        "heroes2" to Section.BUILDER_BASE,
        "units2" to Section.BUILDER_BASE,
    )

    fun parse(
        context: Context,
        text: String,
        builderMultiplier: Int = BUILDER_POTION_MULTIPLIER,
        labMultiplier: Int = RESEARCH_POTION_MULTIPLIER,
    ): Village? = parse(text, builderMultiplier, labMultiplier) { GameData.get(context, it) }

    // Returns null when [text] isn't a Clash of Clans village export.
    fun parse(
        text: String,
        builderMultiplier: Int,
        labMultiplier: Int = RESEARCH_POTION_MULTIPLIER,
        lookup: (Int) -> GameItem?,
    ): Village? {
        val root = try {
            JSONObject(text.trim())
        } catch (e: JSONException) {
            return null
        }
        if (!root.has("tag") || !root.has("timestamp")) return null

        val upgrades = mutableListOf<Upgrade>()
        for (key in root.keys()) collectTimed(root.opt(key), key, 1, lookup, upgrades)
        val buildings = root.entries("buildings")
        val huts = buildings.filter { it.optInt("data") == BUILDER_HUT }.sumOf { it.optInt("cnt", 1) }
        val bob = if (buildings.any { it.optInt("data") == BOB_HUT }) 1 else 0
        val boosts = root.optJSONObject("boosts")

        return Village(
            tag = root.optString("tag"),
            exportedAtMillis = root.optLong("timestamp") * 1000,
            townHall = buildings.firstOrNull { it.optInt("data") == TOWN_HALL }?.optInt("lvl") ?: 0,
            builders = huts + bob,
            hasPetHouse = buildings.any { it.optInt("data") == PET_HOUSE },
            upgrades = upgrades,
            builderBoost = Boost(boosts?.optLong("builder_boost") ?: 0, builderMultiplier),
            labBoost = Boost(boosts?.optLong("lab_boost") ?: 0, labMultiplier),
            petBoost = Boost(boosts?.optLong("pet_boost") ?: 0, PET_POTION_MULTIPLIER),
        )
    }

    // Walks the whole export for running upgrades, so a section added by a game update (Guardians
    // arrived with TH18) or an entry nested inside a building still shows without a parser change.
    private fun collectTimed(
        node: Any?,
        topKey: String,
        depth: Int,
        lookup: (Int) -> GameItem?,
        found: MutableList<Upgrade>,
    ) {
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) collectTimed(node.opt(i), topKey, depth, lookup, found)
            is JSONObject -> {
                if (node.has("data") && node.optLong("timer") > 0) {
                    val item = lookup(node.optInt("data"))
                    found += toUpgrade(item, sectionFor(topKey, depth, item), node)
                }
                for (key in node.keys()) collectTimed(node.opt(key), topKey, depth + 1, lookup, found)
            }
        }
    }

    // Known sections map directly. Anything else is placed by what the game data says it is,
    // with the export's "2" suffix marking the Builder Base.
    private fun sectionFor(topKey: String, depth: Int, item: GameItem?): Section {
        if (depth == 1) SECTIONS[topKey]?.let { return it }
        return when {
            topKey.endsWith("2") -> Section.BUILDER_BASE
            item?.kind == "troop" || item?.kind == "spell" -> Section.LAB
            item?.kind == "pet" -> Section.PETS
            else -> Section.BUILDERS
        }
    }

    private fun toUpgrade(item: GameItem?, section: Section, entry: JSONObject): Upgrade {
        val id = entry.optInt("data")
        val toLevel = entry.optInt("lvl") + 1
        return Upgrade(
            section = section,
            name = item?.name ?: "#$id",
            kind = item?.kind.orEmpty(),
            village = item?.village.orEmpty(),
            toLevel = toLevel,
            workSec = entry.optLong("timer"),
            totalSec = item?.timeToReach(toLevel),
            extra = entry.optBoolean("extra"),
        )
    }

    private fun JSONObject.entries(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }
}
