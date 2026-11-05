package com.cocwidget.widget

import com.cocwidget.widget.WidgetFit.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetFitTest {
    private val header = 32
    private val row = WidgetFit.ROW_DP
    private val village = listOf(Section(6, false), Section(1, false), Section(1, false), Section(4, false))

    private fun hint(sections: List<Section>, available: Int?) = WidgetFit.hint(sections, available, header)

    // 2.7.3 left rows out to fit every header and a more line, so a 360 dp widget with this village
    // showed no upgrades at all. Only the hint may depend on the height. Every row is always drawn,
    // so a launcher reporting the wrong height can't hide anything either.
    @Test
    fun everyRowIsAlwaysDrawn() {
        val renderer = File("src/main/java/com/cocwidget/widget/WidgetRenderer.kt").readText()
        assertTrue(renderer.contains("for (upgrade in upgrades) views.addView(R.id.rows, row(context, village, upgrade, now))"))
        assertFalse(renderer.contains("upgrades.take("))
    }

    @Test
    fun noHintWhenTheHeightIsUnknown() {
        assertNull(hint(village, null))
    }

    @Test
    fun noHintWhenEverythingFits() {
        assertNull(hint(village, 4 * header + 12 * row))
        assertNull(hint(village, 2000))
    }

    @Test
    fun oneRowShortCountsOne() {
        val found = hint(village, 4 * header + 12 * row - 1)!!
        assertEquals(1, found.hidden)
        assertEquals(3, found.section)
    }

    // The room a 360 dp widget has at normal font. Five builders show, as they did before 2.7.3.
    @Test
    fun aNormalWidgetCountsWhatItCantShow() {
        val found = hint(village, 233)!!
        assertEquals(12 - 5, found.hidden)
        assertEquals(0, found.section)
    }

    @Test
    fun aSectionWhoseHeaderDoesntFitCountsWhole() {
        val found = hint(village, 3 * header + 8 * row)!!
        assertEquals(4, found.hidden)
        assertEquals(3, found.section)
    }

    @Test
    fun foldedSectionsCountOnlyIfTheirHeaderIsCutOff() {
        val folded = listOf(Section(6, true), Section(1, true), Section(1, true), Section(4, true))
        assertNull(hint(folded, 4 * header))
        val found = hint(folded, 3 * header)!!
        assertEquals(4, found.hidden)
        assertEquals(3, found.section)
    }

    @Test
    fun anEmptySectionsCutOffHeaderIsNothingToCount() {
        assertNull(hint(listOf(Section(0, false), Section(0, false)), header))
    }

    @Test
    fun thingsAfterTheCutAreHiddenEvenIfTheyWouldFit() {
        val sections = listOf(Section(3, false), Section(1, false))
        val found = hint(sections, header + 2 * row + 30)!!
        assertEquals(2, found.hidden)
        assertEquals(0, found.section)
    }

    @Test
    fun aVerySmallWidgetCountsEverything() {
        assertEquals(12, hint(village, 10)!!.hidden)
        assertEquals(12, hint(village, -50)!!.hidden)
    }

    @Test
    fun moreRoomNeverHidesMore() {
        val hidden = (0..700).map { hint(village, it)?.hidden ?: 0 }
        assertEquals(hidden.sortedDescending(), hidden)
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
        // 12 + 12 padding, title row 28 (the Sync button: text 18 + 10), footer 4 + the hint's 20, safety 4.
        assertEquals(400 - 24 - 28 - 24 - 4, WidgetFit.roomForRows(400, viewButtons = false, helpers = false, normalText))
        // 8 + 4 padding, then the chip: 2 + 2 + its 16 tall text.
        assertEquals(32, WidgetFit.headerDp(normalText))
    }

    @Test
    fun largerTextLeavesLessRoom() {
        for (buttons in listOf(true, false)) for (helpers in listOf(true, false)) {
            assertTrue(WidgetFit.roomForRows(400, buttons, helpers, hugeText) < WidgetFit.roomForRows(400, buttons, helpers, normalText))
        }
        assertTrue(WidgetFit.headerDp(hugeText) > WidgetFit.headerDp(normalText))
    }

    // The sums above copy these sizes, so a layout change has to change them too.
    @Test
    fun theSumsMatchTheLayouts() {
        val xml = { name: String -> File("src/main/res/layout/$name.xml").readText() }
        val widget = xml("widget")
        assertTrue(widget.contains("android:padding=\"12dp\""))
        assertTrue(widget.contains("android:paddingTop=\"5dp\"") && widget.contains("android:paddingBottom=\"5dp\""))
        assertTrue(widget.contains("android:paddingTop=\"2dp\"") && widget.contains("android:paddingBottom=\"2dp\""))
        assertTrue(widget.contains("android:layout_marginTop=\"6dp\""))
        assertTrue(widget.contains("android:layout_marginTop=\"4dp\""))
        val section = xml("widget_section")
        assertTrue(section.contains("android:paddingTop=\"8dp\"") && section.contains("android:paddingBottom=\"4dp\""))
        assertTrue(section.contains("android:paddingTop=\"2dp\"") && section.contains("android:paddingBottom=\"2dp\""))
        assertTrue(section.contains("android:layout_height=\"16dp\""))
        val button = File("src/main/res/values/themes.xml").readText()
        assertTrue(button.contains("<item name=\"android:paddingTop\">3dp</item>") && button.contains("<item name=\"android:paddingBottom\">3dp</item>"))
    }

    @Test
    fun rowHeightMatchesTheLayout() {
        assertTrue(File("src/main/res/layout/widget_row.xml").readText().contains("android:layout_height=\"${WidgetFit.ROW_DP}dp\""))
    }
}
