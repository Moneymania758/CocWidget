package com.cocwidget.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import com.cocwidget.ExportProvider
import com.cocwidget.R
import com.cocwidget.store.Store

// The way to email the selected account's export to the developer, so bugs and game
// updates can be checked against real villages. The email opens ready to send. Hidden until
// there's an export to send.
class ShareCard(private val activity: Activity, private val ui: Ui, private val version: String) {
    fun build(): View? {
        val tag = Store.selected(activity) ?: return null
        val json = Store.exportJson(activity, tag) ?: return null
        if (Contact.EMAIL.isEmpty()) return null
        return ui.card().apply {
            addView(ui.heading(activity.getString(R.string.share_title), 16f, ui.skin.accent))
            addView(ui.text(activity.getString(R.string.share_detail), 14f, ui.skin.dim), ui.matchWidth(6))
            addView(ui.button(activity.getString(R.string.share_button), null, primary = false) { email(tag, json) }, ui.matchWidth(14))
        }
    }

    // The export goes in twice. The attachment stays exact if a mail app wraps the text, and the
    // text still arrives if a mail app drops the attachment.
    private fun email(tag: String, json: String) {
        val export = Store.withoutTag(json)
        ExportProvider.file(activity).writeText(export)
        val uri = ExportProvider.uri(activity)
        val device = Contact.device(version, Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL)
        val subject = Contact.exportSubject(Store.village(activity, tag)?.townHall)
        val body = Contact.exportBody(device, Store.eventOn(activity), export)
        val attached = Intent(Intent.ACTION_SEND).apply {
            type = ExportProvider.MIME_TYPE
            putExtra(Intent.EXTRA_EMAIL, arrayOf(Contact.EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val targets = emailApps().map { Intent(attached).setPackage(it) }.filter { it.resolveActivity(activity.packageManager) != null }
        val open = when (targets.size) {
            // No email app takes an attachment like this, so send the pasted copy only.
            0 -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Contact.EMAIL}"))
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, body)
            1 -> targets.single()
            else -> Intent.createChooser(targets.first(), activity.getString(R.string.share_title))
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.drop(1).toTypedArray())
        }
        try {
            activity.startActivity(open)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.help_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    // Each app is asked by name, so no other kind of app is offered. A mailto: selector on the
    // email itself found no app on a Galaxy S24 Ultra.
    private fun emailApps() = activity.packageManager
        .queryIntentActivities(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")), 0)
        .map { it.activityInfo.packageName }
        .distinct()
}
