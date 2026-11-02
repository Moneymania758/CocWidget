package com.cocwidget

// Countdown maths for the widget, kept free of Android types so unit tests can run it.
object Countdown {
    const val DAY_MS = 86_400_000L
    const val MAX_REFRESH_MS = 15 * 60_000L

    // Chronometer can't show days, so a countdown is shown as whole days plus a chronometer
    // for the rest. The rest is always above zero and at most a day, so the chronometer never
    // starts at 0:00 on a day boundary.
    class Split(val days: Long, val restMs: Long)

    fun split(leftMs: Long): Split {
        val days = (leftMs - 1) / DAY_MS
        return Split(days, leftMs - days * DAY_MS)
    }

    // When the widget must redraw next: a chronometer reaching zero (a day rolling over or an
    // upgrade finishing), a boost ending, or the regular refresh, whichever comes first.
    fun nextRefresh(village: Village?, now: Long): Long {
        var next = now + MAX_REFRESH_MS
        if (village != null) {
            for (upgrade in village.upgrades) {
                val left = village.endMillis(upgrade) - now
                if (left > 0) next = minOf(next, now + split(left).restMs)
            }
            for (boost in listOf(village.builderBoost, village.petBoost)) {
                val end = village.boostEndMillis(boost)
                if (end > now) next = minOf(next, end)
            }
        }
        return maxOf(next, now + 1000)
    }
}
