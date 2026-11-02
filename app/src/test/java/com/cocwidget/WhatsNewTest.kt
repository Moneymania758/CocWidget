package com.cocwidget

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
}
