package com.cocwidget.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.cocwidget.BasesActivity
import com.cocwidget.BedtimeActivity
import com.cocwidget.GuideActivity
import com.cocwidget.MainActivity
import com.cocwidget.MyBasesActivity
import com.cocwidget.PlannerActivity
import com.cocwidget.PotionsActivity
import com.cocwidget.R
import com.cocwidget.SettingsActivity
import com.cocwidget.SyncActivity
import com.cocwidget.game.Village
import com.cocwidget.store.Store

// What every tab has round its content: the account header at the top, a switch when the tab has
// more than one page, and the bar along the bottom. Shared by Screen and the Find bases web page,
// which can't be a Screen.
class TabChrome(private val activity: Activity, private val ui: Ui) {
    // A page within a tab: its label and its screen.
    class Part(val label: Int, val screen: Class<out Activity>, val extras: Intent.() -> Unit = {})

    // The account button, with Import and Settings beside it. Before the first import, the app's
    // name stands in for the account. [onSwitch] runs after another account is picked.
    fun accountHeader(onSwitch: () -> Unit) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val tag = Store.selected(activity)
        if (tag == null) {
            setPadding(ui.dp(4), ui.dp(8), 0, 0)
            addView(LinearLayout(activity).apply {
                gravity = Gravity.CENTER
                background = ui.rounded(ui.skin.accent, 10f)
                addView(ui.icon(R.drawable.ic_clock_line, ui.skin.onAccent, 22))
            }, LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)))
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(ui.heading(activity.getString(R.string.app_name), 24f, ui.skin.title))
                addView(ui.text(activity.getString(R.string.version_line, installedVersion(activity)), 13f, ui.skin.dim))
            }, ui.weighted(12))
        } else {
            addView(accountButton(tag, Store.village(activity, tag), onSwitch), ui.weighted())
            addView(roundButton(R.drawable.ic_import_line, R.string.import_village, ui.skin.primaryBg, ui.skin.primaryText, 0) {
                activity.startActivity(Intent(activity, SyncActivity::class.java))
            }, LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)).apply { marginStart = ui.dp(8) })
        }
        addView(roundButton(R.drawable.ic_settings, R.string.settings_title, ui.skin.card, ui.skin.text, ui.skin.divider) {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
        }, LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)).apply { marginStart = ui.dp(8) })
    }

    // Whose village this is: its Town Hall, name and export time. Tapping it lists every account.
    private fun accountButton(tag: String, village: Village?, onSwitch: () -> Unit) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = ui.dp(52)
        background = ui.rounded(ui.skin.card, 26f, ui.skin.divider, 1f)
        setPadding(ui.dp(4), ui.dp(4), ui.dp(12), ui.dp(4))
        contentDescription = activity.getString(R.string.account_switch)
        val townHall = village?.townHall ?: 0
        addView(ui.townHallBadge(townHall, ui.skin.accent, 42).apply {
            if (townHall > 0) contentDescription = activity.getString(R.string.home_town_hall, townHall)
        })
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.heading(Store.label(activity, tag), 17f, ui.skin.text).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            val line = if (village == null) {
                activity.getString(R.string.account_unreadable)
            } else {
                val exported = TimeText.exported(village.exportedAtMillis, System.currentTimeMillis())
                activity.getString(R.string.account_line, townHall, activity.getString(R.string.exported_at, exported))
            }
            // Two lines on a narrow phone, so the export time isn't cut off.
            addView(ui.text(line, 12f, ui.skin.dim).apply {
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            })
        }, ui.weighted(10))
        addView(ui.icon(R.drawable.ic_chevron, ui.skin.accent, 18).apply { rotation = 90f })
        ui.pressable(this, 26f) { AccountSheet(activity, ui, onSwitch).show() }
    }

    private fun roundButton(icon: Int, description: Int, fill: Int, colour: Int, edge: Int, onClick: () -> Unit) = LinearLayout(activity).apply {
        gravity = Gravity.CENTER
        background = ui.rounded(fill, 26f, edge, if (edge != 0) 1f else 0f)
        contentDescription = activity.getString(description)
        addView(ui.icon(icon, colour, 22))
        ui.pressable(this, 26f, onClick)
    }

    // The pages of a tab side by side, the open one raised. Another page takes this one's place.
    fun switch(parts: List<Part>, current: Class<out Activity>) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        background = ui.rounded(ui.skin.chip, 14f)
        setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
        for ((index, part) in parts.withIndex()) {
            val chosen = part.screen == current
            addView(ui.text(activity.getString(part.label), 14f, if (chosen) ui.skin.text else ui.skin.dim, bold = chosen).apply {
                gravity = Gravity.CENTER
                minHeight = ui.dp(40)
                maxLines = 1
                if (chosen) background = ui.rounded(ui.skin.card, 10f)
                isSelected = chosen
                ui.pressable(this, 10f) {
                    if (!chosen) {
                        activity.startActivity(Intent(activity, part.screen).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).apply(part.extras))
                        leaveQuietly()
                    }
                }
            }, ui.weighted(if (index == 0) 0 else 4))
        }
    }

    // Fills [bar] with a button per tab, [current] marked.
    fun fillBar(bar: LinearLayout, current: Tab) {
        bar.removeAllViews()
        bar.visibility = View.VISIBLE
        bar.setBackgroundColor(ui.skin.card)
        bar.addView(ui.divider())
        bar.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            for (item in Tab.entries) addView(tabButton(item, current), LinearLayout.LayoutParams(0, ui.dp(64), 1f))
        })
    }

    // The icon sits in a pill tinted with the skin's accent while its tab is open.
    private fun tabButton(item: Tab, current: Tab) = LinearLayout(activity).apply {
        val chosen = item == current
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isSelected = chosen
        addView(LinearLayout(activity).apply {
            gravity = Gravity.CENTER
            if (chosen) background = ui.rounded((ui.skin.accent and 0x00FFFFFF) or 0x33000000, 15f)
            addView(ui.icon(item.icon, if (chosen) ui.skin.accent else ui.skin.dim, 20))
        }, LinearLayout.LayoutParams(ui.dp(56), ui.dp(30)))
        addView(ui.text(activity.getString(item.label), 12f, if (chosen) ui.skin.text else ui.skin.dim, bold = chosen), ui.wrap(4))
        ui.pressable(this, 0f) { openTab(item, current) }
    }

    // Up next stays at the bottom of the stack, so Back from any other tab lands there. Another tab
    // takes this one's place rather than piling up.
    private fun openTab(target: Tab, current: Tab) {
        if (target == current) return
        val intent = Intent(activity, screenOf(target)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (target == Tab.UP_NEXT) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
        if (current != Tab.UP_NEXT) leaveQuietly()
    }

    // Finishes without the closing slide, so switching tabs doesn't look like going back.
    private fun leaveQuietly() {
        activity.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }
    }

    companion object {
        // Time to max, Bedtime and Potions share the Plan tab.
        val PLAN = listOf(
            Part(R.string.plan_time_to_max, PlannerActivity::class.java),
            Part(R.string.plan_bedtime, BedtimeActivity::class.java),
            Part(R.string.plan_potions, PotionsActivity::class.java),
        )

        // The player's own bases first, as that's where the tab opens.
        val BASES = listOf(
            Part(R.string.bases_mine, MyBasesActivity::class.java),
            Part(R.string.bases_find, BasesActivity::class.java) { putExtra(BasesActivity.EXTRA_AS_TAB, true) },
        )

        fun installedVersion(activity: Activity) = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty()

        private fun screenOf(tab: Tab): Class<out Activity> = when (tab) {
            Tab.UP_NEXT -> MainActivity::class.java
            Tab.PLAN -> PlannerActivity::class.java
            Tab.GUIDE -> GuideActivity::class.java
            Tab.BASES -> MyBasesActivity::class.java
        }
    }
}
