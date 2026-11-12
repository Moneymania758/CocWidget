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

    private fun small(size: WidgetRenderer.NextSize, theme: WidgetTheme = WidgetTheme.CLASSIC): View {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        val id = shadowOf(manager).createWidget(com.cocwidget.NextWidget::class.java, R.layout.widget_next)
        Store.setWidgetAccount(app, id, tag)
        Store.setWidgetTheme(app, id, theme)
        val village = Store.village(app, tag)!!
        return WidgetRenderer.buildNext(app, id, tag, village, village.exportedAtMillis + 60_000, size).apply(app, FrameLayout(app))
    }

    @Test
    fun theSmallWidgetDrawsEverySizeInEveryTheme() {
        for (size in WidgetRenderer.NextSize.entries) {
            for (theme in WidgetTheme.entries) {
                val root = small(size, theme)
                assertEquals("$size in $theme", app.getColor(theme.text), root.findViewById<TextView>(R.id.name).currentTextColor)
                assertTrue("$size in $theme ticks", root.findViewById<View>(R.id.timer).visibility == View.VISIBLE)
            }
        }
    }

    // Up next lists the three after the next with finish times, which never go out of date.
    @Test
    fun upNextShowsTheThreeAfterTheNext() {
        val root = small(WidgetRenderer.NextSize.UP_NEXT)
        val more = Screens.all(root.findViewById(R.id.next_more)).filter { it.id == R.id.more_at }
        assertEquals(3, more.size)
        assertTrue(more.all { (it as TextView).text.isNotEmpty() })
        assertTrue(Screens.views<Chronometer>(root.findViewById(R.id.next_more)).isEmpty())
    }

    @Test
    fun onUpNextAndGlanceOnlyTheOpenButtonOpensTheApp() {
        for (size in listOf(WidgetRenderer.NextSize.UP_NEXT, WidgetRenderer.NextSize.GLANCE)) {
            val root = small(size)
            assertFalse(root.findViewById<View>(R.id.widget_root).hasOnClickListeners())
            assertTrue(root.findViewById<View>(R.id.open).performClick())
            assertEquals(com.cocwidget.MainActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
        }
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

    // Backing out part way through setup leaves the widget as it was.
    @Test
    fun backingOutOfTheColoursKeepsTheOldLayoutToo() {
        val (id, _) = placed()
        val intent = android.content.Intent(app, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        val activity = Robolectric.buildActivity(WidgetConfigActivity::class.java, intent).setup().get()
        shadowOf((ShadowAlertDialog.getLatestAlertDialog() as AlertDialog).listView).performItemClick(WidgetStyle.TIMELINE.ordinal)
        shadowOf(Looper.getMainLooper()).idle()
        (ShadowAlertDialog.getLatestAlertDialog() as AlertDialog).cancel()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(WidgetStyle.SECTIONS, Store.widgetStyle(app, id))
        assertEquals(WidgetTheme.CLASSIC, Store.widgetTheme(app, id))
        assertTrue(activity.isFinishing)
    }

    // Material You's text follows the phone into dark mode without waiting for a redraw.
    @Test
    fun materialYouTextHasADarkModeColourToo() {
        val (id, tag) = placed()
        Store.setWidgetTheme(app, id, WidgetTheme.YOU)
        val village = Store.village(app, tag)!!
        val views = WidgetRenderer.build(app, id, tag, village, village.exportedAtMillis + 60_000)
        val nightConfig = android.content.res.Configuration(app.resources.configuration).apply {
            uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val night = app.createConfigurationContext(nightConfig)
        val ink = WidgetRenderer.Ink(app, WidgetTheme.YOU)
        assertTrue("day and night differ", ink.text.day != ink.text.night)
        assertEquals(ink.text.day, views.apply(app, FrameLayout(app)).findViewById<TextView>(R.id.title).currentTextColor)
        assertEquals(ink.text.night, views.apply(night, FrameLayout(night)).findViewById<TextView>(R.id.title).currentTextColor)
    }

    // A Timeline gets a minute alarm of its own, and only while one is on the home screen.
    @Test
    fun onlyATimelineSetsTheMinuteAlarm() {
        val (id, _) = placed()
        fun minuteAlarms() = shadowOf(app.getSystemService(android.app.AlarmManager::class.java)).scheduledAlarms
            .count { shadowOf(it.operation).savedIntent.action == VillageWidget.ACTION_TIMELINE_MINUTE }
        // Set before the first draw: Robolectric's stand-in launcher can't redraw one layout as another.
        Store.setWidgetStyle(app, id, WidgetStyle.TIMELINE)
        WidgetRenderer.update(app) { null }
        assertEquals(1, minuteAlarms())
        // No longer a Timeline, so the minute redraw draws nothing and stops its alarm.
        Store.setWidgetStyle(app, id, WidgetStyle.SECTIONS)
        WidgetRenderer.redrawTimelines(app)
        assertEquals(0, minuteAlarms())
    }

    // The small widget's Up next and Glance sizes show the potions running, with their time left.
    @Test
    fun theSmallWidgetShowsRunningPotions() {
        for (size in listOf(WidgetRenderer.NextSize.UP_NEXT, WidgetRenderer.NextSize.GLANCE)) {
            val boosts = small(size).findViewById<android.view.ViewGroup>(R.id.boosts)
            assertEquals("$size", View.VISIBLE, boosts.visibility)
            assertTrue("$size", boosts.childCount > 0)
        }
    }

    // Each extra row needs one row's height, above what this village's header and footer take.
    @Test
    fun pagesAskForOneRowsHeightPerRow() {
        val (_, tag) = placed()
        val village = Store.village(app, tag)!!
        val sizes = WidgetRenderer.pageSizes(village, village.exportedAtMillis + 60_000)
        assertEquals((1..12).toList(), sizes.map { it.second })
        assertTrue(sizes.zipWithNext().all { (a, b) -> b.first - a.first == 40f })
        val chrome = sizes.first().first - 40f
        assertTrue("chrome $chrome", chrome in 123f..193f)
        // Nothing running, no potions: the least chrome.
        val quiet = WidgetRenderer.pageSizes(village, village.exportedAtMillis + 400L * 24 * 3_600_000)
        assertTrue(quiet.first().first <= sizes.first().first)
    }

    // Material You's section headings carry no fixed colours, as those can't follow dark mode.
    @Test
    fun materialYouHeadingsHaveNoFixedColours() {
        val (id, tag) = placed()
        fun spans(theme: WidgetTheme): Int {
            Store.setWidgetTheme(app, id, theme)
            val labels = Screens.all(draw(id, tag)).filter { it.id == R.id.label }.map { (it as TextView).text }
            return labels.sumOf { text -> (text as? android.text.Spanned)?.getSpans(0, text.length, android.text.style.ForegroundColorSpan::class.java)?.size ?: 0 }
        }
        assertTrue(spans(WidgetTheme.CLASSIC) > 0)
        assertEquals(0, spans(WidgetTheme.YOU))
    }
}
