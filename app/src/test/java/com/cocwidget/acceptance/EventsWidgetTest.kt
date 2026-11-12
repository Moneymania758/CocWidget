package com.cocwidget.acceptance

import android.app.AlarmManager
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.EventsWidget
import com.cocwidget.EventsWidgetConfigActivity
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.events.EventDates
import com.cocwidget.events.EventKind
import com.cocwidget.store.Store
import com.cocwidget.widget.Countdown
import com.cocwidget.widget.WidgetRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.time.Instant

// The Events widget, asked for on Discord: pick which events it shows, and Reset to show them all.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EventsWidgetTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = AppWidgetManager.getInstance(app)

    // A Friday with Raid Weekend and Ranked on, Clan Games two days off, and Hammer Jam over.
    private val now = Instant.parse("2026-11-20T12:00:00Z").toEpochMilli()

    private fun place() = shadowOf(manager).createWidget(EventsWidget::class.java, R.layout.widget_events)

    private fun draw(id: Int, narrow: Boolean = false, short: Boolean = false): View =
        WidgetRenderer.buildEvents(app, id, now, EventDates.current(app), narrow, short).apply(app, FrameLayout(app))

    private fun names(widget: View): List<String> {
        val rows = widget.findViewById<ViewGroup>(R.id.event_rows)
        return (0 until rows.childCount).map { rows.getChildAt(it).findViewById<TextView>(R.id.event_name).text.toString() }
    }

    private fun timers(widget: View): List<String> {
        val rows = widget.findViewById<ViewGroup>(R.id.event_rows)
        return (0 until rows.childCount).map { rows.getChildAt(it).findViewById<Chronometer>(R.id.event_timer).text.toString() }
    }

    @Test
    fun itShowsEveryDatedEventSoonestFirstUntilThePlayerPicks() {
        val widget = draw(place())
        assertEquals(
            listOf("Clan Games", "Ranked", "Legend League", "Raid Weekend", "CWL sign-up", "Gold Pass"),
            names(widget),
        )
        // Hammer Jam ended on the 16th and has no next date, so it has no row.
        val timers = timers(widget)
        assertEquals("Starts in 1d 20:00:00", timers[0])
        assertEquals("Battles end in 2d 17:00:00", timers[1])
        assertEquals("Season ends in 10d 20:00:00", timers[5])
        assertEquals(View.GONE, widget.findViewById<View>(R.id.events_reset).visibility)
        assertEquals(View.GONE, widget.findViewById<View>(R.id.events_empty).visibility)
    }

    @Test
    fun aNarrowWidgetPutsEachCountdownUnderItsName() {
        val id = place()
        val narrow = draw(id, narrow = true)
        val wide = draw(id)
        assertEquals(names(wide), names(narrow))
        assertEquals(timers(wide), timers(narrow))
        val row = narrow.findViewById<ViewGroup>(R.id.event_rows).getChildAt(0)
        assertEquals(LinearLayout.VERTICAL, (row.findViewById<View>(R.id.event_name).parent as LinearLayout).orientation)
    }

    @Test
    fun hiddenEventsAreLeftOutAndResetBringsThemBack() {
        val id = place()
        Store.setHiddenEvents(app, id, setOf(EventKind.RANKED, EventKind.LEGEND))
        val widget = draw(id)
        assertEquals(listOf("Clan Games", "Raid Weekend", "CWL sign-up", "Gold Pass"), names(widget))

        val reset = widget.findViewById<TextView>(R.id.events_reset)
        assertEquals(View.VISIBLE, reset.visibility)
        assertEquals("Reset · 2 hidden", reset.text.toString())
        reset.performClick()
        val sent = shadowOf(app).broadcastIntents.last()
        assertEquals(VillageWidget.ACTION_SHOW_ALL_EVENTS, sent.action)
        assertEquals(VillageWidget::class.java.name, sent.component?.className)
        assertEquals(id, sent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))

        // What VillageWidget does with it.
        Store.setHiddenEvents(app, id, emptySet())
        assertEquals(6, names(draw(id)).size)
    }

    @Test
    fun eachWidgetKeepsItsOwnChoice() {
        val a = place()
        val b = place()
        Store.setHiddenEvents(app, a, setOf(EventKind.CLAN_GAMES))
        assertEquals(5, names(draw(a)).size)
        assertEquals(6, names(draw(b)).size)
        Store.forgetWidgets(app, intArrayOf(a))
        assertTrue(Store.hiddenEvents(app, a).isEmpty())
    }

    @Test
    fun withEveryEventHiddenItSaysSoAndOffersReset() {
        val id = place()
        Store.setHiddenEvents(app, id, EventKind.entries.toSet())
        for (short in listOf(false, true)) {
            val widget = draw(id, short = short)
            assertTrue(names(widget).isEmpty())
            val empty = widget.findViewById<TextView>(R.id.events_empty)
            assertEquals(View.VISIBLE, empty.visibility)
            assertEquals(app.getString(R.string.events_widget_all_hidden), empty.text.toString())
            // Hammer Jam has no date, so Reset would bring back six rows, not seven.
            assertEquals("Reset · 6 hidden", widget.findViewById<TextView>(R.id.events_reset).text.toString())
            assertEquals(View.VISIBLE, widget.findViewById<View>(R.id.events_reset).visibility)
        }
    }

    @Test
    fun hidingAnEventWithNoDateShowsNoResetChip() {
        val id = place()
        Store.setHiddenEvents(app, id, setOf(EventKind.HAMMER_JAM))
        val widget = draw(id)
        assertEquals(6, names(widget).size)
        assertEquals(View.GONE, widget.findViewById<View>(R.id.events_reset).visibility)
    }

    @Test
    fun aShortWidgetDropsTheHeaderAndResetSoARowFits() {
        val id = place()
        Store.setHiddenEvents(app, id, setOf(EventKind.RANKED))
        val widget = draw(id, short = true)
        assertEquals(View.GONE, widget.findViewById<View>(R.id.events_header).visibility)
        assertEquals(View.GONE, widget.findViewById<View>(R.id.events_reset).visibility)
        assertEquals("Clan Games", names(widget).first())
        assertEquals(View.VISIBLE, draw(id).findViewById<View>(R.id.events_header).visibility)
    }

    // A resize lands the new layout on top of the views on screen, so tall must undo what short set.
    @Test
    fun aShortWidgetMadeTallAgainGetsItsHeaderBack() {
        val id = place()
        val dates = EventDates.current(app)
        val widget = WidgetRenderer.buildEvents(app, id, now, dates, short = true).apply(app, FrameLayout(app))
        assertEquals(View.GONE, widget.findViewById<View>(R.id.events_header).visibility)
        WidgetRenderer.buildEvents(app, id, now, dates, short = false).reapply(app, widget)
        assertEquals(View.VISIBLE, widget.findViewById<View>(R.id.events_header).visibility)
        assertEquals((12 * app.resources.displayMetrics.density).toInt(), widget.paddingTop)
    }

    @Test
    fun aCountdownWithDaysKeepsItsHourInTheLastHourOfADay() {
        // Sign-up opens 1 Dec 08:00. Half an hour past 8 days before that, "8d 30:00" would read as 30 hours.
        val at = Instant.parse("2026-11-23T07:30:00Z").toEpochMilli()
        val id = place()
        val widget = WidgetRenderer.buildEvents(app, id, at, EventDates.current(app)).apply(app, FrameLayout(app))
        val row = names(widget).indexOf("CWL sign-up")
        assertEquals("Sign-up opens in 8d 0:30:00", timers(widget)[row])
    }

    @Test
    fun theBigWidgetKeepsTheHourToo() {
        val village = TestData.villageOf(now, TestData.upgrade(workSec = 86_400 + 1_800))
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        val widget = WidgetRenderer.build(app, id, village.tag, village, now).apply(app, FrameLayout(app))
        val row = widget.findViewById<ViewGroup>(R.id.rows).findViewById<View>(R.id.timer).parent as View
        assertEquals("1d", row.findViewById<TextView>(R.id.days).text.toString())
        assertEquals("0:30:00", row.findViewById<Chronometer>(R.id.timer).text.toString())
    }

    @Test
    fun editOpensThePickerForThisWidget() {
        val id = place()
        draw(id).findViewById<View>(R.id.events_edit).performClick()
        val started = shadowOf(app).nextStartedActivity
        assertEquals(EventsWidgetConfigActivity::class.java.name, started.component?.className)
        assertEquals(id, started.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }

    // As the widget's Edit chip opens it.
    private fun openPicker(id: Int): AlertDialog {
        val intent = Intent(app, EventsWidgetConfigActivity::class.java)
            .setData(android.net.Uri.parse("cocwidget://events/$id"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        Robolectric.buildActivity(EventsWidgetConfigActivity::class.java, intent).setup()
        return ShadowAlertDialog.getLatestAlertDialog() as AlertDialog
    }

    // The launcher's widget settings ask for the colours first, then the events.
    @Test
    fun theLaunchersSettingsPickATheme() {
        val id = place()
        val intent = Intent(app, EventsWidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        Robolectric.buildActivity(EventsWidgetConfigActivity::class.java, intent).setup()
        val themes = ShadowAlertDialog.getLatestAlertDialog() as AlertDialog
        assertEquals(com.cocwidget.widget.WidgetTheme.entries.size, themes.listView.adapter.count)
        shadowOf(themes.listView).performItemClick(com.cocwidget.widget.WidgetTheme.entries.indexOf(com.cocwidget.widget.WidgetTheme.LIGHT))
        shadowOf(Looper.getMainLooper()).idle()
        // Saved with the events, so backing out of the events list changes nothing.
        assertEquals(com.cocwidget.widget.WidgetTheme.CLASSIC, Store.widgetTheme(app, id))
        val events = ShadowAlertDialog.getLatestAlertDialog() as AlertDialog
        assertEquals(EventKind.entries.size, events.listView.adapter.count)
        events.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(com.cocwidget.widget.WidgetTheme.LIGHT, Store.widgetTheme(app, id))

        val widget = draw(id)
        assertEquals(app.getColor(R.color.w_light_accent), widget.findViewById<android.widget.TextView>(R.id.events_title).currentTextColor)
        val name = Screens.views<android.widget.TextView>(widget).first { it.id == R.id.event_name }
        assertEquals(app.getColor(R.color.w_light_text), name.currentTextColor)
    }

    @Test
    fun aNewEventsWidgetKeepsTheClassicColours() {
        val widget = draw(place())
        assertEquals(app.getColor(R.color.gold), widget.findViewById<android.widget.TextView>(R.id.events_title).currentTextColor)
        val name = Screens.views<android.widget.TextView>(widget).first { it.id == R.id.event_name }
        assertEquals(app.getColor(R.color.text), name.currentTextColor)
    }

    @Test
    fun thePickerHidesWhatIsUntickedAndResetShowsEverything() {
        val id = place()
        val dialog = openPicker(id)
        val list = dialog.listView
        assertEquals(EventKind.entries.size, list.adapter.count)
        // Every event starts ticked.
        assertTrue((0 until list.adapter.count).all { list.isItemChecked(it) })
        shadowOf(list).performItemClick(EventKind.entries.indexOf(EventKind.RAID_WEEKEND))
        shadowOf(list).performItemClick(EventKind.entries.indexOf(EventKind.LEGEND))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        // A dialog button's listener runs from a posted message.
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(setOf(EventKind.RAID_WEEKEND, EventKind.LEGEND), Store.hiddenEvents(app, id))

        // Opened again, the hidden ones are unticked, and Reset ticks them all.
        val again = openPicker(id)
        assertEquals(false, again.listView.isItemChecked(EventKind.entries.indexOf(EventKind.RAID_WEEKEND)))
        again.getButton(AlertDialog.BUTTON_NEUTRAL).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(Store.hiddenEvents(app, id).isEmpty())
    }

    @Test
    fun anEventsWidgetAloneStillGetsRedrawsAndDates() {
        place()
        EventKind.entries.forEach { Store.setEventReminderOn(app, it, false, now) }
        var downloads = 0
        WidgetRenderer.update(app) {
            downloads++
            null
        }
        // With every reminder off, the widget is what asks for the dates.
        assertEquals(1, downloads)
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertTrue(alarms.isNotEmpty())
    }

    @Test
    fun itRedrawsWhenACountdownReachesZero() {
        val minute = 60_000L
        // A day and 5 minutes away: the chronometer shows 1d and runs out in 5 minutes.
        assertEquals(now + 5 * minute, Countdown.nextEventRefresh(listOf(now + Countdown.DAY_MS + 5 * minute), now))
        assertEquals(now + 2 * minute, Countdown.nextEventRefresh(listOf(now + 3 * Countdown.DAY_MS, now + 2 * minute), now))
        assertEquals(now + Countdown.MAX_REFRESH_MS, Countdown.nextEventRefresh(emptyList(), now))
    }
}
