package com.cocwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Version 1.0 put the rows in a scrolling ListView, and on One UI the countdowns inside it
// stopped ticking between redraws. Nothing short of a phone shows that, so this guards the
// layouts: the Timeline's list and the Accounts widget's are the only scrolling views, and nothing
// that goes in them ticks.
class WidgetLayoutTest {
    private val collectionViews = listOf("ListView", "GridView", "StackView", "AdapterViewFlipper")
    private val layouts = File("src/main/res/layout").listFiles { file -> file.name.startsWith("widget") }.orEmpty()
    // Each layout allowed a list, and that list's id.
    private val lists = mapOf("widget.xml" to "timeline_list", "widget_accounts.xml" to "accounts_list")

    @Test
    fun theListsAreTheOnlyScrollingViews() {
        assertTrue("no widget layouts found", layouts.isNotEmpty())
        assertEquals(lists.keys, layouts.map { it.name }.filter { it in lists }.toSet())
        for (layout in layouts) {
            val xml = layout.readText()
            for (view in collectionViews) {
                val count = Regex("<$view\\b").findAll(xml).count()
                val list = lists[layout.name]
                if (list != null && view == "ListView") {
                    assertEquals("${layout.name} has one list", 1, count)
                    assertTrue(Regex("""<ListView\s+android:id="@\+id/$list"""").containsMatchIn(xml))
                } else {
                    assertEquals("${layout.name} uses <$view>, where Chronometers stop ticking", 0, count)
                }
            }
        }
    }

    @Test
    fun nothingInTheListsTicks() {
        val listRows = layouts.filter { it.name.startsWith("widget_timeline") || it.name == "widget_accounts_row.xml" }
        assertEquals(3, listRows.size)
        for (layout in listRows) assertFalse("${layout.name} has a Chronometer", "<Chronometer" in layout.readText())
    }
}
