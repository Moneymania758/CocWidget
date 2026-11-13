package com.cocwidget.plan

import com.cocwidget.game.GameItem
import com.cocwidget.game.Ids
import com.cocwidget.game.Section

// Which home village slots have nothing left to start at this Town Hall: every level still to
// reach is already running. A free slot there is maxed rather than wasted, so it isn't called idle.
object Leftover {
    private const val CACHED_EXPORTS = 8

    // The widget reads the same export every minute, so each one is planned once.
    private val cache = object : LinkedHashMap<String, Set<Section>>(CACHED_EXPORTS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Set<Section>>) = size > CACHED_EXPORTS
    }

    @Synchronized
    fun cached(text: String, items: Map<Int, GameItem>): Set<Section> = cache.getOrPut(text) { of(text, items) }

    // Empty when in doubt, which keeps the idle warnings as they were: an export that can't be
    // planned, or a Town Hall newer than the game data, whose new levels the data doesn't know yet.
    fun of(text: String, items: Map<Int, GameItem>): Set<Section> {
        val plan = try {
            Planner.plan(text, items, 0)
        } catch (e: RuntimeException) {
            null
        } ?: return emptySet()
        val top = items[Ids.TOWN_HALL]?.levelCount ?: 0
        if (plan.townHall <= 0 || plan.townHall > top) return emptySet()
        return of(plan)
    }

    // Walls take no time, so they keep no builder busy and don't count. The lab and the Pet House
    // only count as done once their own building is too: a higher one unlocks more to do there.
    internal fun of(plan: Planner.Plan): Set<Section> {
        val builderItems = plan.groups.filter { it.group.usesBuilders }.flatMap { it.items }
        fun done(items: List<Planner.Item>) = items.none(::waiting)
        fun buildingDone(id: Int) = builderItems.filter { it.id == id }.none(::waiting)
        fun group(group: Planner.Group) = plan.group(group)?.items.orEmpty()
        return buildSet {
            if (done(builderItems)) add(Section.BUILDERS)
            if (done(group(Planner.Group.LAB)) && buildingDone(Ids.LABORATORY)) add(Section.LAB)
            if (done(group(Planner.Group.PETS)) && buildingDone(Ids.PET_HOUSE)) add(Section.PETS)
        }
    }

    // A level that takes time and has copies not yet on it.
    private fun waiting(item: Planner.Item) = item.steps.any { it.seconds > 0 && it.copies > it.running }
}
