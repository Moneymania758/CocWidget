package com.cocwidget

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.game.Village
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.store.UpdateChecker
import com.cocwidget.ui.HomeUpgrades
import com.cocwidget.ui.Pictures
import com.cocwidget.ui.Screen
import com.cocwidget.ui.WhatsNew
import com.cocwidget.widget.NextFinish
import com.cocwidget.widget.WidgetRenderer
import kotlin.concurrent.thread

// The selected village, its tools and every upgrade running in it. Everything about the app
// itself is on the Settings screen.
class MainActivity : Screen() {
    private lateinit var pictures: Pictures
    private var clocks: List<HomeUpgrades.Clock> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    // Counts down on the screen. Something ending changes more than its clock, so that rebuilds.
    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (clocks.any { it.endMillis <= now }) {
                show()
            } else {
                for (clock in clocks) clock.view.text = clock.text(clock.endMillis - now)
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Not after a rotation, so a chip picked since isn't undone.
        if (savedInstanceState == null) openAccount(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openAccount(intent)
    }

    // A widget's title opens the app on that widget's account.
    private fun openAccount(intent: Intent) {
        intent.getStringExtra(EXTRA_ACCOUNT)?.let { Store.setSelected(this, it) }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(ticker, TICK_MS)
        // The screen was just built, so redraw only when an update turns up.
        val before = UpdateChecker.availableUpdate(this)
        thread {
            UpdateChecker.checkIfDue(applicationContext, UpdateChecker.OPEN_APP_INTERVAL_MS)
            runOnUiThread { if (!isDestroyed && UpdateChecker.availableUpdate(this) != before) show() }
        }
    }

    override fun build() {
        content.addView(header())
        UpdateChecker.availableUpdate(this)?.let { version ->
            content.addView(ui.button(getString(R.string.update_available_long, version), null, primary = true) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE)))
            }, ui.matchWidth(16))
        }
        whatsNewCard()?.let { content.addView(it, ui.matchWidth(16)) }
        val tag = Store.selected(this)
        val village = tag?.let { Store.village(this, it) }
        pictures = Pictures(this)
        val now = System.currentTimeMillis()
        val list = village?.let { HomeUpgrades(this, ui, pictures, it, now) }
        content.addView(villageCard(tag, village, list, now), ui.matchWidth(16))
        if (village == null || list == null) {
            content.addView(ui.button(getString(R.string.guide_open), R.drawable.ic_book, primary = false) {
                startActivity(Intent(this, GuideActivity::class.java))
            }, ui.matchWidth(14))
            clocks = emptyList()
        } else {
            content.addView(tools(), ui.matchWidth(14))
            list.helpers()?.let { content.addView(it, ui.matchWidth(14)) }
            for (section in village.sections()) content.addView(list.section(section), ui.matchWidth(14))
            clocks = list.clocks
        }
        pictures.load()
        addNote(getString(R.string.footer))
    }

    private fun header() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(4), ui.dp(8), 0, 0)
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER
            background = ui.rounded(ui.skin.accent, 10f)
            addView(ui.icon(R.drawable.ic_clock_line, 0xFF121417.toInt(), 22))
        }, LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.heading(getString(R.string.app_name), 24f, ui.skin.title))
            addView(ui.text(getString(R.string.version_line, installedVersion()), 13f, ui.skin.dim))
        }, ui.weighted(12))
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER
            contentDescription = getString(R.string.settings_title)
            addView(ui.icon(R.drawable.ic_settings, ui.skin.text, 24))
            ui.pressable(this, 22f) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
    }

    // A card for the version just installed, until it's dismissed.
    private fun whatsNewCard(): View? {
        val installed = installedVersion()
        if (!WhatsNew.showCard(installed, Store.whatsNewSeen(this), Store.hasExport(this))) return null
        val latest = WhatsNew.RELEASES.first()
        return ui.card().apply {
            addView(ui.heading(getString(R.string.whats_new_card, latest.version), 16f, ui.skin.accent))
            addView(ui.text(getString(latest.notes), 14f, ui.skin.dim), ui.matchWidth(6))
            addView(ui.text(getString(R.string.whats_new_got_it), 15f, ui.skin.accent, bold = true).apply {
                minHeight = ui.dp(44)
                gravity = Gravity.CENTER_VERTICAL
                ui.pressable(this, 8f) {
                    Store.setWhatsNewSeen(this@MainActivity, installed)
                    show()
                }
            }, ui.wrap(2))
        }
    }

    private fun villageCard(tag: String?, village: Village?, list: HomeUpgrades?, now: Long): View = ui.card().apply {
        val accounts = Store.accounts(this@MainActivity)
        if (accounts.isNotEmpty()) addView(accountChips(accounts, tag), ui.matchWidth().apply { bottomMargin = ui.dp(14) })
        if (village == null || list == null) {
            addView(ui.heading(getString(R.string.status_none), 18f))
            addView(ui.text(getString(R.string.empty_state), 14f, ui.skin.dim), ui.matchWidth(6))
        } else {
            addView(villageTitle(village, now))
            NextFinish.of(village, now).next?.let { next -> addView(list.nextFinish(next), ui.matchWidth(14)) }
            val line = WidgetRenderer.exportLine(this@MainActivity, village, now)
            addView(ui.text(line.text, 12f, if (line.stale) getColor(R.color.idle) else ui.skin.dim).apply {
                gravity = Gravity.CENTER
            }, ui.matchWidth(12))
        }
        addView(ui.button(getString(R.string.import_village), R.drawable.ic_import_line, primary = true) {
            startActivity(Intent(this@MainActivity, SyncActivity::class.java))
        }, ui.matchWidth(if (village == null) 14 else 6))
        if (village == null) {
            // Nothing to plan yet, and the setup steps are what a first-time user needs.
            addView(ui.button(getString(R.string.fold_setup), null, primary = false) {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_SHOW_SETUP, true))
            }, ui.matchWidth(10))
        }
    }

    // The Town Hall large on a soft glow, beside what's sitting idle. The account chip above says
    // whose village it is.
    private fun villageTitle(village: Village, now: Long) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        if (village.townHall > 0) addView(townHallPicture(village.townHall), LinearLayout.LayoutParams(ui.dp(104), ui.dp(112)))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            if (village.townHall > 0) {
                addView(ui.heading(getString(R.string.home_town_hall, village.townHall), 24f, ui.skin.title))
            }
            val summary = NextFinish.of(village, now)
            addView(ui.text(idleLine(summary), 13f, if (summary.anyIdle) getColor(R.color.idle) else ui.skin.green, bold = true), ui.matchWidth(6))
        }, ui.weighted(12))
    }

    private fun townHallPicture(level: Int) = ImageView(this).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = getString(R.string.home_town_hall, level)
        val glow = ui.skin.accent and 0x00FFFFFF
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(glow or 0x40000000, glow)).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = ui.dp(58).toFloat()
        }
        // Straight away if it's bundled, so it doesn't blink each time the screen is rebuilt.
        val path = Icons.townHallPath(level)
        val bitmap = Icons.load(this@MainActivity, path)
        if (bitmap != null) setImageBitmap(bitmap) else pictures.add(path, this)
    }

    private fun idleLine(summary: NextFinish.Summary): String = if (summary.anyIdle) {
        buildList {
            if (summary.idleBuilders > 0) add(resources.getQuantityString(R.plurals.next_builders_idle, summary.idleBuilders, summary.idleBuilders))
            if (summary.labIdle) add(getString(R.string.next_lab_idle))
            if (summary.petsIdle) add(getString(R.string.next_pets_idle))
        }.joinToString(" · ")
    } else {
        resources.getQuantityString(R.plurals.next_all_busy, summary.builders, summary.builders)
    }

    // The tiles share their row's width and take the height of the tallest label.
    private fun tools() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        isBaselineAligned = false
        val tiles = listOf(
            Triple(R.string.planner_open, R.drawable.ic_chart, PlannerActivity::class.java),
            Triple(R.string.bedtime_open, R.drawable.ic_moon, BedtimeActivity::class.java),
            Triple(R.string.potions_open, R.drawable.ic_flask, PotionsActivity::class.java),
            Triple(R.string.guide_open, R.drawable.ic_book, GuideActivity::class.java),
        )
        for ((index, tile) in tiles.withIndex()) {
            val (label, icon, screen) = tile
            addView(ui.tile(getString(label), icon) {
                startActivity(Intent(this@MainActivity, screen))
            }, ui.weighted(if (index == 0) 0 else 8).apply { height = LinearLayout.LayoutParams.MATCH_PARENT })
        }
    }

    // One chip per account, plus one saying how to add another.
    private fun accountChips(accounts: List<String>, selected: String?) = ui.flow(8).apply {
        for (tag in accounts) {
            val chosen = tag == selected
            val colours = if (chosen) ui.skin.primaryBg to ui.skin.primaryText else ui.skin.chip to ui.skin.text
            addView(ui.chip(Store.label(this@MainActivity, tag), colours.first, colours.second).apply {
                minHeight = ui.dp(36)
                gravity = Gravity.CENTER_VERTICAL
                isSelected = chosen
                ui.pressable(this, 8f) {
                    Store.setSelected(this@MainActivity, tag)
                    show()
                }
            })
        }
        addView(ui.chip(getString(R.string.account_add), colour = ui.skin.dim).apply {
            minHeight = ui.dp(36)
            gravity = Gravity.CENTER_VERTICAL
            ui.pressable(this, 8f) {
                AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.account_add_title)
                    .setMessage(R.string.account_add_steps)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        })
    }

    companion object {
        const val EXTRA_ACCOUNT = "account"
        private const val NOTIFICATION_REQUEST = 1
        private const val TICK_MS = 1000L
    }
}
