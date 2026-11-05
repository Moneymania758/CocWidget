package com.cocwidget

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.cocwidget.plan.PlanFormat
import com.cocwidget.store.Store
import com.cocwidget.store.UpdateChecker
import com.cocwidget.ui.Screen
import com.cocwidget.ui.TimeText
import com.cocwidget.ui.WhatsNew
import kotlin.concurrent.thread

// The selected village and the tools for it. Everything about the app itself is on the Settings
// screen, so this one stays short as features are added.
class MainActivity : Screen() {
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

    override fun onResume() {
        super.onResume()
        // Redraw only when an update turns up, so a name being typed isn't wiped.
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
        content.addView(villageCard(), ui.matchWidth(16))
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

    private fun villageCard(): View = ui.card().apply {
        val accounts = Store.accounts(this@MainActivity)
        val tag = Store.selected(this@MainActivity)
        val village = tag?.let { Store.village(this@MainActivity, it) }
        val now = System.currentTimeMillis()
        if (accounts.isNotEmpty()) addView(accountChips(accounts, tag), ui.matchWidth().apply { bottomMargin = ui.dp(14) })
        if (village == null) {
            addView(ui.heading(getString(R.string.status_none), 18f))
            addView(ui.text(getString(R.string.empty_state), 14f, ui.skin.dim), ui.matchWidth(6))
        } else {
            val running = village.upgrades.filter { village.endMillis(it) > now }
            val exported = TimeText.exported(village.exportedAtMillis, now)
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(ui.heading(running.size.toString(), 40f))
                    val label = resources.getQuantityString(R.plurals.status_running, running.size)
                    addView(ui.text(getString(R.string.status_town_hall, label, village.townHall), 14f, ui.skin.dim))
                }, ui.weighted())
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                    addView(ui.text(getString(R.string.status_exported), 13f, ui.skin.dim))
                    addView(ui.text(exported, 16f, bold = true))
                })
            })
            running.minByOrNull { village.endMillis(it) }?.let { next ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = ui.rounded(ui.skin.chip, 10f)
                    setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
                    addView(android.view.View(this@MainActivity).apply { background = ui.rounded(ui.skin.green, 4f) }, LinearLayout.LayoutParams(ui.dp(8), ui.dp(8)))
                    val step = if (next.supercharge > 0) {
                        getString(R.string.next_finish_supercharge, next.name, next.supercharge)
                    } else {
                        getString(R.string.next_finish, next.name, next.toLevel)
                    }
                    addView(ui.text(step, 14f), ui.weighted(10))
                    addView(ui.text(PlanFormat.duration((village.endMillis(next) - now) / 1000), 14f, ui.skin.green, bold = true))
                }, ui.matchWidth(14))
            }
        }
        addView(ui.button(getString(R.string.sync), R.drawable.ic_sync_line, primary = true) {
            startActivity(Intent(this@MainActivity, SyncActivity::class.java))
        }, ui.matchWidth(14))
        if (village == null) {
            // Nothing to plan yet, and the setup steps are what a first-time user needs.
            addView(ui.button(getString(R.string.fold_setup), null, primary = false) {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_SHOW_SETUP, true))
            }, ui.matchWidth(10))
        } else {
            addView(tools(), ui.matchWidth(10))
        }
    }

    // The tiles share their row's width and take the height of the tallest label.
    private fun tools() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        isBaselineAligned = false
        val tiles = listOf(
            Triple(R.string.planner_open, R.drawable.ic_chart, PlannerActivity::class.java),
            Triple(R.string.bedtime_open, R.drawable.ic_moon, BedtimeActivity::class.java),
            Triple(R.string.potions_open, R.drawable.ic_flask, PotionsActivity::class.java),
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
    }
}
