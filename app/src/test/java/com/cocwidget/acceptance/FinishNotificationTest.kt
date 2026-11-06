package com.cocwidget.acceptance

import android.app.Notification
import android.app.NotificationManager
import com.cocwidget.TestData
import com.cocwidget.game.Ids
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Finish notifications as the app posts them, from real exports whose first timer ended a minute ago.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinishNotificationTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private fun justFinished(json: String, tag: String): JSONObject {
        val village = TestData.village(json)
        val soonest = village.upgrades.minOf { village.endMillis(it) } - village.exportedAtMillis
        val exportedAt = (System.currentTimeMillis() - soonest - 60_000) / 1000
        return JSONObject(json).put("tag", tag).put("timestamp", exportedAt)
    }

    private fun posted(): List<Notification> =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    private val Notification.text get() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    @Test
    fun twoAccountsSayWhichNameAndTownHall() {
        val main = Store.saveExport(app, justFinished(TestData.exportJson(), "#MAIN").toString())
        Store.setDisplayName(app, main, "Ben main")
        Store.saveExport(app, justFinished(TestData.th18ExportJson(), "#SECOND").toString())

        WidgetRenderer.update(app)

        val texts = posted().map { it.text }
        assertTrue(texts.toString(), texts.any { it.startsWith("Ben main (TH16) · ") })
        assertTrue(texts.toString(), texts.any { it.startsWith("#SECOND (TH18) · ") })
        assertTrue(texts.toString(), texts.all { it.startsWith("Ben main (TH16) · ") || it.startsWith("#SECOND (TH18) · ") })
    }

    @Test
    fun oneAccountStaysAsItWas() {
        Store.saveExport(app, justFinished(TestData.exportJson(), "#MAIN").toString())

        WidgetRenderer.update(app)

        assertTrue(posted().isNotEmpty())
        for (notification in posted()) {
            assertTrue(notification.text, notification.text.endsWith(" is free") || notification.text.endsWith(" done"))
            assertTrue(notification.text, "TH" !in notification.text)
            assertNull(notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        }
    }

    @Test
    fun noTownHallInTheExportLeavesItOut() {
        val noHall = justFinished(TestData.exportJson(), "#MAIN")
        val buildings = noHall.getJSONArray("buildings")
        val kept = JSONArray()
        for (i in 0 until buildings.length()) {
            val building = buildings.getJSONObject(i)
            if (building.optInt("data") != Ids.TOWN_HALL) kept.put(building)
        }
        noHall.put("buildings", kept)
        val main = Store.saveExport(app, noHall.toString())
        Store.setDisplayName(app, main, "Ben main")
        Store.saveExport(app, justFinished(TestData.th18ExportJson(), "#SECOND").toString())

        WidgetRenderer.update(app)

        val named = posted().map { it.text }.filter { it.startsWith("Ben main") }
        assertTrue(named.isNotEmpty())
        assertEquals(emptyList<String>(), named.filter { "TH" in it })
        assertTrue(named.toString(), named.all { it.startsWith("Ben main · ") })
    }
}
