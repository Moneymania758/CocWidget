package com.cocwidget.game

import com.cocwidget.TestData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A real export from the day TH19 came out, checked against the TH19 game data.
class Th19ExportTest {
    private val json = TestData.th19ExportJson()
    private val village = TestData.village(json)
    private val plan = TestData.plan(json, village.builders)
    private val items = plan.groups.flatMap { it.items }

    @Test
    fun theFirstTh19BuildingsAreGoingUp() {
        assertEquals(19, village.townHall)
        assertEquals(6, village.builders)
        val building = village.upgrades.filter { it.section == Section.BUILDERS }.map { "${it.name} → ${it.toLevel}" }.sorted()
        assertEquals(
            listOf(
                "Firecracker Tower → 1",
                "Firecracker Tower → 1",
                "Hero Hunter Damage → 10",
                "Spiky Tower → 1",
                "Spiky Tower → 1",
                "Squad Barracks → 1",
            ),
            building,
        )
        val squadBarracks = items.single { it.name == "Squad Barracks" }
        assertEquals(listOf(0), squadBarracks.running)
        assertEquals(12, squadBarracks.maxLevel)
    }

    // The Builder Base data comes from a copy that numbers Builder Base troops and the Battle
    // Copter from 1. The game still doesn't: this account is maxed and every one sits on our top level.
    @Test
    fun theGameStillNumbersBuilderBaseLevelsTheOldWay() {
        val root = JSONObject(json)
        for (key in listOf("units2", "heroes2")) {
            val entries = root.getJSONArray(key)
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val item = TestData.gameData.getValue(entry.getInt("data"))
                assertEquals(item.name, item.levelCount, entry.getInt("lvl"))
            }
        }
        assertTrue(village.upgrades.none { it.section == Section.BUILDER_BASE })
    }

    // Firecracker Towers take an Archer Tower and a Scattershot each, and Multi-Archer Towers and
    // Ricochet Cannons took the rest, so none of the three is left to plan.
    @Test
    fun mergedBuildingsAreGone() {
        val names = items.map { it.name }
        assertTrue(names.none { it == "Archer Tower" || it == "Cannon" || it == "Scattershot" })
        val firecracker = items.single { it.name == "Firecracker Tower" }
        assertEquals(listOf(0, 0), firecracker.running)
        assertEquals(2, firecracker.maxLevel)
    }

    // Every supercharge this account has is done, and TH19 kept them all at Town Hall 18.
    @Test
    fun finishedSuperchargesLeaveNothing() {
        for (name in listOf("Mortar", "Inferno Tower", "Bomb Tower", "Monolith", "Revenge Tower", "Firespitter", "Super Wizard Tower")) {
            val item = items.single { it.name == name }
            assertEquals(name, 0, item.supercharges)
            assertEquals(name, 0L, item.seconds)
        }
    }
}
