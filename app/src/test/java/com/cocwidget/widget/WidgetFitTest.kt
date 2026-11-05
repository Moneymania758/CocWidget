package com.cocwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetFitTest {
    private val header = 30
    private val more = 26
    private val row = WidgetFit.ROW_DP

    private fun fit(rows: List<Int>, available: Int?) = WidgetFit.rowsToShow(rows, available, header, more)

    private fun used(rows: List<Int>, shown: List<Int>) =
        rows.size * header + shown.sum() * row + rows.indices.count { shown[it] < rows[it] } * more

    @Test
    fun everythingShowsWhenTheSizeIsUnknown() {
        assertEquals(listOf(6, 1, 1, 4), fit(listOf(6, 1, 1, 4), null))
    }

    @Test
    fun everythingShowsWhenItFits() {
        val rows = listOf(6, 1, 1, 4)
        val exactly = rows.size * header + rows.sum() * row
        assertEquals(rows, fit(rows, exactly))
    }

    // The case from the emulator test: a TH16 village in the ALL view with the Builder Base cut off.
    // One row short still shows the rest, and says one is hidden.
    @Test
    fun oneRowTooManyLeavesOutOnlyTheLastSectionsLastRow() {
        val rows = listOf(6, 1, 1, 4)
        val exactly = rows.size * header + rows.sum() * row
        assertEquals(listOf(6, 1, 1, 3), fit(rows, exactly - 1))
    }

    @Test
    fun aSectionThatCantFitAnyRowsStillGetsItsMoreLine() {
        val rows = listOf(6, 1, 1, 4)
        val room = rows.size * header + 8 * row + more
        assertEquals(listOf(6, 1, 1, 0), fit(rows, room))
    }

    @Test
    fun earlierSectionsComeFirst() {
        assertEquals(listOf(2, 0, 0, 0), fit(listOf(6, 1, 1, 4), 4 * header + 4 * more + 2 * row))
    }

    @Test
    fun foldedSectionsNeedNoMoreLine() {
        val rows = listOf(0, 0, 0, 4)
        assertEquals(listOf(0, 0, 0, 3), fit(rows, 4 * header + more + 3 * row))
    }

    @Test
    fun aVerySmallWidgetShowsNoRowsAndNothingNegative() {
        assertEquals(listOf(0, 0, 0, 0), fit(listOf(6, 1, 1, 4), 10))
        assertEquals(listOf(0, 0), fit(listOf(6, 1), -50))
    }

    @Test
    fun whatItShowsAlwaysFitsWhenTheHeadersAndMoreLinesCan() {
        for (rows in listOf(listOf(6, 1, 1, 4), listOf(1, 1, 0, 9), listOf(0, 3, 2, 0), listOf(5, 0), listOf(12))) {
            val floor = rows.size * header + rows.count { it > 0 } * more
            val top = rows.size * header + rows.sum() * row
            for (available in floor..top) {
                val shown = fit(rows, available)
                assertTrue("$rows in $available: $shown", used(rows, shown) <= available)
                assertTrue("$rows in $available: $shown", shown.indices.all { shown[it] in 0..rows[it] })
            }
        }
    }

    @Test
    fun moreRoomNeverTakesRowsFromTheFirstSection() {
        val rows = listOf(6, 1, 1, 4)
        val shown = (0..700).map { fit(rows, it).first() }
        assertEquals(shown.sorted(), shown)
    }

    @Test
    fun noRoomForAnotherRowGoesToWaste() {
        for (rows in listOf(listOf(6, 1, 1, 4), listOf(1, 1, 0, 9), listOf(0, 3, 2, 0), listOf(12))) {
            val floor = rows.size * header + rows.count { it > 0 } * more
            val top = rows.size * header + rows.sum() * row
            for (available in floor..top) {
                val shown = fit(rows, available)
                if (shown != rows) assertTrue("$rows in $available: $shown", available - used(rows, shown) < row + more)
            }
        }
    }

    // A line of text in the sizes the widget uses, roughly as a phone at normal font size.
    private val normalText: (Int) -> Int = { sp -> sp + sp / 3 }
    private val hugeText: (Int) -> Int = { sp -> 2 * (sp + sp / 3) }

    @Test
    fun theFrameTakesItsOptionalLinesOffTheRoom() {
        val plain = WidgetFit.roomForRows(400, viewButtons = false, helpers = false, normalText)
        assertEquals(plain - (6 + 14 + 6), WidgetFit.roomForRows(400, viewButtons = true, helpers = false, normalText))
        assertEquals(plain - (4 + 14), WidgetFit.roomForRows(400, viewButtons = false, helpers = true, normalText))
        assertEquals(plain + 100, WidgetFit.roomForRows(500, viewButtons = false, helpers = false, normalText))
    }

    @Test
    fun theFrameMatchesItsSizesAtNormalText() {
        // 12 + 12 padding, title row 28 (the Sync button: text 18 + 10), footer 4 + 14, safety 4.
        assertEquals(400 - 24 - 28 - 18 - 4, WidgetFit.roomForRows(400, viewButtons = false, helpers = false, normalText))
        // 8 + 4 padding, then the chip: 2 + 2 + its 16 tall text.
        assertEquals(32, WidgetFit.headerDp(normalText))
        assertEquals(26, WidgetFit.moreDp(normalText))
    }

    @Test
    fun largerTextLeavesLessRoom() {
        for (buttons in listOf(true, false)) for (helpers in listOf(true, false)) {
            assertTrue(WidgetFit.roomForRows(400, buttons, helpers, hugeText) < WidgetFit.roomForRows(400, buttons, helpers, normalText))
        }
        assertTrue(WidgetFit.headerDp(hugeText) > WidgetFit.headerDp(normalText))
        assertTrue(WidgetFit.moreDp(hugeText) > WidgetFit.moreDp(normalText))
    }

    // The sums above copy these sizes, so a layout change has to change them too.
    @Test
    fun theSumsMatchTheLayouts() {
        val xml = { name: String -> File("src/main/res/layout/$name.xml").readText() }
        val widget = xml("widget")
        assertTrue(widget.contains("android:padding=\"12dp\""))
        assertTrue(widget.contains("android:paddingTop=\"5dp\"") && widget.contains("android:paddingBottom=\"5dp\""))
        assertTrue(widget.contains("android:layout_marginTop=\"6dp\""))
        assertTrue(widget.contains("android:layout_marginTop=\"4dp\""))
        val section = xml("widget_section")
        assertTrue(section.contains("android:paddingTop=\"8dp\"") && section.contains("android:paddingBottom=\"4dp\""))
        assertTrue(section.contains("android:paddingTop=\"2dp\"") && section.contains("android:paddingBottom=\"2dp\""))
        assertTrue(section.contains("android:layout_height=\"16dp\""))
        assertTrue(xml("widget_more").contains("android:minHeight=\"26dp\""))
        val button = File("src/main/res/values/themes.xml").readText()
        assertTrue(button.contains("<item name=\"android:paddingTop\">3dp</item>") && button.contains("<item name=\"android:paddingBottom\">3dp</item>"))
    }

    @Test
    fun rowHeightMatchesTheLayout() {
        assertTrue(File("src/main/res/layout/widget_row.xml").readText().contains("android:layout_height=\"${WidgetFit.ROW_DP}dp\""))
    }
}
