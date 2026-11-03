package com.cocwidget.plan

import com.cocwidget.game.Section
import com.cocwidget.game.Village
import java.time.Instant
import java.time.ZoneId

// Which upgrade to start so it finishes while you're awake, and no builder, lab or pet house sits
// idle overnight. Times are minutes after midnight, in the phone's time zone.
object Bedtime {
    class Sleep(val bedMinute: Int, val wakeMinute: Int)

    class Option(val step: Planner.Step, val finishMillis: Long, val awake: Boolean)

    // The window can cross midnight (23:00 to 07:00) or not (01:00 to 09:00).
    fun asleep(millis: Long, sleep: Sleep, zone: ZoneId): Boolean {
        val time = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        val minute = time.hour * 60 + time.minute
        return if (sleep.bedMinute <= sleep.wakeMinute) {
            minute >= sleep.bedMinute && minute < sleep.wakeMinute
        } else {
            minute >= sleep.bedMinute || minute < sleep.wakeMinute
        }
    }

    // The next wake-up time after [millis].
    fun nextWake(millis: Long, sleep: Sleep, zone: ZoneId): Long {
        val at = Instant.ofEpochMilli(millis).atZone(zone)
        var wake = at.toLocalDate().atTime(sleep.wakeMinute / 60, sleep.wakeMinute % 60).atZone(zone)
        if (!wake.isAfter(at)) wake = wake.plusDays(1)
        return wake.toInstant().toEpochMilli()
    }

    // A slot that frees up while you're asleep can't be used until you wake.
    fun startAt(freeMillis: Long, sleep: Sleep, zone: ZoneId): Long =
        if (asleep(freeMillis, sleep, zone)) nextWake(freeMillis, sleep, zone) else freeMillis

    // When [section] next has a free slot: now if one is idle, or when the soonest running
    // upgrade ends. The event Goblin's slot doesn't count.
    fun freeAt(village: Village, section: Section, slots: Int, now: Long): Long {
        val ends = village.upgrades
            .filter { it.section == section && !it.extra }
            .map { village.endMillis(it) }
            .filter { it > now }
        return if (ends.size < slots) now else ends.min()
    }

    // Soonest finish first, with whether each one lands while you're awake.
    fun options(steps: List<Planner.Step>, startMillis: Long, sleep: Sleep, zone: ZoneId): List<Option> = steps
        .map { step ->
            val finish = startMillis + step.seconds * 1000
            Option(step, finish, !asleep(finish, sleep, zone))
        }
        .sortedWith(compareBy<Option> { it.finishMillis }.thenBy { it.step.name })
}
