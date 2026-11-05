package com.cocwidget.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.R

// A screen of cards made in code. It's rebuilt on every visit, so a sync or a theme change made
// meanwhile shows.
abstract class Screen : Activity() {
    protected lateinit var content: LinearLayout
    protected lateinit var ui: Ui

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)
        content = findViewById(R.id.content)
    }

    override fun onResume() {
        super.onResume()
        show()
    }

    // Empties the screen and builds it again in the chosen skin.
    protected fun show() {
        ui = Ui(this, Skin.current(this))
        ui.applyWindow(findViewById<ScrollView>(R.id.scroll))
        content.removeAllViews()
        build()
    }

    protected abstract fun build()

    // A back arrow and the screen's title. Callers can add more to the end of the row.
    protected fun backHeader(title: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(this@Screen).apply {
            gravity = Gravity.CENTER
            contentDescription = getString(R.string.back)
            addView(ui.icon(R.drawable.ic_back, ui.skin.text, 22))
            ui.pressable(this, 22f) { finish() }
        }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
        addView(ui.heading(getString(title), 22f, ui.skin.title), ui.weighted(4))
    }

    protected fun installedVersion() = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    // Shown by the planner screens before the first sync.
    protected fun addNothingSynced() {
        content.addView(ui.card().apply { addView(ui.text(getString(R.string.planner_none), 15f)) }, ui.matchWidth(16))
    }

    // Small dim text between cards, lined up with the cards' contents.
    protected fun addNote(text: CharSequence, sizeSp: Float = 12f) {
        content.addView(ui.text(text, sizeSp, ui.skin.dim), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
    }
}
