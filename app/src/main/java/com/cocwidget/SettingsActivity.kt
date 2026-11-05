package com.cocwidget

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import com.cocwidget.ui.HelpCard
import com.cocwidget.ui.Screen
import com.cocwidget.ui.SettingsCard
import com.cocwidget.ui.ShareCard

// Everything about the app rather than the village: settings, help and the way to About.
class SettingsActivity : Screen() {
    // Folded sections the person opened, kept while the screen is rebuilt.
    private val open = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_SHOW_SETUP, false)) open += R.string.fold_setup
    }

    override fun build() {
        content.addView(backHeader(R.string.settings_title))
        val settings = SettingsCard(this, ui, ::show)
        content.addView(settings.general(), ui.matchWidth(12))
        settings.account()?.let { content.addView(it, ui.matchWidth(16)) }
        content.addView(HelpCard(this, ui, installedVersion(), open).build(), ui.matchWidth(16))
        ShareCard(this, ui, installedVersion()).build()?.let { content.addView(it, ui.matchWidth(16)) }
        content.addView(ui.card(0).apply { addView(aboutRow()) }, ui.matchWidth(16))
    }

    private fun aboutRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = ui.dp(52)
        setPadding(ui.dp(16), 0, ui.dp(16), 0)
        addView(ui.text(getString(R.string.fold_about), 15f), ui.weighted())
        addView(ui.icon(R.drawable.ic_chevron, ui.skin.dim, 18))
        ui.pressable(this, 0f) { startActivity(Intent(this@SettingsActivity, AboutActivity::class.java)) }
    }

    companion object {
        const val EXTRA_SHOW_SETUP = "show_setup"
    }
}
