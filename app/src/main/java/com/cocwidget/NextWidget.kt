package com.cocwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer

// The small widget: only the next upgrade to finish. VillageWidget's refresh draws both kinds,
// so this one hands its updates over to it.
class NextWidget : AppWidgetProvider() {
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
