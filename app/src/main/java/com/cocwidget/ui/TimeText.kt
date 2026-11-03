package com.cocwidget.ui

import android.text.format.DateUtils
import java.text.DateFormat

// Times as the phone's own settings show them.
object TimeText {
    // When an export was taken: the time if it was today, otherwise the date.
    fun exported(millis: Long, now: Long): CharSequence =
        DateUtils.formatSameDayTime(millis, now, DateFormat.MEDIUM, DateFormat.SHORT)
}
