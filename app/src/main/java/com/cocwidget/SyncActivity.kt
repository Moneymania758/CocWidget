package com.cocwidget

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.cocwidget.game.VillageParser
import com.cocwidget.store.Store
import com.cocwidget.ui.WhatsNew

// Invisible screen opened by the widget's Sync button. Android only lets the app with
// input focus read the clipboard, so a widget can't read it directly.
class SyncActivity : Activity() {
    private var started = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || started) return
        started = true

        val clip = getSystemService(ClipboardManager::class.java).primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text == null || VillageParser.parse(this, text) == null) {
            Toast.makeText(this, R.string.sync_not_export, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        save(text)
    }

    private fun save(json: String) {
        if (WhatsNew.firstSyncOfFreshInstall(Store.whatsNewSeen(this), Store.hasExport(this))) {
            Store.setWhatsNewSeen(this, packageManager.getPackageInfo(packageName, 0).versionName.orEmpty())
        }
        val tag = Store.saveExport(this, json)
        VillageWidget.requestRefresh(this)
        // With more than one account, say which one the export belonged to.
        val done = if (Store.accounts(this).size > 1) getString(R.string.sync_done_account, Store.label(this, tag)) else getString(R.string.sync_done)
        Toast.makeText(this, done, Toast.LENGTH_SHORT).show()
        // Most people only ever sync and never open the app, so ask here, once.
        if (shouldAskForNotifications()) {
            Store.setAskedNotifications(this)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
        } else {
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }

    private fun shouldAskForNotifications() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Store.askedNotifications(this) &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    private companion object {
        const val NOTIFICATION_REQUEST = 1
    }
}
