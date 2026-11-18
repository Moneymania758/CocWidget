package com.cocwidget.plan

import com.cocwidget.game.GameItem
import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner.Group

// What happened between an import and the one before it, taken at [previousAt]: the upgrade
// levels finished in each section, and about how long that section's slots sat idle.
data class Between(
    val previousAt: Long,
    val levels: Map<Section, Int>,
    val idleSeconds: Map<Section, Long>,
)

object Betweens {
    // The sections with slots to keep busy. The Builder Base isn't counted.
    val SECTIONS = listOf(Section.BUILDERS, Section.LAB, Section.PETS)

    // Null when either isn't an export, or [after] isn't later.
    //
    // Idle time is the slots' time between the two less the time they were busy: what was left
    // on the upgrades running in [before], what had passed on the ones running in [after] that
    // weren't, and the full time of levels started and finished in between. Upgrades are taken
    // at normal speed, so potions, helpers and the Gold Pass, which make them shorter, make idle
    // time read lower than it was, never higher.
    fun of(before: String, after: String, items: Map<Int, GameItem>): Between? {
        val then = VillageParser.parse(before) { items[it] } ?: return null
        val now = VillageParser.parse(after) { items[it] } ?: return null
        val span = (now.exportedAtMillis - then.exportedAtMillis) / 1000
        if (span <= 0) return null
        val finished = Finished.between(before, after, items) ?: return null

        val levels = HashMap<Section, Int>()
        val busy = HashMap<Section, Long>()
        fun add(section: Section, seconds: Long) = busy.merge(section, seconds.coerceIn(0, span), Long::plus)

        // Running then: busy until it ended or the next import, whichever came first.
        val runningThen = then.upgrades.filter { counts(it) }
        for (upgrade in runningThen) add(upgrade.section, (then.endMillis(upgrade) - then.exportedAtMillis) / 1000)
        // Running now and not then: busy for as long as it has been going.
        val leftThen = runningThen.map(::key).toMutableList()
        for (upgrade in now.upgrades.filter { counts(it) }) {
            if (leftThen.remove(key(upgrade))) continue
            val total = upgrade.totalSec ?: continue
            add(upgrade.section, total - upgrade.workSec)
        }
        // Finished in between: each level counts, and those not running then took their full time.
        val finishedThen = runningThen.map(::key).toMutableList()
        for (row in finished) {
            val section = sectionOf(row.group) ?: continue
            val item = items[row.id] ?: continue
            val steps = if (row.supercharge) (row.fromStage + 1..row.toStage).map { row.to to it } else (row.from + 1..row.to).map { it to 0 }
            repeat(row.copies) {
                for ((level, stage) in steps) {
                    levels.merge(section, 1, Int::plus)
                    if (finishedThen.remove(Key(section, item.name, level, stage))) continue
                    val seconds = if (stage > 0) item.supercharge?.times?.getOrNull(stage - 1)?.toLong() ?: 0 else item.secondsToReach(level)
                    add(section, seconds)
                }
            }
        }

        val idle = SECTIONS.filter { it in now.sections() }.associateWith { section ->
            val slots = now.slotsFor(section)
            (slots * span - (busy[section] ?: 0)).coerceAtLeast(0)
        }
        return Between(then.exportedAtMillis, SECTIONS.associateWith { levels[it] ?: 0 }.filterKeys { it in idle }, idle)
    }

    private data class Key(val section: Section, val name: String, val level: Int, val stage: Int)

    private fun key(upgrade: Upgrade) = Key(upgrade.section, upgrade.name, upgrade.toLevel, upgrade.supercharge)

    // The Goblin's slot is extra, so it never leaves a regular one idle.
    private fun counts(upgrade: Upgrade) = upgrade.section in SECTIONS && !upgrade.extra

    // Walls and equipment take no time, so they're left out of both counts.
    private fun sectionOf(group: Group): Section? = when (group) {
        Group.WALLS, Group.EQUIPMENT -> null
        Group.LAB -> Section.LAB
        Group.PETS -> Section.PETS
        else -> Section.BUILDERS
    }
}
