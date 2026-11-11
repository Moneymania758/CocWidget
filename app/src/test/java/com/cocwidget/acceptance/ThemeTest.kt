package com.cocwidget.acceptance

import android.graphics.drawable.ColorDrawable
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import com.cocwidget.MainActivity
import com.cocwidget.SettingsActivity
import com.cocwidget.TestData
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import kotlin.math.pow

// The theme picker in Settings, the Light theme and System, which follows the phone's dark mode.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val lightBars = APPEARANCE_LIGHT_STATUS_BARS or APPEARANCE_LIGHT_NAVIGATION_BARS
    private val names = listOf("System", "Light", "Clean", "Widget", "Game")

    private fun pick(name: String) {
        val settings = Screens.open<SettingsActivity>()
        Screens.tappable(Screens.root(settings), name)!!.performClick()
    }

    private fun barsOf(activity: MainActivity) = activity.window.insetsController!!.systemBarsAppearance and lightBars

    @Test
    fun thePickerOffersSystemLightCleanWidgetAndGame() {
        val texts = Screens.texts(Screens.root(Screens.open<SettingsActivity>()))
        assertEquals(names, texts.filter { it in names })
    }

    @Test
    fun pickingLightPaintsTheScreensLightWithDarkBarIcons() {
        pick("Light")
        assertEquals("light", Store.skin(app))
        val main = Screens.open<MainActivity>()
        assertEquals(Skin.LIGHT.bg, (main.window.decorView.background as ColorDrawable).color)
        assertEquals(lightBars, barsOf(main))
    }

    @Test
    fun aDarkThemeKeepsLightBarIcons() {
        pick("Clean")
        assertEquals(0, barsOf(Screens.open()))
    }

    @Test
    fun systemFollowsThePhonesDarkMode() {
        pick("System")
        assertEquals("system", Store.skin(app))
        RuntimeEnvironment.setQualifiers("+night")
        assertEquals(Skin.CLEAN, Skin.current(app))
        RuntimeEnvironment.setQualifiers("+notnight")
        assertEquals(Skin.LIGHT, Skin.current(app))
    }

    @Test
    fun systemStaysTheChosenTileNotTheThemeItResolvesTo() {
        pick("System")
        val settings = Screens.open<SettingsActivity>()
        val root = Screens.root(settings)
        assertTrue(Screens.tappable(root, "System")!!.isSelected)
        assertTrue(!Screens.tappable(root, "Light")!!.isSelected)
    }

    // A dialog asked on Settings, here Remove this account: light on Light, dark on a dark theme.
    private fun removeDialogIsLight(theme: String): Boolean {
        Store.setSelected(app, Store.saveExport(app, TestData.exportJson()))
        Store.setSkin(app, theme)
        val settings = Screens.open<SettingsActivity>()
        Screens.tappable(Screens.root(settings), "Remove this account")!!.performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val attrs = dialog.context.obtainStyledAttributes(intArrayOf(android.R.attr.isLightTheme))
        return attrs.getBoolean(0, false).also { attrs.recycle() }
    }

    @Test
    fun dialogsAreLightOnLight() {
        assertTrue(removeDialogIsLight("light"))
    }

    @Test
    fun dialogsStayDarkOnADarkTheme() {
        assertTrue(!removeDialogIsLight("clean"))
    }

    @Test
    fun everyScreenWordInTheLightThemeReadsOnItsBackground() {
        val s = Skin.LIGHT
        val words = mapOf(
            "text" to s.text, "dim" to s.dim, "title" to s.title, "accent" to s.accent, "green" to s.green,
            "idle" to s.idle, "helper" to s.helper, "goblin" to s.goblin, "secondaryText" to s.secondaryText,
        )
        val grounds = mapOf("bg" to s.bg, "card" to s.card, "chip" to s.chip, "sub" to s.sub, "runningBg" to s.runningBg)
        for ((word, colour) in words) {
            for ((ground, behind) in grounds) {
                val ratio = contrast(colour, behind)
                assertTrue("$word on $ground is ${"%.2f".format(ratio)}", ratio >= 4.5)
            }
        }
        assertTrue(contrast(s.runningText, s.runningBg) >= 4.5)
        assertTrue(contrast(s.primaryText, s.primaryBg) >= 4.5)
        assertTrue(contrast(s.onAccent, s.accent) >= 4.5)
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(colour: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((colour shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    @Test
    fun theWidgetPickersLeaveTheStatusBarClear() {
        val theme = app.resources.newTheme().apply { applyStyle(com.cocwidget.R.style.Translucent, true) }
        val bars = theme.obtainStyledAttributes(intArrayOf(android.R.attr.statusBarColor, android.R.attr.navigationBarColor))
        assertEquals(android.graphics.Color.TRANSPARENT, bars.getColor(0, -1))
        assertEquals(android.graphics.Color.TRANSPARENT, bars.getColor(1, -1))
        bars.recycle()
    }
}
