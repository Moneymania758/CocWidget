package com.cocwidget

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

const val BUILDER_POTION_MULTIPLIER = 10
// Community figure, not confirmed by Supercell.
const val BUILDER_BITE_MULTIPLIER = 2
const val PET_POTION_MULTIPLIER = 24

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
    val petBoost: Boost,
) {
    fun boostFor(section: Section): Boost = when (section) {
        Section.BUILDERS -> builderBoost
        Section.PETS -> petBoost
        else -> Boost.NONE
    }

    fun boostEndMillis(boost: Boost): Long = exportedAtMillis + boost.seconds * 1000

    fun withUpgrades(upgrades: List<Upgrade>) =
        Village(tag, exportedAtMillis, townHall, builders, hasPetHouse, upgrades, builderBoost, petBoost)

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

    private val SECTIONS = listOf(
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

    fun parse(context: Context, text: String, builderMultiplier: Int = BUILDER_POTION_MULTIPLIER): Village? =
        parse(text, builderMultiplier) { GameData.get(context, it) }

    // Returns null when [text] isn't a Clash of Clans village export.
    fun parse(text: String, builderMultiplier: Int, lookup: (Int) -> GameItem?): Village? {
        val root = try {
            JSONObject(text.trim())
        } catch (e: JSONException) {
            return null
        }
        if (!root.has("tag") || !root.has("timestamp")) return null

        val upgrades = SECTIONS.flatMap { (key, section) ->
            root.entries(key).filter { it.optLong("timer") > 0 }.map { toUpgrade(lookup, section, it) }
        }
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
            petBoost = Boost(boosts?.optLong("pet_boost") ?: 0, PET_POTION_MULTIPLIER),
        )
    }

    private fun toUpgrade(lookup: (Int) -> GameItem?, section: Section, entry: JSONObject): Upgrade {
        val id = entry.optInt("data")
        val item = lookup(id)
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
