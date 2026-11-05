package com.cocwidget.ui

import com.cocwidget.store.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WhatsNewTest {
    @Test
    fun newestEntryIsTheAppVersion() {
        // Gradle runs unit tests from the module folder.
        val versionName = Regex("""versionName = "([^"]+)"""").find(File("build.gradle.kts").readText())!!.groupValues[1]
        assertEquals(versionName, WhatsNew.RELEASES.first().version)
    }

    @Test
    fun versionsAreNewestFirstWithoutGaps() {
        val versions = WhatsNew.RELEASES.map { it.version }
        assertEquals(versions.sortedWith { a, b -> if (UpdateChecker.isNewer(a, b)) -1 else 1 }, versions)
        assertEquals(versions.size, versions.toSet().size)
    }

    // Android turns a real line break in a string into a space, which runs two bullets together.
    @Test
    fun notesBreakLinesWithAnEscapeNotARealLineBreak() {
        val notes = Regex("""<string name="(whats_new_\d[^"]*)">([^<]*)</string>""")
            .findAll(File("src/main/res/values/strings.xml").readText())
            .toList()
        assertTrue("no notes found", notes.isNotEmpty())
        for (note in notes) assertFalse(note.groupValues[1], "\n" in note.groupValues[2])
    }

    @Test
    fun cardShowsOncePerUpdate() {
        assertTrue(WhatsNew.showCard("1.7", lastSeen = "1.6", hasSynced = true))
        assertFalse(WhatsNew.showCard("1.7", lastSeen = "1.7", hasSynced = true))
    }

    @Test
    fun updatingFromBeforeTheListShowsTheCard() {
        // 1.6 and earlier recorded nothing, but had a synced village.
        assertTrue(WhatsNew.showCard("1.7", lastSeen = null, hasSynced = true))
    }

    @Test
    fun aFreshInstallShowsNoCard() {
        assertFalse(WhatsNew.showCard("1.7", lastSeen = null, hasSynced = false))
    }

    @Test
    fun onlyAFreshInstallsFirstSyncMarksTheVersionSeen() {
        assertTrue(WhatsNew.firstSyncOfFreshInstall(lastSeen = null, hasSynced = false))
        // Syncing a second account, or after an update from 1.6, must leave the card to show.
        assertFalse(WhatsNew.firstSyncOfFreshInstall(lastSeen = null, hasSynced = true))
        assertFalse(WhatsNew.firstSyncOfFreshInstall(lastSeen = "1.7", hasSynced = false))
    }

    @Test
    fun aFreshInstallStillSeesTheCardOnItsNextUpdate() {
        // What its first sync recorded.
        val lastSeen = "1.7"
        assertFalse(WhatsNew.showCard("1.7", lastSeen, hasSynced = true))
        assertTrue(WhatsNew.showCard("1.8", lastSeen, hasSynced = true))
    }
}
