package com.cocwidget.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.R
import com.cocwidget.game.Village
import com.cocwidget.store.Store
import com.cocwidget.widget.NextFinish
import com.cocwidget.widget.WidgetRenderer

// Every account in a sheet from the bottom, each saying what needs doing in it, so a player with
// many villages sees which one wants them without opening each. [onPick] runs after the switch.
class AccountSheet(private val activity: Activity, private val ui: Ui, private val onPick: () -> Unit) {
    private val dialog = Dialog(activity)
    private val rows = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    private class Account(val tag: String, val label: String, val village: Village?)

    fun show() {
        val now = System.currentTimeMillis()
        val accounts = Store.accounts(activity).map { Account(it, Store.label(activity, it), Store.village(activity, it)) }
        val selected = Store.selected(activity)
        val sheet = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ui.skin.card)
                val r = ui.dp(24).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(16))
            addView(LinearLayout(activity).apply {
                background = ui.rounded(ui.skin.divider, 2f)
            }, LinearLayout.LayoutParams(ui.dp(40), ui.dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = ui.dp(12)
            })
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                addView(ui.heading(activity.getString(R.string.accounts_title), 20f, ui.skin.title), ui.weighted())
                addView(ui.text(activity.resources.getQuantityString(R.plurals.accounts_count, accounts.size, accounts.size), 13f, ui.skin.dim))
            })
            if (accounts.size > SEARCH_FROM) addView(search(accounts, selected, now), ui.matchWidth(12))
            addView(ScrollView(activity).apply { addView(rows) }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                // Long lists scroll inside the sheet instead of pushing it off the top.
                minOf(ui.dp(ROW_DP) * (accounts.size + 1), (activity.resources.displayMetrics.heightPixels * 0.6f).toInt()),
            ).apply { topMargin = ui.dp(8) })
        }
        fill(accounts, selected, now)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(sheet)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun search(accounts: List<Account>, selected: String?, now: Long) = EditText(activity).apply {
        hint = activity.getString(R.string.accounts_search)
        inputType = InputType.TYPE_CLASS_TEXT
        setSingleLine()
        textSize = 15f
        setTextColor(ui.skin.text)
        setHintTextColor(ui.skin.dim)
        backgroundTintList = ColorStateList.valueOf(ui.skin.dim)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val wanted = s?.toString()?.trim().orEmpty()
                fill(accounts.filter { wanted.isEmpty() || it.label.contains(wanted, ignoreCase = true) }, selected, now)
            }
        })
    }

    private fun fill(accounts: List<Account>, selected: String?, now: Long) {
        rows.removeAllViews()
        for (account in accounts) rows.addView(row(account, account.tag == selected, now))
        rows.addView(ui.text(activity.getString(R.string.account_add), 15f, ui.skin.accent, bold = true).apply {
            minHeight = ui.dp(ROW_DP)
            gravity = Gravity.CENTER_VERTICAL
            ui.pressable(this, 8f) {
                AlertDialog.Builder(activity, ui.skin.dialogTheme)
                    .setTitle(R.string.account_add_title)
                    .setMessage(R.string.account_add_steps)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        })
    }

    private fun row(account: Account, chosen: Boolean, now: Long) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = ui.dp(ROW_DP)
        isSelected = chosen
        val townHall = account.village?.townHall ?: 0
        addView(ui.townHallBadge(townHall, if (chosen) ui.skin.accent else ui.skin.divider, 40))
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.text(account.label, 15f, bold = true))
            val (status, idle) = status(account.village, now)
            addView(ui.text(status, 12f, if (idle) ui.skin.idle else ui.skin.green, bold = true), ui.matchWidth(2))
        }, ui.weighted(12))
        if (chosen) addView(ui.icon(R.drawable.ic_check, ui.skin.accent, 20))
        ui.pressable(this, 8f) {
            Store.setSelected(activity, account.tag)
            dialog.dismiss()
            onPick()
        }
    }

    // What the row says, and whether it wants the player (idle or out of date) or not.
    private fun status(village: Village?, now: Long): Pair<String, Boolean> {
        if (village == null) return activity.getString(R.string.account_unreadable) to true
        if (WidgetRenderer.exportLine(activity, village, now).stale) return activity.getString(R.string.account_out_of_date) to true
        val summary = NextFinish.of(village, now)
        return idleLine(activity, summary) to summary.anyIdle
    }

    companion object {
        private const val ROW_DP = 58
        // More accounts than this get a search box.
        private const val SEARCH_FROM = 6

        // "2 builders idle · Lab idle", or that every builder is busy.
        fun idleLine(activity: Activity, summary: NextFinish.Summary): String = if (summary.anyIdle) {
            buildList {
                if (summary.idleBuilders > 0) add(activity.resources.getQuantityString(R.plurals.next_builders_idle, summary.idleBuilders, summary.idleBuilders))
                if (summary.labIdle) add(activity.getString(R.string.next_lab_idle))
                if (summary.petsIdle) add(activity.getString(R.string.next_pets_idle))
            }.joinToString(" · ")
        } else {
            activity.resources.getQuantityString(R.plurals.next_all_busy, summary.builders, summary.builders)
        }
    }
}
