package com.cocwidget.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.BedtimeActivity
import com.cocwidget.GuideActivity
import com.cocwidget.MainActivity
import com.cocwidget.MyBasesActivity
import com.cocwidget.PlannerActivity
import com.cocwidget.PotionsActivity
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

    // A tab's title, with no back arrow. Callers can add more to the end of the row.
    protected fun tabHeader(title: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = ui.dp(44)
        addView(ui.heading(getString(title), 26f, ui.skin.title), ui.weighted(4))
    }

    // Time to max, Bedtime and Potions share the Plan tab, one switch along its top.
    protected fun planSwitch(current: Class<out Screen>) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        background = ui.rounded(ui.skin.chip, 14f)
        setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
        val parts = listOf(
            R.string.plan_time_to_max to PlannerActivity::class.java,
            R.string.plan_bedtime to BedtimeActivity::class.java,
            R.string.plan_potions to PotionsActivity::class.java,
        )
        for ((index, part) in parts.withIndex()) {
            val (label, screen) = part
            val chosen = screen == current
            addView(ui.text(getString(label), 14f, if (chosen) ui.skin.text else ui.skin.dim, bold = chosen).apply {
                gravity = Gravity.CENTER
                minHeight = ui.dp(40)
                maxLines = 1
                if (chosen) background = ui.rounded(ui.skin.card, 10f)
                isSelected = chosen
                ui.pressable(this, 10f) { if (!chosen) swapTo(screen) }
            }, ui.weighted(if (index == 0) 0 else 4))
        }
    }

    private fun showTabBar() {
        val bar = findViewById<LinearLayout>(R.id.tab_bar)
        bar.removeAllViews()
        val current = tab
        if (current == null) {
            bar.visibility = View.GONE
            return
        }
        bar.visibility = View.VISIBLE
        bar.setBackgroundColor(ui.skin.card)
        bar.addView(ui.divider())
        bar.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for (item in Tab.entries) addView(tabButton(item, item == current), LinearLayout.LayoutParams(0, ui.dp(64), 1f))
        })
    }

    // The icon sits in a pill tinted with the skin's accent while its tab is open.
    private fun tabButton(item: Tab, chosen: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isSelected = chosen
        addView(LinearLayout(this@Screen).apply {
            gravity = Gravity.CENTER
            if (chosen) background = ui.rounded((ui.skin.accent and 0x00FFFFFF) or 0x33000000, 15f)
            addView(ui.icon(item.icon, if (chosen) ui.skin.accent else ui.skin.dim, 20))
        }, LinearLayout.LayoutParams(ui.dp(56), ui.dp(30)))
        addView(ui.text(getString(item.label), 12f, if (chosen) ui.skin.text else ui.skin.dim, bold = chosen), ui.wrap(4))
        ui.pressable(this, 0f) { openTab(item) }
    }

    // Up next stays at the bottom of the stack, so Back from any other tab lands there. Another tab
    // takes this one's place rather than piling up.
    private fun openTab(target: Tab) {
        if (target == tab) return
        val intent = Intent(this, screenOf(target)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (target == Tab.UP_NEXT) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        if (tab != Tab.UP_NEXT) leaveQuietly()
    }

    private fun swapTo(screen: Class<out Screen>) {
        startActivity(Intent(this, screen).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
        leaveQuietly()
    }

    // Finishes without the closing slide, so switching tabs doesn't look like going back.
    private fun leaveQuietly() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
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

// The tabs along the bottom, in order.
enum class Tab(val label: Int, val icon: Int) {
    UP_NEXT(R.string.tab_up_next, R.drawable.ic_clock_line),
    PLAN(R.string.tab_plan, R.drawable.ic_chart),
    GUIDE(R.string.tab_guide, R.drawable.ic_book),
    BASES(R.string.tab_bases, R.drawable.ic_bases),
}

private fun screenOf(tab: Tab): Class<out Activity> = when (tab) {
    Tab.UP_NEXT -> MainActivity::class.java
    Tab.PLAN -> PlannerActivity::class.java
    Tab.GUIDE -> GuideActivity::class.java
    Tab.BASES -> MyBasesActivity::class.java
}
