package com.cocwidget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.cocwidget.events.EventDates
import com.cocwidget.events.EventSchedule
import com.cocwidget.game.Section
import com.cocwidget.game.Village
import com.cocwidget.store.Store
import com.cocwidget.store.UpdateChecker
import com.cocwidget.ui.Pictures
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Tab
import com.cocwidget.ui.UpNextList
import com.cocwidget.ui.WhatsNew
import com.cocwidget.widget.WidgetRenderer
import kotlin.concurrent.thread

// The selected village and every upgrade running in it, under the account button. Everything about
// the app itself is on the Settings screen.
class MainActivity : Screen() {
    override val tab = Tab.UP_NEXT
    private lateinit var pictures: Pictures
    private var clocks: List<UpNextList.Clock> = emptyList()
    // The section a status tile cut the stream down to, or null for everything.
    private var only: Section? = null
    private val handler = Handler(Looper.getMainLooper())

    // Counts down on the screen. Something ending changes more than its clock, so that rebuilds.
    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (clocks.any { it.endMillis <= now }) {
                show()
            } else {
                for (clock in clocks) clock.draw(now)
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
            // A new install has no alarm until now.
            WidgetRenderer.checkEvents(applicationContext)
            runOnUiThread { if (!isDestroyed && UpdateChecker.availableUpdate(this) != before) show() }
        }
    }

    override fun build() {
        addTabTop()
        UpdateChecker.availableUpdate(this)?.let { version ->
            content.addView(ui.button(getString(R.string.update_available_long, version), null, primary = true) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE)))
            }, ui.matchWidth(16))
        }
        whatsNewCard()?.let { content.addView(it, ui.matchWidth(16)) }
        val tag = Store.selected(this)
        val village = tag?.let { Store.village(this, it) }
        if (village == null || only !in village.sections()) only = null
        pictures = Pictures(this)
        val now = System.currentTimeMillis()
        val list = UpNextList(this, ui, pictures, village, now, only) { section ->
            only = section
            show()
        }
        if (tag == null || village == null) {
            content.addView(emptyCard(village == null && tag != null), ui.matchWidth(16))
        } else {
            noticeCard(tag, village, now)?.let { content.addView(it, ui.matchWidth(16)) }
        }
        list.tiles()?.let { content.addView(it, ui.matchWidth(16)) }
        list.filterNote()?.let { content.addView(it, ui.matchWidth(4)) }
        // Only the events the player asked to be reminded of, so a weekly one doesn't crowd the list.
        val events = EventSchedule.countdowns(now, EventDates.current(this)).filter { Store.eventReminderOn(this, it.kind) }
        content.addView(list.timeline(events), ui.matchWidth(12))
        clocks = list.clocks
        pictures.load()
        addNote(getString(R.string.footer))
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

    // Before the first import, or when the saved export can't be read.
    private fun emptyCard(unreadable: Boolean): View = ui.card().apply {
        addView(ui.heading(getString(if (unreadable) R.string.account_unreadable else R.string.status_none), 18f))
        addView(ui.text(getString(R.string.empty_state), 14f, ui.skin.dim), ui.matchWidth(6))
        addView(ui.button(getString(R.string.import_village), R.drawable.ic_import_line, primary = true) {
            startActivity(Intent(this@MainActivity, SyncActivity::class.java))
        }, ui.matchWidth(14))
        // Nothing to plan yet, and the setup steps are what a first-time user needs.
        addView(ui.button(getString(R.string.fold_setup), null, primary = false) {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_SHOW_SETUP, true))
        }, ui.matchWidth(10))
    }

    // Only when the export is out of date or holds finished upgrades: the warning, and Clear.
    private fun noticeCard(tag: String, village: Village, now: Long): View? {
        val line = WidgetRenderer.exportLine(this, village, now)
        val finished = village.upgrades.count { village.endMillis(it) <= now }
        if (!line.stale && finished == 0) return null
        return ui.card(14).apply {
            if (line.stale) addView(ui.text(line.text, 13f, ui.skin.idle, bold = true))
            if (finished > 0) {
                addView(ui.button(resources.getQuantityString(R.plurals.clear_finished, finished, finished), null, primary = false) {
                    Store.clearFinished(this@MainActivity, tag, System.currentTimeMillis())
                    VillageWidget.requestRefresh(this@MainActivity)
                    show()
                }, ui.matchWidth(if (line.stale) 10 else 0))
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT = "account"
        private const val NOTIFICATION_REQUEST = 1
        private const val TICK_MS = 1000L
    }
}
