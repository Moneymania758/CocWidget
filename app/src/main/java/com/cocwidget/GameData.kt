package com.cocwidget

import android.content.Context
import org.json.JSONObject

class GameItem(val name: String, val kind: String, val village: String, private val times: IntArray) {
    // Seconds to upgrade to [level], or null when the bundled data doesn't know it.
    fun timeToReach(level: Int): Long? = times.getOrNull(level - 1)?.takeIf { it > 0 }?.toLong()
}

// Names and upgrade times keyed by the export's "data" id. Built by tools/build-coc-data.js.
object GameData {
    @Volatile
    private var items: Map<Int, GameItem>? = null

    fun get(context: Context, id: Int): GameItem? = load(context)[id]

    private fun load(context: Context): Map<Int, GameItem> {
        items?.let { return it }
        val text = context.assets.open("coc_data.json").bufferedReader().use { it.readText() }
        return parse(text).also { items = it }
    }

    fun parse(text: String): Map<Int, GameItem> {
        val root = JSONObject(text)
        val map = HashMap<Int, GameItem>(root.length())
        for (key in root.keys()) {
            val entry = root.getJSONObject(key)
            val times = entry.getJSONArray("t")
            map[key.toInt()] = GameItem(
                name = entry.getString("n"),
                kind = entry.getString("k"),
                village = entry.optString("v"),
                times = IntArray(times.length()) { times.optInt(it) },
            )
        }
        return map
    }
}
