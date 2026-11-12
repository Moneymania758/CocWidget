package com.cocwidget.acceptance

import android.appwidget.AppWidgetManager
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import com.cocwidget.AccountsWidget
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.store.Store
import com.cocwidget.widget.NextFinish
import com.cocwidget.widget.WidgetRenderer
import com.cocwidget.widget.WidgetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// The Accounts widget: every account in one list, with what's idle and its next finish.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountsWidgetTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = AppWidgetManager.getInstance(app)

    private fun place() = shadowOf(manager).createWidget(AccountsWidget::class.java, R.layout.widget_accounts)

    private fun threeAccounts(): List<String> = listOf(TestData.exportJson(), TestData.th18ExportJson(), TestData.superchargingExportJson())
        .map { Store.saveExport(app, it) }

    // A moment when the fixtures have upgrades running: a minute after the latest export.
    private fun busyMoment(tags: List<String>) = tags.maxOf { Store.village(app, it)!!.exportedAtMillis } + 60_000

    private fun row(tag: String, now: Long, theme: WidgetTheme = WidgetTheme.DARK): View =
        WidgetRenderer.accountRow(app, tag, Store.village(app, tag), now, WidgetRenderer.Ink(app, theme)).apply(app, FrameLayout(app))

    private fun text(view: View, id: Int) = view.findViewById<TextView>(id).text.toString()

    @Test
    fun withNoAccountItSaysToImport() {
        val widget = WidgetRenderer.buildAccounts(app, place(), System.currentTimeMillis()).apply(app, FrameLayout(app))
        assertEquals(View.VISIBLE, widget.findViewById<View>(R.id.accounts_empty).visibility)
        assertEquals(app.getString(R.string.accounts_widget_empty), text(widget, R.id.accounts_empty))
        assertEquals(View.GONE, widget.findViewById<View>(R.id.accounts_list).visibility)
    }

    @Test
    fun everyAccountGetsARow() {
        val tags = threeAccounts()
        val widget = WidgetRenderer.buildAccounts(app, place(), busyMoment(tags)).apply(app, FrameLayout(app))
        assertEquals(View.GONE, widget.findViewById<View>(R.id.accounts_empty).visibility)
        assertEquals(View.VISIBLE, widget.findViewById<View>(R.id.accounts_list).visibility)
        assertEquals(tags.toSet(), WidgetRenderer.accountsInOrder(app, busyMoment(tags)).map { it.first }.toSet())
    }

    // Anything idle first, as that needs the player now, then whichever finishes soonest.
    @Test
    fun idleAccountsComeFirstThenTheSoonestFinish() {
        val tags = threeAccounts()
        for (now in listOf(busyMoment(tags), busyMoment(tags) + 3L * 24 * 3_600_000)) {
            val order = WidgetRenderer.accountsInOrder(app, now).map { (_, village) -> NextFinish.of(village!!, now) to village }
            for ((a, b) in order.zipWithNext()) {
                val (first, firstVillage) = a
                val (second, secondVillage) = b
                assertTrue("idle before busy", first.anyIdle || !second.anyIdle)
                if (first.anyIdle == second.anyIdle) {
                    val firstEnd = first.next?.let(firstVillage::endMillis) ?: Long.MAX_VALUE
                    val secondEnd = second.next?.let(secondVillage::endMillis) ?: Long.MAX_VALUE
                    assertTrue("soonest first", firstEnd <= secondEnd)
                }
            }
        }
    }

    @Test
    fun aRowNamesTheAccountItsIdleSlotsAndItsNextFinish() {
        val tags = threeAccounts()
        val now = busyMoment(tags)
        for (tag in tags) {
            val village = Store.village(app, tag)!!
            val summary = NextFinish.of(village, now)
            val row = row(tag, now)
            assertEquals(Store.label(app, tag), text(row, R.id.acc_name))
            assertTrue("no countdowns in a scrolling list", Screens.views<Chronometer>(row).isEmpty())
            val idle = row.findViewById<TextView>(R.id.acc_idle)
            assertEquals(if (summary.anyIdle) View.VISIBLE else View.GONE, idle.visibility)
            val next = summary.next
            if (next != null) {
                assertEquals(WidgetRenderer.upgradeName(next).toString(), text(row, R.id.acc_next))
                assertTrue(text(row, R.id.acc_left), Regex("""^\d+(d \d+h|h \d+m|m)$""").matches(text(row, R.id.acc_left)))
                assertTrue(text(row, R.id.acc_at).isNotEmpty())
            }
        }
        assertTrue("the fixtures have something running", tags.any { NextFinish.of(Store.village(app, it)!!, now).next != null })
    }

    @Test
    fun anAccountWhoseExportCantBeReadComesLastAndSaysSo() {
        val tags = threeAccounts()
        val broken = tags.first()
        app.getSharedPreferences(Store.fileName(broken), 0).edit().putString("export", "{ not json").commit()
        val now = busyMoment(tags.drop(1))
        assertEquals(broken, WidgetRenderer.accountsInOrder(app, now).last().first)
        assertEquals(app.getString(R.string.account_unreadable), text(row(broken, now), R.id.acc_next))
    }

    @Test
    fun itTakesTheWidgetsTheme() {
        val tags = threeAccounts()
        val id = place()
        Store.setWidgetTheme(app, id, WidgetTheme.LIGHT)
        val widget = WidgetRenderer.buildAccounts(app, id, busyMoment(tags)).apply(app, FrameLayout(app))
        assertEquals(app.getColor(WidgetTheme.LIGHT.accent), widget.findViewById<TextView>(R.id.accounts_title).currentTextColor)
        assertEquals(app.getColor(WidgetTheme.LIGHT.text), row(tags.first(), busyMoment(tags), WidgetTheme.LIGHT).findViewById<TextView>(R.id.acc_name).currentTextColor)
    }

    // Its times are text, so like a Timeline it's redrawn each minute.
    @Test
    fun itSetsTheMinuteAlarm() {
        threeAccounts()
        fun minuteAlarms() = shadowOf(app.getSystemService(android.app.AlarmManager::class.java)).scheduledAlarms
            .count { shadowOf(it.operation).savedIntent.action == VillageWidget.ACTION_TIMELINE_MINUTE }
        WidgetRenderer.update(app) { null }
        assertEquals(0, minuteAlarms())
        place()
        WidgetRenderer.update(app) { null }
        assertEquals(1, minuteAlarms())
    }
}
