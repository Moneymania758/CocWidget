package com.cocwidget.acceptance

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.game.Village
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetFit
import com.cocwidget.widget.WidgetRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// The big widget as a launcher gets it: real RemoteViews, applied to real views. Whether a launcher
// then fits them on screen still needs a phone.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BigWidgetTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = AppWidgetManager.getInstance(app)

    private class Shown(val root: View, val village: Village)

    // A minute after the export, as if just synced, at a height in dp. 0 leaves the height unknown.
    private fun show(json: String, height: Int = 0): Shown {
        val tag = Store.saveExport(app, json)
        val village = Store.village(app, tag)!!
        val id = shadowOf(manager).createWidget(com.cocwidget.VillageWidget::class.java, R.layout.widget)
        if (height > 0) {
            manager.updateAppWidgetOptions(id, Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            })
        }
        val views = WidgetRenderer.build(app, manager, id, tag, village, village.exportedAtMillis + 60_000)
        return Shown(views.apply(app, FrameLayout(app)), village)
    }

    private fun rows(shown: Shown) = shown.root.findViewById<LinearLayout>(R.id.rows)

    private fun drawn(shown: Shown, id: Int) = Screens.all(rows(shown)).count { it.id == id && it.visibility == View.VISIBLE }

    // 2.7.3 left rows out at normal sizes, and a 360 dp widget showed no upgrades at all. Whatever
    // height a launcher reports, every upgrade must be drawn.
    @Test
    fun everyUpgradeIsDrawnAtEveryHeight() {
        for (height in listOf(0, 110, 240, 360, 480, 720, 2000)) {
            val shown = show(TestData.exportJson(), height)
            assertEquals("at $height dp", shown.village.upgrades.size, Screens.all(rows(shown)).count { it.id == R.id.name })
        }
    }

    @Test
    fun theHintShowsOnlyWhenRowsDontFit() {
        val more = { height: Int -> show(TestData.exportJson(), height).root.findViewById<TextView>(R.id.more) }
        assertEquals(View.GONE, more(0).visibility)
        assertEquals(View.GONE, more(2000).visibility)
        val short = more(240)
        assertEquals(View.VISIBLE, short.visibility)
        assertTrue(short.text.toString(), short.text.startsWith("+") && short.text.endsWith("more ›"))
    }

    @Test
    fun aRecentExportShowsTheRightHelpersFace() {
        val shown = show(TestData.th18ExportJson())
        val now = shown.village.exportedAtMillis + 60_000
        val helped = shown.village.upgrades.filter { shown.village.endMillis(it) > now && shown.village.isHelped(it, now) }
        assertTrue("the fixture has helpers running", helped.isNotEmpty())
        assertEquals(helped.size, drawn(shown, R.id.helper))
    }

    @Test
    fun aRunningBoostShowsItsChip() {
        val shown = show(TestData.th18ExportJson())
        assertTrue(drawn(shown, R.id.badge_chip) + drawn(shown, R.id.badge_snack_chip) > 0)
    }

    @Test
    fun viewButtonsShowWhenThereIsABuilderBase() {
        assertEquals(View.VISIBLE, show(TestData.exportJson()).root.findViewById<View>(R.id.views).visibility)
    }

    @Test
    fun theFooterAlwaysSaysWhenItWasExported() {
        val footer = show(TestData.exportJson()).root.findViewById<TextView>(R.id.footer)
        assertEquals(View.VISIBLE, footer.visibility)
        assertTrue(footer.text.toString(), footer.text.startsWith("Exported"))
    }

    @Test
    fun aWidgetWithNoAccountShowsTheSetupText() {
        val id = shadowOf(manager).createWidget(com.cocwidget.VillageWidget::class.java, R.layout.widget)
        val root = WidgetRenderer.build(app, manager, id, null, null, 0).apply(app, FrameLayout(app))
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.empty).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.footer).visibility)
    }

    // What a player can read at sizes people really use, with a real TH16 village. The table in the
    // 2.7.4 fix: the rows a widget shows must never fall behind what 2.7.2 showed.
    @Test
    fun aNormalWidgetShowsAsManyWholeRowsAsFit() {
        val text: (Int) -> Int = { sp -> kotlin.math.ceil(sp * 1.327).toInt() }
        val sections = listOf(WidgetFit.Section(6, false), WidgetFit.Section(1, false), WidgetFit.Section(1, false), WidgetFit.Section(4, false))
        val expectedRowsAtLeast = mapOf(240 to 1, 300 to 3, 360 to 4, 420 to 6, 480 to 7, 540 to 8, 600 to 9, 720 to 12)
        for ((height, rows) in expectedRowsAtLeast) {
            val room = WidgetFit.roomForRows(height, viewButtons = true, helpers = true, text)
            val hidden = WidgetFit.hint(sections, room, WidgetFit.headerDp(text))?.hidden ?: 0
            assertTrue("$height dp shows ${12 - hidden}", 12 - hidden >= rows)
        }
    }
}
