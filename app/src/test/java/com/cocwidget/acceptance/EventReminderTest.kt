package com.cocwidget.acceptance

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import android.widget.Switch
import com.cocwidget.R
import com.cocwidget.SettingsActivity
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.events.EventDates
import com.cocwidget.events.EventKind
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

// Event reminders as the app posts them, with no account imported and no widget placed.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EventReminderTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val hour = 3_600_000L
    private val before = at("2026-11-20T12:00:00Z")
    private val clanGames = at("2026-11-22T08:00:00Z")

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun posted(): List<Notification> =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    private val Notification.title get() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()

    private fun alarms() = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms

    @Before
    fun noNetwork() {
        EventDates.download = { null }
    }

    @After
    fun realNetwork() {
        EventDates.download = EventDates::fromGitHub
    }

    @Test
    fun theFirstCheckPostsNothingEvenOnAnEventsStart() {
        WidgetRenderer.checkEvents(app, clanGames)
        assertTrue(posted().isEmpty())
    }

    @Test
    fun itRemindsWithNoAccountAndNoWidget() {
        WidgetRenderer.checkEvents(app, before)
        WidgetRenderer.checkEvents(app, clanGames)

        val reminder = posted().single()
        assertEquals(app.getString(R.string.event_clan_games_started), reminder.title)
        assertEquals("events", reminder.channelId)
    }

    @Test
    fun itOnlyRemindsOnce() {
        WidgetRenderer.checkEvents(app, before)
        WidgetRenderer.checkEvents(app, clanGames)
        WidgetRenderer.checkEvents(app, clanGames + hour)
        assertEquals(1, posted().size)
    }

    @Test
    fun aSwitchedOffEventStaysQuiet() {
        WidgetRenderer.checkEvents(app, before)
        Store.setEventReminderOn(app, EventKind.CLAN_GAMES, false, before)
        WidgetRenderer.checkEvents(app, clanGames)
        assertTrue(posted().isEmpty())
    }

    @Test
    fun switchingOnDoesntFireWhatHasAlreadyStarted() {
        val friday = at("2026-11-13T07:00:00Z")
        WidgetRenderer.checkEvents(app, friday - 24 * hour)
        Store.setEventReminderOn(app, EventKind.RAID_WEEKEND, true, friday + hour)
        WidgetRenderer.checkEvents(app, friday + 2 * hour)
        assertTrue(posted().none { it.title == app.getString(R.string.event_raid_started) })
    }

    @Test
    fun theWeeklyOnesStartOffAndTheMonthlyOnesOn() {
        assertTrue(Store.eventReminderOn(app, EventKind.CLAN_GAMES))
        assertTrue(Store.eventReminderOn(app, EventKind.CWL))
        assertTrue(Store.eventReminderOn(app, EventKind.GOLD_PASS))
        assertTrue(Store.eventReminderOn(app, EventKind.HAMMER_JAM))
        assertFalse(Store.eventReminderOn(app, EventKind.RAID_WEEKEND))
        assertFalse(Store.eventReminderOn(app, EventKind.RANKED))
        assertFalse(Store.eventReminderOn(app, EventKind.LEGEND))
    }

    @Test
    fun theAlarmIsSetForTheNextReminderWithinADay() {
        val now = clanGames - 2 * hour
        WidgetRenderer.checkEvents(app, now)
        assertEquals(clanGames, alarms().minOf { it.triggerAtTime })
    }

    @Test
    fun theAlarmStillWakesDailyToCheckForNewDates() {
        val now = at("2026-11-17T12:00:00Z")
        WidgetRenderer.checkEvents(app, now)
        assertEquals(now + 24 * hour, alarms().minOf { it.triggerAtTime })
    }

    @Test
    fun noAlarmWithEveryEventOff() {
        for (kind in EventKind.entries) Store.setEventReminderOn(app, kind, false, before)
        WidgetRenderer.checkEvents(app, before)
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun aDownloadedDateIsUsed() {
        val friday = at("2026-12-05T08:00:00Z")
        EventDates.download = {
            """{"version":1,"updated":"2099-01-01","events":[{"id":"clan_games:2026-12","kind":"clan_games","start":"2026-12-05T08:00:00Z","end":"2026-12-11T08:00:00Z"}]}"""
        }
        WidgetRenderer.checkEvents(app, friday - hour)
        WidgetRenderer.checkEvents(app, friday)
        assertEquals(app.getString(R.string.event_clan_games_started), posted().single().title)
    }

    @Test
    fun aBadDownloadKeepsTheDatesWeHave() {
        EventDates.download = { "<html>rate limited</html>" }
        WidgetRenderer.checkEvents(app, before)
        WidgetRenderer.checkEvents(app, clanGames)
        assertEquals(1, posted().size)
        assertTrue(EventDates.current(app).events.any { it.kind == EventKind.HAMMER_JAM })
    }

    @Test
    fun aBadDownloadDoesntReplaceAGoodOne() {
        EventDates.download = { """{"version":1,"updated":"2099-01-01","events":[]}""" }
        WidgetRenderer.checkEvents(app, before)
        EventDates.download = { "<html>rate limited</html>" }
        WidgetRenderer.checkEvents(app, before + 13 * hour)
        assertEquals("2099-01-01", EventDates.current(app).updated)
    }

    @Test
    fun itOnlyDownloadsEveryTwelveHours() {
        var downloads = 0
        EventDates.download = {
            downloads++
            null
        }
        WidgetRenderer.checkEvents(app, before)
        WidgetRenderer.checkEvents(app, before + hour)
        assertEquals(1, downloads)
        WidgetRenderer.checkEvents(app, before + 13 * hour)
        assertEquals(2, downloads)
    }

    // The way in on a real phone: the alarm, a restart and an app update all end up in update().
    @Test
    fun theWidgetRefreshChecksEventsWithNoWidgetPlaced() {
        WidgetRenderer.update(app)
        val alarm = alarms().single()
        assertEquals(VillageWidget.ACTION_REFRESH, shadowOf(alarm.operation).savedIntent.action)
    }

    @Test
    fun theAlarmFiringPostsTheReminder() {
        WidgetRenderer.checkEvents(app, System.currentTimeMillis() - 2 * hour)
        // A reminder an hour ago, so the real refresh finds it due.
        val soon = java.time.Instant.ofEpochMilli(System.currentTimeMillis() - hour).toString()
        EventDates.download = { null }
        Store.setEventDates(
            app,
            """{"version":1,"updated":"2099-01-01","events":[{"id":"test","kind":"legend","start":"$soon"}]}""",
        )
        Store.setEventReminderOn(app, EventKind.LEGEND, true, System.currentTimeMillis() - 2 * hour)

        VillageWidget().onReceive(app, Intent(app, VillageWidget::class.java).setAction(VillageWidget.ACTION_REFRESH))
        shadowOf(Looper.getMainLooper()).idle()
        val deadline = System.currentTimeMillis() + 5000
        while (posted().isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(20)

        assertEquals(app.getString(R.string.event_legend_started), posted().single().title)
    }

    @Test
    fun theEventAlarmDoesntReplaceTheFinishAlarm() {
        val now = System.currentTimeMillis()
        val json = TestData.exportJson()
        val exportedAt = now / 1000
        Store.saveExport(app, JSONObject(json).put("timestamp", exportedAt).toString())
        WidgetRenderer.update(app)
        assertTrue(alarms().size >= 2)
    }

    @Test
    fun settingsListsEveryEventWithItsSwitch() {
        val page = Screens.open<SettingsActivity>()
        val texts = Screens.texts(Screens.root(page))
        assertTrue(app.getString(R.string.event_reminders_title) in texts)
        for (kind in EventKind.entries) assertTrue(app.getString(kind.label), app.getString(kind.label) in texts)
    }

    @Test
    fun aSwitchInSettingsSavesTheChoice() {
        val page = Screens.open<SettingsActivity>()
        val label = app.getString(EventKind.RAID_WEEKEND.label)
        val switch = Screens.views<Switch>(Screens.root(page)).single { it.contentDescription == label }
        assertFalse(switch.isChecked)
        switch.performClick()
        assertTrue(Store.eventReminderOn(app, EventKind.RAID_WEEKEND))
    }
}
