package com.cocwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTest {
    @Test
    fun deviceAddsTheMakerOnlyWhenTheModelLacksIt() {
        assertEquals("Clash Timers 2.0, Android 15, Samsung SM-S918B", Contact.device("2.0", "15", "samsung", "SM-S918B"))
        assertEquals("Clash Timers 2.0, Android 14, OnePlus 12", Contact.device("2.0", "14", "OnePlus", "OnePlus 12"))
    }

    @Test
    fun issueLinkFillsThePhoneField() {
        assertEquals(
            "${Contact.ISSUES}?template=bug.yml&device=Clash%20Timers%202.0%2C%20Android%2015%2C%20Pixel%208",
            Contact.issueUrl("Clash Timers 2.0, Android 15, Pixel 8"),
        )
    }

    @Test
    fun emailSpacesAreNotPlusSigns() {
        val uri = Contact.emailUri("Clash Timers 2.0, Android 15, Pixel 8")
        assertTrue("Clash%20Timers%20problem" in uri)
        assertFalse("+" in uri)
    }
}
