package com.cocwidget.ui

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.events.EventSchedule
import com.cocwidget.game.Section
import com.cocwidget.game.UpNext
import com.cocwidget.game.UpNext.Group
import com.cocwidget.game.UpNext.Item
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.plan.PlanFormat
import com.cocwidget.store.Icons
import com.cocwidget.widget.Countdown
import com.cocwidget.widget.WidgetRenderer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// Up next on the main screen: a tile per section saying what is free, which also filters, then one
// stream of everything that ends, grouped by the wall clock. Free slots and finished upgrades sit
// under Now.
class UpNextList(
    private val activity: Activity,
    private val ui: Ui,
    private val pictures: Pictures,
    private val village: Village?,
    private val now: Long,
    // The section the stream is cut down to, or null for everything.
    private val only: Section?,
    private val onFilter: (Section?) -> Unit,
) {
    // A countdown on screen. The main screen redraws these each second, and rebuilds when one ends.
    class Clock(val view: TextView, val endMillis: Long, val text: (Long) -> CharSequence)

    val clocks = mutableListOf<Clock>()

    private val zone = ZoneId.systemDefault()
    private val timeFormat = DateFormat.getTimeFormat(activity)
    private val locale: Locale = activity.resources.configuration.locales[0]

    private fun clock(view: TextView, endMillis: Long, text: (Long) -> CharSequence) {
        view.text = text(endMillis - now)
        clocks += Clock(view, endMillis, text)
    }

    // The dashboard: one tile per section, as wide as the row allows. Null without a village.
    fun tiles(): View? {
        val village = village ?: return null
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false
            for ((index, section) in village.sections().withIndex()) {
                addView(tile(village, section), ui.weighted(if (index == 0) 0 else 8).apply { height = ViewGroup.LayoutParams.MATCH_PARENT })
            }
        }
    }

    private fun tile(village: Village, section: Section) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val chosen = only == section
        background = ui.rounded(ui.skin.card, 16f, if (chosen) ui.skin.accent else ui.skin.divider, if (chosen) 2f else 1f)
        setPadding(ui.dp(10), ui.dp(9), ui.dp(8), ui.dp(9))
        isSelected = chosen
        val (big, status, colour) = tileText(village, section)
        addView(oneLine(ui.text(activity.getString(tileName(section)), 11f, ui.skin.dim, bold = true)))
        addView(oneLine(ui.heading(big, 18f, ui.skin.text)).apply { fontFeatureSettings = "tnum" }, ui.matchWidth(2))
        addView(oneLine(ui.text(status, 11f, colour, bold = true)), ui.matchWidth(2))
        ui.pressable(this, 16f) { onFilter(if (chosen) null else section) }
    }

    private fun oneLine(view: TextView) = view.apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun tileName(section: Section) = when (section) {
        Section.BUILDERS -> R.string.upnext_tile_builders
        Section.LAB -> R.string.upnext_tile_lab
        Section.PETS -> R.string.upnext_tile_pets
        Section.BUILDER_BASE -> R.string.upnext_tile_builder_base
    }

    // The big line and the status under it. Builders count their slots; the lab and pets give the
    // time left on what's running; the Builder Base, which has no slot count, how much runs there.
    private fun tileText(village: Village, section: Section): Triple<String, String, Int> {
        val running = village.upgrades.filter { it.section == section && !it.extra && village.endMillis(it) > now }
        val idle = village.idleIn(section, now)
        return when (section) {
            Section.BUILDERS -> Triple(
                activity.getString(R.string.upnext_tile_count, village.busyIn(section, now), village.slotsFor(section)),
                if (idle > 0) activity.resources.getQuantityString(R.plurals.upnext_idle_count, idle, idle) else activity.getString(R.string.upnext_busy),
                if (idle > 0) ui.skin.idle else ui.skin.green,
            )
            Section.LAB, Section.PETS -> running.minOfOrNull { village.endMillis(it) }?.let { end ->
                Triple(PlanFormat.duration((end - now) / 1000), activity.getString(R.string.upnext_busy), ui.skin.green)
            } ?: Triple(activity.getString(R.string.upnext_idle), activity.getString(R.string.upnext_free_now), ui.skin.idle)
            Section.BUILDER_BASE -> if (running.isNotEmpty()) {
                Triple(running.size.toString(), activity.getString(R.string.upnext_busy), ui.skin.green)
            } else {
                Triple(activity.getString(R.string.upnext_idle), activity.getString(R.string.upnext_nothing_running), ui.skin.dim)
            }
        }
    }

    // "Showing Builders only", with the way back. Null when nothing is filtered.
    fun filterNote(): View? {
        val section = only ?: return null
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val name = activity.getString(tileName(section))
            addView(ui.text(activity.getString(R.string.upnext_showing, name), 13f, ui.skin.dim), ui.weighted())
            addView(ui.text(activity.getString(R.string.upnext_show_all), 14f, ui.skin.accent, bold = true).apply {
                minHeight = ui.dp(44)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(ui.dp(8), 0, ui.dp(4), 0)
                ui.pressable(this, 8f) { onFilter(null) }
            })
        }
    }

    // The stream, a heading per group that has anything in it.
    fun timeline(events: List<EventSchedule.Countdown>): View = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val items = UpNext.items(village, events, now, only)
        var shown: Group? = null
        for (item in items) {
            val group = UpNext.groupOf(item.atMillis, now, zone)
            if (group != shown) {
                addView(ui.text(activity.getString(groupName(group)), 12f, if (group == Group.NOW) ui.skin.idle else ui.skin.dim, bold = true).apply {
                    isAllCaps = true
                    letterSpacing = 0.06f
                }, ui.matchWidth(if (shown == null) 4 else 18))
                shown = group
            }
            addView(row(item, group), ui.matchWidth(8))
        }
    }

    private fun groupName(group: Group) = when (group) {
        Group.NOW -> R.string.upnext_now
        Group.NEXT_HOUR -> R.string.upnext_next_hour
        Group.TODAY -> R.string.upnext_today
        Group.TOMORROW -> R.string.upnext_tomorrow
        Group.THIS_WEEK -> R.string.upnext_this_week
        Group.LATER -> R.string.upnext_later
    }

    // The time it ends on the left, as on a calendar, then the card.
    private fun row(item: Item, group: Group) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        val waiting = item is Item.Idle || (item is Item.HelpersReady && item.atMillis <= now)
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(0, ui.dp(12), 0, 0)
            if (!waiting) {
                addView(ui.text(timeFormat.format(Date(item.atMillis)), 13f, ui.skin.text, bold = true).apply { fontFeatureSettings = "tnum" })
                dayOf(item.atMillis, group)?.let { addView(ui.text(it, 11f, ui.skin.dim)) }
            }
        }, LinearLayout.LayoutParams(ui.dp(48), ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(card(item, waiting), ui.weighted(10))
    }

    // Today needs no day. Within the week the weekday says it, further off the date.
    private fun dayOf(atMillis: Long, group: Group): String? {
        val pattern = when (group) {
            Group.NEXT_HOUR, Group.TODAY -> return null
            Group.NOW -> if (sameDay(atMillis)) return null else "EEE"
            Group.TOMORROW, Group.THIS_WEEK -> "EEE"
            Group.LATER -> "d MMM"
        }
        return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(atMillis).atZone(zone))
    }

    private fun sameDay(atMillis: Long) =
        Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    private fun card(item: Item, waiting: Boolean) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = if (waiting) dashed() else ui.rounded(ui.skin.card, 16f, ui.skin.divider, 1f)
        setPadding(ui.dp(10), ui.dp(10), ui.dp(12), ui.dp(10))
        when (item) {
            is Item.Idle -> idle(item)
            is Item.Running -> upgrade(item.upgrade)
            is Item.HelpersReady -> helpers(item)
            is Item.BoostEnds -> boost(item)
            is Item.Event -> event(item.countdown)
        }
    }

    private fun dashed() = GradientDrawable().apply {
        cornerRadius = ui.dp(16).toFloat()
        setStroke(ui.dp(1.5f), ui.skin.idle, ui.dp(6).toFloat(), ui.dp(4).toFloat())
    }

    private fun LinearLayout.middle(title: CharSequence, line: CharSequence?, titleColour: Int = ui.skin.text, extra: LinearLayout.() -> Unit = {}) {
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(oneLine(ui.text(title, 14f, titleColour, bold = true)), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                extra()
            })
            line?.let { addView(oneLine(ui.text(it, 12f, ui.skin.dim)), ui.matchWidth(2)) }
        }, ui.weighted(10))
    }

    private fun LinearLayout.right(text: CharSequence, colour: Int): TextView = ui.text(text, 14f, colour, bold = true).apply {
        fontFeatureSettings = "tnum"
    }.also { addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = ui.dp(8) }) }

    private fun LinearLayout.countdown(endMillis: Long, colour: Int = ui.skin.text) {
        val view = right("", colour)
        clock(view, endMillis) { Countdown.clock(it) }
    }

    private fun picture(path: String?) = ui.picture().also { view -> path?.let { pictures.add(it, view) } }

    private fun pictureSize() = LinearLayout.LayoutParams(ui.dp(40), ui.dp(40))

    private fun LinearLayout.idle(item: Item.Idle) {
        val (building, title, line) = when (item.section) {
            Section.BUILDERS -> Triple("Builder's Hut", activity.resources.getQuantityString(R.plurals.upnext_builders_free, item.count, item.count), R.string.upnext_start_builder)
            Section.LAB -> Triple("Laboratory", activity.getString(R.string.upnext_lab_free), R.string.upnext_start_lab)
            else -> Triple("Pet House", activity.getString(R.string.upnext_pets_free), R.string.upnext_start_pet)
        }
        val stand = Upgrade(item.section, building, "building", "home", 2, 0, null, false)
        addView(picture(Icons.path(stand)), pictureSize())
        middle(title, activity.getString(line), ui.skin.idle)
        right(activity.getString(R.string.upnext_free), ui.skin.idle)
    }

    private fun LinearLayout.upgrade(upgrade: Upgrade) {
        val village = village ?: return
        val end = village.endMillis(upgrade)
        val left = end - now
        addView(picture(Icons.path(upgrade)), pictureSize())
        val kind = activity.getString(kindName(upgrade.section))
        middle(WidgetRenderer.upgradeName(upgrade), if (upgrade.extra) activity.getString(R.string.upnext_kind_goblin, kind) else kind) {
            if (upgrade.extra) addView(rowFace(R.drawable.ic_face_goblin, ui.skin.goblin, R.string.goblin))
            if (left > 0 && village.isHelped(upgrade, now)) {
                addView(rowFace(WidgetRenderer.helperFace(upgrade.section), ui.skin.helper, R.string.helper))
            }
        }
        if (left > 0) countdown(end, if (left < HOUR_MS) ui.skin.green else ui.skin.text) else right(activity.getString(R.string.done), ui.skin.green)
    }

    private fun kindName(section: Section) = when (section) {
        Section.BUILDERS -> R.string.upnext_kind_builder
        Section.LAB -> R.string.upnext_kind_lab
        Section.PETS -> R.string.upnext_kind_pets
        Section.BUILDER_BASE -> R.string.upnext_kind_builder_base
    }

    private fun rowFace(face: Int, ring: Int, description: Int) = ui.face(face, 26, ring, activity.getString(description)).apply {
        (layoutParams as LinearLayout.LayoutParams).marginStart = ui.dp(6)
    }

    // Both helpers share one work day, so one row, with a face for each that's unlocked.
    private fun LinearLayout.helpers(item: Item.HelpersReady) {
        val village = village ?: return
        val helpers = village.helpers
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (helpers.apprenticeLevel > 0) addView(ui.face(WidgetRenderer.helperFace(Section.BUILDERS), 30, ui.skin.helper, activity.getString(R.string.helper)))
            if (helpers.assistantLevel > 0) {
                addView(ui.face(WidgetRenderer.helperFace(Section.LAB), 30, ui.skin.helper, activity.getString(R.string.helper)).apply {
                    if (helpers.apprenticeLevel > 0) (layoutParams as LinearLayout.LayoutParams).marginStart = -ui.dp(8)
                })
            }
        })
        val levels = listOfNotNull(
            helpers.apprenticeLevel.takeIf { it > 0 }?.let { activity.getString(R.string.upnext_apprentice, it) },
            helpers.assistantLevel.takeIf { it > 0 }?.let { activity.getString(R.string.upnext_assistant, it) },
        ).joinToString(" · ")
        val ready = item.atMillis <= now
        middle(activity.getString(R.string.home_helpers), levels, if (ready) ui.skin.idle else ui.skin.text)
        if (ready) right(activity.getString(R.string.home_helpers_ready), ui.skin.idle) else countdown(item.atMillis, ui.skin.helper)
    }

    private fun LinearLayout.boost(item: Item.BoostEnds) {
        addView(ImageView(activity).apply {
            setImageResource(if (item.snack) WidgetRenderer.snackIcon(item.section) else WidgetRenderer.potionIcon(item.section))
            background = ui.rounded(ui.skin.chip, 8f)
            setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, pictureSize())
        middle(activity.getString(R.string.upnext_boost_ends, boostName(item)), activity.getString(kindName(item.section)))
        countdown(item.atMillis)
    }

    // "Builder Potion (10x)", as the Potions screen names them.
    private fun boostName(item: Item.BoostEnds): String {
        val name = when (item.section) {
            Section.BUILDERS -> if (item.snack) R.string.potion_bite else R.string.potion_builder
            Section.LAB -> if (item.snack) R.string.potion_soup else R.string.potion_research
            Section.PETS -> if (item.snack) R.string.potion_soup else R.string.potion_pet
            Section.BUILDER_BASE -> return activity.getString(R.string.upnext_clock_tower)
        }
        return activity.getString(name, item.boost.multiplier)
    }

    private fun LinearLayout.event(countdown: EventSchedule.Countdown) {
        addView(LinearLayout(activity).apply {
            gravity = Gravity.CENTER
            background = ui.rounded(ui.skin.chip, 8f)
            addView(ui.icon(R.drawable.ic_clock_line, ui.skin.accent, 22))
        }, pictureSize())
        val line = activity.getString(if (countdown.running) R.string.upnext_event_ends else R.string.upnext_event_starts)
        middle(activity.getString(countdown.kind.label), line)
        countdown(countdown.target, if (countdown.running) ui.skin.green else ui.skin.text)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
