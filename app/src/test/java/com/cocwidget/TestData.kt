package com.cocwidget

import com.cocwidget.game.Boost
import com.cocwidget.game.GameData
import com.cocwidget.game.GameItem
import com.cocwidget.game.Helpers
import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner
import com.cocwidget.game.Ids
import org.json.JSONObject
import java.io.File

// A real village export (player tag replaced) and the bundled game data, shared by the tests.
object TestData {
    // Gradle runs unit tests from the module folder, so this is the real bundled asset.
    val gameData: Map<Int, GameItem> by lazy {
        GameData.parse(File("src/main/assets/coc_data.json").readText())
    }

    fun exportJson(): String = javaClass.getResource("/village_export.json")!!.readText()

    // A TH18 export (tag replaced) from a user whose Logger upgrade went missing. It has
    // Guardians, helper shifts, a lab boost and merges up to TH18, none of which the TH16 export has.
    fun th18ExportJson(): String = javaClass.getResource("/village_export_th18.json")!!.readText()

    // A TH18 export (tag replaced) from a friend with seven supercharges running and a maxed
    // Builder Base with nothing upgrading.
    fun superchargingExportJson(): String = javaClass.getResource("/village_export_th18_supercharging.json")!!.readText()

    // The TH18 export with every entry in [sections] at Town Hall 18's max and nothing running
    // there. [buildings] sets those buildings to a level, or to their max when the level is null.
    fun maxedTh18(
        vararg sections: String,
        buildings: Map<Int, Int?> = emptyMap(),
        townHall: Int = 18,
        edit: (JSONObject) -> Unit = {},
    ): String {
        val root = JSONObject(th18ExportJson())
        for (section in sections) {
            val entries = root.getJSONArray(section)
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val item = gameData[entry.getInt("data")] ?: continue
                entry.put("lvl", maxOf(entry.getInt("lvl"), item.maxLevelAt(18)))
                entry.remove("timer")
            }
        }
        val list = root.getJSONArray("buildings")
        for (i in 0 until list.length()) {
            val entry = list.getJSONObject(i)
            val id = entry.getInt("data")
            if (id == Ids.TOWN_HALL) entry.put("lvl", townHall)
            if (id in buildings) {
                entry.put("lvl", buildings[id] ?: gameData.getValue(id).maxLevelAt(18))
                entry.remove("timer")
            }
        }
        edit(root)
        return root.toString()
    }

    fun superchargingVillage(): Village =
        VillageParser.parse(superchargingExportJson()) { gameData[it] }!!

    fun village(json: String = exportJson()): Village =
        VillageParser.parse(json) { gameData[it] }!!

    fun th18Village(json: String = th18ExportJson()): Village =
        VillageParser.parse(json) { gameData[it] }!!

    fun plan(json: String = exportJson(), builders: Int = 6, eventPercent: Int = 0): Planner.Plan =
        Planner.plan(json, gameData, builders, eventPercent)!!

    fun upgrade(
        workSec: Long,
        section: Section = Section.BUILDERS,
        name: String = "Cannon",
        extra: Boolean = false,
        helperSec: Long = 0,
    ) = Upgrade(
        section = section,
        name = name,
        kind = "building",
        village = "home",
        toLevel = 2,
        workSec = workSec,
        totalSec = workSec * 2,
        extra = extra,
        helperSec = helperSec,
    )

    fun villageOf(
        exportedAtMillis: Long,
        vararg upgrades: Upgrade,
        builderBoost: Boost = Boost.NONE,
        helpers: Helpers = Helpers.NONE,
        builderSnack: Boost = Boost.NONE,
        labSnack: Boost = Boost.NONE,
        nothingToStart: Set<Section> = emptySet(),
    ) = Village(
        tag = "#TEST",
        exportedAtMillis = exportedAtMillis,
        townHall = 16,
        builders = 6,
        hasPetHouse = true,
        upgrades = upgrades.toList(),
        builderBoost = builderBoost,
        labBoost = Boost.NONE,
        petBoost = Boost.NONE,
        helpers = helpers,
        builderSnack = builderSnack,
        labSnack = labSnack,
        nothingToStart = nothingToStart,
    )
}
