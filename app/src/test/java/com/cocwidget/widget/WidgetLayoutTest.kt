package com.cocwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Version 1.0 put the rows in a scrolling ListView, and on One UI the countdowns inside it
// stopped ticking between redraws. Nothing short of a phone shows that, so this guards the
// layouts: the Timeline's list is the only scrolling view, and nothing that goes in it ticks.
class WidgetLayoutTest {
    private val collectionViews = listOf("ListView", "GridView", "StackView", "AdapterViewFlipper")
    private val layouts = File("src/main/res/layout").listFiles { file -> file.name.startsWith("widget") }.orEmpty()

    @Test
    fun theTimelinesListIsTheOnlyScrollingView() {
        assertTrue("no widget layouts found", layouts.isNotEmpty())
        for (layout in layouts) {
            val xml = layout.readText()
            for (view in collectionViews) {
                val count = Regex("<$view\\b").findAll(xml).count()
                if (layout.name == "widget.xml" && view == "ListView") {
                    assertEquals("widget.xml has one list, the Timeline's", 1, count)
                    assertTrue(Regex("""<ListView\s+android:id="@\+id/timeline_list"""").containsMatchIn(xml))
                } else {
                    assertEquals("${layout.name} uses <$view>, where Chronometers stop ticking", 0, count)
                }
            }
        }
    }

    @Test
    fun nothingInTheTimelinesListTicks() {
        val listRows = layouts.filter { it.name.startsWith("widget_timeline") }
        assertEquals(2, listRows.size)
        for (layout in listRows) assertFalse("${layout.name} has a Chronometer", "<Chronometer" in layout.readText())
    }
}
