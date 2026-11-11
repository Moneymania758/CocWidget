package com.cocwidget.widget

import com.cocwidget.game.Village
import java.util.Locale

// Countdown maths for the widget, kept free of Android types so unit tests can run it.
object Countdown {
    const val DAY_MS = 86_400_000L
    const val MAX_REFRESH_MS = 15 * 60_000L
    const val HOUR_MS = 3_600_000L

    // Chronometer can't show days, so a countdown is shown as whole days plus a chronometer
    // for the rest. The rest is always above zero and at most a day, so the chronometer never
    // starts at 0:00 on a day boundary.
    class Split(val days: Long, val restMs: Long)

    fun split(leftMs: Long): Split {
        val days = (leftMs - 1) / DAY_MS
        return Split(days, leftMs - days * DAY_MS)
    }

    // Chronometer drops the hour below 60 minutes, so after a day count "1d 59:59" would read as 59
    // hours. With days in front and under an hour left, the format puts the hour back: "1d 0:59:59".
    fun hourPrefix(split: Split): String = if (split.days > 0 && split.restMs < HOUR_MS) "0:" else ""

    // The chronometer format for a countdown whose days are shown in front of it. Null is plain.
    fun chronometerFormat(split: Split): String? = hourPrefix(split).takeIf { it.isNotEmpty() }?.let { "$it%s" }

    // How long until a countdown with [leftMs] to go must be redrawn: when its chronometer reaches
    // zero, or, with days in front, when it drops under an hour and needs the hour put back.
    fun untilRedraw(leftMs: Long): Long {
        val split = split(leftMs)
        return if (split.days > 0 && split.restMs >= HOUR_MS) split.restMs - HOUR_MS + 1 else split.restMs
    }

    // A running clock for the app's own screens, where a text can hold days: "3d 04:12:55",
    // "04:12:55" or "12:55". Rounds up, so it reads 00:01 until the moment it ends.
    fun clock(leftMs: Long): String {
        val total = (leftMs.coerceAtLeast(0) + 999) / 1000
        val days = total / 86_400
        val hours = total % 86_400 / 3_600
        val minutes = total % 3_600 / 60
        val seconds = total % 60
        val time = if (days > 0 || hours > 0) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
        return if (days > 0) "${days}d $time" else time
    }

    // When the widget must redraw next: a chronometer reaching zero (a day rolling over or an
    // upgrade finishing) or dropping under an hour, a boost or helper shift ending, the helpers becoming ready, or the
    // regular refresh, whichever comes first.
    fun nextRefresh(village: Village?, now: Long): Long {
        var next = now + MAX_REFRESH_MS
        if (village != null) {
            val ends = mutableListOf(village.helpersReadyMillis())
            for (upgrade in village.upgrades) {
                val left = village.endMillis(upgrade) - now
                if (left > 0) next = minOf(next, now + untilRedraw(left))
                ends += village.helperShiftEndMillis(upgrade)
            }
            for (boost in listOf(village.builderBoost, village.builderSnack, village.labSnack, village.labBoost, village.petBoost, village.clockTowerBoost)) {
                ends += village.boostEndMillis(boost)
            }
            for (end in ends) if (end > now) next = minOf(next, end)
        }
        return maxOf(next, now + 1000)
    }

    // When an Events widget must redraw: a countdown's chronometer reaching zero (a day rolling
    // over, or an event starting or ending) or dropping under an hour, or the regular refresh.
    fun nextEventRefresh(targets: List<Long>, now: Long): Long {
        var next = now + MAX_REFRESH_MS
        for (target in targets) if (target > now) next = minOf(next, now + untilRedraw(target - now))
        return maxOf(next, now + 1000)
    }
}
