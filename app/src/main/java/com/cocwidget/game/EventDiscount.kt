package com.cocwidget.game

import kotlin.math.abs

// Hammer Jam and the Gold Pass boost cut upgrade times, but the export doesn't say which upgrades
// were started with them. Upgrades new since the last sync while either applies are taken to be
// discounted, and their progress bar total is cut to match. Timers and notifications are
// unaffected: they use the real time left from the export.
object EventDiscount {
    const val PERCENT = 50
    // Allowance when matching an upgrade to its predicted time left: clock drift and rounding.
    private const val MATCH_SLACK_SEC = 600L

    // ids[i] names village.upgrades[i] and carries over between syncs. [discounted] holds each
    // discounted upgrade's price as a percent of full: 50 in Hammer Jam, 80 with a 20% Gold Pass.
    class Tracked(val ids: List<String>, val discounted: Map<String, Int>)

    // [eventOn] and [goldPass] are what applied when [current] was exported.
    fun track(
        previous: Village?,
        previousIds: List<String>,
        previousDiscounted: Map<String, Int>,
        current: Village,
        eventOn: Boolean,
        goldPass: Int = 0,
    ): Tracked {
        val ids = arrayOfNulls<String>(current.upgrades.size)
        val inherited = BooleanArray(current.upgrades.size)

        if (previous != null) {
            val oldIds = previousIds.takeIf { it.size == previous.upgrades.size }
                ?: previous.upgrades.indices.map { "${previous.exportedAtMillis}#$it" }
            val elapsed = ((current.exportedAtMillis - previous.exportedAtMillis) / 1000).coerceAtLeast(0)
            val currentByBase = current.upgrades.indices.groupBy { base(current.upgrades[it]) }

            previous.upgrades.indices
                .groupBy { base(previous.upgrades[it]) }
                .forEach { (base, oldIndexes) ->
                    val candidates = currentByBase[base].orEmpty().toMutableList()
                    // Each old upgrade takes the current copy nearest its predicted time left.
                    // Time left only ever goes down, so a copy well above the prediction can't be it.
                    for (old in oldIndexes) {
                        val predicted = predictedWork(previous, previous.upgrades[old], elapsed)
                        val match = candidates
                            .filter { current.upgrades[it].workSec <= predicted + MATCH_SLACK_SEC }
                            .minByOrNull { abs(current.upgrades[it].workSec - predicted) }
                            ?: continue
                        candidates.remove(match)
                        ids[match] = oldIds[old]
                        inherited[match] = true
                    }
                }
        }

        val finalIds = ids.mapIndexed { index, id -> id ?: "${current.exportedAtMillis}#$index" }
        val discounted = HashMap<String, Int>()
        for ((index, upgrade) in current.upgrades.withIndex()) {
            val id = finalIds[index]
            val isNew = previous != null && !inherited[index]
            val percent = previousDiscounted[id] ?: if (isNew) pricePercent(upgrade, eventOn, goldPass) else 100
            if (percent < 100 && couldBeDiscounted(upgrade, percent)) discounted[id] = percent
        }
        return Tracked(finalIds, discounted)
    }

    fun apply(village: Village, ids: List<String>, discounted: Map<String, Int>): Village {
        if (discounted.isEmpty() || ids.size != village.upgrades.size) return village
        val upgrades = village.upgrades.mapIndexed { index, upgrade ->
            val total = upgrade.totalSec
            val percent = discounted[ids[index]]
            if (percent != null && total != null && eligible(upgrade)) upgrade.copy(totalSec = total * percent / 100) else upgrade
        }
        return village.withUpgrades(upgrades)
    }

    // What an upgrade started now costs as a percent of full price, as Time to max prices it:
    // Hammer Jam's half, then the Gold Pass boost on builder and lab upgrades. Supercharges,
    // crafted defences and the Builder Base stay at full price.
    fun pricePercent(upgrade: Upgrade, eventOn: Boolean, goldPass: Int): Int {
        if (!eligible(upgrade)) return 100
        val event = if (eventOn) 100 - PERCENT else 100
        return if (upgrade.section == Section.BUILDERS || upgrade.section == Section.LAB) event * (100 - goldPass) / 100 else event
    }

    private fun base(upgrade: Upgrade) = "${upgrade.section}|${upgrade.name}|${upgrade.toLevel}|${upgrade.supercharge}|${upgrade.extra}"

    private fun predictedWork(village: Village, upgrade: Upgrade, elapsedSec: Long) =
        upgrade.workSec - village.paceFor(upgrade).workDoneIn(elapsedSec)

    // More time left than the discounted total means it can't have been started at a discount.
    private fun couldBeDiscounted(upgrade: Upgrade, percent: Int): Boolean {
        val total = upgrade.totalSec ?: return false
        return eligible(upgrade) && upgrade.workSec <= total * percent / 100
    }

    // Supercell's Hammer Jam post (1 Nov 2026) leaves supercharges, crafted defences and the
    // Builder Base out of the discount.
    private fun eligible(upgrade: Upgrade) =
        upgrade.supercharge == 0 && upgrade.defence.isEmpty() && upgrade.section != Section.BUILDER_BASE

}
