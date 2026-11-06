package com.cocwidget.events

import com.cocwidget.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

// The game events worth a reminder. [label] names one in Settings.
enum class EventKind(val key: String, val onByDefault: Boolean, val label: Int) {
    CLAN_GAMES("clan_games", true, R.string.event_clan_games),
    CWL("cwl", true, R.string.event_cwl),
    GOLD_PASS("gold_pass", true, R.string.event_gold_pass),
    HAMMER_JAM("hammer_jam", true, R.string.event_hammer_jam),
    // Weekly, or for few players, so people opt in.
    RAID_WEEKEND("raid_weekend", false, R.string.event_raid),
    RANKED("ranked", false, R.string.event_ranked),
    LEGEND("legend", false, R.string.event_legend),
    ;

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

// When events happen and which reminders that makes. Regular events come from rules, and the
// dates file adds one-offs and corrects the rules. An entry with a rule's id replaces that rule's
// event. Supercell's calendar gives dates only, so the hours are checked separately (see the
// ledger).
object EventSchedule {
    // A phone off for longer than this stays quiet about what it missed.
    private const val LATE_MS = 12 * 60 * 60_000L
    private const val DAY_MS = 24 * 60 * 60_000L
    private const val MONTHS_BACK = 1L
    private const val MONTHS_AHEAD = 2L
    private const val WEEKLY_DAYS_BACK = 8L
    private const val WEEKLY_DAYS_AHEAD = 63L
    private const val SEASON_HOUR = 8
    private const val RAID_HOUR = 7
    private const val RANKED_HOUR = 17

    class Occurrence(val id: String, val kind: EventKind, val start: Long, val end: Long?)

    class Reminder(val id: String, val kind: EventKind, val at: Long, val title: Int, val text: Int)

    // [notified] is what to remember as handled. [next] is the next reminder still to come.
    class Decision(val due: List<Reminder>, val notified: Set<String>, val next: Long?)

    fun reminders(now: Long, dates: EventDates.Dates): List<Reminder> {
        val replaced = dates.events.map { it.id }.toSet() + dates.cancelled
        return (rules(now).filter { it.id !in replaced } + dates.events).flatMap(::remindersFor).sortedBy { it.at }
    }

    fun decide(
        reminders: List<Reminder>,
        isOn: (EventKind) -> Boolean,
        since: (EventKind) -> Long,
        notified: Set<String>,
        now: Long,
    ): Decision {
        val wanted = reminders.filter { isOn(it.kind) }
        val passed = wanted.filter { it.at <= now }
        val due = passed.filter { it.id !in notified && now - it.at <= LATE_MS && it.at > since(it.kind) }
        // Only ids still in the window are kept, so the set doesn't grow forever.
        val kept = (notified intersect reminders.map { it.id }.toSet()) + passed.map { it.id }
        return Decision(due, kept, wanted.firstOrNull { it.at > now }?.at)
    }

    private fun rules(now: Long): List<Occurrence> {
        val today = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate()
        val thisMonth = YearMonth.from(today)
        val monthly = (-MONTHS_BACK..MONTHS_AHEAD).map(thisMonth::plusMonths).flatMap { month ->
            val first = month.atDay(1)
            listOf(
                Occurrence("${EventKind.CLAN_GAMES.key}:$month", EventKind.CLAN_GAMES, at(month.atDay(22), SEASON_HOUR), at(month.atDay(28), SEASON_HOUR)),
                Occurrence("${EventKind.CWL.key}:$month", EventKind.CWL, at(first, SEASON_HOUR), null),
                Occurrence("${EventKind.GOLD_PASS.key}:$month", EventKind.GOLD_PASS, at(first, SEASON_HOUR), at(month.plusMonths(1).atDay(1), SEASON_HOUR)),
            )
        }
        val weekly = (-WEEKLY_DAYS_BACK..WEEKLY_DAYS_AHEAD).map(today::plusDays).mapNotNull { day ->
            when (day.dayOfWeek) {
                DayOfWeek.FRIDAY -> Occurrence("${EventKind.RAID_WEEKEND.key}:$day", EventKind.RAID_WEEKEND, at(day, RAID_HOUR), at(day.plusDays(3), RAID_HOUR))
                DayOfWeek.MONDAY -> Occurrence("${EventKind.RANKED.key}:$day", EventKind.RANKED, at(day, RANKED_HOUR), null)
                else -> null
            }
        }
        return monthly + weekly
    }

    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun remindersFor(event: Occurrence): List<Reminder> {
        val started = Part("started", event.start)
        val dayBefore = event.end?.let { Part("ending", it - DAY_MS) }
        val ended = event.end?.let { Part("ended", it) }
        val parts = when (event.kind) {
            EventKind.CLAN_GAMES -> listOf(
                started to (R.string.event_clan_games_started to R.string.event_clan_games_started_text),
                dayBefore to (R.string.event_clan_games_ending to R.string.event_clan_games_ending_text),
                ended to (R.string.event_clan_games_ended to R.string.event_clan_games_ended_text),
            )
            EventKind.CWL -> listOf(started to (R.string.event_cwl_open to R.string.event_cwl_open_text))
            EventKind.GOLD_PASS -> listOf(
                event.end?.let { Part("final_week", it - 7 * DAY_MS) } to (R.string.event_gold_pass_final_week to R.string.event_gold_pass_final_week_text),
                dayBefore to (R.string.event_gold_pass_last_day to R.string.event_gold_pass_last_day_text),
            )
            EventKind.HAMMER_JAM -> listOf(
                started to (R.string.event_hammer_jam_started to R.string.event_hammer_jam_started_text),
                dayBefore to (R.string.event_hammer_jam_ending to R.string.event_hammer_jam_ending_text),
            )
            EventKind.RAID_WEEKEND -> listOf(started to (R.string.event_raid_started to R.string.event_raid_started_text))
            EventKind.RANKED -> listOf(started to (R.string.event_ranked_open to R.string.event_ranked_open_text))
            EventKind.LEGEND -> listOf(started to (R.string.event_legend_started to R.string.event_legend_started_text))
        }
        return parts.mapNotNull { (part, words) ->
            part?.let { Reminder("${event.id}:${it.name}", event.kind, it.at, words.first, words.second) }
        }
    }

    private class Part(val name: String, val at: Long)
}
