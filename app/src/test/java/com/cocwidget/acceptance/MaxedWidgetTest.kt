package com.cocwidget.acceptance

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.game.Ids
import com.cocwidget.game.Section
import com.cocwidget.plan.Bedtime
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// A maxed lab on the big widget: not idle, a green "Lab maxed" chip until it's tapped away, then
// nothing about the lab at all.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaxedWidgetTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private val maxedLab = TestData.maxedTh18("units", "spells", "siege_machines", buildings = mapOf(Ids.LABORATORY to null))

    // Drawn for a widget id no launcher knows, so no redraw of a placed widget can land mid-test.
    private fun draw(tag: String): View {
        val village = Store.village(app, tag)!!
        return WidgetRenderer.build(app, 9_002, tag, village, village.exportedAtMillis + 60_000).apply(app, FrameLayout(app))
    }

    private fun chips(root: View) = listOf(R.id.chip_1, R.id.chip_2, R.id.chip_3)
        .map { root.findViewById<TextView>(it) }
        .filter { it.visibility == View.VISIBLE }

    private fun texts(root: View) = Screens.texts(root)

    private val labMaxed get() = app.getString(R.string.maxed_chip, app.getString(R.string.maxed_lab))

    @Test
    fun aMaxedLabIsNotIdleAndSaysSo() {
        val tag = Store.saveExport(app, maxedLab)
        val village = Store.village(app, tag)!!
        assertTrue(village.maxed(Section.LAB, village.exportedAtMillis + 60_000))
        val root = draw(tag)
        assertFalse(texts(root).any { it == app.getString(R.string.next_lab_idle) })
        assertTrue(chips(root).any { it.text.toString() == labMaxed })
        assertTrue(texts(root).any { it.startsWith("LAB") && it.contains("maxed") && !it.contains("idle") })
    }

    @Test
    fun aTapOnTheChipHidesItForThisTownHall() {
        val tag = Store.saveExport(app, maxedLab)
        val chip = chips(draw(tag)).first { it.text.toString() == labMaxed }
        assertTrue(chip.hasOnClickListeners())
        chip.performClick()
        val sent = shadowOf(app).broadcastIntents.last()
        assertEquals(VillageWidget.ACTION_HIDE_MAXED, sent.action)
        assertEquals(tag, sent.getStringExtra(VillageWidget.EXTRA_ACCOUNT))
        assertEquals(Section.LAB.name, sent.getStringExtra(VillageWidget.EXTRA_SECTION))
        assertEquals(18, sent.getIntExtra(VillageWidget.EXTRA_TOWN_HALL, 0))
    }

    @Test
    fun onceHiddenTheLabIsGoneFromTheWidget() {
        val tag = Store.saveExport(app, maxedLab)
        Store.hideMaxed(app, tag, Section.LAB, 18)
        val root = draw(tag)
        assertFalse(chips(root).any { it.text.toString() == labMaxed })
        assertFalse(texts(root).any { it.startsWith("LAB") })
        // Pets still have work, so their header stays.
        assertTrue(texts(root).any { it.startsWith("PETS") })
    }

    // The next Town Hall brings new levels, and says so again once those are done.
    @Test
    fun hidingAtOneTownHallLeavesTheNextAlone() {
        val tag = Store.saveExport(app, maxedLab)
        Store.hideMaxed(app, tag, Section.LAB, 17)
        assertTrue(chips(draw(tag)).any { it.text.toString() == labMaxed })
    }

    // The launcher applies each update on top of the last, so a chip that was the tap-to-hide one
    // mustn't keep that tap once it says something else.
    @Test
    fun aChipThatNoLongerSaysMaxedDoesNothingWhenTapped() {
        val maxedTag = Store.saveExport(app, maxedLab)
        val root = draw(maxedTag)
        assertTrue(chips(root).any { it.hasOnClickListeners() })
        val tag = Store.saveExport(app, TestData.exportJson())
        val village = Store.village(app, tag)!!
        WidgetRenderer.build(app, 9_002, tag, village, village.exportedAtMillis + 60_000).reapply(app, root)
        val now = chips(root)
        assertTrue(now.isNotEmpty())
        assertTrue(now.none { it.text.toString() == labMaxed })
        assertTrue(now.none { it.hasOnClickListeners() })
    }

    @Test
    fun aFinishWhileAsleepHasAMoon() {
        val tag = Store.saveExport(app, TestData.exportJson())
        // Asleep all day but the last minute, then never.
        Store.setSleep(app, Bedtime.Sleep(0, 23 * 60 + 59))
        val village = Store.village(app, tag)!!
        val end = village.endMillis(village.upgrades.first())
        val now = village.exportedAtMillis
        val asleep = Bedtime.asleep(end, Store.sleep(app), java.time.ZoneId.systemDefault())
        assertEquals(asleep, WidgetRenderer.finishText(app, end, now).startsWith("☾ "))
        Store.setSleep(app, Bedtime.Sleep(0, 0))
        assertFalse(WidgetRenderer.finishText(app, end, now).startsWith("☾"))
    }
}
