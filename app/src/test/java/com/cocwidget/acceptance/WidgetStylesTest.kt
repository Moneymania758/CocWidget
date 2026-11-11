package com.cocwidget.acceptance

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.os.Looper
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.WidgetConfigActivity
import com.cocwidget.game.UpNext
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import com.cocwidget.widget.WidgetStyle
import com.cocwidget.widget.WidgetTheme
import com.cocwidget.widget.WidgetView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

// The big widget's three layouts and four colour themes, as a launcher gets them.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetStylesTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = AppWidgetManager.getInstance(app)

    private fun placed(json: String = TestData.th18ExportJson()): Pair<Int, String> {
        val tag = Store.saveExport(app, json)
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, id, tag)
        return id to tag
    }

    private fun draw(id: Int, tag: String, perPage: Int = 4): View {
        val village = Store.village(app, tag)!!
        return WidgetRenderer.build(app, id, tag, village, village.exportedAtMillis + 60_000, perPage).apply(app, FrameLayout(app))
    }

    private fun visible(root: View, id: Int) = root.findViewById<View>(id).visibility == View.VISIBLE

    @Test
    fun aWidgetPlacedBeforeThereWasAChoiceKeepsSectionsInClassic() {
        val (id, _) = placed()
        assertEquals(WidgetStyle.SECTIONS, Store.widgetStyle(app, id))
        assertEquals(WidgetTheme.CLASSIC, Store.widgetTheme(app, id))
    }

    @Test
    fun everyStyleDrawsInEveryTheme() {
        val (id, tag) = placed()
        for (style in WidgetStyle.entries) {
            for (theme in WidgetTheme.entries) {
                Store.setWidgetStyle(app, id, style)
                Store.setWidgetTheme(app, id, theme)
                val root = draw(id, tag)
                assertEquals("$style in $theme", app.getColor(theme.text), root.findViewById<TextView>(R.id.title).currentTextColor)
            }
        }
    }

    @Test
    fun sectionsTicksEveryRowWithItsFinishTime() {
        val (id, tag) = placed()
        val root = draw(id, tag)
        val village = Store.village(app, tag)!!
        val running = village.upgrades.count { village.endMillis(it) > village.exportedAtMillis + 60_000 }
        val ticking = Screens.views<Chronometer>(root.findViewById(R.id.rows)).count { it.id == R.id.timer && it.visibility == View.VISIBLE }
        assertTrue("the fixture has upgrades running", running > 0)
        assertEquals(running, ticking)
        val finishes = Screens.all(root.findViewById(R.id.rows)).filter { it.id == R.id.finish && it.visibility == View.VISIBLE }
        assertEquals(running, finishes.size)
        assertTrue(finishes.all { (it as TextView).text.isNotEmpty() })
        assertFalse(visible(root, R.id.timeline_list))
    }

    @Test
    fun theHeaderSaysWhatIsIdle() {
        val (id, tag) = placed()
        val root = draw(id, tag)
        assertTrue(visible(root, R.id.chips))
        assertTrue(visible(root, R.id.chip_1))
    }

    @Test
    fun timelineTicksTheNextOnACardAndListsTheRest() {
        val (id, tag) = placed()
        Store.setWidgetStyle(app, id, WidgetStyle.TIMELINE)
        val root = draw(id, tag)
        assertTrue(visible(root, R.id.hero))
        assertTrue(root.findViewById<View>(R.id.hero_timer) is Chronometer)
        assertTrue(visible(root, R.id.timeline_list))
        assertFalse(visible(root, R.id.rows))
    }

    // On One UI a countdown inside a scrolling list stops between redraws, so the list's rows
    // carry text only, and every upgrade still gets a row.
    @Test
    fun theTimelinesListHasARowForEachUpgradeAndNoCountdowns() {
        val (_, tag) = placed()
        val village = Store.village(app, tag)!!
        val now = village.exportedAtMillis + 60_000
        val items = WidgetRenderer.timelineItems(village, WidgetView.ALL, now)
        val rows = WidgetRenderer.timelineRows(app, village, items, now, WidgetRenderer.Ink(app, WidgetTheme.DARK)).map { it.apply(app, FrameLayout(app)) }
        assertTrue(rows.none { row -> Screens.views<Chronometer>(row).isNotEmpty() })
        val names = rows.flatMap { Screens.all(it) }.filter { it.id == R.id.tl_name }.map { (it as TextView).text.toString() }
        assertEquals(items.size, names.size)
        assertEquals(village.upgrades.size, items.count { it is UpNext.Item.Running })
        val left = rows.flatMap { Screens.all(it) }.filter { it.id == R.id.tl_left }.map { (it as TextView).text.toString() }
        assertTrue(left.any { Regex("""^\d+(d \d+h|h \d+m|m)$""").matches(it) })
    }

    @Test
    fun pagesShowAPageAtATimeAndTheArrowsMoveThroughThem() {
        val (id, tag) = placed()
        Store.setWidgetStyle(app, id, WidgetStyle.PAGES)
        val village = Store.village(app, tag)!!
        val pages = (village.upgrades.size + 1) / 2
        assertTrue("the fixture fills more than one page", pages > 1)

        val first = draw(id, tag, perPage = 2)
        assertEquals(2, Screens.all(first.findViewById(R.id.rows)).count { it.id == R.id.name })
        assertEquals("1/$pages", first.findViewById<TextView>(R.id.page_label).text.toString())
        assertFalse(first.findViewById<View>(R.id.page_up).isEnabled)

        first.findViewById<View>(R.id.page_down).performClick()
        val sent = shadowOf(app).broadcastIntents.last()
        assertEquals(VillageWidget.ACTION_SET_PAGE, sent.action)
        assertEquals(1, sent.getIntExtra(VillageWidget.EXTRA_PAGE, -1))
        Store.setWidgetPage(app, id, sent.getIntExtra(VillageWidget.EXTRA_PAGE, 0))
        val second = draw(id, tag, perPage = 2)
        assertEquals("2/$pages", second.findViewById<TextView>(R.id.page_label).text.toString())
        assertTrue(second.findViewById<View>(R.id.page_up).isEnabled)
    }

    @Test
    fun aPageLeftPastTheEndComesBackToTheLast() {
        val (id, tag) = placed()
        Store.setWidgetStyle(app, id, WidgetStyle.PAGES)
        Store.setWidgetPage(app, id, 99)
        val village = Store.village(app, tag)!!
        val pages = (village.upgrades.size + 1) / 2
        assertEquals("$pages/$pages", draw(id, tag, perPage = 2).findViewById<TextView>(R.id.page_label).text.toString())
        assertFalse(draw(id, tag, perPage = 2).findViewById<View>(R.id.page_down).isEnabled)
    }

    @Test
    fun placingABigWidgetAsksForTheLayoutThenTheColours() {
        val (id, _) = placed()
        val intent = android.content.Intent(app, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        val activity = Robolectric.buildActivity(WidgetConfigActivity::class.java, intent).setup().get()

        val styles = (ShadowAlertDialog.getLatestAlertDialog() as AlertDialog).listView
        assertEquals(WidgetStyle.entries.size, styles.adapter.count)
        shadowOf(styles).performItemClick(WidgetStyle.TIMELINE.ordinal)
        shadowOf(Looper.getMainLooper()).idle()

        val themes = (ShadowAlertDialog.getLatestAlertDialog() as AlertDialog).listView
        assertEquals(WidgetTheme.entries.size, themes.adapter.count)
        shadowOf(themes).performItemClick(WidgetTheme.LIGHT.ordinal)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(WidgetStyle.TIMELINE, Store.widgetStyle(app, id))
        assertEquals(WidgetTheme.LIGHT, Store.widgetTheme(app, id))
        assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun aRemovedWidgetForgetsItsLayoutColoursAndPage() {
        val (id, _) = placed()
        Store.setWidgetStyle(app, id, WidgetStyle.PAGES)
        Store.setWidgetTheme(app, id, WidgetTheme.YOU)
        Store.setWidgetPage(app, id, 3)
        Store.forgetWidgets(app, intArrayOf(id))
        assertEquals(WidgetStyle.SECTIONS, Store.widgetStyle(app, id))
        assertEquals(WidgetTheme.CLASSIC, Store.widgetTheme(app, id))
        assertEquals(0, Store.widgetPage(app, id))
    }
}
