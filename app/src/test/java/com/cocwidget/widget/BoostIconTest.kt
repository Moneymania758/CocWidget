package com.cocwidget.widget

import com.cocwidget.R
import com.cocwidget.game.Section
import org.junit.Assert.assertEquals
import org.junit.Test

class BoostIconTest {
    @Test
    fun everyPotionHasItsOwnPicture() {
        val pictures = Section.entries.map { WidgetRenderer.potionIcon(it) }
        assertEquals(pictures.size, pictures.toSet().size)
    }

    @Test
    fun snacksShowTheItemThatRuns() {
        assertEquals(R.drawable.ic_boost_builder_bite, WidgetRenderer.snackIcon(Section.BUILDERS))
        assertEquals(R.drawable.ic_boost_study_soup, WidgetRenderer.snackIcon(Section.LAB))
        assertEquals(R.drawable.ic_boost_study_soup, WidgetRenderer.snackIcon(Section.PETS))
    }
}
