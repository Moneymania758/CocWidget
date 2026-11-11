package com.cocwidget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.cocwidget.events.EventKind
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin

// Invisible screen that picks which events one Events widget shows. It opens from the widget's
// Edit chip, and from the widget's settings on a launcher that offers them. Reset shows every
// event again.
class EventsWidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // A launcher that opens this when the widget is placed keeps the widget either way, as
        // every event shows until the player picks.
        setResult(RESULT_OK, result())
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val kinds = EventKind.entries
        val hidden = Store.hiddenEvents(this, widgetId)
        val shown = BooleanArray(kinds.size) { kinds[it] !in hidden }
        AlertDialog.Builder(this, Skin.current(this).dialogTheme)
            .setTitle(R.string.events_widget_pick_title)
            .setMultiChoiceItems(kinds.map { getString(it.label) }.toTypedArray(), shown) { _, which, on -> shown[which] = on }
            // Closing the screen with the dialog still up leaks its window, so each button dismisses first.
            .setPositiveButton(R.string.events_widget_done) { dialog, _ ->
                dialog.dismiss()
                save(kinds.filterIndexed { i, _ -> !shown[i] }.toSet())
            }
            .setNeutralButton(R.string.events_widget_reset) { dialog, _ ->
                dialog.dismiss()
                save(emptySet())
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun save(hidden: Set<EventKind>) {
        Store.setHiddenEvents(this, widgetId, hidden)
        VillageWidget.requestRefresh(this)
        finish()
    }

    private fun result() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}
