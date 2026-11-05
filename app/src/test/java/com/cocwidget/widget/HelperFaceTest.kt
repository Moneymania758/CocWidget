package com.cocwidget.widget

import com.cocwidget.R
import com.cocwidget.game.Section
import org.junit.Assert.assertEquals
import org.junit.Test

class HelperFaceTest {
    @Test
    fun rowsShowTheHelperWhoWorksThere() {
        assertEquals(R.drawable.ic_face_apprentice, WidgetRenderer.helperFace(Section.BUILDERS))
        assertEquals(R.drawable.ic_face_assistant, WidgetRenderer.helperFace(Section.LAB))
    }
}
