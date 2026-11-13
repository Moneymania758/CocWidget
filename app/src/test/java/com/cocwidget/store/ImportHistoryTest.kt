package com.cocwidget.store

import com.cocwidget.TestData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

// Past imports can't be recovered later, so every import has to land in the history, whole.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImportHistoryTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val dir get() = File(app.filesDir, "history_test")

    private fun at(json: String, seconds: Long, tag: String? = null) =
        JSONObject(json).put("timestamp", seconds).apply { if (tag != null) put("tag", tag) }.toString()

    @Test
    fun anExportComesBackExactlyAsImported() {
        val json = TestData.exportJson()
        assertTrue(ImportHistory.record(dir, json))
        val entry = ImportHistory.entries(dir).single()
        assertEquals(1793625123000, entry.exportedAtMillis)
        assertEquals(json, ImportHistory.read(entry))
        assertTrue("kept gzipped", entry.file.length() < json.length / 3)
    }

    @Test
    fun theSameExportTwiceIsKeptOnceAndEntriesComeOldestFirst() {
        val json = TestData.exportJson()
        ImportHistory.record(dir, at(json, 300))
        ImportHistory.record(dir, at(json, 100))
        ImportHistory.record(dir, at(json, 300))
        ImportHistory.record(dir, at(json, 200))
        assertEquals(listOf(100_000L, 200_000L, 300_000L), ImportHistory.entries(dir).map { it.exportedAtMillis })
    }

    @Test
    fun somethingThatIsNotAnExportIsSkipped() {
        assertFalse(ImportHistory.record(dir, "not json"))
        assertFalse(ImportHistory.record(dir, """{"tag":"#TEST0000"}"""))
        assertTrue(ImportHistory.entries(dir).isEmpty())
    }

    // A crash between writing and renaming leaves a partial file. It's never read as an entry.
    @Test
    fun leftoverPartialsAndStrangeFilesAreNotEntries() {
        dir.mkdirs()
        File(dir, "100.partial").writeText("half")
        File(dir, "notes.txt").writeText("x")
        File(dir, "x.json.gz").writeText("x")
        assertTrue(ImportHistory.entries(dir).isEmpty())
    }

    @Test
    fun importingKeepsTheExportAlreadyThereAndTheNewOne() {
        // An account imported before history was kept: its export is stored, its history empty.
        val tag = Store.saveExport(app, at(TestData.exportJson(), 100))
        ImportHistory.delete(ImportHistory.dir(app, tag))

        Store.saveExport(app, at(TestData.exportJson(), 200))
        assertEquals(listOf(100_000L, 200_000L), Store.history(app, tag).map { it.exportedAtMillis })

        Store.saveExport(app, at(TestData.exportJson(), 300))
        assertEquals(listOf(100_000L, 200_000L, 300_000L), Store.history(app, tag).map { it.exportedAtMillis })
    }

    @Test
    fun eachAccountHasItsOwnHistoryAndRemovingOneDeletesOnlyThat() {
        val main = Store.saveExport(app, at(TestData.exportJson(), 100, "#MAIN"))
        val second = Store.saveExport(app, at(TestData.th18ExportJson(), 200, "#SECOND"))
        assertEquals(listOf(100_000L), Store.history(app, main).map { it.exportedAtMillis })
        assertEquals(listOf(200_000L), Store.history(app, second).map { it.exportedAtMillis })

        Store.removeAccount(app, main)
        assertTrue(Store.history(app, main).isEmpty())
        assertFalse(ImportHistory.dir(app, main).exists())
        assertEquals(1, Store.history(app, second).size)
    }
}
