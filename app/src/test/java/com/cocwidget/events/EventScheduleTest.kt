package com.cocwidget.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventScheduleTest {
    private val hour = 3_600_000L
    private val noFile = EventDates.Dates("2026-11-06", emptyList(), emptySet())

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun times(kind: EventKind, now: String, dates: EventDates.Dates = noFile) =
        EventSchedule.reminders(at(now), dates).filter { it.kind == kind }.map { Instant.ofEpochMilli(it.at).toString() }

    private fun decide(
        now: Long,
        dates: EventDates.Dates = noFile,
        off: Set<EventKind> = emptySet(),
        since: Long = 0,
        notified: Set<String> = emptySet(),
    ) = EventSchedule.decide(EventSchedule.reminders(now, dates), { it !in off }, { since }, notified, now)

    @Test
    fun clanGamesStartsOnThe22ndEndsOnThe28thAndWarnsADayBefore() {
        val times = times(EventKind.CLAN_GAMES, "2026-11-06T12:00:00Z")
        assertTrue(times.toString(), "2026-11-22T08:00:00Z" in times)
        assertTrue(times.toString(), "2026-11-27T08:00:00Z" in times)
        assertTrue(times.toString(), "2026-11-28T08:00:00Z" in times)
    }

    @Test
    fun clanWarLeagueSignUpOpensOnTheFirst() {
        assertTrue("2026-12-01T08:00:00Z" in times(EventKind.CWL, "2026-11-06T12:00:00Z"))
    }

    @Test
    fun goldPassWarnsAWeekAndADayBeforeTheSeasonEnds() {
        val times = times(EventKind.GOLD_PASS, "2026-11-06T12:00:00Z")
        assertTrue(times.toString(), "2026-11-24T08:00:00Z" in times)
        assertTrue(times.toString(), "2026-11-30T08:00:00Z" in times)
    }

    @Test
    fun goldPassCountsBackFromTheFirstAcrossAYearEndAndFebruary() {
        val december = times(EventKind.GOLD_PASS, "2026-12-10T12:00:00Z")
        assertTrue(december.toString(), "2026-12-31T08:00:00Z" in december)
        val february = times(EventKind.GOLD_PASS, "2027-02-10T12:00:00Z")
        assertTrue(february.toString(), "2027-02-22T08:00:00Z" in february)
        assertTrue(february.toString(), "2027-02-28T08:00:00Z" in february)
    }

    @Test
    fun raidWeekendStartsEveryFridayAt0700() {
        val times = times(EventKind.RAID_WEEKEND, "2026-11-06T12:00:00Z")
        assertTrue(times.toString(), "2026-11-06T07:00:00Z" in times)
        assertTrue(times.toString(), "2026-11-13T07:00:00Z" in times)
    }

    @Test
    fun rankedBattlesOpenEveryMondayAt1700() {
        val times = times(EventKind.RANKED, "2026-11-06T12:00:00Z")
        assertTrue(times.toString(), "2026-11-09T17:00:00Z" in times)
        assertTrue(times.toString(), "2026-11-16T17:00:00Z" in times)
    }

    @Test
    fun hammerJamAndLegendOnlyComeFromTheDatesFile() {
        assertTrue(times(EventKind.HAMMER_JAM, "2026-11-06T12:00:00Z").isEmpty())
        assertTrue(times(EventKind.LEGEND, "2026-11-06T12:00:00Z").isEmpty())

        val dates = EventDates.Dates(
            "2026-11-06",
            listOf(
                EventSchedule.Occurrence("hammer-jam-2026", EventKind.HAMMER_JAM, at("2026-11-01T08:00:00Z"), at("2026-11-16T08:00:00Z")),
                EventSchedule.Occurrence("legend-2026-11-23", EventKind.LEGEND, at("2026-11-23T05:00:00Z"), null),
            ),
            emptySet(),
        )
        assertEquals(
            listOf("2026-11-01T08:00:00Z", "2026-11-15T08:00:00Z"),
            times(EventKind.HAMMER_JAM, "2026-11-06T12:00:00Z", dates),
        )
        assertEquals(listOf("2026-11-23T05:00:00Z"), times(EventKind.LEGEND, "2026-11-06T12:00:00Z", dates))
    }

    @Test
    fun aDatesFileEntryWithARulesIdMovesThatEvent() {
        val moved = EventDates.Dates(
            "2026-11-06",
            listOf(EventSchedule.Occurrence("clan_games:2026-12", EventKind.CLAN_GAMES, at("2026-12-20T08:00:00Z"), at("2026-12-26T08:00:00Z"))),
            emptySet(),
        )
        val times = times(EventKind.CLAN_GAMES, "2026-12-01T12:00:00Z", moved)
        assertTrue(times.toString(), "2026-12-20T08:00:00Z" in times)
        assertFalse(times.toString(), "2026-12-22T08:00:00Z" in times)
    }

    @Test
    fun aDatesFileCanCancelARulesEvent() {
        val cancelled = EventDates.Dates("2026-11-06", emptyList(), setOf("clan_games:2026-12"))
        assertFalse("2026-12-22T08:00:00Z" in times(EventKind.CLAN_GAMES, "2026-12-01T12:00:00Z", cancelled))
    }

    @Test
    fun aSecondClanWarLeagueSitsBesideTheRuleOne() {
        // June 2026 had two.
        val extra = EventDates.Dates(
            "2026-11-06",
            listOf(EventSchedule.Occurrence("cwl:2026-12-extra", EventKind.CWL, at("2026-12-15T08:00:00Z"), null)),
            emptySet(),
        )
        val times = times(EventKind.CWL, "2026-12-05T12:00:00Z", extra)
        assertTrue(times.toString(), "2026-12-15T08:00:00Z" in times)
        assertTrue(times.toString(), "2027-01-01T08:00:00Z" in times)
    }

    private fun countdown(kind: EventKind, now: String, dates: EventDates.Dates = noFile) =
        EventSchedule.countdowns(at(now), dates).firstOrNull { it.kind == kind }

    @Test
    fun beforeAnEventTheCountdownIsToItsStart() {
        val clanGames = countdown(EventKind.CLAN_GAMES, "2026-11-06T12:00:00Z")!!
        assertFalse(clanGames.running)
        assertEquals(at("2026-11-22T08:00:00Z"), clanGames.target)
    }

    @Test
    fun duringAnEventTheCountdownIsToItsEnd() {
        val clanGames = countdown(EventKind.CLAN_GAMES, "2026-11-25T12:00:00Z")!!
        assertTrue(clanGames.running)
        assertEquals(at("2026-11-28T08:00:00Z"), clanGames.target)
    }

    @Test
    fun theCountdownChangesAtTheExactMoment() {
        assertFalse(countdown(EventKind.CLAN_GAMES, "2026-11-22T07:59:59Z")!!.running)
        assertTrue(countdown(EventKind.CLAN_GAMES, "2026-11-22T08:00:00Z")!!.running)
        // Over, so it moves on to December's.
        val over = countdown(EventKind.CLAN_GAMES, "2026-11-28T08:00:00Z")!!
        assertFalse(over.running)
        assertEquals(at("2026-12-22T08:00:00Z"), over.target)
    }

    @Test
    fun theGoldPassSeasonIsAlwaysOn() {
        val season = countdown(EventKind.GOLD_PASS, "2026-11-06T12:00:00Z")!!
        assertTrue(season.running)
        assertEquals(at("2026-12-01T08:00:00Z"), season.target)
    }

    @Test
    fun clanWarLeagueSignUpClosesOnTheThird() {
        val open = countdown(EventKind.CWL, "2026-12-02T12:00:00Z")!!
        assertTrue(open.running)
        assertEquals(at("2026-12-03T08:00:00Z"), open.target)
        // Closed, so on to January's sign-up. Clans' wars run at their own times.
        assertEquals(at("2027-01-01T08:00:00Z"), countdown(EventKind.CWL, "2026-12-04T12:00:00Z")!!.target)
    }

    @Test
    fun raidWeekendCountsDownToMondayOnceItsOn() {
        val raid = countdown(EventKind.RAID_WEEKEND, "2026-11-07T12:00:00Z")!!
        assertTrue(raid.running)
        assertEquals(at("2026-11-09T07:00:00Z"), raid.target)
        assertEquals(at("2026-11-13T07:00:00Z"), countdown(EventKind.RAID_WEEKEND, "2026-11-10T12:00:00Z")!!.target)
    }

    @Test
    fun rankedStopsAt0500AndReopensAt1700OnMonday() {
        val battles = countdown(EventKind.RANKED, "2026-11-12T12:00:00Z")!!
        assertTrue(battles.running)
        assertEquals(at("2026-11-16T05:00:00Z"), battles.target)
        val gap = countdown(EventKind.RANKED, "2026-11-16T10:00:00Z")!!
        assertFalse(gap.running)
        assertEquals(at("2026-11-16T17:00:00Z"), gap.target)
    }

    @Test
    fun aOneOffOnlyCountsDownWhileItHasADate() {
        val dates = EventDates.Dates(
            "2026-11-06",
            listOf(EventSchedule.Occurrence("hj", EventKind.HAMMER_JAM, at("2026-11-01T08:00:00Z"), at("2026-11-16T08:00:00Z"))),
            emptySet(),
        )
        assertEquals(at("2026-11-16T08:00:00Z"), countdown(EventKind.HAMMER_JAM, "2026-11-06T12:00:00Z", dates)!!.target)
        assertNull(countdown(EventKind.HAMMER_JAM, "2026-11-16T08:00:00Z", dates))
        assertNull(countdown(EventKind.HAMMER_JAM, "2026-11-06T12:00:00Z"))
    }

    @Test
    fun countdownsAreSoonestFirst() {
        val targets = EventSchedule.countdowns(at("2026-11-06T12:00:00Z"), noFile).map { it.target }
        assertEquals(targets.sorted(), targets)
    }

    @Test
    fun aReminderIsDueOnceItsTimeComes() {
        val start = at("2026-11-22T08:00:00Z")
        assertTrue(decide(start - 1).due.none { it.id == "clan_games:2026-11:started" })
        assertTrue(decide(start).due.any { it.id == "clan_games:2026-11:started" })
    }

    @Test
    fun aReminderTooLateStaysQuiet() {
        val start = at("2026-11-22T08:00:00Z")
        assertTrue(decide(start + 11 * hour).due.any { it.id == "clan_games:2026-11:started" })
        assertTrue(decide(start + 13 * hour).due.none { it.id == "clan_games:2026-11:started" })
    }

    @Test
    fun aReminderOnlyFiresOnce() {
        val start = at("2026-11-22T08:00:00Z")
        val first = decide(start)
        assertTrue(first.notified.contains("clan_games:2026-11:started"))
        assertTrue(decide(start + hour, notified = first.notified).due.none { it.id == "clan_games:2026-11:started" })
    }

    @Test
    fun aKindSwitchedOffStaysQuietAndIsLeftOutOfTheNextAlarm() {
        val friday = at("2026-11-13T07:00:00Z")
        val off = decide(friday, off = setOf(EventKind.RAID_WEEKEND))
        assertTrue(off.due.none { it.kind == EventKind.RAID_WEEKEND })
        // The next one on is Ranked, on Monday 16 Nov, not next Friday's raid.
        val sunday = at("2026-11-15T09:00:00Z")
        assertEquals(at("2026-11-16T17:00:00Z"), decide(sunday, off = setOf(EventKind.RAID_WEEKEND)).next)
    }

    @Test
    fun nothingFiresFromBeforeTheSwitchWasTurnedOn() {
        val start = at("2026-11-22T08:00:00Z")
        assertTrue(decide(start + hour, since = start + 1).due.none { it.id == "clan_games:2026-11:started" })
    }

    @Test
    fun noNextReminderWithEveryKindOff() {
        assertNull(decide(at("2026-11-06T12:00:00Z"), off = EventKind.entries.toSet()).next)
    }

    @Test
    fun oldNotifiedIdsAreDropped() {
        val decision = decide(at("2026-11-06T12:00:00Z"), notified = setOf("clan_games:2020-01:started"))
        assertFalse(decision.notified.contains("clan_games:2020-01:started"))
    }
}
