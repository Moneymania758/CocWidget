package com.cocwidget.acceptance

import android.os.Looper
import android.view.MotionEvent
import com.cocwidget.PlannerActivity
import com.cocwidget.ProgressActivity
import com.cocwidget.TestData
import com.cocwidget.plan.Progress
import com.cocwidget.store.ImportHistory
import com.cocwidget.store.ProgressHistory
import com.cocwidget.store.Store
import com.cocwidget.ui.ProgressChart
import com.cocwidget.ui.ProgressText
import com.cocwidget.ui.Skin
import com.cocwidget.ui.TimeText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

// The Progress card on Time to max and the page it opens, from the imports kept.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressScreenTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val day = 86_400L
    private val queen = 28000001
    private val tag = "#TEST0000"

    // Midday, so a few hours either way stays on the same day.
    private val today = LocalDate.of(2026, 11, 10).atTime(12, 0).atZone(ZoneId.systemDefault()).toEpochSecond()

    // The TH16 export taken at [seconds] with the Archer Queen at [queenLevel] (95 in the export).
    private fun export(seconds: Long, queenLevel: Int = 95, json: String = TestData.exportJson()): String {
        val root = JSONObject(json).put("timestamp", seconds).put("tag", tag)
        root.optJSONArray("heroes")?.let { heroes ->
            for (i in 0 until heroes.length()) if (heroes.getJSONObject(i).optInt("data") == queen) heroes.getJSONObject(i).put("lvl", queenLevel)
        }
        return root.toString()
    }

    private fun import(json: String) = Store.setSelected(app, Store.saveExport(app, json))

    private fun point(json: String) = Progress.point(json, TestData.gameData)!!

    private fun texts(screen: android.app.Activity) = Screens.texts(Screens.root(screen))

    private fun loaded(): ProgressActivity {
        val screen = Screens.open<ProgressActivity>()
        screen.loading!!.join()
        shadowOf(Looper.getMainLooper()).idle()
        Screens.layOut(screen)
        return screen
    }

    @Test
    fun withOneImportTheCardSaysToImportAgain() {
        val json = export(today)
        import(json)
        val shown = texts(Screens.open<PlannerActivity>())
        assertTrue(shown.contains("Upgrade time done at Town Hall 16"))
        assertTrue(shown.contains(ProgressText.percent(point(json).done)))
        assertTrue(shown.contains("Import again later to see how this moves"))
    }

    @Test
    fun theCardShowsTheChangeSinceTheFirstImportThisMonth() {
        import(export(today - 40 * day, 93))
        val monthAgo = export(today - 20 * day, 94)
        import(monthAgo)
        import(export(today - 5 * day, 94))
        val latest = export(today, 95)
        import(latest)

        val change = ProgressText.change(point(latest).done, point(monthAgo).done)
        assertNotNull(change)
        val since = "$change since ${TimeText.day((today - 20 * day) * 1000)}"
        assertTrue(texts(Screens.open<PlannerActivity>()).contains(since))
    }

    @Test
    fun aNewTownHallStartsAgainOnTheCard() {
        import(export(today - day))
        import(JSONObject(TestData.th18ExportJson()).put("timestamp", today).put("tag", tag).toString())
        val shown = texts(Screens.open<PlannerActivity>())
        assertTrue(shown.contains("Upgrade time done at Town Hall 18"))
        assertTrue(shown.contains("New Town Hall since ${TimeText.day((today - day) * 1000)}, so this starts again"))
    }

    @Test
    fun theCardOpensTheProgressPage() {
        import(export(today))
        val screen = Screens.open<PlannerActivity>()
        Screens.tappable(Screens.root(screen), "Upgrade time done at Town Hall 16")!!.performClick()
        assertEquals(ProgressActivity::class.java.name, shadowOf(app).nextStartedActivity.component!!.className)
    }

    @Test
    fun thePageWorksItOutOffTheMainThreadThenDrawsTheLine() {
        val first = export(today - 20 * day, 93)
        import(first)
        val latest = export(today, 95)
        import(latest)

        // Holding its lock keeps the work from finishing while the page is first drawn.
        val screen = synchronized(ProgressHistory) {
            Screens.open<ProgressActivity>().also { assertTrue(texts(it).contains("Working out your progress…")) }
        }
        screen.loading!!.join()
        shadowOf(Looper.getMainLooper()).idle()

        val root = Screens.root(screen)
        val chart = Screens.views<ProgressChart>(root).single()
        val now = ProgressText.percent(point(latest).done)
        val then = ProgressText.percent(point(first).done)
        assertEquals("Upgrade time done, from $then on ${TimeText.day((today - 20 * day) * 1000)} to $now on ${TimeText.day(today * 1000)}", chart.contentDescription)
        // The last day is picked to start with.
        assertTrue(texts(screen).contains("${TimeText.day(today * 1000)} · Town Hall 16 · $now"))
        // Heroes moved, and their row says by how much.
        val heroes = TestData.plan(latest).group(com.cocwidget.plan.Planner.Group.HEROES)!!.done
        val heroesThen = point(first).groups.getValue(com.cocwidget.plan.Planner.Group.HEROES)
        assertTrue(texts(screen).contains(ProgressText.change(heroes, heroesThen)))
    }

    @Test
    fun tappingTheChartsStartPicksTheFirstDay() {
        val first = export(today - 20 * day, 93)
        import(first)
        import(export(today, 95))
        val screen = loaded()
        val chart = Screens.views<ProgressChart>(Screens.root(screen)).single()
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 1f, chart.height / 2f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 1f, chart.height / 2f)
        assertEquals(0, chart.selected)
        assertTrue(texts(screen).contains("${TimeText.day((today - 20 * day) * 1000)} · Town Hall 16 · ${ProgressText.percent(point(first).done)}"))
    }

    @Test
    fun importsFromOneDayDontMakeALineYet() {
        import(export(today - 3_600, 94))
        import(export(today, 95))
        assertTrue(texts(loaded()).contains("Your line starts once you've imported on two different days."))
    }

    @Test
    fun eachImportIsWorkedOutOnceUntilTheAppChanges() {
        val json = export(today)
        import(json)
        val file = File(ImportHistory.dir(app, tag), "progress.json")
        assertEquals(point(json).done, ProgressHistory.all(app, tag).single().done)
        assertTrue(file.exists())

        // A kept figure is used as it is.
        val kept = JSONObject(file.readText())
        kept.getJSONArray("points").getJSONObject(0).put("done", 0.125)
        file.writeText(kept.toString())
        assertEquals(0.125f, ProgressHistory.all(app, tag).single().done)

        // Kept by another version of the app, it's worked out again.
        file.writeText(kept.put("version", kept.getLong("version") - 1).toString())
        assertEquals(point(json).done, ProgressHistory.all(app, tag).single().done)
    }

    @Test
    fun thePageBuildsInEverySkin() {
        import(export(today - 20 * day, 93))
        import(export(today, 95))
        for (skin in Skin.ALL) {
            Store.setSkin(app, skin.key)
            assertEquals(1, Screens.views<ProgressChart>(Screens.root(loaded())).size)
        }
    }
}
