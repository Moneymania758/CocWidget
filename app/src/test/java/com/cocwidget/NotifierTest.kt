package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifierTest {
    private val exportedAt = 1_800_000_000_000L
    private val village = TestData.villageOf(exportedAt, TestData.upgrade(workSec = 100))

    @Test
    fun nothingBeforeItFinishes() {
        val decision = Notifier.decide(village, emptySet(), exportedAt + 50_000)
        assertTrue(decision.toNotify.isEmpty())
        assertTrue(decision.notified.isEmpty())
    }

    @Test
    fun notifiesOnceWhenItFinishes() {
        val first = Notifier.decide(village, emptySet(), exportedAt + 101_000)
        assertEquals(1, first.toNotify.size)

        val second = Notifier.decide(village, first.notified, exportedAt + 106_000)
        assertTrue(second.toNotify.isEmpty())
    }

    @Test
    fun longFinishedUpgradesStayQuietButAreRemembered() {
        // Finished three hours ago, for example while the phone was off.
        val decision = Notifier.decide(village, emptySet(), exportedAt + 100_000 + 3 * 3_600_000)
        assertTrue(decision.toNotify.isEmpty())
        assertEquals(1, decision.notified.size)
    }

    @Test
    fun forgetsUpgradesNoLongerInTheExport() {
        val decision = Notifier.decide(village, setOf("an-old-export:upgrade"), exportedAt + 50_000)
        assertFalse("an-old-export:upgrade" in decision.notified)
    }

    @Test
    fun twoCopiesOfTheSameBuildingNotifySeparately() {
        val twoTowers = TestData.villageOf(
            exportedAt,
            TestData.upgrade(workSec = 100, name = "Bomb Tower"),
            TestData.upgrade(workSec = 120, name = "Bomb Tower", extra = true),
        )
        val decision = Notifier.decide(twoTowers, emptySet(), exportedAt + 130_000)
        assertEquals(2, decision.toNotify.size)
    }
}
