package com.cocwidget

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import kotlin.math.abs

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
        val village = text?.let { VillageParser.parse(this, it) }
        if (text == null || village == null) {
            Toast.makeText(this, R.string.sync_not_export, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (village.builderBoost.seconds > 0 && !sameBoostAsLastSync(village)) askBuilderBoost(text) else save(text)
    }

    // A boost confirmed at the last sync ends at the same moment, so there's no need to ask again.
    private fun sameBoostAsLastSync(village: Village): Boolean {
        val previous = Store.village(this) ?: return false
        if (previous.builderBoost.seconds <= 0) return false
        val previousEnd = previous.boostEndMillis(previous.builderBoost)
        val end = village.boostEndMillis(village.builderBoost)
        return abs(previousEnd - end) < BOOST_MATCH_MS
    }

    // The export gives the builder boost's time left but not whether it's a Potion or a Bite.
    private fun askBuilderBoost(json: String) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.boost_title)
            .setMessage(R.string.boost_message)
            .setPositiveButton(R.string.boost_potion) { _, _ ->
                Store.setBuilderMultiplier(this, BUILDER_POTION_MULTIPLIER)
                save(json)
            }
            .setNegativeButton(R.string.boost_bite) { _, _ ->
                Store.setBuilderMultiplier(this, BUILDER_BITE_MULTIPLIER)
                save(json)
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun save(json: String) {
        Store.saveExport(this, json)
        VillageWidget.requestRefresh(this)
        Toast.makeText(this, R.string.sync_done, Toast.LENGTH_SHORT).show()
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
        const val BOOST_MATCH_MS = 2 * 60_000L
        const val NOTIFICATION_REQUEST = 1
    }
}
