package com.cocwidget

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Sideloaded apps don't update themselves, so we ask GitHub whether a newer release exists.
// The releases/latest endpoint skips pre-releases, so test builds never show up here.
object UpdateChecker {
    const val RELEASES_PAGE = "https://github.com/Moneymania758/CocWidget/releases/latest"
    private const val LATEST_API = "https://api.github.com/repos/Moneymania758/CocWidget/releases/latest"
    private const val CHECK_INTERVAL_MS = 12 * 60 * 60_000L

    // Network call, so only call this off the main thread.
    fun checkIfDue(context: Context) {
        val now = System.currentTimeMillis()
        if (now - Store.lastUpdateCheck(context) < CHECK_INTERVAL_MS) return
        // Recorded before the request so a failing network doesn't retry on every refresh.
        Store.setLastUpdateCheck(context, now)
        try {
            val connection = URL(LATEST_API).openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val latest = JSONObject(body).optString("tag_name").removePrefix("v")
                if (latest.isNotEmpty()) Store.setLatestVersion(context, latest)
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            // Offline. The next check is due in 12 hours.
        } catch (e: JSONException) {
            // Unexpected reply. Same as offline.
        }
    }

    // The newer version's name, or null when this install is up to date.
    fun availableUpdate(context: Context): String? {
        val latest = Store.latestVersion(context) ?: return null
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        return latest.takeIf { isNewer(it, installed) }
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
