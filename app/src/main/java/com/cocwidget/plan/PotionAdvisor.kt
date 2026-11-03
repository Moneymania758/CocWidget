package com.cocwidget.plan

import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village

// Whether an hour-long potion would be fully used now. It makes every slot in its section work
// [multiplier] times as fast, so each busy slot gains up to multiplier - 1 hours. An idle slot
// gains nothing, and one whose upgrade finishes partway through gains less. The event Goblin's
// slot and helpers aren't counted.
object PotionAdvisor {
    const val HOUR_SEC = 3600L

    // [finishing] holds the upgrades that would finish inside the hour, with the real seconds
    // each would take. [boostLeftSec] is a boost already running, and then nothing else is
    // worked out.
    class Advice(
        val idle: Int,
        val finishing: List<Pair<Upgrade, Long>>,
        val savedSec: Long,
        val possibleSec: Long,
        val boostLeftSec: Long,
    ) {
        val full: Boolean get() = boostLeftSec == 0L && savedSec >= possibleSec
    }

    fun advise(village: Village, section: Section, slots: Int, multiplier: Int, now: Long): Advice {
        val boostLeft = ((village.boostEndMillis(village.boostFor(section)) - now) / 1000).coerceAtLeast(0)
        val possible = slots * (multiplier - 1) * HOUR_SEC
        if (boostLeft > 0) return Advice(0, emptyList(), 0, possible, boostLeft)

        val elapsed = ((now - village.exportedAtMillis) / 1000).coerceAtLeast(0)
        val busy = village.upgrades
            .filter { it.section == section && !it.extra }
            .map { it to (it.workSec - village.paceFor(it).workDoneIn(elapsed)).coerceAtLeast(0) }
            .filter { it.second > 0 }
        val boosted = multiplier * HOUR_SEC
        var saved = 0L
        val finishing = ArrayList<Pair<Upgrade, Long>>()
        for ((upgrade, work) in busy) {
            if (work >= boosted) {
                saved += (multiplier - 1) * HOUR_SEC
            } else {
                val real = (work + multiplier - 1) / multiplier
                saved += work - real
                finishing += upgrade to real
            }
        }
        return Advice(
            idle = (slots - busy.size).coerceAtLeast(0),
            finishing = finishing.sortedBy { it.second },
            savedSec = saved,
            possibleSec = possible,
            boostLeftSec = 0,
        )
    }
}
