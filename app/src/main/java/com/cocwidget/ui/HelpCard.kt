package com.cocwidget.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.cocwidget.R

// The setup steps and the ways to get help. Reports and emails start filled in with [version] and
// the phone. [open] holds the folds opened, kept while the screen is rebuilt.
class HelpCard(
    private val activity: Activity,
    private val ui: Ui,
    private val version: String,
    private val open: MutableSet<Int>,
) {
    fun build(): View = ui.card(0).apply {
        addView(fold(R.string.fold_setup, activity.getString(R.string.main_steps)))
        addView(ui.divider())
        // What it can't do and why things happen. The questions are in bold.
        addView(fold(R.string.fold_faq, activity.getText(R.string.faq)))
        addView(ui.divider())
        val device = Contact.device(version, Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL)
        addView(linkRow(R.string.help_report, R.string.help_report_detail) {
            open(Intent(Intent.ACTION_VIEW, Uri.parse(Contact.issueUrl(device))))
        })
        if (Contact.EMAIL.isNotEmpty()) {
            addView(ui.divider())
            addView(linkRow(R.string.help_email, R.string.help_email_detail) {
                open(Intent(Intent.ACTION_SENDTO, Uri.parse(Contact.emailUri(device))))
            })
        }
        if (Contact.DISCORD.isNotEmpty()) {
            addView(ui.divider())
            addView(linkRow(R.string.help_discord, R.string.help_discord_detail) {
                open(Intent(Intent.ACTION_VIEW, Uri.parse(Contact.DISCORD)))
            })
        }
    }

    // A row that opens to show [body] underneath, and closes again.
    private fun fold(title: Int, body: CharSequence) = LinearLayout(activity).apply {
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

    private fun linkRow(title: Int, detail: Int, onClick: () -> Unit) = ui.row().apply {
        minimumHeight = ui.dp(52)
        addView(ui.text(activity.getString(title), 15f))
        addView(ui.text(activity.getString(detail), 13f, ui.skin.dim), ui.matchWidth(3))
        ui.pressable(this, 0f, onClick)
    }

    // A phone with no email app (or no browser) would otherwise crash here.
    private fun open(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.help_no_app, Toast.LENGTH_SHORT).show()
        }
    }
}
