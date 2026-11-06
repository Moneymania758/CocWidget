package com.cocwidget.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class EventDatesTest {
    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun file(events: String, updated: String = "2026-11-06", version: Int = 1) =
        """{"version":$version,"updated":"$updated","events":[$events]}"""

    @Test
    fun readsAnEventWithAnEnd() {
        val dates = EventDates.parse(
            file("""{"id":"hj","kind":"hammer_jam","start":"2026-11-01T08:00:00Z","end":"2026-11-16T08:00:00Z"}"""),
        )!!
        val event = dates.events.single()
        assertEquals("hj", event.id)
        assertEquals(EventKind.HAMMER_JAM, event.kind)
        assertEquals(at("2026-11-01T08:00:00Z"), event.start)
        assertEquals(at("2026-11-16T08:00:00Z"), event.end)
    }

    @Test
    fun readsACancellation() {
        val dates = EventDates.parse(file("""{"id":"clan_games:2026-12","kind":"clan_games","cancelled":true}"""))!!
        assertTrue(dates.events.isEmpty())
        assertEquals(setOf("clan_games:2026-12"), dates.cancelled)
    }

    @Test
    fun skipsWhatItCantReadAndKeepsTheRest() {
        val dates = EventDates.parse(
            file(
                """{"id":"a","kind":"something_new","start":"2026-11-01T08:00:00Z"},""" +
                    """{"id":"b","kind":"legend","start":"not a date"},""" +
                    """{"id":"c","kind":"hammer_jam","start":"2026-11-16T08:00:00Z","end":"2026-11-01T08:00:00Z"},""" +
                    """{"kind":"legend","start":"2026-11-23T05:00:00Z"},""" +
                    """{"id":"${"x".repeat(100)}","kind":"legend","start":"2026-11-23T05:00:00Z"},""" +
                    """{"id":"good","kind":"legend","start":"2026-11-23T05:00:00Z"}""",
            ),
        )!!
        assertEquals(listOf("good"), dates.events.map { it.id })
    }

    @Test
    fun aFileThatIsntOneIsRefused() {
        assertNull(EventDates.parse(null))
        assertNull(EventDates.parse("<html>404</html>"))
        assertNull(EventDates.parse("""{"updated":"2026-11-06"}"""))
        assertNull(EventDates.parse(file("", updated = "soon")))
        assertNull(EventDates.parse(file("", version = 2)))
    }

    @Test
    fun theNewerFileWins() {
        val old = EventDates.parse(file("", updated = "2026-11-01"))!!
        val new = EventDates.parse(file("", updated = "2026-11-06"))!!
        assertEquals(new, EventDates.newest(old, new))
        assertEquals(new, EventDates.newest(new, old))
        assertEquals(old, EventDates.newest(old, null))
    }

    // The checked-in file, which the app also downloads from GitHub.
    @Test
    fun theBundledFileHasSupercellsHammerJamTimes() {
        val dates = EventDates.parse(File("src/main/assets/events.json").readText())
        assertNotNull(dates)
        val jam = dates!!.events.single { it.kind == EventKind.HAMMER_JAM }
        assertEquals(at("2026-11-01T08:00:00Z"), jam.start)
        assertEquals(at("2026-11-16T08:00:00Z"), jam.end)
        assertTrue(dates.events.any { it.kind == EventKind.LEGEND && it.start == at("2026-11-23T05:00:00Z") })
    }
}
