package com.cocwidget

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

// Lets the app picked in the share sheet read the one export being shared, as an attachment. Not
// exported, so only an app handed the link can reach it, and every link gives this one file.
class ExportProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) = MIME_TYPE

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(file(requireContext()), ParcelFileDescriptor.MODE_READ_ONLY)

    // Mail apps ask this for the attachment's name and size.
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
        val file = file(requireContext())
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns, 1).apply {
            addRow(columns.map {
                when (it) {
                    OpenableColumns.DISPLAY_NAME -> file.name
                    OpenableColumns.SIZE -> file.length()
                    else -> null
                }
            })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0

    companion object {
        const val MIME_TYPE = "application/json"

        fun file(context: Context) = File(context.cacheDir, "village_export.json")

        fun uri(context: Context): Uri = Uri.parse("content://${context.packageName}.export/${file(context).name}")
    }
}
