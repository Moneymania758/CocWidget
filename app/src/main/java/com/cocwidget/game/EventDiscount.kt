package com.cocwidget.game

import kotlin.math.abs

// Events like Hammer Jam cut upgrade times, but the export doesn't say which upgrades were
// started during one. With the event switch on, upgrades that are new since the last sync are
// taken to be discounted, and their progress bar total is cut to match. Timers and
// notifications are unaffected: they use the real time left from the export.
object EventDiscount {
    const val PERCENT = 50
    // Allowance when matching an upgrade to its predicted time left: clock drift and rounding.
    private const val MATCH_SLACK_SEC = 600L

    // ids[i] names village.upgrades[i] and carries over between syncs.
    class Tracked(val ids: List<String>, val discounted: Set<String>)

    fun track(
        previous: Village?,
        previousIds: List<String>,
        previousDiscounted: Set<String>,
        current: Village,
        eventOn: Boolean,
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
        val discounted = current.upgrades.indices.filter { index ->
            val upgrade = current.upgrades[index]
            val id = finalIds[index]
            val isNew = previous != null && !inherited[index]
            (id in previousDiscounted || (eventOn && isNew)) && couldBeDiscounted(upgrade)
        }.map { finalIds[it] }.toSet()

        return Tracked(finalIds, discounted)
    }

    fun apply(village: Village, ids: List<String>, discounted: Set<String>): Village {
        if (discounted.isEmpty() || ids.size != village.upgrades.size) return village
        val upgrades = village.upgrades.mapIndexed { index, upgrade ->
            val total = upgrade.totalSec
            if (ids[index] in discounted && total != null) upgrade.copy(totalSec = discountedTotal(total)) else upgrade
        }
        return village.withUpgrades(upgrades)
    }

    private fun base(upgrade: Upgrade) = "${upgrade.section}|${upgrade.name}|${upgrade.toLevel}|${upgrade.extra}"

    private fun predictedWork(village: Village, upgrade: Upgrade, elapsedSec: Long) =
        upgrade.workSec - village.paceFor(upgrade).workDoneIn(elapsedSec)

    // More time left than the discounted total means it can't have been started at a discount.
    private fun couldBeDiscounted(upgrade: Upgrade): Boolean {
        val total = upgrade.totalSec ?: return false
        return upgrade.workSec <= discountedTotal(total)
    }

    private fun discountedTotal(total: Long) = total * (100 - PERCENT) / 100
}
