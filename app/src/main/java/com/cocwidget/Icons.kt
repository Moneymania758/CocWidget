package com.cocwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Icons come from ClashKing's asset host and are cached on disk, shrunk to widget size.
object Icons {
    private const val BASE_URL = "https://assets.clashk.ing/"
    private const val SIZE_PX = 96

    fun load(context: Context, upgrade: Upgrade): Bitmap? = load(context, path(upgrade))

    fun load(context: Context, path: String?): Bitmap? {
        val file = file(context, path ?: return null)
        return if (file.length() > 0) BitmapFactory.decodeFile(file.path) else null
    }

    fun fetchMissing(context: Context, upgrades: List<Upgrade>) {
        fetchPaths(context, upgrades.mapNotNull { path(it) })
    }

    // Returns true when anything new was downloaded.
    fun fetchPaths(context: Context, paths: List<String>): Boolean {
        var fetched = false
        for (path in paths.distinct()) {
            val file = file(context, path)
            if (!file.exists()) {
                download(path, file)
                fetched = true
            }
        }
        return fetched
    }

    // The picture for a planner item at [level]: buildings and traps look different per level.
    fun path(item: GameItem, level: Int): String? = path(item.name, item.kind, item.village, maxOf(1, level), item.defence)

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

    private fun file(context: Context, path: String) =
        File(File(context.filesDir, "icons"), path.replace('/', '_').removeSuffix(".webp") + ".png")

    private fun path(upgrade: Upgrade): String? =
        path(upgrade.name, upgrade.kind, upgrade.village, maxOf(1, upgrade.toLevel - 1), upgrade.defence)

    private fun path(rawName: String, kind: String, rawVillage: String, level: Int, defence: String): String? {
        val name = clean(rawName)
        val village = if (rawVillage == "builderBase") "builder-base" else "home-village"
        return when (kind) {
            "building" -> "buildings/$village/$name/level_$level.webp"
            "trap" -> "traps/$village/$name/level_$level.webp"
            "troop" -> "troops/$name/icon.webp"
            "spell" -> "spells/$name.webp"
            "hero" -> "heroes/$name/icon.webp"
            "pet" -> "pets/$name/icon.webp"
            "guardian" -> "guardians/$name/icon.webp"
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
