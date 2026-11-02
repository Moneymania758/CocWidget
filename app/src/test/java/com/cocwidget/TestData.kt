package com.cocwidget

import java.io.File

// A real village export (player tag replaced) and the bundled game data, shared by the tests.
object TestData {
    // Gradle runs unit tests from the module folder, so this is the real bundled asset.
    val gameData: Map<Int, GameItem> by lazy {
        GameData.parse(File("src/main/assets/coc_data.json").readText())
    }

    fun exportJson(): String = javaClass.getResource("/village_export.json")!!.readText()

    fun village(builderMultiplier: Int = BUILDER_POTION_MULTIPLIER): Village =
        VillageParser.parse(exportJson(), builderMultiplier) { gameData[it] }!!

    fun upgrade(
        workSec: Long,
        section: Section = Section.BUILDERS,
        name: String = "Cannon",
        extra: Boolean = false,
    ) = Upgrade(
        section = section,
        name = name,
        kind = "building",
        village = "home",
        toLevel = 2,
        workSec = workSec,
        totalSec = workSec * 2,
        extra = extra,
    )

    fun villageOf(
        exportedAtMillis: Long,
        vararg upgrades: Upgrade,
        builderBoost: Boost = Boost.NONE,
    ) = Village(
        tag = "#TEST",
        exportedAtMillis = exportedAtMillis,
        townHall = 16,
        builders = 6,
        hasPetHouse = true,
        upgrades = upgrades.toList(),
        builderBoost = builderBoost,
        petBoost = Boost.NONE,
    )
}
