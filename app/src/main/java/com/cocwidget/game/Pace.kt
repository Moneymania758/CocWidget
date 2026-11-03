package com.cocwidget.game

const val BUILDER_POTION_MULTIPLIER = 10
// Community figure, not confirmed by Supercell.
const val BUILDER_BITE_MULTIPLIER = 2
const val PET_POTION_MULTIPLIER = 24
const val RESEARCH_POTION_MULTIPLIER = 24
const val STUDY_SOUP_MULTIPLIER = 4
// Supercell's Builder Base news, March 2020.
const val CLOCK_TOWER_MULTIPLIER = 10

// A potion running for [seconds] more real seconds, making upgrades go [multiplier] times faster.
class Boost(val seconds: Long, val multiplier: Int) {
    companion object {
        val NONE = Boost(0, 1)
    }
}

// The Builder's Apprentice and Lab Assistant levels, and the seconds left at export of the
// 23-hour work day they share (0 when they are ready for a new job).
class Helpers(val apprenticeLevel: Int, val assistantLevel: Int, val cooldownSec: Long) {
    companion object {
        val NONE = Helpers(0, 0, 0)
    }
}

// How fast one upgrade's timer runs after the export. A boost multiplies normal speed, and a
// helper on shift adds its level on top: the game says potions don't affect helpers, so a level
// 12 Lab Assistant during a Research Potion runs research at 24 + 12 = 36x.
class Pace(private val boost: Boost, private val helperSec: Long = 0, private val helperLevel: Int = 0) {
    // The rate only changes when the boost or the shift ends.
    private val changes = listOf(boost.seconds, if (helperLevel > 0) helperSec else 0L).filter { it > 0 }.sorted()

    private fun rateAt(t: Long): Long =
        (if (t < boost.seconds) boost.multiplier.toLong() else 1L) + (if (t < helperSec) helperLevel else 0)

    fun realSecondsFor(work: Long): Long {
        var t = 0L
        var left = work
        for (end in changes) {
            if (end <= t) continue
            val rate = rateAt(t)
            if (left <= (end - t) * rate) return t + (left + rate - 1) / rate
            left -= (end - t) * rate
            t = end
        }
        return t + left
    }

    fun workDoneIn(elapsed: Long): Long {
        var t = 0L
        var done = 0L
        for (end in changes) {
            if (end <= t) continue
            if (elapsed <= end) return done + rateAt(t) * (elapsed - t)
            done += rateAt(t) * (end - t)
            t = end
        }
        return done + (elapsed - t)
    }
}
