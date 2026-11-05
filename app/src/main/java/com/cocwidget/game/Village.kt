package com.cocwidget.game

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
    // The supercharge stage being worked on, or 0 for a level upgrade. [toLevel] is then the
    // building's final level, the one it stays on.
    val supercharge: Int = 0,
)

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
    // Builder Base unlocked, whether or not anything is upgrading there.
    val hasBuilderBase: Boolean = false,
    // A Builder Bite, which runs on its own clock beside a Builder Potion.
    val builderSnack: Boost = Boost.NONE,
    // A Study Soup, which runs on its own clock beside a Research or Pet Potion.
    val labSnack: Boost = Boost.NONE,
) {
    // What this village has, in the order they're shown.
    fun sections(): List<Section> = buildList {
        add(Section.BUILDERS)
        add(Section.LAB)
        if (hasPetHouse) add(Section.PETS)
        if (hasBuilderBase || upgrades.any { it.section == Section.BUILDER_BASE }) add(Section.BUILDER_BASE)
    }

    fun slotsFor(section: Section): Int = when (section) {
        Section.BUILDERS -> builders
        Section.LAB, Section.PETS -> 1
        Section.BUILDER_BASE -> 0
    }

    // Running upgrades in a section, leaving out the Goblin's slot.
    fun busyIn(section: Section, nowMillis: Long): Int =
        upgrades.count { it.section == section && !it.extra && endMillis(it) > nowMillis }

    // The Builder Base has no slot count, so it reads idle only when nothing runs there.
    fun idleIn(section: Section, nowMillis: Long): Int =
        if (section == Section.BUILDER_BASE) {
            if (upgrades.any { it.section == section && endMillis(it) > nowMillis }) 0 else 1
        } else {
            maxOf(0, slotsFor(section) - busyIn(section, nowMillis))
        }

    fun boostFor(section: Section): Boost = when (section) {
        Section.BUILDERS -> builderBoost
        Section.LAB -> labBoost
        Section.PETS -> petBoost
        Section.BUILDER_BASE -> clockTowerBoost
    }

    // The game's text for Study Soup names both Laboratory research and Pet House upgrades.
    fun snackFor(section: Section): Boost = when (section) {
        Section.BUILDERS -> builderSnack
        Section.LAB, Section.PETS -> labSnack
        Section.BUILDER_BASE -> Boost.NONE
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
        Pace(boostFor(upgrade.section), upgrade.helperSec, helperLevelFor(upgrade.section), snackFor(upgrade.section))

    fun withUpgrades(upgrades: List<Upgrade>) =
        Village(tag, exportedAtMillis, townHall, builders, hasPetHouse, upgrades, builderBoost, labBoost, petBoost, helpers, clockTowerBoost, hasBuilderBase, builderSnack, labSnack)

    fun endMillis(upgrade: Upgrade): Long =
        exportedAtMillis + paceFor(upgrade).realSecondsFor(upgrade.workSec) * 1000

    fun progress(upgrade: Upgrade, nowMillis: Long): Float {
        val total = upgrade.totalSec ?: return 0f
        val elapsed = ((nowMillis - exportedAtMillis) / 1000).coerceAtLeast(0)
        val left = (upgrade.workSec - paceFor(upgrade).workDoneIn(elapsed)).coerceAtLeast(0)
        return (1f - left.toFloat() / total).coerceIn(0f, 1f)
    }
}
