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
    // Folds opened and names typed but not saved, by account. The screen is rebuilt on every
    // visit, and after a rotation these come back from the saved state.
    private val open = mutableSetOf<Int>()
    private val nameDrafts = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(EXTRA_SHOW_SETUP, false)) open += R.string.fold_setup
        } else {
            savedInstanceState.getIntegerArrayList(STATE_OPEN)?.let { open += it }
            savedInstanceState.getBundle(STATE_DRAFTS)?.let { drafts ->
                for (tag in drafts.keySet()) drafts.getString(tag)?.let { nameDrafts[tag] = it }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntegerArrayList(STATE_OPEN, ArrayList(open))
        outState.putBundle(STATE_DRAFTS, Bundle().apply { for ((tag, name) in nameDrafts) putString(tag, name) })
    }

    override fun build() {
        content.addView(backHeader(R.string.settings_title))
        val settings = SettingsCard(this, ui, nameDrafts, ::show)
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
        private const val STATE_OPEN = "open"
        private const val STATE_DRAFTS = "name_drafts"
    }
}
