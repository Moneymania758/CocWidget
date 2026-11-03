package com.cocwidget.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import com.cocwidget.R

// The main screen's ways to get help. Reports and emails start filled in with [version] and the
// phone.
class HelpCard(private val activity: Activity, private val ui: Ui, private val version: String) {
    fun build(): View = ui.card(0).apply {
        addView(ui.heading(activity.getString(R.string.help_title), 16f, ui.skin.accent).apply {
            setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(4))
        })
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
