package com.cocwidget.widget

import com.cocwidget.TestData.upgrade
import com.cocwidget.TestData.villageOf
import com.cocwidget.game.Boost
import com.cocwidget.game.BUILDER_POTION_MULTIPLIER
import com.cocwidget.game.Section
import com.cocwidget.plan.Bedtime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

// Maxed sections, the potion moment and quiet nights, worked out without Android.
class MaxedAndOvernightTest {
    private val now = 1_800_000_000_000L
    private val hour = 3_600L
    private val utc = ZoneId.of("UTC")
    private val quiet = Notifier.Quiet(Bedtime.Sleep(23 * 60, 7 * 60), utc)

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2027, 1, day, hour, minute).atZone(utc).toInstant().toEpochMilli()

    @Test
    fun aSectionWithNothingToStartIsNeverIdle() {
        val village = villageOf(now, nothingToStart = setOf(Section.LAB))
        assertEquals(0, village.idleIn(Section.LAB, now))
        val summary = NextFinish.of(village, now)
        assertFalse(summary.labIdle)
        assertTrue(summary.petsIdle)
        assertEquals(listOf(Section.LAB), summary.maxed)
    }

    @Test
    fun maxedOnlyOnceItsLastUpgradeEnds() {
        val village = villageOf(now, upgrade(hour, Section.LAB), nothingToStart = setOf(Section.LAB))
        assertFalse(village.maxed(Section.LAB, now))
        assertEquals(emptyList<Section>(), NextFinish.of(village, now).maxed)
        assertTrue(village.maxed(Section.LAB, now + hour * 1000))
    }

    // The Goblin researches too, so the lab isn't done while it works.
    @Test
    fun theGoblinsResearchKeepsTheLabFromMaxed() {
        val village = villageOf(now, upgrade(hour, Section.LAB, extra = true), nothingToStart = setOf(Section.LAB))
        assertFalse(village.maxed(Section.LAB, now))
        assertFalse(NextFinish.of(village, now).labIdle)
    }

    @Test
    fun buildersWithNothingToStartAreNeverIdle() {
        val village = villageOf(now, upgrade(hour), nothingToStart = setOf(Section.BUILDERS))
        val summary = NextFinish.of(village, now)
        assertEquals(0, summary.idleBuilders)
        assertTrue(summary.buildersDone)
    }

    private fun builders(vararg work: Long, builderBoost: Boost = Boost.NONE, nothingToStart: Set<Section> = emptySet()) =
        villageOf(now, *work.mapIndexed { i, w -> upgrade(w, name = "Building $i") }.toTypedArray(), builderBoost = builderBoost, nothingToStart = nothingToStart)

    @Test
    fun aPotionIsDueWhenEveryBuilderStaysBusyForTheHour() {
        assertEquals(listOf(Section.BUILDERS), WidgetRenderer.potionNow(builders(*LongArray(6) { 24 * hour }), now))
    }

    @Test
    fun noPotionWithABuilderIdle() {
        assertFalse(Section.BUILDERS in WidgetRenderer.potionNow(builders(*LongArray(5) { 24 * hour }), now))
    }

    // Under BUILDER_POTION_MULTIPLIER hours of work, one builder finishes inside the hour.
    @Test
    fun noPotionWhenSomethingFinishesInsideTheHour() {
        val work = LongArray(6) { 24 * hour }.also { it[0] = (BUILDER_POTION_MULTIPLIER - 1) * hour }
        assertFalse(Section.BUILDERS in WidgetRenderer.potionNow(builders(*work), now))
    }

    @Test
    fun noPotionWhileOneRuns() {
        val village = builders(*LongArray(6) { 24 * hour }, builderBoost = Boost(1_800, BUILDER_POTION_MULTIPLIER))
        assertFalse(Section.BUILDERS in WidgetRenderer.potionNow(village, now))
    }

    // Quiet nights: 23:00 to 07:00 UTC. The upgrade ends at 02:00.
    private val night = villageOf(at(10, 1), upgrade(hour, name = "Cannon"))

    @Test
    fun nothingIsAnnouncedOrRememberedWhileAsleep() {
        val decision = Notifier.decide(night, emptySet(), at(10, 2, 1), quiet)
        assertTrue(decision.toNotify.isEmpty())
        assertTrue(decision.notified.isEmpty())
    }

    @Test
    fun whatFinishedInTheNightComesAtWakeUp() {
        val decision = Notifier.decide(night, emptySet(), at(10, 7, 0), quiet)
        assertEquals(1, decision.toNotify.size)
        assertEquals(1, decision.overnight.size)
        assertTrue(Notifier.decide(night, decision.notified, at(10, 7, 5), quiet).toNotify.isEmpty())
    }

    // Two hours after it ends would usually be too late. After a night it counts from wake-up.
    @Test
    fun anEarlyNightFinishStillComesAtWakeUp() {
        val early = villageOf(at(9, 23, 30), upgrade(hour, name = "Cannon"))
        assertEquals(1, Notifier.decide(early, emptySet(), at(10, 7, 10), quiet).toNotify.size)
    }

    @Test
    fun longAfterWakeUpItStaysQuietButIsRemembered() {
        val decision = Notifier.decide(night, emptySet(), at(10, 9, 30), quiet)
        assertTrue(decision.toNotify.isEmpty())
        assertEquals(1, decision.notified.size)
    }

    @Test
    fun twoNightFinishesAreOneOvernightPair() {
        val two = villageOf(at(10, 1), upgrade(hour, name = "Cannon"), upgrade(2 * hour, name = "Archer Tower"))
        assertEquals(2, Notifier.decide(two, emptySet(), at(10, 7, 1), quiet).overnight.size)
    }

    @Test
    fun aDaytimeFinishIsntOvernight() {
        val day = villageOf(at(10, 12), upgrade(hour, name = "Cannon"))
        val decision = Notifier.decide(day, emptySet(), at(10, 13, 1), quiet)
        assertEquals(1, decision.toNotify.size)
        assertTrue(decision.overnight.isEmpty())
    }

    @Test
    fun withQuietNightsOffTheNightIsLikeTheDay() {
        assertEquals(1, Notifier.decide(night, emptySet(), at(10, 2, 1), null).toNotify.size)
    }

    @Test
    fun theFinishAlarmWaitsForWakeUp() {
        assertEquals(at(10, 7), Notifier.postAt(at(10, 2), quiet))
        assertEquals(at(10, 12), Notifier.postAt(at(10, 12), quiet))
        assertEquals(at(10, 2), Notifier.postAt(at(10, 2), null))
    }
}
