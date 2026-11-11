package com.cocwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer

// The Events widget: a live countdown for each game event the player chose. It needs no account.
// VillageWidget's refresh draws every kind of widget, so this one hands its updates over to it.
class EventsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        VillageWidget.requestRefresh(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Store.forgetWidgets(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        WidgetRenderer.cancel(context)
    }
}
