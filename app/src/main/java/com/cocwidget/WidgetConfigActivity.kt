package com.cocwidget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin
import com.cocwidget.widget.WidgetStyle
import com.cocwidget.widget.WidgetTheme

// Invisible screen the launcher opens when a widget is placed, and from the widget's settings
// later. It asks which account the widget shows (with two or more), then for the big widget which
// layout, then which colours. With three or more accounts a tap on the widget's account chip opens
// it too, and then it asks only for the account.
class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val skin by lazy { Skin.current(this) }
    // The answers so far. Nothing is saved until the last one, so backing out part way changes nothing.
    private var account: String? = null
    private var style: WidgetStyle? = null
    private var theme: WidgetTheme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Backing out of a new widget's setup leaves the widget off the home screen.
        setResult(RESULT_CANCELED, result())
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        askAccount()
    }

    // The account chip's tap carries a pick:// address, and wants the account and nothing else.
    private val accountOnly get() = intent.data?.scheme == "cocwidget" && intent.data?.host == "pick"

    private val isBig: Boolean
        get() = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className != NextWidget::class.java.name

    private fun askAccount() {
        val accounts = Store.accounts(this)
        if (accounts.size <= 1) {
            account = accounts.firstOrNull()
            afterAccount()
            return
        }
        val current = Store.widgetAccount(this, widgetId)
        AlertDialog.Builder(this, skin.dialogTheme)
            .setTitle(R.string.widget_pick_title)
            .setSingleChoiceItems(accounts.map { Store.label(this, it) }.toTypedArray(), accounts.indexOf(current)) { dialog, which ->
                // Closing the screen with the dialog still up leaks its window.
                dialog.dismiss()
                account = accounts[which]
                afterAccount()
            }
            .setOnCancelListener { finish() }
            .show()
            .setCanceledOnTouchOutside(false)
    }

    private fun afterAccount() {
        when {
            accountOnly -> done()
            isBig -> askStyle()
            else -> askTheme()
        }
    }

    private fun askStyle() {
        val styles = WidgetStyle.entries
        val names = styles.map { getString(it.label) + "\n" + getString(it.hint) }.toTypedArray()
        AlertDialog.Builder(this, skin.dialogTheme)
            .setTitle(R.string.widget_style_title)
            .setSingleChoiceItems(names, styles.indexOf(Store.widgetStyle(this, widgetId))) { dialog, which ->
                dialog.dismiss()
                style = styles[which]
                askTheme()
            }
            .setOnCancelListener { finish() }
            .show()
            .setCanceledOnTouchOutside(false)
    }

    private fun askTheme() {
        val themes = WidgetTheme.entries
        AlertDialog.Builder(this, skin.dialogTheme)
            .setTitle(R.string.widget_theme_title)
            .setSingleChoiceItems(themes.map { getString(it.label) }.toTypedArray(), themes.indexOf(Store.widgetTheme(this, widgetId))) { dialog, which ->
                dialog.dismiss()
                theme = themes[which]
                done()
            }
            .setOnCancelListener { finish() }
            .show()
            .setCanceledOnTouchOutside(false)
    }

    private fun done() {
        account?.let { Store.setWidgetAccount(this, widgetId, it) }
        style?.let { Store.setWidgetStyle(this, widgetId, it) }
        theme?.let { Store.setWidgetTheme(this, widgetId, it) }
        VillageWidget.requestRefresh(this)
        setResult(RESULT_OK, result())
        finish()
    }

    private fun result() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}
