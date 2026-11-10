package com.cocwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import com.cocwidget.widget.WidgetView
import kotlin.concurrent.thread

class VillageWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> refreshAsync(context)
            ACTION_TOGGLE_SECTION -> {
                val tag = intent.getStringExtra(EXTRA_ACCOUNT)
                val section = intent.getStringExtra(EXTRA_SECTION)
                if (tag != null && section != null) Store.toggleSection(context, tag, section)
                refreshAsync(context)
            }
            ACTION_SET_VIEW -> {
                val tag = intent.getStringExtra(EXTRA_ACCOUNT)
                val view = WidgetView.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_VIEW) }
                if (tag != null && view != null) Store.setWidgetView(context, tag, view)
                refreshAsync(context)
            }
            ACTION_CLEAR_FINISHED -> {
                val tag = intent.getStringExtra(EXTRA_ACCOUNT)
                val view = WidgetView.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_VIEW) }
                if (tag != null && view != null) Store.clearFinished(context, tag, System.currentTimeMillis(), view.shows)
                refreshAsync(context)
            }
            ACTION_SWITCH_ACCOUNT -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) Store.switchWidgetAccount(context, widgetId)
                refreshAsync(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Store.forgetWidgets(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        WidgetRenderer.cancel(context)
    }

    // Icon downloads need the network, so rendering runs off the main thread.
    private fun refreshAsync(context: Context) {
        val appContext = context.applicationContext
        val pending = goAsync()
        thread {
            try {
                WidgetRenderer.update(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.cocwidget.action.REFRESH"
        const val ACTION_TOGGLE_SECTION = "com.cocwidget.action.TOGGLE_SECTION"
        const val ACTION_SET_VIEW = "com.cocwidget.action.SET_VIEW"
        const val ACTION_SWITCH_ACCOUNT = "com.cocwidget.action.SWITCH_ACCOUNT"
        const val ACTION_CLEAR_FINISHED = "com.cocwidget.action.CLEAR_FINISHED"
        const val EXTRA_SECTION = "section"
        const val EXTRA_VIEW = "view"
        const val EXTRA_ACCOUNT = "account"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, VillageWidget::class.java).setAction(ACTION_REFRESH))
        }
    }
}
