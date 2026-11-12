package com.cocwidget.game

import com.cocwidget.events.EventSchedule
import java.time.Instant
import java.time.ZoneId

// The main screen's one stream: what is free now, then everything that ends, in time order, in
// groups by the wall clock. Kept free of Android types so unit tests can run it.
object UpNext {
    enum class Group { NOW, NEXT_HOUR, TODAY, TOMORROW, THIS_WEEK, LATER }

    sealed class Item(val atMillis: Long) {
        // Free slots in a section. The Builder Base never shows here, as a maxed one is free for good.
        class Idle(val section: Section, val count: Int, now: Long) : Item(now)

        // An upgrade, finished or still running.
        class Running(val upgrade: Upgrade, endMillis: Long) : Item(endMillis)

        // The helpers' next work day, or now when it has already started.
        class HelpersReady(readyMillis: Long) : Item(readyMillis)

        // A potion or a snack running out. [section] is where it shows when the list is filtered.
        class BoostEnds(val section: Section, val boost: Boost, val snack: Boolean, endMillis: Long) : Item(endMillis)

        class Event(val countdown: EventSchedule.Countdown) : Item(countdown.target)
    }

    private const val HOUR = 3_600_000L

    // A weekday alone names a day only within the next six. A week on it's today's name again, so
    // "Thu" next Thursday would read as this morning.
    fun weekdayIsEnough(atMillis: Long, now: Long, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate().isBefore(Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(7))

    fun groupOf(atMillis: Long, now: Long, zone: ZoneId): Group {
        if (atMillis <= now) return Group.NOW
        if (atMillis - now <= HOUR) return Group.NEXT_HOUR
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val day = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate()
        return when {
            day == today -> Group.TODAY
            day == today.plusDays(1) -> Group.TOMORROW
            weekdayIsEnough(atMillis, now, zone) -> Group.THIS_WEEK
            else -> Group.LATER
        }
    }

    // [only] keeps one section, or null for everything. Events only show unfiltered.
    fun items(village: Village?, events: List<EventSchedule.Countdown>, now: Long, only: Section?): List<Item> {
        val out = mutableListOf<Item>()
        if (village != null) {
            for (section in village.sections()) {
                if (only != null && section != only) continue
                if (section != Section.BUILDER_BASE) {
                    val idle = village.idleIn(section, now)
                    if (idle > 0) out += Item.Idle(section, idle, now)
                }
                for (upgrade in village.upgrades.filter { it.section == section }) out += Item.Running(upgrade, village.endMillis(upgrade))
                boostEnds(village, section, village.boostFor(section), snack = false, now)?.let { out += it }
                // Study Soup is the lab's and the Pet House's, so unfiltered it shows once, as the lab's.
                if (section != Section.PETS || only == Section.PETS) {
                    boostEnds(village, section, village.snackFor(section), snack = true, now)?.let { out += it }
                }
            }
            val helpers = village.helpers
            val wanted = when (only) {
                null -> helpers.apprenticeLevel > 0 || helpers.assistantLevel > 0
                Section.BUILDERS -> helpers.apprenticeLevel > 0
                Section.LAB -> helpers.assistantLevel > 0
                else -> false
            }
            if (wanted) out += Item.HelpersReady(maxOf(village.helpersReadyMillis(), now))
        }
        if (only == null) events.forEach { out += Item.Event(it) }
        // Free slots, then ready helpers, then finished upgrades, then everything by its time.
        return out.sortedWith(compareBy<Item>({ if (it.atMillis <= now) 0 else 1 }, { rank(it, now) }, { it.atMillis }))
    }

    private fun rank(item: Item, now: Long): Int = if (item.atMillis > now) 0 else when (item) {
        is Item.Idle -> 0
        is Item.HelpersReady -> 1
        else -> 2
    }

    private fun boostEnds(village: Village, section: Section, boost: Boost, snack: Boolean, now: Long): Item.BoostEnds? {
        val end = village.boostEndMillis(boost)
        return if (boost.seconds > 0 && end > now) Item.BoostEnds(section, boost, snack, end) else null
    }
}
