package com.cocwidget.acceptance

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.game.Section
import com.cocwidget.game.Village
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import com.cocwidget.widget.WidgetView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private fun show(json: String, height: Int = 0, at: (Village) -> Long = { it.exportedAtMillis + 60_000 }): Shown {
        val tag = Store.saveExport(app, json)
        val village = Store.village(app, tag)!!
        val id = shadowOf(manager).createWidget(com.cocwidget.VillageWidget::class.java, R.layout.widget)
        if (height > 0) {
            manager.updateAppWidgetOptions(id, Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            })
        }
        val views = WidgetRenderer.build(app, id, tag, village, at(village))
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

    private fun headers(shown: Shown) = Screens.all(rows(shown)).filter { it.id == R.id.label }.map { (it as TextView).text.toString() }

    private fun showIn(view: WidgetView, json: String = TestData.exportJson(), at: (Village) -> Long = { it.exportedAtMillis + 60_000 }): Shown {
        Store.setWidgetView(app, Store.saveExport(app, json), view)
        return show(json, at = at)
    }

    @Test
    fun homeShowsOnlyTheHomeVillage() {
        val shown = showIn(WidgetView.HOME)
        val home = shown.village.upgrades.filter { it.section != Section.BUILDER_BASE }
        assertTrue("the fixture has the Builder Base", shown.village.upgrades.any { it.section == Section.BUILDER_BASE })
        assertFalse(headers(shown).any { it.startsWith("BUILDER BASE") })
        assertTrue(headers(shown).any { it.startsWith("BUILDERS") })
        assertEquals(home.size, Screens.all(rows(shown)).count { it.id == R.id.name })
        assertEquals(View.VISIBLE, shown.root.findViewById<View>(R.id.helpers).visibility)
    }

    @Test
    fun builderBaseShowsOnlyTheBuilderBase() {
        val shown = showIn(WidgetView.BUILDER_BASE)
        assertEquals(listOf(true), headers(shown).map { it.startsWith("BUILDER BASE") })
        assertEquals(shown.village.upgrades.count { it.section == Section.BUILDER_BASE }, Screens.all(rows(shown)).count { it.id == R.id.name })
        assertEquals(View.GONE, shown.root.findViewById<View>(R.id.helpers).visibility)
    }

    // The footer's finished count is for what's on screen, so home timers don't nag on the Builder Base.
    @Test
    fun theFooterCountsOnlyTheViewsFinishedUpgrades() {
        val village = TestData.village()
        val late = village.upgrades.maxOf { village.endMillis(it) } + 1
        val home = village.upgrades.count { it.section != Section.BUILDER_BASE }
        val line = WidgetRenderer.exportLine(app, village, late, WidgetView.HOME.shows)
        assertTrue(line.text, line.text.contains("$home finished since"))
    }

    @Test
    fun aViewPickedBeforeTheUpdateStays() {
        val tag = Store.saveExport(app, TestData.exportJson())
        Store.setCollapsedSections(app, tag, WidgetView.BUILDER_BASE.folded.map { it.name }.toSet())
        assertEquals(listOf(true), headers(show(TestData.exportJson())).map { it.startsWith("BUILDER BASE") })
    }

    // Folding by hand used to unlight the view and bring every other section back.
    @Test
    fun foldingASectionByHandKeepsTheView() {
        val tag = Store.saveExport(app, TestData.exportJson())
        Store.setWidgetView(app, tag, WidgetView.HOME)
        Store.toggleSection(app, tag, Section.LAB.name)
        val shown = show(TestData.exportJson())
        assertFalse(headers(shown).any { it.startsWith("BUILDER BASE") })
        assertTrue(headers(shown).any { it.startsWith("LAB") && it.contains("▸") })
    }

    @Test
    fun pickingAViewUnfoldsItsSections() {
        val tag = Store.saveExport(app, TestData.exportJson())
        Store.setCollapsedSections(app, tag, setOf(Section.BUILDER_BASE.name, Section.LAB.name))
        Store.setWidgetView(app, tag, WidgetView.BUILDER_BASE)
        assertEquals(setOf(Section.LAB.name), Store.collapsedSections(app, tag))
    }

    // Late enough for some home and some Builder Base upgrades to have finished, early enough for some to run.
    private fun halfway(village: Village): Long {
        val ends = village.upgrades.map { village.endMillis(it) }.sorted()
        return ends[ends.size / 2]
    }

    private fun clear(shown: Shown) = shown.root.findViewById<TextView>(R.id.clear)

    @Test
    fun theClearChipCountsWhatHasFinishedInTheView() {
        val village = TestData.village()
        val at = halfway(village)
        val home = village.upgrades.count { it.section != Section.BUILDER_BASE && village.endMillis(it) <= at }
        assertTrue("the fixture has home upgrades finished halfway", home > 0)
        val chip = clear(showIn(WidgetView.HOME, at = { at }))
        assertEquals(View.VISIBLE, chip.visibility)
        assertEquals(app.resources.getQuantityString(R.plurals.widget_clear_finished, home, home), chip.text.toString())
    }

    @Test
    fun noClearChipWhenNothingHasFinished() {
        assertEquals(View.GONE, clear(show(TestData.exportJson())).visibility)
    }

    @Test
    fun clearingOneViewKeepsTheOthersFinishedUpgrades() {
        val tag = Store.saveExport(app, TestData.exportJson())
        val village = Store.village(app, tag)!!
        val at = village.upgrades.maxOf { village.endMillis(it) }
        Store.clearFinished(app, tag, at, WidgetView.BUILDER_BASE.shows)
        val left = Store.village(app, tag)!!.upgrades
        assertFalse(left.any { it.section == Section.BUILDER_BASE })
        assertEquals(village.upgrades.count { it.section != Section.BUILDER_BASE }, left.size)
    }

    // 2.12.1 kept one clear time for the whole village. A clear made there must still hold.
    @Test
    fun aClearFromVersionTwoTwelveOneStillHolds() {
        val tag = Store.saveExport(app, TestData.exportJson())
        val village = Store.village(app, tag)!!
        val at = village.upgrades.maxOf { village.endMillis(it) }
        app.getSharedPreferences(Store.fileName(tag), android.content.Context.MODE_PRIVATE).edit().putLong("cleared_finished_at", at).commit()
        assertTrue(Store.village(app, tag)!!.upgrades.isEmpty())
        Store.saveExport(app, TestData.exportJson())
        assertEquals(village.upgrades.size, Store.village(app, tag)!!.upgrades.size)
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
