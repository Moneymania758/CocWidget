package com.cocwidget.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun newerVersions() {
        assertTrue(UpdateChecker.isNewer("1.3", "1.2"))
        assertTrue(UpdateChecker.isNewer("2", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.2.1", "1.2"))
    }

    @Test
    fun comparesNumbersNotText() {
        // As text "1.10" sorts before "1.9".
        assertTrue(UpdateChecker.isNewer("1.10", "1.9"))
    }

    @Test
    fun sameOrOlderIsNotAnUpdate() {
        assertFalse(UpdateChecker.isNewer("1.2", "1.2"))
        assertFalse(UpdateChecker.isNewer("1.2", "1.2.0"))
        assertFalse(UpdateChecker.isNewer("1.2", "1.3"))
    }
}
