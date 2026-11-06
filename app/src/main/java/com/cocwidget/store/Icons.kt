package com.cocwidget.store

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cocwidget.game.GameItem
import com.cocwidget.game.Upgrade
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Icons come from ClashKing's asset host and are cached on disk, shrunk to widget size. The few
// ClashKing doesn't have are bundled in assets/icons, named as the cache names them. So are the
// Town Halls, in their original format and size, as the main screen shows one large.
object Icons {
    private const val BASE_URL = "https://assets.clashk.ing/"
    private const val SIZE_PX = 96

    // Buildings whose top levels ClashKing has no picture for yet. They show the highest it has.
    private val LAST_PICTURE = mapOf("blacksmith" to 9)

    @Volatile
    private var bundled: Set<String>? = null

    fun load(context: Context, upgrade: Upgrade): Bitmap? = load(context, path(upgrade))

    fun load(context: Context, path: String?): Bitmap? {
        val name = fileName(path ?: return null)
        bundledFile(context, name)?.let { bundled -> return context.assets.open("icons/$bundled").use { BitmapFactory.decodeStream(it) } }
        val file = File(File(context.filesDir, "icons"), name)
        return if (file.length() > 0) BitmapFactory.decodeFile(file.path) else null
    }

    fun fetchMissing(context: Context, upgrades: List<Upgrade>) {
        fetchPaths(context, upgrades.mapNotNull { path(it) })
    }

    // Returns true when anything new was downloaded.
    fun fetchPaths(context: Context, paths: List<String>): Boolean {
        var fetched = false
        for (path in paths.distinct()) {
            val name = fileName(path)
            val file = File(File(context.filesDir, "icons"), name)
            if (bundledFile(context, name) == null && !file.exists()) {
                download(path, file)
                fetched = true
            }
        }
        return fetched
    }

    // The picture for a planner item at [level]: buildings and traps look different per level.
    fun path(item: GameItem, level: Int): String? = path(item.name, item.kind, item.village, maxOf(1, level), item.defence)

    // A Town Hall above the bundled ones (a new one, before an app update) downloads like any icon.
    fun townHallPath(level: Int) = "buildings/home-village/town_hall/level_$level.webp"

    private fun download(path: String, file: File) {
        try {
            val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            try {
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val original = connection.inputStream.use { BitmapFactory.decodeStream(it) } ?: return
                        val height = SIZE_PX * original.height / original.width
                        val scaled = Bitmap.createScaledBitmap(original, SIZE_PX, height, true)
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    // An empty file means "no icon exists", so we stop asking for it.
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        file.parentFile?.mkdirs()
                        file.createNewFile()
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            // Offline. The next refresh tries again.
        }
    }

    fun fileName(path: String) = path.replace('/', '_').removeSuffix(".webp") + ".png"

    private fun bundledFile(context: Context, name: String): String? =
        listOf(name, name.removeSuffix(".png") + ".webp").firstOrNull { it in bundled(context) }

    private fun bundled(context: Context): Set<String> =
        bundled ?: context.assets.list("icons").orEmpty().toSet().also { bundled = it }

    fun path(upgrade: Upgrade): String? =
        path(upgrade.name, upgrade.kind, upgrade.village, if (upgrade.supercharge > 0) upgrade.toLevel else maxOf(1, upgrade.toLevel - 1), upgrade.defence)

    private fun path(rawName: String, kind: String, rawVillage: String, level: Int, defence: String): String? {
        val name = clean(rawName)
        val village = if (rawVillage == "builderBase") "builder-base" else "home-village"
        return when (kind) {
            "building" -> "buildings/$village/$name/level_${minOf(level, LAST_PICTURE[name] ?: level)}.webp"
            "trap" -> "traps/$village/$name/level_$level.webp"
            "troop" -> "troops/$name/icon.webp"
            "spell" -> "spells/$name.webp"
            "hero" -> "heroes/$name/icon.webp"
            "pet" -> "pets/$name/icon.webp"
            "guardian" -> "guardians/$name/icon.webp"
            "equipment" -> "equipment/$name.webp"
            // Modules have no icon of their own, so they show their defence.
            "module" -> "buildings/seasonal-defense/${clean(defence)}/level_1.webp"
            else -> null
        }
    }

    // ClashKing's file naming rule, from the ClashKingAssets README.
    private fun clean(name: String) = name.lowercase()
        .replace(" ", "_")
        .replace(".", "")
        .replace("?", "")
        .replace("\\q", "")
        .replace("’", "")
}
