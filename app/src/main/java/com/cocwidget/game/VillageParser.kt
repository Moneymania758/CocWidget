package com.cocwidget.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs

object VillageParser {
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
        val root = root(text) ?: return null
        val upgrades = mutableListOf<Upgrade>()
        for (key in root.keys()) collectTimed(root.opt(key), key, 1, lookup, upgrades)
        val buildings = root.entries("buildings")
        val huts = buildings.filter { it.optInt("data") == Ids.BUILDER_HUT }.sumOf { it.optInt("cnt", 1) }
        val bob = if (buildings.any { it.optInt("data") == Ids.BOB_HUT }) 1 else 0
        val boosts = root.optJSONObject("boosts")
        val labSec = boosts?.optLong("lab_boost") ?: 0
        val petSec = boosts?.optLong("pet_boost") ?: 0
        val helpers = root.entries("helpers")
        fun helperLevel(id: Int) = helpers.firstOrNull { it.optInt("data") == id }?.optInt("lvl") ?: 0

        return Village(
            tag = root.optString("tag"),
            exportedAtMillis = root.optLong("timestamp") * 1000,
            townHall = buildings.firstOrNull { it.optInt("data") == Ids.TOWN_HALL }?.optInt("lvl") ?: 0,
            builders = huts + bob,
            hasPetHouse = buildings.any { it.optInt("data") == Ids.PET_HOUSE },
            upgrades = upgrades,
            builderBoost = Boost(boosts?.optLong("builder_boost") ?: 0, builderMultiplier),
            labBoost = Boost(labSec, labMultiplier),
            petBoost = Boost(petSec, petMultiplier(labMultiplier, labSec, petSec)),
            // Every helper carries the same cooldown. None means no work day is running.
            helpers = Helpers(
                apprenticeLevel = helperLevel(Ids.BUILDERS_APPRENTICE),
                assistantLevel = helperLevel(Ids.LAB_ASSISTANT),
                cooldownSec = helpers.maxOfOrNull { it.optLong("helper_cooldown") } ?: 0,
            ),
            // The Clock Tower speeds up building and research on the Builder Base. Its 22-hour
            // cooldown starts when the boost ends, so clocktower_cooldown includes the boost.
            clockTowerBoost = Boost(boosts?.optLong("clocktower_boost") ?: 0, CLOCK_TOWER_MULTIPLIER),
            hasBuilderBase = root.entries("buildings2").isNotEmpty(),
        )
    }

    // Study Soup speeds up the Pet House as well as the lab (ClashKing's description of it). A pet
    // boost ending with a Soup's lab boost is taken to be the same Soup, at its speed. Unchecked
    // against an export: if the game doesn't set the pet boost for a Soup, this never applies.
    internal fun petMultiplier(labMultiplier: Int, labSec: Long, petSec: Long): Int =
        if (labMultiplier == STUDY_SOUP_MULTIPLIER && petSec > 0 && abs(petSec - labSec) <= SAME_BOOST_SEC) {
            STUDY_SOUP_MULTIPLIER
        } else {
            PET_POTION_MULTIPLIER
        }

    // Two boosts ending this close together come from the same item.
    private const val SAME_BOOST_SEC = 120L

    // The export's top level, or null when [text] isn't a Clash of Clans village export.
    internal fun root(text: String): JSONObject? {
        val root = try {
            JSONObject(text.trim())
        } catch (e: JSONException) {
            return null
        }
        return root.takeIf { it.has("tag") && it.has("timestamp") }
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
        val level = entry.optInt("lvl")
        // A building on its final level can only be supercharging. The export's supercharge is
        // the stages done, so the one running is the next.
        val charge = item?.supercharge?.takeIf { level >= it.level }
        val stage = if (charge != null) entry.optInt("supercharge") + 1 else 0
        val toLevel = if (charge != null) level else level + 1
        return Upgrade(
            section = section,
            name = item?.name ?: "#$id",
            kind = item?.kind.orEmpty(),
            village = item?.village.orEmpty(),
            toLevel = toLevel,
            workSec = entry.optLong("timer"),
            totalSec = if (charge != null) charge.times.getOrNull(stage - 1)?.toLong() else item?.timeToReach(toLevel),
            extra = entry.optBoolean("extra"),
            helperSec = entry.optLong("helper_timer"),
            helperRepeats = entry.optBoolean("helper_recurrent"),
            defence = item?.defence.orEmpty(),
            supercharge = stage,
        )
    }

    private fun JSONObject.entries(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }
}
