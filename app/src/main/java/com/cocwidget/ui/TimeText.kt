package com.cocwidget.ui

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Locale

// Times as the phone's own settings show them.
object TimeText {
    // When an export was taken: the time if it was today, otherwise the date.
    fun exported(millis: Long, now: Long): CharSequence =
        DateUtils.formatSameDayTime(millis, now, DateFormat.MEDIUM, DateFormat.SHORT)

    // "6 Nov", in the phone's own order.
    fun day(millis: Long): String =
        android.text.format.DateFormat.format(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMM"), millis).toString()
}
