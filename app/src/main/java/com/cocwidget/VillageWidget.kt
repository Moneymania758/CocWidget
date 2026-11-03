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
                val tag = intent.getStringExtra(EXTRA_ACCOUNT)
                val section = intent.getStringExtra(EXTRA_SECTION)
                if (tag != null && section != null) Store.toggleSection(context, tag, section)
                refreshAsync(context)
            }
            ACTION_SET_VIEW -> {
                val tag = intent.getStringExtra(EXTRA_ACCOUNT)
                val view = WidgetView.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_VIEW) }
                if (tag != null && view != null) Store.setCollapsedSections(context, tag, view.folded.map { it.name }.toSet())
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
        const val EXTRA_SECTION = "section"
        const val EXTRA_VIEW = "view"
        const val EXTRA_ACCOUNT = "account"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, VillageWidget::class.java).setAction(ACTION_REFRESH))
        }
    }
}
