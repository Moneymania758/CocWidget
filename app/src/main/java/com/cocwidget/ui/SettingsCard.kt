package com.cocwidget.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import com.cocwidget.R
import com.cocwidget.VillageWidget
import com.cocwidget.store.Store

// The main screen's settings: theme, event switch, the selected account's name and removal, and
// folding rows of help text. [open] holds the folds opened, kept while the screen is rebuilt.
// [redraw] rebuilds the screen after a change that alters it.
class SettingsCard(
    private val activity: Activity,
    private val ui: Ui,
    private val open: MutableSet<Int>,
    private val redraw: () -> Unit,
) {
    fun build(): View = ui.card(0).apply {
        addView(themePicker())
        addView(ui.divider())
        addView(eventRow())
        val tag = Store.selected(activity)
        if (tag != null) {
            addView(ui.divider())
            addView(nameRow(tag))
            addView(ui.divider())
            addView(removeRow(tag))
        }
        addView(ui.divider())
        addView(fold(R.string.fold_setup, activity.getString(R.string.main_steps)))
        addView(ui.divider())
        val notes = WhatsNew.RELEASES.joinToString("\n\n") { "${it.version}\n${activity.getString(it.notes)}" }
        addView(fold(R.string.fold_whats_new, notes))
        addView(ui.divider())
        addView(fold(R.string.fold_about, activity.getString(R.string.credit) + "\n\n" + activity.getString(R.string.font_credit)))
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

    private fun eventRow() = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(16), ui.dp(14), ui.dp(12), ui.dp(14))
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.text(activity.getString(R.string.event_switch), 15f))
            addView(ui.text(activity.getString(R.string.event_explain), 13f, ui.skin.dim), ui.matchWidth(3))
        }, ui.weighted())
        addView(Switch(activity).apply {
            contentDescription = activity.getString(R.string.event_switch)
            isChecked = Store.eventOn(activity)
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(ui.skin.accent, ui.skin.dim))
            trackTintList = ColorStateList(states, intArrayOf(ui.skin.accent, ui.skin.divider))
            setOnCheckedChangeListener { _, on -> Store.setEventOn(activity, on) }
        }, ui.wrap().apply { marginStart = ui.dp(12) })
    }

    private fun nameRow(tag: String) = ui.row().apply {
        addView(ui.text(activity.getString(R.string.name_title), 15f))
        val name = EditText(activity).apply {
            setText(Store.displayName(activity, tag))
            hint = activity.getString(R.string.name_hint)
            setTextColor(ui.skin.text)
            setHintTextColor(ui.skin.dim)
            backgroundTintList = ColorStateList.valueOf(ui.skin.dim)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(name, ui.weighted())
            addView(ui.button(activity.getString(R.string.save_name), null, primary = false) {
                Store.setDisplayName(activity, tag, name.text.toString().trim())
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
                    VillageWidget.requestRefresh(activity)
                    redraw()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    // A row that opens to show [body] underneath, and closes again.
    private fun fold(title: Int, body: String) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val chevron = ui.icon(R.drawable.ic_chevron, ui.skin.dim, 18)
        val text = ui.text(body, 14f, ui.skin.dim).apply { setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(16)) }
        fun refresh() {
            val isOpen = title in open
            chevron.rotation = if (isOpen) 90f else 0f
            text.visibility = if (isOpen) View.VISIBLE else View.GONE
        }
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ui.dp(52)
            setPadding(ui.dp(16), 0, ui.dp(16), 0)
            addView(ui.text(activity.getString(title), 15f), ui.weighted())
            addView(chevron)
            ui.pressable(this, 0f) {
                if (!open.remove(title)) open += title
                refresh()
            }
        })
        addView(text)
        refresh()
    }
}
