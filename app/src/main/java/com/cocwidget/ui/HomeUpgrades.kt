package com.cocwidget.ui

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.game.Boost
import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.store.Icons
import com.cocwidget.widget.Countdown
import com.cocwidget.widget.WidgetRenderer

// The main screen's list: the helpers, then a card per section with its upgrades, soonest first as
// on the big widget. Every upgrade is here, however little of the widget fits on the home screen.
class HomeUpgrades(
    private val activity: Activity,
    private val ui: Ui,
    private val pictures: Pictures,
    private val village: Village,
    private val now: Long,
) {
    // A countdown on screen. The main screen redraws these each second, and rebuilds when one ends.
    class Clock(val view: TextView, val endMillis: Long, val text: (Long) -> CharSequence)

    val clocks = mutableListOf<Clock>()

    private val helperRing = activity.getColor(R.color.helper)
    private val goblinRing = activity.getColor(R.color.goblin)
    private val idleColour = activity.getColor(R.color.idle)

    private fun clock(view: TextView, endMillis: Long, text: (Long) -> CharSequence) {
        view.text = text(endMillis - now)
        clocks += Clock(view, endMillis, text)
    }

    // What finishes first across the whole village.
    fun nextFinish(next: Upgrade): View = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ui.rounded(ui.skin.chip, 10f)
        setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        addView(View(activity).apply { background = ui.rounded(ui.skin.green, 4f) }, LinearLayout.LayoutParams(ui.dp(8), ui.dp(8)))
        val step = if (next.supercharge > 0) {
            activity.getString(R.string.next_finish_supercharge, next.name, next.supercharge)
        } else {
            activity.getString(R.string.next_finish, next.name, next.toLevel)
        }
        addView(ui.text(step, 14f), ui.weighted(10))
        addView(ui.text("", 14f, ui.skin.green, bold = true).apply {
            fontFeatureSettings = "tnum"
            clock(this, village.endMillis(next)) { Countdown.clock(it) }
        }, startMargin(8))
    }

    // Null until the village has a helper. They share one work day, so they share one countdown.
    fun helpers(): View? {
        val helpers = village.helpers
        if (helpers.apprenticeLevel == 0 && helpers.assistantLevel == 0) return null
        return ui.card(12).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (helpers.apprenticeLevel > 0) addView(helperFace(Section.BUILDERS, helpers.apprenticeLevel))
            if (helpers.assistantLevel > 0) {
                addView(helperFace(Section.LAB, helpers.assistantLevel), startMargin(if (helpers.apprenticeLevel > 0) 10 else 0))
            }
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(ui.heading(activity.getString(R.string.home_helpers), 16f, ui.skin.title))
                val readyAt = village.helpersReadyMillis()
                if (readyAt > now) {
                    addView(ui.text("", 13f, ui.skin.dim).also { line ->
                        clock(line, readyAt) { left -> activity.getString(R.string.home_helpers_ready_in, Countdown.clock(left)) }
                    })
                } else {
                    addView(ui.text(activity.getString(R.string.home_helpers_ready), 13f, idleColour, bold = true))
                }
            }, ui.weighted(12))
        }
    }

    private fun startMargin(dpValue: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { marginStart = ui.dp(dpValue) }

    private fun helperFace(section: Section, level: Int): View = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(ui.face(WidgetRenderer.helperFace(section), 44, helperRing, activity.getString(R.string.helper)))
        addView(ui.text(activity.getString(R.string.home_level, level), 11f, ui.skin.dim, bold = true).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        })
    }

    fun section(section: Section): View = ui.card(14).apply {
        val upgrades = village.upgrades.filter { it.section == section }.sortedBy { village.endMillis(it) }
        addView(sectionTitle(section, upgrades))
        sectionBoosts(section)?.let { addView(it, ui.matchWidth(8)) }
        if (upgrades.isEmpty()) {
            addView(ui.text(activity.getString(R.string.next_nothing), 14f, ui.skin.dim), ui.matchWidth(8))
        }
        for (upgrade in upgrades) addView(row(upgrade), ui.matchWidth(12))
    }

    private fun sectionTitle(section: Section, upgrades: List<Upgrade>) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val title = when (section) {
            Section.BUILDERS -> activity.getString(R.string.home_builders, village.busyIn(section, now), village.slotsFor(section))
            Section.LAB -> activity.getString(R.string.home_lab)
            Section.PETS -> activity.getString(R.string.home_pets)
            Section.BUILDER_BASE -> activity.getString(R.string.home_builder_base)
        }
        addView(ui.heading(title, 17f, ui.skin.title), ui.weighted())
        if (upgrades.any { it.extra && village.endMillis(it) > now }) {
            addView(ui.text(activity.getString(R.string.home_goblin), 13f, goblinRing, bold = true))
        }
        val idle = village.idleIn(section, now)
        if (idle > 0) {
            val label = if (section == Section.BUILDERS) activity.resources.getQuantityString(R.plurals.home_idle_count, idle, idle) else activity.getString(R.string.home_idle)
            addView(ui.text(label, 13f, idleColour, bold = true), startMargin(10))
        }
    }

    private fun sectionBoosts(section: Section): View? {
        val chips = listOfNotNull(
            boostChip(WidgetRenderer.potionIcon(section), village.boostFor(section)),
            boostChip(WidgetRenderer.snackIcon(section), village.snackFor(section)),
        )
        if (chips.isEmpty()) return null
        return ui.flow(6).apply { chips.forEach { addView(it) } }
    }

    private fun boostChip(icon: Int, boost: Boost): View? {
        val end = village.boostEndMillis(boost)
        if (boost.seconds <= 0 || end <= now) return null
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ui.rounded(ui.skin.chip, 8f)
            setPadding(ui.dp(6), ui.dp(3), ui.dp(9), ui.dp(3))
            addView(ImageView(activity).apply {
                setImageResource(icon)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(ui.dp(20), ui.dp(20)).apply { marginEnd = ui.dp(5) })
            addView(ui.text("", 13f, ui.skin.text, bold = true).apply {
                fontFeatureSettings = "tnum"
                clock(this, end) { left -> activity.getString(R.string.home_boost, boost.multiplier, Countdown.clock(left)) }
            })
        }
    }

    private fun rowFace(face: Int, ring: Int, description: Int) = ui.face(face, 26, ring, activity.getString(description)).apply {
        (layoutParams as LinearLayout.LayoutParams).marginStart = ui.dp(6)
    }

    private fun row(upgrade: Upgrade) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val left = village.endMillis(upgrade) - now
        addView(ui.picture().also { pictures.add(Icons.path(upgrade), it) }, LinearLayout.LayoutParams(ui.dp(46), ui.dp(46)))
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(ui.text(WidgetRenderer.upgradeName(upgrade), 14f, ui.skin.text, bold = true), ui.weighted())
                if (upgrade.extra) {
                    addView(rowFace(R.drawable.ic_face_goblin, goblinRing, R.string.goblin))
                }
                if (left > 0 && village.isHelped(upgrade, now)) {
                    addView(rowFace(WidgetRenderer.helperFace(upgrade.section), helperRing, R.string.helper))
                }
                addView(ui.text("", 14f, ui.skin.text, bold = true).apply {
                    fontFeatureSettings = "tnum"
                    if (left > 0) {
                        clock(this, village.endMillis(upgrade)) { Countdown.clock(it) }
                    } else {
                        text = activity.getString(R.string.done)
                        setTextColor(ui.skin.green)
                    }
                }, startMargin(8))
            })
            addView(ui.bar(village.progress(upgrade, now)).also {
                (it.layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(6)
            })
        }, ui.weighted(10))
    }
}
