package com.cocwidget.widget

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// When something finishes, as a widget row shows it beside its countdown: "19:42" today, "Thu 09:05"
// within the week, "14 Nov 09:05" further off. The time follows the phone's 12 or 24 hour setting.
object FinishTime {
    private const val WEEK_MS = 7 * Countdown.DAY_MS

    fun of(context: Context, atMillis: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = DateFormat.getTimeFormat(context).apply { timeZone = java.util.TimeZone.getTimeZone(zone) }.format(Date(atMillis))
        val day = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (day == today) return time
        val locale: Locale = context.resources.configuration.locales[0]
        val pattern = if (atMillis - now < WEEK_MS) "EEE" else "d MMM"
        return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(atMillis).atZone(zone)) + " " + time
    }
}
