package com.cocwidget.acceptance

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.os.Looper
import android.widget.Switch
import com.cocwidget.MainActivity
import com.cocwidget.R
import com.cocwidget.SettingsActivity
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.events.EventDates
import com.cocwidget.events.EventKind
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // Offline unless a test says otherwise.
    private fun check(now: Long, download: () -> String? = { null }) {
        WidgetRenderer.checkEvents(app, now, download)
    }

    @Test
    fun theFirstCheckPostsNothingEvenOnAnEventsStart() {
        check(clanGames)
        assertTrue(posted().isEmpty())
    }

    @Test
    fun itRemindsWithNoAccountAndNoWidget() {
        check(before)
        check(clanGames)

        val reminder = posted().single()
        assertEquals(app.getString(R.string.event_clan_games_started), reminder.title)
        assertEquals("events", reminder.channelId)
    }

    @Test
    fun itOnlyRemindsOnce() {
        check(before)
        check(clanGames)
        check(clanGames + hour)
        assertEquals(1, posted().size)
    }

    @Test
    fun aSwitchedOffEventStaysQuiet() {
        check(before)
        Store.setEventReminderOn(app, EventKind.CLAN_GAMES, false, before)
        check(clanGames)
        assertTrue(posted().isEmpty())
    }

    @Test
    fun switchingOnDoesntFireWhatHasAlreadyStarted() {
        val friday = at("2026-11-13T07:00:00Z")
        check(friday - 24 * hour)
        Store.setEventReminderOn(app, EventKind.RAID_WEEKEND, true, friday + hour)
        check(friday + 2 * hour)
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
        check(now)
        assertEquals(clanGames, alarms().minOf { it.triggerAtTime })
    }

    @Test
    fun theAlarmStillWakesDailyToCheckForNewDates() {
        val now = at("2026-11-17T12:00:00Z")
        check(now)
        assertEquals(now + 24 * hour, alarms().minOf { it.triggerAtTime })
    }

    @Test
    fun noAlarmWithEveryEventOff() {
        for (kind in EventKind.entries) Store.setEventReminderOn(app, kind, false, before)
        check(before)
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun aDownloadedDateIsUsed() {
        val friday = at("2026-12-05T08:00:00Z")
        check(friday - hour) {
            """{"version":1,"updated":"2099-01-01","events":[{"id":"clan_games:2026-12","kind":"clan_games","start":"2026-12-05T08:00:00Z","end":"2026-12-11T08:00:00Z"}]}"""
        }
        check(friday)
        assertEquals(app.getString(R.string.event_clan_games_started), posted().single().title)
    }

    @Test
    fun aBadDownloadKeepsTheDatesWeHave() {
        check(before) { "<html>rate limited</html>" }
        check(clanGames)
        assertEquals(1, posted().size)
        assertTrue(EventDates.current(app).events.any { it.kind == EventKind.HAMMER_JAM })
    }

    @Test
    fun aBadDownloadDoesntReplaceAGoodOne() {
        check(before) { """{"version":1,"updated":"2099-01-01","events":[]}""" }
        check(before + 13 * hour) { "<html>rate limited</html>" }
        assertEquals("2099-01-01", EventDates.current(app).updated)
    }

    @Test
    fun itOnlyDownloadsEveryTwelveHours() {
        var downloads = 0
        val counted = {
            downloads++
            null
        }
        check(before, counted)
        check(before + hour, counted)
        assertEquals(1, downloads)
        check(before + 13 * hour, counted)
        assertEquals(2, downloads)
    }

    // The way in on a real phone: the alarm, a restart and an app update all end up in update().
    @Test
    fun theWidgetRefreshChecksEventsWithNoWidgetPlaced() {
        WidgetRenderer.update(app) { null }
        val alarm = alarms().single()
        assertEquals(VillageWidget.ACTION_REFRESH, shadowOf(alarm.operation).savedIntent.action)
    }

    @Test
    fun theAlarmFiringPostsTheReminder() {
        check(System.currentTimeMillis() - 2 * hour)
        // A reminder an hour ago, so the real refresh finds it due.
        val soon = java.time.Instant.ofEpochMilli(System.currentTimeMillis() - hour).toString()
        // Downloading isn't due, so the real refresh below stays off the network.
        Store.setLastEventDatesCheck(app, System.currentTimeMillis())
        Store.setEventDates(
            app,
            """{"version":1,"updated":"2099-01-01","events":[{"id":"test","kind":"legend","start":"$soon"}]}""",
        )
        Store.setEventReminderOn(app, EventKind.LEGEND, true, System.currentTimeMillis() - 2 * hour)

        alarms().single().operation!!.send()
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
        WidgetRenderer.update(app) { null }
        assertTrue(alarms().size >= 2)
    }

    @Test
    fun remindersAndTheAlarmDontWaitForTheDownload() {
        check(before)
        var postedFirst = false
        var alarmSetFirst = false
        // The first check's alarm is earlier than this, so a later one means it was set again.
        check(clanGames) {
            postedFirst = posted().isNotEmpty()
            alarmSetFirst = alarms().minOf { it.triggerAtTime } > clanGames
            null
        }
        assertTrue(postedFirst)
        assertTrue(alarmSetFirst)
    }

    @Test
    fun aDateThatArrivesInADownloadSetsTheAlarmStraightAway() {
        val now = at("2026-11-17T12:00:00Z")
        val soon = now + hour
        check(now) {
            """{"version":1,"updated":"2099-01-01","events":[{"id":"clan_games:2026-11","kind":"clan_games","start":"${Instant.ofEpochMilli(soon)}","end":"2026-11-28T08:00:00Z"}]}"""
        }
        assertEquals(soon, alarms().minOf { it.triggerAtTime })
    }

    @Test
    fun withNotificationsRefusedNothingCountsAsSent() {
        val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
        check(before)
        manager.setNotificationsEnabled(false)
        check(clanGames)
        assertTrue(posted().isEmpty())

        // Allowed again within the 12 hours, so it still arrives.
        manager.setNotificationsEnabled(true)
        check(clanGames + hour)
        assertEquals(app.getString(R.string.event_clan_games_started), posted().single().title)
    }

    @Test
    fun settingsSaysWhenNotificationsAreRefused() {
        val warning = app.getString(R.string.event_notifications_off)
        assertFalse(warning in Screens.texts(Screens.root(Screens.open<SettingsActivity>())))

        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        val page = Screens.open<SettingsActivity>()
        assertTrue(warning in Screens.texts(Screens.root(page)))
        assertTrue(Screens.tappable(Screens.root(page), app.getString(R.string.event_notifications_open)) != null)
    }

    private fun countdownTexts(page: MainActivity) =
        Screens.texts(Screens.root(page)).filter { COUNTDOWN.containsMatchIn(it) }

    @Test
    fun theMainScreenCountsDownWithNoVillage() {
        val page = Screens.open<MainActivity>()
        assertTrue(app.getString(R.string.event_home_title) in Screens.texts(Screens.root(page)))
        assertEquals(3, countdownTexts(page).size)
    }

    @Test
    fun theMainScreenCountsDownWithAVillageToo() {
        Store.saveExport(app, TestData.th18ExportJson())
        val page = Screens.open<MainActivity>()
        assertTrue(app.getString(R.string.event_home_title) in Screens.texts(Screens.root(page)))
    }

    @Test
    fun showAllAddsTheRestAndShowFewerPutsThemBack() {
        val page = Screens.open<MainActivity>()
        val all = Screens.texts(Screens.root(page)).single { it.startsWith("Show all") }
        Screens.tappable(Screens.root(page), all)!!.performClick()
        val shown = countdownTexts(page).size
        assertTrue("$shown", shown > 3)
        assertEquals(all, app.getString(R.string.event_show_all, shown))
        Screens.tappable(Screens.root(page), app.getString(R.string.event_show_fewer))!!.performClick()
        assertEquals(3, countdownTexts(page).size)
    }

    @Test
    fun theRemindersLinkOpensSettings() {
        val page = Screens.open<MainActivity>()
        Screens.tappable(Screens.root(page), app.getString(R.string.event_home_settings))!!.performClick()
        assertEquals(SettingsActivity::class.java.name, shadowOf(page).nextStartedActivity.component?.className)
    }

    @Test
    fun theCountdownTicksWithAVillageToo() {
        Store.saveExport(app, TestData.th18ExportJson())
        val page = Screens.open<MainActivity>()
        val before = countdownTexts(page)
        assertEquals(3, before.size)
        Thread.sleep(1100)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3))
        assertTrue(before != countdownTexts(page))
    }

    @Test
    fun theCountdownTicks() {
        val page = Screens.open<MainActivity>()
        val before = countdownTexts(page)
        // The clocks read the real time, and the screen ticks on its handler.
        Thread.sleep(1100)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3))
        assertTrue(before != countdownTexts(page))
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

    private companion object {
        // Every wording an event row's clock can have, and nothing an upgrade's clock says.
        val COUNTDOWN = Regex("^(Starts in|Ends in|Sign-up (opens|closes) in|Season (starts|ends) in|Battles (open|end) in|New Tournament in) ")
    }
}
