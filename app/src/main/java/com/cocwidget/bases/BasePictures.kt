package com.cocwidget.bases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// Screenshots of saved bases, one per base, kept in the app's own files. A picked or shared picture
// is copied in at once, made smaller, so it stays after the other app's permission ends.
object BasePictures {
    // Plenty for a phone screen, and keeps each file to a few hundred KB.
    private const val MAX_EDGE = 1600

    fun file(context: Context, id: String) = File(File(context.filesDir, "bases"), "$id.jpg")

    // Where a picture waits until its base is saved.
    fun draft(context: Context, name: String) = File(File(context.cacheDir, "base_drafts"), "$name.jpg")

    fun delete(context: Context, id: String) {
        file(context, id).delete()
    }

    // False if it can't be read as a picture.
    fun copy(context: Context, uri: Uri, to: File): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) {
            false
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = sample(longest, MAX_EDGE) }
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (decoded == null) false else write(fit(decoded), to)
        }
    } catch (_: Exception) {
        false
    } catch (_: OutOfMemoryError) {
        false
    }

    // Moves a draft to its base's file.
    fun keep(draft: File, to: File): Boolean {
        to.parentFile?.mkdirs()
        if (draft.renameTo(to)) return true
        return try {
            draft.copyTo(to, overwrite = true)
            draft.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Decoded at about [width] pixels wide, for showing. Null if it's missing or unreadable.
    fun load(file: File, width: Int): Bitmap? = try {
        if (!file.exists()) {
            null
        } else {
            // Through streams closed here. decodeFile kept the file open in the tests on Windows, so
            // a removed picture couldn't be deleted.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            file.inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply { inSampleSize = sample(bounds.outWidth, width) }
            file.inputStream().use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

    // The largest power of two that keeps [size] at or above [wanted].
    private fun sample(size: Int, wanted: Int): Int {
        var sample = 1
        while (wanted > 0 && size / (sample * 2) >= wanted) sample *= 2
        return sample
    }

    private fun fit(bitmap: Bitmap): Bitmap {
        val scale = MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale >= 1f) return bitmap
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun write(bitmap: Bitmap, to: File): Boolean {
        to.parentFile?.mkdirs()
        return FileOutputStream(to).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    }
}
