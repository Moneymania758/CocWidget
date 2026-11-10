package com.cocwidget.acceptance

import android.appwidget.AppWidgetManager
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.cocwidget.MainActivity
import com.cocwidget.NextWidget
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Switching a widget between accounts with a tap, so one widget can serve several villages.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetSwitchTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = AppWidgetManager.getInstance(app)

    private fun big(id: Int): View {
        val tag = Store.widgetAccount(app, id)
        val village = tag?.let { Store.village(app, it) }
        return WidgetRenderer.build(app, id, tag, village, (village?.exportedAtMillis ?: 0) + 60_000).apply(app, FrameLayout(app))
    }

    // Taps a control, checks it asks VillageWidget to switch this widget, and carries it out.
    private fun tapSwitch(view: View, id: Int) {
        view.performClick()
        val sent = shadowOf(app).broadcastIntents.last()
        assertEquals(VillageWidget.ACTION_SWITCH_ACCOUNT, sent.action)
        assertEquals(VillageWidget::class.java.name, sent.component?.className)
        assertEquals(id, sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
        Store.switchWidgetAccount(app, sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }

    @Test
    fun theNextAccountGoesRoundInOrder() {
        val accounts = listOf("#A", "#B", "#C")
        assertEquals("#B", Store.nextAccount("#A", accounts))
        assertEquals("#C", Store.nextAccount("#B", accounts))
        assertEquals("#A", Store.nextAccount("#C", accounts))
        assertEquals("#A", Store.nextAccount("#GONE", accounts))
        assertNull(Store.nextAccount("#A", listOf("#A")))
        assertNull(Store.nextAccount(null, emptyList()))
    }

    @Test
    fun withOneAccountTheTitleIsPlainAndOpensTheApp() {
        Store.saveExport(app, TestData.exportJson())
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        val title = big(id).findViewById<TextView>(R.id.title)
        assertFalse(hasSwitch(title))
        title.performClick()
        assertEquals(MainActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
    }

    @Test
    fun tappingTheBigWidgetsTitleMovesItThroughEveryAccount() {
        val first = Store.saveExport(app, TestData.exportJson())
        val second = Store.saveExport(app, TestData.th18ExportJson())
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, id, first)

        val title = big(id).findViewById<TextView>(R.id.title)
        assertEquals(Store.label(app, first), title.text.toString())
        assertTrue(hasSwitch(title))
        tapSwitch(title, id)
        assertEquals(second, Store.widgetAccount(app, id))
        // The redrawn widget shows the second account and switches back to the first.
        val after = big(id)
        assertEquals(Store.label(app, second), after.findViewById<TextView>(R.id.title).text.toString())
        assertTrue(hasSwitch(after.findViewById(R.id.title)))
        assertEquals("TH18", after.findViewById<TextView>(R.id.town_hall).text.toString())
        tapSwitch(after.findViewById(R.id.title), id)
        assertEquals(first, Store.widgetAccount(app, id))
    }

    @Test
    fun switchingOneWidgetLeavesTheOthersAlone() {
        val first = Store.saveExport(app, TestData.exportJson())
        Store.saveExport(app, TestData.th18ExportJson())
        val a = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        val b = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, a, first)
        Store.setWidgetAccount(app, b, first)
        tapSwitch(big(a).findViewById(R.id.title), a)
        assertEquals(first, Store.widgetAccount(app, b))
    }

    @Test
    fun theSmallWidgetHasAnAccountChipOnlyWithTwoAccounts() {
        val first = Store.saveExport(app, TestData.exportJson())
        val id = shadowOf(manager).createWidget(NextWidget::class.java, R.layout.widget_next)
        fun small(): View {
            val tag = Store.widgetAccount(app, id)!!
            val village = Store.village(app, tag)!!
            return WidgetRenderer.buildNext(app, id, tag, village, village.exportedAtMillis + 60_000).apply(app, FrameLayout(app))
        }
        assertEquals(View.GONE, small().findViewById<View>(R.id.account).visibility)

        val second = Store.saveExport(app, TestData.th18ExportJson())
        Store.setWidgetAccount(app, id, first)
        val shown = small()
        val chip = shown.findViewById<View>(R.id.account)
        assertEquals(View.VISIBLE, chip.visibility)
        assertEquals(Store.label(app, first), shown.findViewById<TextView>(R.id.account_name).text.toString())
        tapSwitch(chip, id)
        assertEquals(second, Store.widgetAccount(app, id))
        assertEquals(Store.label(app, second), small().findViewById<TextView>(R.id.account_name).text.toString())
    }

    // A widget that was switched keeps no stale chip once an account is removed.
    @Test
    fun theChipGoesWhenOnlyOneAccountIsLeft() {
        val first = Store.saveExport(app, TestData.exportJson())
        val second = Store.saveExport(app, TestData.th18ExportJson())
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, id, second)
        Store.removeAccount(app, second)
        assertEquals(first, Store.widgetAccount(app, id))
        assertFalse(hasSwitch(big(id).findViewById(R.id.title)))
    }

    // An account whose export can't be read must not trap the widget on it.
    @Test
    fun theBigWidgetCanSwitchAwayFromAnAccountItCannotRead() {
        val first = Store.saveExport(app, TestData.exportJson())
        Store.saveExport(app, TestData.th18ExportJson())
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, id, first)
        val title = WidgetRenderer.build(app, id, first, null, 0).apply(app, FrameLayout(app)).findViewById<TextView>(R.id.title)
        assertTrue(hasSwitch(title))
        tapSwitch(title, id)
    }

    // A long name is cut short with "…" and keeps the switch arrow, the TH label and Import, on a
    // widget at its smallest default width. Native graphics, so text has its real size.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun aLongNameIsCutShortAndLeavesRoomForTheRest() {
        val first = Store.saveExport(app, TestData.exportJson())
        Store.saveExport(app, TestData.th18ExportJson())
        Store.setDisplayName(app, first, "My Main Farming Account Number One")
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, id, first)
        val root = big(id)
        val width = (250 * app.resources.displayMetrics.density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val title = root.findViewById<TextView>(R.id.title)
        val townHall = root.findViewById<TextView>(R.id.town_hall)
        val sync = root.findViewById<TextView>(R.id.sync)
        val row = sync.parent as View
        assertTrue(hasSwitch(title))
        assertTrue("the name is cut short", title.layout.getEllipsisCount(0) > 0)
        assertTrue("the TH label shows", townHall.width > 0)
        assertEquals("the TH label keeps its full width", townHall.measuredWidth, townHall.width)
        assertTrue("the TH label ends before Import", (townHall.parent as View).right <= (townHall.parent.parent as View).width)
        assertTrue("Import fits", sync.right <= row.width)
        assertEquals("Import keeps its full width", sync.measuredWidth, sync.width)
    }

    private fun hasSwitch(title: TextView) = title.compoundDrawablesRelative[2] != null
}
