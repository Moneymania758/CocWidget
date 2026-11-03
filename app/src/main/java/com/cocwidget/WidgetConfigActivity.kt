package com.cocwidget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.cocwidget.store.Store

// Invisible screen the launcher opens when a widget is placed, and from the widget's settings
// later, to pick which account the widget shows. With one account or none there's nothing to
// pick, so it closes straight away.
class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Backing out of a new widget's picker leaves the widget off the home screen.
        setResult(RESULT_CANCELED, result())
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val accounts = Store.accounts(this)
        if (accounts.size <= 1) {
            pick(accounts.firstOrNull())
            return
        }
        val current = Store.widgetAccount(this, widgetId)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.widget_pick_title)
            .setSingleChoiceItems(accounts.map { Store.label(this, it) }.toTypedArray(), accounts.indexOf(current)) { _, which ->
                pick(accounts[which])
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun pick(tag: String?) {
        tag?.let { Store.setWidgetAccount(this, widgetId, it) }
        VillageWidget.requestRefresh(this)
        setResult(RESULT_OK, result())
        finish()
    }

    private fun result() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}
