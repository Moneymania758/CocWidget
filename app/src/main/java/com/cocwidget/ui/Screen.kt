package com.cocwidget.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.R

// A screen of cards made in code. It's rebuilt on every visit, so a sync or a theme change made
// meanwhile shows.
abstract class Screen : Activity() {
    protected lateinit var content: LinearLayout
    protected lateinit var ui: Ui

    // The bar's tab this screen is, or null for a screen opened from one, which has a back arrow
    // instead of the bar.
    protected open val tab: Tab? = null

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
        showTabBar()
    }

    protected abstract fun build()

    // A back arrow and the screen's title. Callers can add more to the end of the row.
    protected fun backHeader(title: Int) = backHeader(getString(title))

    protected fun backHeader(title: CharSequence) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(this@Screen).apply {
            gravity = Gravity.CENTER
            contentDescription = getString(R.string.back)
            addView(ui.icon(R.drawable.ic_back, ui.skin.text, 22))
            ui.pressable(this, 22f) { leave() }
        }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
        addView(ui.heading(title, 22f, ui.skin.title), ui.weighted(4))
    }

    // The back arrow. A screen holding unsaved work asks first.
    protected open fun leave() = finish()

    // A tab's top: the account header and, for a tab with pages, the switch between them.
    // Switching account rebuilds the screen for the new one.
    protected fun addTabTop(parts: List<TabChrome.Part>? = null) {
        val chrome = TabChrome(this, ui)
        content.addView(chrome.accountHeader { show() })
        parts?.let { content.addView(chrome.switch(it, this::class.java), ui.matchWidth(12)) }
    }

    private fun showTabBar() {
        val bar = findViewById<LinearLayout>(R.id.tab_bar)
        val current = tab
        if (current == null) {
            bar.removeAllViews()
            bar.visibility = View.GONE
        } else {
            TabChrome(this, ui).fillBar(bar, current)
        }
    }

    protected fun installedVersion() = TabChrome.installedVersion(this)

    // Shown by the planner screens before the first sync.
    protected fun addNothingSynced() {
        content.addView(ui.card().apply { addView(ui.text(getString(R.string.planner_none), 15f)) }, ui.matchWidth(16))
    }

    // Small dim text between cards, lined up with the cards' contents.
    protected fun addNote(text: CharSequence, sizeSp: Float = 12f) {
        content.addView(ui.text(text, sizeSp, ui.skin.dim), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
    }
}

// The tabs along the bottom, in order.
enum class Tab(val label: Int, val icon: Int) {
    UP_NEXT(R.string.tab_up_next, R.drawable.ic_clock_line),
    PLAN(R.string.tab_plan, R.drawable.ic_chart),
    GUIDE(R.string.tab_guide, R.drawable.ic_book),
    BASES(R.string.tab_bases, R.drawable.ic_bases),
}
