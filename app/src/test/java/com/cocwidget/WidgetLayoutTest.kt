package com.cocwidget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Version 1.0 put the rows in a scrolling ListView, and on One UI the countdowns inside it
// stopped ticking between redraws. Nothing short of a phone shows that, so this guards the
// layouts against collection views coming back.
class WidgetLayoutTest {
    private val collectionViews = listOf("ListView", "GridView", "StackView", "AdapterViewFlipper")

    @Test
    fun widgetLayoutsUseNoCollectionViews() {
        val layouts = File("src/main/res/layout").listFiles { file -> file.name.startsWith("widget") }.orEmpty()
        assertTrue("no widget layouts found", layouts.isNotEmpty())
        for (layout in layouts) {
            val xml = layout.readText()
            for (view in collectionViews) {
                assertFalse("${layout.name} uses <$view>, where Chronometers stop ticking", "<$view" in xml)
            }
        }
    }
}
