package com.cocwidget.widget

import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village

// What the small widget shows: the running upgrade that finishes first, and which home village
// slots sit idle. Kept free of Android types so unit tests can run it.
object NextFinish {
    class Summary(
        // Null when nothing is running.
        val next: Upgrade?,
        val builders: Int,
        val idleBuilders: Int,
        val labIdle: Boolean,
        val petsIdle: Boolean,
    ) {
        val anyIdle get() = idleBuilders > 0 || labIdle || petsIdle
    }

    // Idle counts match the big widget's section headers. The Goblin's slot doesn't count, and
    // the Builder Base is left out: a maxed one is idle for good, which isn't worth a warning.
    fun of(village: Village, now: Long): Summary {
        val running = village.upgrades.filter { village.endMillis(it) > now }
        fun busy(section: Section) = running.count { it.section == section && !it.extra }
        return Summary(
            next = running.minByOrNull { village.endMillis(it) },
            builders = village.builders,
            idleBuilders = maxOf(0, village.builders - busy(Section.BUILDERS)),
            labIdle = busy(Section.LAB) == 0,
            petsIdle = village.hasPetHouse && busy(Section.PETS) == 0,
        )
    }
}
