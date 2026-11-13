package com.cocwidget.store

import android.content.Context
import com.cocwidget.game.GameData
import com.cocwidget.plan.Progress
import com.cocwidget.plan.ProgressPoint
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Each import's progress, worked out once and kept in its account's history folder. A phone takes
// a few tens of milliseconds an import, too long for a year of them on the main thread.
object ProgressHistory {
    private const val FILE = "progress.json"

    // Every import's point, oldest first. Works out any not kept yet, so call it off the main
    // thread. Locked, as a page rebuilt meanwhile starts a second run.
    @Synchronized
    fun all(context: Context, tag: String): List<ProgressPoint> {
        val dir = ImportHistory.dir(context, tag)
        val file = File(dir, FILE)
        val version = appVersion(context)
        val kept = Progress.parse(file.takeIf { it.exists() }?.readText(), version)
        val items = GameData.all(context)
        val points = ImportHistory.entries(dir).mapNotNull { entry ->
            kept[entry.exportedAtMillis] ?: ImportHistory.read(entry)?.let { Progress.point(it, items) }
        }
        if (points.map { it.exportedAtMillis }.toSet() != kept.keys) write(dir, file, Progress.toJson(points, version))
        return points
    }

    // Written beside it first, so a crash halfway leaves the old one. Losing it only means
    // working it out again.
    private fun write(dir: File, file: File, text: String) {
        // Gone if the account was removed meanwhile.
        if (!dir.isDirectory) return
        val partial = File(dir, "$FILE.partial")
        try {
            partial.writeText(text)
            Files.move(partial.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: IOException) {
            partial.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(context: Context): Long =
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
}
