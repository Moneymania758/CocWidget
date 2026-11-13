package com.cocwidget.store

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// Every export imported, kept whole so later screens (% maxed over time, upgrades per week) can be
// worked out from them, including ones not thought of yet. Past imports can't be recovered later,
// so they're kept from now on. One gzipped file per export in its account's folder, named after
// the export's own timestamp, so importing the same export twice keeps one copy. An export is
// about 10 KB, and about 1.5 KB gzipped.
object ImportHistory {
    private const val SUFFIX = ".json.gz"

    class Entry(val exportedAtMillis: Long, internal val file: File)

    fun dir(context: Context, tag: String) = File(File(context.filesDir, "history"), Store.fileName(tag))

    // False if the export has no timestamp or the file can't be written. Never throws, as a lost
    // history entry mustn't stop an import.
    fun record(dir: File, json: String): Boolean {
        val seconds = timestampOf(json) ?: return false
        val file = File(dir, seconds.toString() + SUFFIX)
        if (file.exists()) return true
        // Written beside it first, so a crash halfway never leaves a broken entry.
        val partial = File(dir, "$seconds.partial")
        return try {
            dir.mkdirs()
            GZIPOutputStream(partial.outputStream()).use { it.write(json.toByteArray(Charsets.UTF_8)) }
            partial.renameTo(file).also { moved -> if (!moved) partial.delete() }
        } catch (_: IOException) {
            partial.delete()
            false
        }
    }

    // Oldest first.
    fun entries(dir: File): List<Entry> =
        dir.listFiles().orEmpty()
            .mapNotNull { file -> file.name.removeSuffix(SUFFIX).takeIf { file.name.endsWith(SUFFIX) }?.toLongOrNull()?.let { Entry(it * 1000, file) } }
            .sortedBy { it.exportedAtMillis }

    // The export as imported, or null if the file has gone or can't be read.
    fun read(entry: Entry): String? = try {
        GZIPInputStream(entry.file.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (_: IOException) {
        null
    }

    fun delete(dir: File) {
        dir.deleteRecursively()
    }

    private fun timestampOf(json: String): Long? = try {
        JSONObject(json).optLong("timestamp").takeIf { it > 0 }
    } catch (_: JSONException) {
        null
    }
}
