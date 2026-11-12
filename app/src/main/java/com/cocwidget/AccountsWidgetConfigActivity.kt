package com.cocwidget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin
import com.cocwidget.widget.WidgetTheme

// Invisible screen that picks an Accounts widget's colours, from the launcher's widget settings.
// It shows every account, so there is nothing else to choose.
class AccountsWidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // A launcher that opens this when the widget is placed keeps the widget either way.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val themes = WidgetTheme.entries
        val dialog = AlertDialog.Builder(this, Skin.current(this).dialogTheme)
            .setTitle(R.string.widget_theme_title)
            .setSingleChoiceItems(themes.map { getString(it.label) }.toTypedArray(), themes.indexOf(Store.widgetTheme(this, widgetId))) { dialog, which ->
                // Closing the screen with the dialog still up leaks its window, so it's dismissed first.
                dialog.dismiss()
                Store.setWidgetTheme(this, widgetId, themes[which])
                VillageWidget.requestRefresh(this)
                finish()
            }
            .setOnCancelListener { finish() }
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}
