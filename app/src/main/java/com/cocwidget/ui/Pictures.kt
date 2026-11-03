package com.cocwidget.ui

import android.app.Activity
import android.graphics.Bitmap
import android.widget.ImageView
import com.cocwidget.store.Icons
import kotlin.concurrent.thread

// Item pictures on a screen. Ones already cached show straight away. Missing ones download in the
// background, and the rows fill in when they arrive.
class Pictures(private val activity: Activity) {
    private val wanted = mutableMapOf<String, MutableList<ImageView>>()

    fun add(path: String?, view: ImageView) {
        if (path != null) wanted.getOrPut(path) { mutableListOf() } += view
    }

    fun clear() = wanted.clear()

    fun load() {
        val views = wanted.mapValues { it.value.toList() }
        thread {
            fun decode() = views.keys.mapNotNull { path -> Icons.load(activity, path)?.let { path to it } }.toMap()
            post(decode(), views)
            if (Icons.fetchPaths(activity.applicationContext, views.keys.toList())) post(decode(), views)
        }
    }

    private fun post(bitmaps: Map<String, Bitmap>, views: Map<String, List<ImageView>>) = activity.runOnUiThread {
        if (activity.isDestroyed) return@runOnUiThread
        for ((path, bitmap) in bitmaps) views[path]?.forEach { it.setImageBitmap(bitmap) }
    }
}
