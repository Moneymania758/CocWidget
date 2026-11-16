package com.cocwidget.plan

import com.cocwidget.game.GameItem
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner.Group

// Copies of one item that went up between two imports: from level [from] to [to], or 0 to [to]
// for a copy built since. A supercharge keeps its level and goes from stage [fromStage] to
// [toStage].
data class FinishedUpgrade(
    val id: Int,
    val name: String,
    val group: Group,
    val from: Int,
    val to: Int,
    val copies: Int,
    val fromStage: Int = 0,
    val toStage: Int = 0,
) {
    val supercharge get() = from == to
}

// What was upgraded between two imports, worked out from the levels in each.
object Finished {
    private class Copy(val level: Int, val stage: Int) : Comparable<Copy> {
        override fun compareTo(other: Copy) = compareValuesBy(this, other, { it.level }, { it.stage })
    }

    // Everything the planner counts (the home village, no Builder Base) that went up from
    // [before] to [after], in the planner's group order, then by name. Null when either isn't an
    // export.
    fun between(before: String, after: String, items: Map<Int, GameItem>): List<FinishedUpgrade>? {
        val then = copies(before, items) ?: return null
        val now = copies(after, items) ?: return null
        val found = mutableMapOf<FinishedUpgrade, Int>()
        for ((id, list) in now) {
            val item = items.getValue(id)
            val group = Group.of(item.group) ?: continue
            val earlier = then[id].orEmpty()
            // Highest with highest, so a copy built since is the lowest one left over.
            for ((index, copy) in list.withIndex()) {
                val old = earlier.getOrNull(index) ?: Copy(0, 0)
                if (copy <= old) continue
                val key = if (copy.level > old.level) {
                    FinishedUpgrade(id, item.name, group, old.level, copy.level, 0)
                } else {
                    FinishedUpgrade(id, item.name, group, copy.level, copy.level, 0, old.stage, copy.stage)
                }
                found.merge(key, 1, Int::plus)
            }
        }
        return found.map { (upgrade, copies) -> upgrade.copy(copies = copies) }
            .sortedWith(compareBy({ it.group.ordinal }, { it.name }, { -it.to }, { -it.toStage }))
    }

    // Each item's copies, highest first. A copy at level 0 isn't built yet, so it's left out.
    private fun copies(text: String, items: Map<Int, GameItem>): Map<Int, List<Copy>>? {
        val root = VillageParser.root(text) ?: return null
        val copies = HashMap<Int, MutableList<Copy>>()
        for (section in Planner.SECTIONS) {
            val entries = root.optJSONArray(section) ?: continue
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                val id = entry.optInt("data")
                if (items[id]?.group.isNullOrEmpty()) continue
                val level = entry.optInt("lvl")
                if (level <= 0) continue
                repeat(entry.optInt("cnt", 1)) {
                    copies.getOrPut(id) { mutableListOf() } += Copy(level, entry.optInt("supercharge"))
                }
            }
        }
        return copies.mapValues { it.value.sortedDescending() }
    }
}
