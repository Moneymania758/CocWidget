package com.cocwidget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.cocwidget.bases.BaseLinks
import com.cocwidget.bases.BaseSite
import com.cocwidget.bases.LayoutOpener
import com.cocwidget.bases.Route
import com.cocwidget.game.GameData
import com.cocwidget.game.Ids
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin
import com.cocwidget.ui.Ui

// Base layouts from a site, shown inside the app as the site shows them. A tap on a base's link
// opens it in Clash of Clans, ready to copy, and nothing else leaves the site's own pages.
class BasesActivity : Activity() {
    private lateinit var ui: Ui
    private lateinit var bar: LinearLayout
    private lateinit var loading: ProgressBar
    internal lateinit var web: WebView
        private set

    internal var site = BaseSite.CLASH_CODES
        private set
    internal var townHall = 0
        private set
    private var myTownHall = 0
    private var maxTownHall = 0
    // Set on a site change. The old site's pages are dropped once the new site's first page is in
    // the history, as clearHistory() keeps the page showing when it's called.
    private var dropOldSite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = Ui(this, Skin.current(this))
        myTownHall = Store.current(this)?.village?.townHall ?: 0
        maxTownHall = GameData.all(this)[Ids.TOWN_HALL]?.levelCount ?: 0
        site = BaseSite.named(Store.basesSite(this))
        val asked = intent.getIntExtra(EXTRA_TOWN_HALL, 0)
        townHall = (asked.takeIf { it > 0 } ?: myTownHall.takeIf { it > 0 } ?: maxTownHall).coerceAtLeast(site.firstTownHall)

        bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8))
        }
        loading = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        web = WebView(this).apply {
            // The sites need it to work. Nothing in the app is offered to their scripts and files
            // are off, so a page can't reach anything of ours.
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            // Off, so a base link that opens a new tab comes through shouldOverrideUrlLoading.
            settings.setSupportMultipleWindows(false)
            webViewClient = Client()
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    loading.progress = progress
                    loading.visibility = if (progress < 100) View.VISIBLE else View.GONE
                }
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            addView(bar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(loading, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(3)))
            addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
        ui.applyWindow(root)
        drawBar()
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    // Back steps through the site first, then leaves.
    @Deprecated("Activity's own back handling, kept as this screen has no AndroidX.")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    private fun load() = web.loadUrl(site.page(townHall))

    // The title, a chip per site and a chip per Town Hall. Redrawn on every pick.
    private fun drawBar() {
        bar.removeAllViews()
        bar.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@BasesActivity).apply {
                gravity = Gravity.CENTER
                contentDescription = getString(R.string.back)
                addView(ui.icon(R.drawable.ic_back, ui.skin.text, 22))
                ui.pressable(this, 22f) { finish() }
            }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
            addView(ui.heading(getString(R.string.bases_title), 20f, ui.skin.title), ui.weighted(4))
        })
        bar.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for ((index, each) in BaseSite.entries.withIndex()) {
                addView(pick(each.label, each == site) { pickSite(each) }, ui.wrap().apply { if (index > 0) marginStart = ui.dp(8) })
            }
        }, ui.matchWidth(4))
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var chosen: View? = null
        for (level in site.firstTownHall..maxOf(maxTownHall, site.firstTownHall)) {
            val label = if (level == myTownHall) getString(R.string.bases_my_town_hall, level) else getString(R.string.town_hall_chip, level)
            val chip = pick(label, level == townHall) { pickTownHall(level) }
            if (level == townHall) chosen = chip
            chips.addView(chip, ui.wrap().apply { if (level > site.firstTownHall) marginStart = ui.dp(6) })
        }
        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        }
        bar.addView(scroller, ui.matchWidth(8))
        chosen?.let { chip -> scroller.post { scroller.scrollTo((chip.left - ui.dp(48)).coerceAtLeast(0), 0) } }
        bar.addView(ui.text(getString(R.string.bases_note, site.label), 12f, ui.skin.dim), ui.matchWidth(8))
    }

    private fun pick(label: String, chosen: Boolean, onPick: () -> Unit): TextView {
        val colours = if (chosen) ui.skin.primaryBg to ui.skin.primaryText else ui.skin.chip to ui.skin.text
        return ui.chip(label, colours.first, colours.second).apply {
            minHeight = ui.dp(36)
            gravity = Gravity.CENTER_VERTICAL
            isSelected = chosen
            ui.pressable(this, 8f) { onPick() }
        }
    }

    internal fun pickSite(next: BaseSite) {
        if (next == site) return
        site = next
        Store.setBasesSite(this, next.name)
        townHall = townHall.coerceAtLeast(site.firstTownHall)
        dropOldSite = true
        drawBar()
        load()
    }

    internal fun pickTownHall(level: Int) {
        townHall = level
        drawBar()
        load()
    }

    // What a tap on a link inside the site does. True when the app has dealt with it.
    internal fun handle(url: String): Boolean = when (BaseLinks.route(url, site)) {
        Route.STAY -> false
        Route.LAYOUT -> {
            LayoutOpener.open(this, url, myTownHall)
            true
        }
        Route.BROWSER -> {
            LayoutOpener.outside(this, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
            true
        }
        Route.DROP -> true
    }

    private inner class Client : WebViewClient() {
        // Only the page itself. Adverts in frames load where they are.
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            request.isForMainFrame && handle(request.url.toString())

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            loading.visibility = View.VISIBLE
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            if (dropOldSite && site.owns(url?.let { Uri.parse(it).host })) {
                dropOldSite = false
                view.clearHistory()
            }
        }
    }

    companion object {
        const val EXTRA_TOWN_HALL = "town_hall"
    }
}
