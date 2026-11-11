package com.cocwidget.ui

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.cocwidget.R
import com.cocwidget.SettingsActivity
import com.cocwidget.events.EventDates
import com.cocwidget.events.EventSchedule
import com.cocwidget.widget.Countdown

// The main screen's live event countdowns, soonest first. Only a few show until asked, so the
// upgrades stay near the top. Countdowns show whether or not that event's reminder is on.
class HomeEvents(
    private val activity: Activity,
    private val ui: Ui,
    private val showAll: Boolean,
    private val onToggle: () -> Unit,
) {
    val clocks = mutableListOf<HomeUpgrades.Clock>()

    // Null with no event dated, which only a broken dates file with no rules could cause.
    fun card(now: Long): View? {
        val all = EventSchedule.countdowns(now, EventDates.current(activity))
        if (all.isEmpty()) return null
        return ui.card().apply {
            addView(ui.heading(activity.getString(R.string.event_home_title), 16f, ui.skin.accent))
            for (countdown in if (showAll) all else all.take(FEW)) addView(row(countdown, now), ui.matchWidth(10))
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                if (all.size > FEW) {
                    val label = if (showAll) activity.getString(R.string.event_show_fewer) else activity.getString(R.string.event_show_all, all.size)
                    addView(link(label, onToggle), ui.wrap())
                }
                addView(View(activity), ui.weighted())
                addView(link(activity.getString(R.string.event_home_settings)) {
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                }, ui.wrap())
            }, ui.matchWidth(4))
        }
    }

    private fun row(countdown: EventSchedule.Countdown, now: Long) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(ui.text(activity.getString(countdown.kind.label), 15f))
        val label = countdown.label
        val text = { left: Long -> activity.getString(label, Countdown.clock(left)) }
        val clock = ui.text(text(countdown.target - now), 13f, if (countdown.running) ui.skin.green else ui.skin.dim)
        clocks += HomeUpgrades.Clock(clock, countdown.target, text)
        addView(clock, ui.matchWidth(2))
    }

    private fun link(label: String, onClick: () -> Unit) = ui.text(label, 14f, ui.skin.accent, bold = true).apply {
        minHeight = ui.dp(44)
        gravity = Gravity.CENTER_VERTICAL
        ui.pressable(this, 8f, onClick)
    }

    private companion object {
        const val FEW = 3
    }
}
