package com.cocwidget.events

import android.content.Context
import com.cocwidget.store.Store
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

// The dates file: one-off events and corrections to the rules. It ships in the app, and the same
// file is downloaded from GitHub, so a date can be fixed without a release. It holds event kinds
// and dates only. The notification words are in the app, so a download can't put words on anyone's
// phone.
object EventDates {
    private const val URL_DATES = "https://raw.githubusercontent.com/Moneymania758/CocWidget/main/app/src/main/assets/events.json"
    private const val ASSET = "events.json"
    private const val CHECK_INTERVAL_MS = 12 * 60 * 60_000L
    private const val TIMEOUT_MS = 3000
    private const val FORMAT_VERSION = 1
    private const val MAX_CHARS = 64 * 1024
    private const val MAX_EVENTS = 200
    private const val MAX_ID_LENGTH = 64

    // [cancelled] holds ids of rule events that aren't happening.
    class Dates(val updated: String, val events: List<EventSchedule.Occurrence>, val cancelled: Set<String>)

    // What the app knows now: the bundled file, or the saved download if it's at least as new.
    fun current(context: Context): Dates {
        val bundled = parse(bundledText(context)) ?: Dates("", emptyList(), emptySet())
        val saved = parse(Store.eventDates(context)) ?: return bundled
        return newest(saved, bundled)
    }

    // True if a new file was saved. [download] is network, so only call this off the main thread.
    fun refreshIfDue(context: Context, now: Long, download: () -> String? = ::fromGitHub): Boolean {
        val last = Store.lastEventDatesCheck(context)
        if (now >= last && now - last < CHECK_INTERVAL_MS) return false
        // Recorded before the request so a failing network doesn't retry on every refresh.
        Store.setLastEventDatesCheck(context, now)
        val text = download() ?: return false
        if (parse(text) == null) return false
        Store.setEventDates(context, text)
        return true
    }

    fun newest(first: Dates, second: Dates?): Dates =
        if (second == null || first.updated >= second.updated) first else second

    // Null when [text] isn't a dates file this version understands. Entries it can't read are
    // skipped, so a new kind of event in the file doesn't stop older installs using the rest.
    fun parse(text: String?): Dates? {
        if (text == null) return null
        try {
            val root = JSONObject(text)
            if (root.optInt("version") != FORMAT_VERSION) return null
            val updated = root.getString("updated")
            LocalDate.parse(updated)
            val entries = root.getJSONArray("events")
            val events = ArrayList<EventSchedule.Occurrence>()
            val cancelled = HashSet<String>()
            for (i in 0 until minOf(entries.length(), MAX_EVENTS)) {
                val entry = entries.optJSONObject(i) ?: continue
                val id = entry.optString("id")
                val kind = EventKind.of(entry.optString("kind"))
                if (id.isEmpty() || id.length > MAX_ID_LENGTH || kind == null) continue
                if (entry.optBoolean("cancelled")) {
                    cancelled += id
                    continue
                }
                occurrence(entry, id, kind)?.let { events += it }
            }
            return Dates(updated, events, cancelled)
        } catch (e: JSONException) {
            return null
        } catch (e: DateTimeParseException) {
            return null
        }
    }

    fun fromGitHub(): String? = try {
        val connection = URL(URL_DATES).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) readLimited(connection) else null
        } finally {
            connection.disconnect()
        }
    } catch (e: IOException) {
        // Offline. The next check is due in 12 hours.
        null
    }

    // A file cut off at the limit isn't valid JSON, so it's refused like any other bad download.
    private fun readLimited(connection: HttpURLConnection): String = connection.inputStream.bufferedReader().use { reader ->
        val buffer = CharArray(MAX_CHARS)
        var filled = 0
        while (filled < MAX_CHARS) {
            val count = reader.read(buffer, filled, MAX_CHARS - filled)
            if (count < 0) break
            filled += count
        }
        String(buffer, 0, filled)
    }

    private fun occurrence(entry: JSONObject, id: String, kind: EventKind): EventSchedule.Occurrence? {
        val start = instant(entry.optString("start")) ?: return null
        val endText = entry.optString("end")
        val end = if (endText.isEmpty()) null else instant(endText) ?: return null
        if (end != null && end <= start) return null
        return EventSchedule.Occurrence(id, kind, start, end)
    }

    private fun instant(text: String): Long? = try {
        Instant.parse(text).toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    private fun bundledText(context: Context): String? = try {
        context.assets.open(ASSET).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        null
    }
}
