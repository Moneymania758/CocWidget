package com.cocwidget

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
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

        val questions = ArrayDeque<BoostQuestion>()
        if (isNewBoost(village) { it.builderBoost }) questions += BUILDER_QUESTION
        if (isNewBoost(village) { it.labBoost }) questions += LAB_QUESTION
        askNext(questions, text)
    }

    // The export gives a boost's time left but not which item caused it, and the items run at
    // different speeds. A boost confirmed at the last sync ends at the same moment, so there's
    // no need to ask about it again.
    private fun isNewBoost(village: Village, boost: (Village) -> Boost): Boolean {
        if (boost(village).seconds <= 0) return false
        val previous = Store.village(this) ?: return true
        if (boost(previous).seconds <= 0) return true
        return abs(previous.boostEndMillis(boost(previous)) - village.boostEndMillis(boost(village))) >= BOOST_MATCH_MS
    }

    private fun askNext(questions: ArrayDeque<BoostQuestion>, json: String) {
        val question = questions.removeFirstOrNull() ?: return save(json)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(question.title)
            .setMessage(R.string.boost_message)
            .setPositiveButton(question.strong.first) { _, _ ->
                question.save(this, question.strong.second)
                askNext(questions, json)
            }
            .setNegativeButton(question.weak.first) { _, _ ->
                question.save(this, question.weak.second)
                askNext(questions, json)
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

    // The two items that can cause a boost, strongest first, each with its string and multiplier.
    private class BoostQuestion(
        val title: Int,
        val strong: Pair<Int, Int>,
        val weak: Pair<Int, Int>,
        val save: (Context, Int) -> Unit,
    )

    private companion object {
        const val BOOST_MATCH_MS = 2 * 60_000L
        const val NOTIFICATION_REQUEST = 1

        val BUILDER_QUESTION = BoostQuestion(
            R.string.boost_title,
            R.string.boost_potion to BUILDER_POTION_MULTIPLIER,
            R.string.boost_bite to BUILDER_BITE_MULTIPLIER,
            Store::setBuilderMultiplier,
        )
        val LAB_QUESTION = BoostQuestion(
            R.string.lab_boost_title,
            R.string.lab_boost_potion to RESEARCH_POTION_MULTIPLIER,
            R.string.lab_boost_soup to STUDY_SOUP_MULTIPLIER,
            Store::setLabMultiplier,
        )
    }
}
