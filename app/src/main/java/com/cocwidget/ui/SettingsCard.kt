package com.cocwidget.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import com.cocwidget.R
import com.cocwidget.VillageWidget
import com.cocwidget.events.EventKind
import com.cocwidget.events.EventReminders
import com.cocwidget.store.Store

// The Settings screen's cards. [nameDrafts] holds names typed but not saved, by account, so they
// outlive a rebuild. [redraw] rebuilds the screen after a change that alters it.
class SettingsCard(
    private val activity: Activity,
    private val ui: Ui,
    private val nameDrafts: MutableMap<String, String>,
    private val redraw: () -> Unit,
) {
    // Settings for the whole app, whichever account is selected.
    fun general(): View = ui.card(0).apply {
        addView(themePicker())
        addView(ui.divider())
        addView(eventRow())
    }

    // A switch for each game event, with when its next reminder is due.
    fun eventReminders(): View = ui.card(0).apply {
        addView(ui.heading(activity.getString(R.string.event_reminders_title), 16f, ui.skin.accent).apply {
            setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(4))
        })
        addView(ui.text(activity.getString(R.string.event_reminders_explain), 13f, ui.skin.dim).apply {
            setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(10))
        })
        if (!EventReminders.notificationsAllowed(activity)) addView(notificationsOffRow())
        val now = System.currentTimeMillis()
        for (kind in EventKind.entries) {
            addView(ui.divider())
            addView(switchRow(activity.getString(kind.label), nextReminder(kind, now), Store.eventReminderOn(activity, kind)) { on ->
                Store.setEventReminderOn(activity, kind, on, System.currentTimeMillis())
                VillageWidget.requestRefresh(activity)
            })
        }
    }

    // Without this, the switches look on and nothing ever arrives. The screen is rebuilt on return,
    // so this goes once they're allowed.
    private fun notificationsOffRow() = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(14))
        addView(ui.text(activity.getString(R.string.event_notifications_off), 13f, ui.skin.accent))
        addView(ui.button(activity.getString(R.string.event_notifications_open), null, primary = false) {
            activity.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
            )
        }, ui.matchWidth(8))
    }

    private fun nextReminder(kind: EventKind, now: Long): String {
        val next = EventReminders.nextFor(activity, kind, now) ?: return activity.getString(R.string.event_no_date)
        val flags = DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
        return activity.getString(R.string.event_next, DateUtils.formatDateTime(activity, next, flags))
    }

    // The selected account's name and removal. Hidden until there's an account.
    fun account(): View? {
        val tag = Store.selected(activity) ?: return null
        return ui.card(0).apply {
            addView(ui.heading(Store.label(activity, tag), 16f, ui.skin.accent).apply {
                setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(4))
            })
            addView(nameRow(tag))
            addView(ui.divider())
            addView(removeRow(tag))
        }
    }

    private fun themePicker() = ui.row().apply {
        addView(ui.text(activity.getString(R.string.theme_label), 15f))
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            for ((index, skin) in Skin.ALL.withIndex()) {
                val chosen = skin.key == ui.skin.key
                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    minimumHeight = ui.dp(64)
                    background = ui.rounded(ui.skin.chip, 10f, if (chosen) ui.skin.accent else ui.skin.divider, if (chosen) 2f else 1f)
                    setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
                    isSelected = chosen
                    addView(LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        background = ui.rounded(0, 5f)
                        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                        clipToOutline = true
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        for (colour in skin.swatch) {
                            addView(View(activity).apply { setBackgroundColor(colour) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                        }
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(18)))
                    addView(ui.text(activity.getString(skin.label), 13f).apply { gravity = Gravity.CENTER }, ui.matchWidth(6))
                    ui.pressable(this, 10f) {
                        Store.setSkin(activity, skin.key)
                        redraw()
                    }
                }, ui.weighted(if (index == 0) 0 else 8))
            }
        }, ui.matchWidth(10))
    }

    private fun eventRow() = switchRow(
        activity.getString(R.string.event_switch),
        activity.getString(R.string.event_explain),
        Store.eventOn(activity),
    ) { on -> Store.setEventOn(activity, on) }

    private fun switchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(16), ui.dp(14), ui.dp(12), ui.dp(14))
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.text(title, 15f))
            addView(ui.text(detail, 13f, ui.skin.dim), ui.matchWidth(3))
        }, ui.weighted())
        addView(Switch(activity).apply {
            contentDescription = title
            isChecked = checked
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(ui.skin.accent, ui.skin.dim))
            trackTintList = ColorStateList(states, intArrayOf(ui.skin.accent, ui.skin.divider))
            setOnCheckedChangeListener { _, on -> onChange(on) }
        }, ui.wrap().apply { marginStart = ui.dp(12) })
    }

    private fun nameRow(tag: String) = ui.row().apply {
        addView(ui.text(activity.getString(R.string.name_title), 15f))
        val name = EditText(activity).apply {
            setText(nameDrafts[tag] ?: Store.displayName(activity, tag))
            hint = activity.getString(R.string.name_hint)
            setTextColor(ui.skin.text)
            setHintTextColor(ui.skin.dim)
            backgroundTintList = ColorStateList.valueOf(ui.skin.dim)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(text: Editable?) {
                    nameDrafts[tag] = text.toString()
                }
            })
        }
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(name, ui.weighted())
            addView(ui.button(activity.getString(R.string.save_name), null, primary = false) {
                Store.setDisplayName(activity, tag, name.text.toString().trim())
                nameDrafts.remove(tag)
                VillageWidget.requestRefresh(activity)
                Toast.makeText(activity, R.string.name_saved, Toast.LENGTH_SHORT).show()
                redraw()
            }, ui.wrap().apply { marginStart = ui.dp(10) })
        }, ui.matchWidth(4))
    }

    // Asks first: the export is gone until it's synced again.
    private fun removeRow(tag: String) = ui.row().apply {
        minimumHeight = ui.dp(52)
        gravity = Gravity.CENTER_VERTICAL
        addView(ui.text(activity.getString(R.string.account_remove), 15f))
        ui.pressable(this, 0f) {
            AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(activity.getString(R.string.account_remove_title, Store.label(activity, tag)))
                .setMessage(R.string.account_remove_message)
                .setPositiveButton(R.string.account_remove_confirm) { _, _ ->
                    Store.removeAccount(activity, tag)
                    nameDrafts.remove(tag)
                    VillageWidget.requestRefresh(activity)
                    redraw()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
