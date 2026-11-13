package com.cocwidget.ui

import android.content.Context
import com.cocwidget.R
import com.cocwidget.game.Section
import com.cocwidget.game.UpNext
import com.cocwidget.widget.NextFinish

// The words Up next uses, shared by the main screen, the account sheet and the widgets so they
// can't drift apart.
object UpNextLabels {
    fun group(group: UpNext.Group) = when (group) {
        UpNext.Group.NOW -> R.string.upnext_now
        UpNext.Group.NEXT_HOUR -> R.string.upnext_next_hour
        UpNext.Group.TODAY -> R.string.upnext_today
        UpNext.Group.TOMORROW -> R.string.upnext_tomorrow
        UpNext.Group.THIS_WEEK -> R.string.upnext_this_week
        UpNext.Group.LATER -> R.string.upnext_later
    }

    // What does the upgrade: "Builder", "Lab".
    fun kind(section: Section) = when (section) {
        Section.BUILDERS -> R.string.upnext_kind_builder
        Section.LAB -> R.string.upnext_kind_lab
        Section.PETS -> R.string.upnext_kind_pets
        Section.BUILDER_BASE -> R.string.upnext_kind_builder_base
    }

    // "2 builders free", "Lab free".
    fun idleTitle(context: Context, section: Section, count: Int): String = when (section) {
        Section.BUILDERS -> context.resources.getQuantityString(R.plurals.upnext_builders_free, count, count)
        Section.LAB -> context.getString(R.string.upnext_lab_free)
        else -> context.getString(R.string.upnext_pets_free)
    }

    // What to do about a free slot.
    fun startHint(section: Section) = when (section) {
        Section.BUILDERS -> R.string.upnext_start_builder
        Section.LAB -> R.string.upnext_start_lab
        else -> R.string.upnext_start_pet
    }

    // Each idle thing, as "2 builders idle" and "Lab idle". Empty when everything is busy.
    fun idleParts(context: Context, summary: NextFinish.Summary): List<String> = buildList {
        if (summary.idleBuilders > 0) add(context.resources.getQuantityString(R.plurals.next_builders_idle, summary.idleBuilders, summary.idleBuilders))
        if (summary.labIdle) add(context.getString(R.string.next_lab_idle))
        if (summary.petsIdle) add(context.getString(R.string.next_pets_idle))
    }

    // "2 builders idle · Lab idle", or that every builder is busy. With nothing left for the
    // builders to start, "all busy" would be wrong once they finish, so it's only that nothing is idle.
    fun idleLine(context: Context, summary: NextFinish.Summary): String =
        idleParts(context, summary).joinToString(" · ").ifEmpty {
            if (summary.buildersDone) {
                context.getString(R.string.next_nothing_idle)
            } else {
                context.resources.getQuantityString(R.plurals.next_all_busy, summary.builders, summary.builders)
            }
        }

    // "Lab maxed", for a section with nothing left at this Town Hall.
    fun maxed(context: Context, section: Section): String = context.getString(
        when (section) {
            Section.BUILDERS -> R.string.maxed_builders
            Section.LAB -> R.string.maxed_lab
            else -> R.string.maxed_pets
        },
    )
}
