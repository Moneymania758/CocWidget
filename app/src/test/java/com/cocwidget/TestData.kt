package com.cocwidget

import com.cocwidget.game.BUILDER_POTION_MULTIPLIER
import com.cocwidget.game.Boost
import com.cocwidget.game.GameData
import com.cocwidget.game.GameItem
import com.cocwidget.game.Helpers
import com.cocwidget.game.RESEARCH_POTION_MULTIPLIER
import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner
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

    fun superchargingVillage(): Village =
        VillageParser.parse(superchargingExportJson(), BUILDER_POTION_MULTIPLIER) { gameData[it] }!!

    fun village(builderMultiplier: Int = BUILDER_POTION_MULTIPLIER): Village =
        VillageParser.parse(exportJson(), builderMultiplier) { gameData[it] }!!

    fun th18Village(labMultiplier: Int = RESEARCH_POTION_MULTIPLIER): Village =
        VillageParser.parse(th18ExportJson(), BUILDER_POTION_MULTIPLIER, labMultiplier) { gameData[it] }!!

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
    )
}
