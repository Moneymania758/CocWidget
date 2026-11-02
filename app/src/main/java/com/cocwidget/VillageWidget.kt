package com.cocwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

class VillageWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> refreshAsync(context)
            ACTION_TOGGLE_SECTION -> {
                intent.getStringExtra(EXTRA_SECTION)?.let { Store.toggleSection(context, it) }
                refreshAsync(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context)
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
        const val EXTRA_SECTION = "section"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, VillageWidget::class.java).setAction(ACTION_REFRESH))
        }
    }
}
