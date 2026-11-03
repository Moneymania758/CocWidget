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
// Supercell's Builder Base news, March 2020.
const val CLOCK_TOWER_MULTIPLIER = 10

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
    // Real seconds left at export of a helper's one-hour shift on this upgrade.
    val helperSec: Long = 0,
    // "Keep assigned until upgrade is complete" is ticked for a helper on this upgrade.
    val helperRepeats: Boolean = false,
    // Crafted defence modules only: the defence the module belongs to.
    val defence: String = "",
)

// A potion running for [seconds] more real seconds, making upgrades go [multiplier] times faster.
class Boost(val seconds: Long, val multiplier: Int) {
    companion object {
        val NONE = Boost(0, 1)
    }
}

// The Builder's Apprentice and Lab Assistant levels, and the seconds left at export of the
// 23-hour work day they share (0 when they are ready for a new job).
class Helpers(val apprenticeLevel: Int, val assistantLevel: Int, val cooldownSec: Long) {
    companion object {
        val NONE = Helpers(0, 0, 0)
    }
}

// How fast one upgrade's timer runs after the export. A boost multiplies normal speed, and a
// helper on shift adds its level on top: the game says potions don't affect helpers, so a level
// 12 Lab Assistant during a Research Potion runs research at 24 + 12 = 36x.
class Pace(private val boost: Boost, private val helperSec: Long = 0, private val helperLevel: Int = 0) {
    // The rate only changes when the boost or the shift ends.
    private val changes = listOf(boost.seconds, if (helperLevel > 0) helperSec else 0L).filter { it > 0 }.sorted()

    private fun rateAt(t: Long): Long =
        (if (t < boost.seconds) boost.multiplier.toLong() else 1L) + (if (t < helperSec) helperLevel else 0)

    fun realSecondsFor(work: Long): Long {
        var t = 0L
        var left = work
        for (end in changes) {
            if (end <= t) continue
            val rate = rateAt(t)
            if (left <= (end - t) * rate) return t + (left + rate - 1) / rate
            left -= (end - t) * rate
            t = end
        }
        return t + left
    }

    fun workDoneIn(elapsed: Long): Long {
        var t = 0L
        var done = 0L
        for (end in changes) {
            if (end <= t) continue
            if (elapsed <= end) return done + rateAt(t) * (elapsed - t)
            done += rateAt(t) * (end - t)
            t = end
        }
        return done + (elapsed - t)
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
    val helpers: Helpers,
    val clockTowerBoost: Boost = Boost.NONE,
) {
    fun boostFor(section: Section): Boost = when (section) {
        Section.BUILDERS -> builderBoost
        Section.LAB -> labBoost
        Section.PETS -> petBoost
        Section.BUILDER_BASE -> clockTowerBoost
    }

    // The Apprentice helps builders, the Assistant the lab. Nothing helps pets or the Builder Base.
    fun helperLevelFor(section: Section): Int = when (section) {
        Section.BUILDERS -> helpers.apprenticeLevel
        Section.LAB -> helpers.assistantLevel
        else -> 0
    }

    fun boostEndMillis(boost: Boost): Long = exportedAtMillis + boost.seconds * 1000

    fun helperShiftEndMillis(upgrade: Upgrade): Long = exportedAtMillis + upgrade.helperSec * 1000

    fun helpersReadyMillis(): Long = exportedAtMillis + helpers.cooldownSec * 1000

    // Only the shift running at export speeds the timer up. Whether a kept assignment starts
    // again by itself when the work day resets isn't known, so later shifts aren't counted.
    fun isHelped(upgrade: Upgrade, nowMillis: Long): Boolean =
        helperLevelFor(upgrade.section) > 0 && (upgrade.helperRepeats || helperShiftEndMillis(upgrade) > nowMillis)

    fun paceFor(upgrade: Upgrade): Pace =
        Pace(boostFor(upgrade.section), upgrade.helperSec, helperLevelFor(upgrade.section))

    fun withUpgrades(upgrades: List<Upgrade>) =
        Village(tag, exportedAtMillis, townHall, builders, hasPetHouse, upgrades, builderBoost, labBoost, petBoost, helpers, clockTowerBoost)

    fun endMillis(upgrade: Upgrade): Long =
        exportedAtMillis + paceFor(upgrade).realSecondsFor(upgrade.workSec) * 1000

    fun progress(upgrade: Upgrade, nowMillis: Long): Float {
        val total = upgrade.totalSec ?: return 0f
        val elapsed = ((nowMillis - exportedAtMillis) / 1000).coerceAtLeast(0)
        val left = (upgrade.workSec - paceFor(upgrade).workDoneIn(elapsed)).coerceAtLeast(0)
        return (1f - left.toFloat() / total).coerceIn(0f, 1f)
    }
}

object VillageParser {
    private const val TOWN_HALL = 1000001
    private const val BUILDER_HUT = 1000015
    private const val BOB_HUT = 1000064
    private const val PET_HOUSE = 1000068
    private const val BUILDERS_APPRENTICE = 93000000
    private const val LAB_ASSISTANT = 93000001

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
        val helpers = root.entries("helpers")
        fun helperLevel(id: Int) = helpers.firstOrNull { it.optInt("data") == id }?.optInt("lvl") ?: 0

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
            // Every helper carries the same cooldown. None means no work day is running.
            helpers = Helpers(
                apprenticeLevel = helperLevel(BUILDERS_APPRENTICE),
                assistantLevel = helperLevel(LAB_ASSISTANT),
                cooldownSec = helpers.maxOfOrNull { it.optLong("helper_cooldown") } ?: 0,
            ),
            // The Clock Tower speeds up building and research on the Builder Base. Its 22-hour
            // cooldown starts when the boost ends, so clocktower_cooldown includes the boost.
            clockTowerBoost = Boost(boosts?.optLong("clocktower_boost") ?: 0, CLOCK_TOWER_MULTIPLIER),
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
            helperSec = entry.optLong("helper_timer"),
            helperRepeats = entry.optBoolean("helper_recurrent"),
            defence = item?.defence.orEmpty(),
        )
    }

    private fun JSONObject.entries(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }
}
