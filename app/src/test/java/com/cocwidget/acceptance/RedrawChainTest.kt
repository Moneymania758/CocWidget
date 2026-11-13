package com.cocwidget.acceptance

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.widget.Countdown
import com.cocwidget.widget.WidgetRenderer
import com.cocwidget.widget.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

// Each redraw books the next. If one is cut short before it does, the widget freezes: text times
// go stale and countdowns run past zero, as "5d -17:08". So the next redraw is booked first, and
// one widget failing to draw can't stop the others.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RedrawChainTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = AppWidgetManager.getInstance(app)

    private fun placed(tag: String): Int {
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        Store.setWidgetAccount(app, id, tag)
        return id
    }

    // A setting saved as the wrong type throws when it's read, so this widget can't be drawn.
    private fun broken(tag: String): Int = placed(tag).also { id ->
        Store.setWidgetStyle(app, id, WidgetStyle.TIMELINE)
        app.getSharedPreferences("village", Context.MODE_PRIVATE).edit().putInt("widget_theme_$id", 1).commit()
    }

    private fun alarms(action: String) = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        .filter { shadowOf(it.operation).savedIntent.action == action }

    @Test
    fun aWidgetThatCantDrawDoesntStopTheOthers() {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        broken(tag)
        val good = placed(tag)
        val before = Screens.texts(shadowOf(manager).getViewFor(good))
        WidgetRenderer.update(app) { null }
        val after = Screens.texts(shadowOf(manager).getViewFor(good))
        assertTrue("the healthy widget was drawn", after != before && after.contains(Store.label(app, tag)))
    }

    @Test
    fun aWidgetThatCantDrawStillGetsItsNextRedraw() {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        broken(tag)
        val start = System.currentTimeMillis()
        WidgetRenderer.update(app) { null }
        // The redraw alarm, not the ones for finishes and event reminders.
        val refresh = alarms(VillageWidget.ACTION_REFRESH).filter { shadowOf(it.operation).requestCode == 0 }
        assertEquals(1, refresh.size)
        assertTrue(refresh.single().triggerAtTime <= start + Countdown.MAX_REFRESH_MS + 5_000)
        assertEquals(1, alarms(VillageWidget.ACTION_TIMELINE_MINUTE).size)
        // The minute redraw too.
        WidgetRenderer.redrawTimelines(app)
        assertEquals(1, alarms(VillageWidget.ACTION_TIMELINE_MINUTE).size)
    }

    // Downloads stop once a refresh's time for them is up, and pick up next time.
    @Test
    fun picturesStopDownloadingWhenTheTimeIsUp() {
        val path = "buildings/home-village/cannon/level_1.webp"
        assertFalse(Icons.fetchPaths(app, listOf(path), deadline = 0))
        assertFalse(File(File(app.filesDir, "icons"), Icons.fileName(path)).exists())
    }
}
