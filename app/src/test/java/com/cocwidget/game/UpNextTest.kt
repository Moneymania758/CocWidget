package com.cocwidget.game

import com.cocwidget.TestData
import com.cocwidget.events.EventKind
import com.cocwidget.events.EventSchedule
import com.cocwidget.game.UpNext.Group
import com.cocwidget.game.UpNext.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class UpNextTest {
    private val zone: ZoneId = ZoneOffset.UTC

    // Wednesday 19:00, the drafts' "now".
    private val now = LocalDateTime.of(2026, 11, 11, 19, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val hour = 3_600_000L

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 11, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun group(at: Long) = UpNext.groupOf(at, now, zone)

    @Test
    fun groupsByWallClock() {
        assertEquals(Group.NOW, group(now))
        assertEquals(Group.NOW, group(now - hour))
        assertEquals(Group.NEXT_HOUR, group(now + hour))
        assertEquals(Group.TODAY, group(at(11, 23, 59)))
        assertEquals(Group.TOMORROW, group(at(12, 0, 30)))
        assertEquals(Group.TOMORROW, group(at(12, 23, 59)))
        assertEquals(Group.THIS_WEEK, group(at(13, 1)))
        assertEquals(Group.THIS_WEEK, group(now + 7 * 24 * hour - 1))
        assertEquals(Group.LATER, group(now + 7 * 24 * hour))
    }

    // Just after midnight, "next hour" wins over "tomorrow" for the first hour.
    @Test
    fun theNextHourComesBeforeTheDay() {
        val late = at(11, 23, 30)
        assertEquals(Group.NEXT_HOUR, UpNext.groupOf(at(12, 0, 15), late, zone))
    }

    private fun village(vararg upgrades: Upgrade, helpers: Helpers = Helpers.NONE, builderBoost: Boost = Boost.NONE, labSnack: Boost = Boost.NONE) =
        TestData.villageOf(now, *upgrades, helpers = helpers, builderBoost = builderBoost, labSnack = labSnack)

    @Test
    fun idleSlotsComeFirstUnderNow() {
        val v = village(TestData.upgrade(600), TestData.upgrade(7_200, Section.LAB, "Wizard"))
        val items = UpNext.items(v, emptyList(), now, null)
        val idle = items.filterIsInstance<Item.Idle>()
        // Six builders with one busy, the lab busy, the Pet House empty.
        assertEquals(listOf(Section.BUILDERS to 5, Section.PETS to 1), idle.map { it.section to it.count })
        assertTrue(items.take(2).all { it is Item.Idle })
    }

    @Test
    fun theBuilderBaseIsNeverIdleHere() {
        val v = village().let { Village(it.tag, it.exportedAtMillis, 16, 6, false, emptyList(), Boost.NONE, Boost.NONE, Boost.NONE, Helpers.NONE, hasBuilderBase = true) }
        assertTrue(UpNext.items(v, emptyList(), now, null).filterIsInstance<Item.Idle>().none { it.section == Section.BUILDER_BASE })
    }

    @Test
    fun upgradesRunSoonestFirstAndFinishedOnesSitUnderNow() {
        val later = TestData.upgrade(7_200, name = "Later")
        val sooner = TestData.upgrade(600, name = "Sooner")
        val v = TestData.villageOf(now - 1_200_000, later, sooner)
        val running = UpNext.items(v, emptyList(), now, null).filterIsInstance<Item.Running>()
        assertEquals(listOf("Sooner", "Later"), running.map { it.upgrade.name })
        assertEquals(Group.NOW, group(running.first().atMillis))
    }

    @Test
    fun aFilterKeepsOneSection() {
        val v = village(TestData.upgrade(600), TestData.upgrade(7_200, Section.LAB, "Wizard"))
        val lab = UpNext.items(v, emptyList(), now, Section.LAB)
        assertTrue(lab.isNotEmpty())
        assertTrue(lab.all { it is Item.Running && it.upgrade.section == Section.LAB })
    }

    @Test
    fun eventsShowOnlyUnfiltered() {
        val event = EventSchedule.Countdown(EventKind.CLAN_GAMES, running = false, target = now + 30 * hour)
        val v = village()
        assertEquals(1, UpNext.items(v, listOf(event), now, null).count { it is Item.Event })
        assertEquals(0, UpNext.items(v, listOf(event), now, Section.BUILDERS).count { it is Item.Event })
    }

    @Test
    fun eventsWorkWithNoVillage() {
        val event = EventSchedule.Countdown(EventKind.CLAN_GAMES, running = true, target = now + hour / 2)
        assertEquals(listOf(event.target), UpNext.items(null, listOf(event), now, null).map { it.atMillis })
    }

    @Test
    fun helpersShowWhenReadyAndUnderTheirSections() {
        val helpers = Helpers(apprenticeLevel = 5, assistantLevel = 0, cooldownSec = 3_600)
        val v = village(helpers = helpers)
        val ready = UpNext.items(v, emptyList(), now, null).filterIsInstance<Item.HelpersReady>().single()
        assertEquals(now + hour, ready.atMillis)
        assertEquals(1, UpNext.items(v, emptyList(), now, Section.BUILDERS).count { it is Item.HelpersReady })
        // Only the Apprentice is unlocked, so the lab's list leaves them out.
        assertEquals(0, UpNext.items(v, emptyList(), now, Section.LAB).count { it is Item.HelpersReady })
    }

    @Test
    fun helpersAlreadyReadySitUnderNow() {
        val v = village(helpers = Helpers(5, 4, 0))
        val ready = UpNext.items(v, emptyList(), now, null).filterIsInstance<Item.HelpersReady>().single()
        assertEquals(Group.NOW, group(ready.atMillis))
    }

    @Test
    fun aRunningBoostEndsInTheStreamOnce() {
        // Study Soup speeds the lab and the Pet House, but its end is one moment.
        val v = village(builderBoost = Boost(1_800, 10), labSnack = Boost(3_000, 2))
        val ends = UpNext.items(v, emptyList(), now, null).filterIsInstance<Item.BoostEnds>()
        assertEquals(listOf(now + 1_800_000, now + 3_000_000), ends.map { it.atMillis })
        assertEquals(1, UpNext.items(v, emptyList(), now, Section.PETS).count { it is Item.BoostEnds })
    }

    @Test
    fun theStreamIsInTimeOrderAfterNow() {
        val v = village(TestData.upgrade(90_000), TestData.upgrade(600, Section.LAB, "Wizard"), helpers = Helpers(5, 4, 7_200))
        val event = EventSchedule.Countdown(EventKind.CLAN_GAMES, running = false, target = now + 3 * hour)
        val later = UpNext.items(v, listOf(event), now, null).filter { it.atMillis > now }
        assertEquals(later.map { it.atMillis }.sorted(), later.map { it.atMillis })
    }
}
