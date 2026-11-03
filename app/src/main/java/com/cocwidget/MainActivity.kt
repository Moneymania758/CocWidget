package com.cocwidget

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.Toast
import java.text.DateFormat
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var ui: Ui
    // Folded sections the person opened, kept while the screen is rebuilt.
    private val open = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)
        content = findViewById(R.id.content)
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
        show()
        // Redraw only when an update turns up, so a name being typed isn't wiped.
        val before = UpdateChecker.availableUpdate(this)
        thread {
            UpdateChecker.checkIfDue(applicationContext, UpdateChecker.OPEN_APP_INTERVAL_MS)
            runOnUiThread { if (!isDestroyed && UpdateChecker.availableUpdate(this) != before) show() }
        }
    }

    private fun show() {
        ui = Ui(this, Skin.current(this))
        ui.applyWindow(findViewById<ScrollView>(R.id.scroll))
        content.removeAllViews()
        content.addView(header())
        UpdateChecker.availableUpdate(this)?.let { version ->
            content.addView(ui.button(getString(R.string.update_available_long, version), null, primary = true) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE)))
            }, ui.matchWidth(16))
        }
        whatsNewCard()?.let { content.addView(it, ui.matchWidth(16)) }
        content.addView(villageCard(), ui.matchWidth(16))
        content.addView(settingsCard(), ui.matchWidth(16))
        content.addView(helpCard(), ui.matchWidth(16))
        content.addView(ui.text(getString(R.string.footer), 12f, ui.skin.dim), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
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
    }

    private fun installedVersion() = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

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
            val exported = DateUtils.formatSameDayTime(village.exportedAtMillis, now, DateFormat.MEDIUM, DateFormat.SHORT)
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
                    addView(ui.text(getString(R.string.next_finish, next.name, next.toLevel), 14f), ui.weighted(10))
                    addView(ui.text(PlanFormat.duration((village.endMillis(next) - now) / 1000), 14f, ui.skin.green, bold = true))
                }, ui.matchWidth(14))
            }
        }
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(ui.button(getString(R.string.sync), R.drawable.ic_sync_line, primary = true) {
                startActivity(Intent(this@MainActivity, SyncActivity::class.java))
            }, ui.weighted())
            addView(ui.button(getString(R.string.planner_open), R.drawable.ic_chart, primary = false) {
                startActivity(Intent(this@MainActivity, PlannerActivity::class.java))
            }, ui.weighted(10))
        }, ui.matchWidth(14))
        addView(ui.button(getString(R.string.bedtime_open), R.drawable.ic_clock_line, primary = false) {
            startActivity(Intent(this@MainActivity, BedtimeActivity::class.java))
        }, ui.matchWidth(10))
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

    private fun settingsCard(): View = ui.card(0).apply {
        addView(themePicker())
        addView(ui.divider())
        addView(eventRow())
        val tag = Store.selected(this@MainActivity)
        if (tag != null) {
            addView(ui.divider())
            addView(nameRow(tag))
            addView(ui.divider())
            addView(removeRow(tag))
        }
        addView(ui.divider())
        addView(fold(R.string.fold_setup, getString(R.string.main_steps)))
        addView(ui.divider())
        val notes = WhatsNew.RELEASES.joinToString("\n\n") { "${it.version}\n${getString(it.notes)}" }
        addView(fold(R.string.fold_whats_new, notes))
        addView(ui.divider())
        addView(fold(R.string.fold_about, getString(R.string.credit) + "\n\n" + getString(R.string.font_credit)))
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
    }

    private fun themePicker() = row().apply {
        addView(ui.text(getString(R.string.theme_label), 15f))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            for ((index, skin) in Skin.ALL.withIndex()) {
                val chosen = skin.key == ui.skin.key
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    minimumHeight = ui.dp(64)
                    background = ui.rounded(ui.skin.chip, 10f, if (chosen) ui.skin.accent else ui.skin.divider, if (chosen) 2f else 1f)
                    setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
                    isSelected = chosen
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        background = ui.rounded(0, 5f)
                        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                        clipToOutline = true
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        for (colour in skin.swatch) {
                            addView(View(this@MainActivity).apply { setBackgroundColor(colour) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                        }
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(18)))
                    addView(ui.text(getString(skin.label), 13f).apply { gravity = Gravity.CENTER }, ui.matchWidth(6))
                    ui.pressable(this, 10f) {
                        Store.setSkin(this@MainActivity, skin.key)
                        show()
                    }
                }, ui.weighted(if (index == 0) 0 else 8))
            }
        }, ui.matchWidth(10))
    }

    private fun eventRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(16), ui.dp(14), ui.dp(12), ui.dp(14))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.text(getString(R.string.event_switch), 15f))
            addView(ui.text(getString(R.string.event_explain), 13f, ui.skin.dim), ui.matchWidth(3))
        }, ui.weighted())
        addView(Switch(this@MainActivity).apply {
            contentDescription = getString(R.string.event_switch)
            isChecked = Store.eventOn(this@MainActivity)
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(ui.skin.accent, ui.skin.dim))
            trackTintList = ColorStateList(states, intArrayOf(ui.skin.accent, ui.skin.divider))
            setOnCheckedChangeListener { _, on -> Store.setEventOn(this@MainActivity, on) }
        }, ui.wrap().apply { marginStart = ui.dp(12) })
    }

    private fun nameRow(tag: String) = row().apply {
        addView(ui.text(getString(R.string.name_title), 15f))
        val name = EditText(this@MainActivity).apply {
            setText(Store.displayName(this@MainActivity, tag))
            hint = getString(R.string.name_hint)
            setTextColor(ui.skin.text)
            setHintTextColor(ui.skin.dim)
            backgroundTintList = ColorStateList.valueOf(ui.skin.dim)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(name, ui.weighted())
            addView(ui.button(getString(R.string.save_name), null, primary = false) {
                Store.setDisplayName(this@MainActivity, tag, name.text.toString().trim())
                VillageWidget.requestRefresh(this@MainActivity)
                Toast.makeText(this@MainActivity, R.string.name_saved, Toast.LENGTH_SHORT).show()
                show()
            }, ui.wrap().apply { marginStart = ui.dp(10) })
        }, ui.matchWidth(4))
    }

    // Asks first: the export is gone until it's synced again.
    private fun removeRow(tag: String) = row().apply {
        minimumHeight = ui.dp(52)
        gravity = Gravity.CENTER_VERTICAL
        addView(ui.text(getString(R.string.account_remove), 15f))
        ui.pressable(this, 0f) {
            AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(getString(R.string.account_remove_title, Store.label(this@MainActivity, tag)))
                .setMessage(R.string.account_remove_message)
                .setPositiveButton(R.string.account_remove_confirm) { _, _ ->
                    Store.removeAccount(this@MainActivity, tag)
                    VillageWidget.requestRefresh(this@MainActivity)
                    show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun helpCard(): View = ui.card(0).apply {
        addView(ui.heading(getString(R.string.help_title), 16f, ui.skin.accent).apply {
            setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(4))
        })
        val device = Contact.device(installedVersion(), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL)
        addView(linkRow(R.string.help_report, R.string.help_report_detail) {
            open(Intent(Intent.ACTION_VIEW, Uri.parse(Contact.issueUrl(device))))
        })
        if (Contact.EMAIL.isNotEmpty()) {
            addView(ui.divider())
            addView(linkRow(R.string.help_email, R.string.help_email_detail) {
                open(Intent(Intent.ACTION_SENDTO, Uri.parse(Contact.emailUri(device))))
            })
        }
        if (Contact.DISCORD.isNotEmpty()) {
            addView(ui.divider())
            addView(linkRow(R.string.help_discord, R.string.help_discord_detail) {
                open(Intent(Intent.ACTION_VIEW, Uri.parse(Contact.DISCORD)))
            })
        }
    }

    private fun linkRow(title: Int, detail: Int, onClick: () -> Unit) = row().apply {
        minimumHeight = ui.dp(52)
        addView(ui.text(getString(title), 15f))
        addView(ui.text(getString(detail), 13f, ui.skin.dim), ui.matchWidth(3))
        ui.pressable(this, 0f, onClick)
    }

    // A phone with no email app (or no browser) would otherwise crash here.
    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.help_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    // A row that opens to show [body] underneath, and closes again.
    private fun fold(title: Int, body: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val chevron = ui.icon(R.drawable.ic_chevron, ui.skin.dim, 18)
        val text = ui.text(body, 14f, ui.skin.dim).apply { setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(16)) }
        fun refresh() {
            val isOpen = title in open
            chevron.rotation = if (isOpen) 90f else 0f
            text.visibility = if (isOpen) View.VISIBLE else View.GONE
        }
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ui.dp(52)
            setPadding(ui.dp(16), 0, ui.dp(16), 0)
            addView(ui.text(getString(title), 15f), ui.weighted())
            addView(chevron)
            ui.pressable(this, 0f) {
                if (!open.remove(title)) open += title
                refresh()
            }
        })
        addView(text)
        refresh()
    }

    companion object {
        const val EXTRA_ACCOUNT = "account"
        private const val NOTIFICATION_REQUEST = 1
    }
}
