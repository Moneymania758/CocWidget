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
        val views = WidgetRenderer.build(app, id, tag, village, village.exportedAtMillis + 60_000)
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

    // A Xiaomi launcher reported far less than the widget's real height, so the widget said "+3 more"
    // with nothing left to show. Nothing it says may depend on the height a launcher reports.
    @Test
    fun whateverHeightTheLauncherReportsTheWidgetClaimsNothingAboutIt() {
        for (height in listOf(0, 110, 240, 360, 480, 720, 2000)) {
            val root = show(TestData.exportJson(), height).root
            val link = root.findViewById<TextView>(R.id.all_upgrades)
            assertEquals("at $height dp", View.VISIBLE, link.visibility)
            assertEquals("All upgrades ›", link.text.toString())
            val claims = Screens.texts(root).filter { Regex("""\+\d+ more""").containsMatchIn(it) }
            assertEquals("at $height dp", emptyList<String>(), claims)
        }
    }

    @Test
    fun theAllUpgradesLinkOpensTheMainScreenOnItsAccount() {
        val shown = show(TestData.exportJson())
        shown.root.findViewById<View>(R.id.all_upgrades).performClick()
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(com.cocwidget.MainActivity::class.java.name, intent.component?.className)
        assertEquals(shown.village.tag, intent.getStringExtra(com.cocwidget.MainActivity.EXTRA_ACCOUNT))
    }

    @Test
    fun aWidgetWithNoAccountHasNoLink() {
        val id = shadowOf(manager).createWidget(com.cocwidget.VillageWidget::class.java, R.layout.widget)
        val root = WidgetRenderer.build(app, id, null, null, 0).apply(app, FrameLayout(app))
        assertEquals(View.GONE, root.findViewById<View>(R.id.all_upgrades).visibility)
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
        val root = WidgetRenderer.build(app, id, null, null, 0).apply(app, FrameLayout(app))
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.empty).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.footer).visibility)
    }
}
