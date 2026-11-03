package com.cocwidget.store

import com.cocwidget.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// The app promises a shared export has no player tag in it, so that's checked on real exports.
class ShareExportTest {
    @Test
    fun takesTheTagOutOfRealExports() {
        for (json in listOf(TestData.exportJson(), TestData.th18ExportJson())) {
            val tag = Store.tagOf(json)
            val shared = Store.withoutTag(json)
            assertFalse(tag in shared)
            assertEquals(Store.SHARED_TAG, Store.tagOf(shared))
        }
    }

    @Test
    fun leavesTheRestExactlyAsExported() {
        val json = TestData.exportJson()
        assertEquals(json, Store.withoutTag(json).replace(Store.SHARED_TAG, Store.tagOf(json)))
    }

    @Test
    fun takesTheTagOutWhereverItAppears() {
        val shared = Store.withoutTag("""{"tag":"#ABC123","clan":{"leader":"#ABC123"}}""")
        assertFalse("#ABC123" in shared)
    }

    @Test
    fun anExportWithNoTagIsLeftAlone() {
        val json = """{"timestamp":1800000000,"note":"why?"}"""
        assertEquals(json, Store.withoutTag(json))
    }
}
