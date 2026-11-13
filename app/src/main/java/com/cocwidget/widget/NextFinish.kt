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
        // Home village sections maxed for this Town Hall, in the order shown.
        val maxed: List<Section> = emptyList(),
        // Builders with nothing left to start, so no count of busy builders means anything.
        val buildersDone: Boolean = false,
    ) {
        val anyIdle get() = idleBuilders > 0 || labIdle || petsIdle
    }

    // Idle counts match the big widget's section headers. The Goblin's slot doesn't count, and
    // the Builder Base is left out: a maxed one is idle for good, which isn't worth a warning. Nor
    // is a home section with nothing left to start: it shows as maxed once its last upgrade ends.
    fun of(village: Village, now: Long): Summary {
        val running = village.upgrades.filter { village.endMillis(it) > now }
        val home = listOf(Section.BUILDERS, Section.LAB, Section.PETS).filter { it != Section.PETS || village.hasPetHouse }
        return Summary(
            next = running.minByOrNull { village.endMillis(it) },
            builders = village.builders,
            idleBuilders = village.idleIn(Section.BUILDERS, now),
            labIdle = village.idleIn(Section.LAB, now) > 0,
            petsIdle = village.hasPetHouse && village.idleIn(Section.PETS, now) > 0,
            maxed = home.filter { village.maxed(it, now) },
            buildersDone = Section.BUILDERS in village.nothingToStart,
        )
    }
}
