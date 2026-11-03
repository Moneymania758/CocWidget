package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetViewTest {
    private val all = setOf(Section.BUILDERS, Section.LAB, Section.PETS, Section.BUILDER_BASE)

    @Test
    fun eachViewMatchesItsOwnFolds() {
        for (view in WidgetView.entries) {
            assertEquals(view, WidgetView.matching(view.folded.map { it.name }.toSet(), all))
        }
    }

    @Test
    fun foldingOneSectionByHandLightsNoView() {
        assertNull(WidgetView.matching(setOf("BUILDER_BASE", "LAB"), all))
    }

    @Test
    fun foldsOnSectionsNotShownAreIgnored() {
        // No Pet House yet, so Builder Base without PETS folded still counts.
        val noPets = all - Section.PETS
        assertEquals(WidgetView.BUILDER_BASE, WidgetView.matching(setOf("BUILDERS", "LAB"), noPets))
        assertEquals(WidgetView.HOME, WidgetView.matching(setOf("BUILDER_BASE", "PETS"), noPets))
    }
}
