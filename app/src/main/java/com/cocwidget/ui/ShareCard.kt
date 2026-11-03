package com.cocwidget.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.view.View
import com.cocwidget.ExportProvider
import com.cocwidget.R
import com.cocwidget.store.Store

// The main screen's way to send the selected account's export to the developer, so bugs and game
// updates can be checked against real villages. Attached rather than pasted, because a mail app
// may wrap or change a long message. Hidden until there's an export to send.
class ShareCard(private val activity: Activity, private val ui: Ui, private val version: String) {
    fun build(): View? {
        val tag = Store.selected(activity) ?: return null
        val json = Store.exportJson(activity, tag) ?: return null
        if (Contact.EMAIL.isEmpty()) return null
        return ui.card().apply {
            addView(ui.heading(activity.getString(R.string.share_title), 16f, ui.skin.accent))
            addView(ui.text(activity.getString(R.string.share_detail), 14f, ui.skin.dim), ui.matchWidth(6))
            val label = activity.getString(R.string.share_button, Store.label(activity, tag))
            addView(ui.button(label, null, primary = false) { share(json) }, ui.matchWidth(14))
        }
    }

    private fun share(json: String) {
        ExportProvider.file(activity).writeText(Store.withoutTag(json))
        val uri = ExportProvider.uri(activity)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = ExportProvider.MIME_TYPE
            putExtra(Intent.EXTRA_EMAIL, arrayOf(Contact.EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_subject))
            putExtra(Intent.EXTRA_TEXT, Contact.device(version, Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL))
            putExtra(Intent.EXTRA_STREAM, uri)
            // The share sheet passes the right to read the file on only if it's set this way.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, activity.getString(R.string.share_title)))
    }
}
