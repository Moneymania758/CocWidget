package com.cocwidget.ui

import android.content.Context
import com.cocwidget.R
import com.cocwidget.plan.ProgressPoint
import java.util.Locale
import kotlin.math.roundToInt

// Progress figures as the Time to max card and the Progress page both word them.
object ProgressText {
    // "81.4%"
    fun percent(done: Float): String = String.format(Locale.getDefault(), "%.1f%%", tenths(done) / 10.0)

    // "+2.6%" between two shares as shown, so it always matches the two figures on screen. Null
    // when they show the same.
    fun change(now: Float, then: Float): String? {
        val tenths = tenths(now) - tenths(then)
        return if (tenths == 0) null else String.format(Locale.getDefault(), "%+.1f%%", tenths / 10.0)
    }

    // "+2.6% since 6 Nov", or why there's nothing to compare with yet.
    fun since(context: Context, latest: ProgressPoint, earlier: ProgressPoint?): String {
        if (earlier == null) return context.getString(R.string.progress_first)
        val day = TimeText.day(earlier.exportedAtMillis)
        if (earlier.townHall != latest.townHall) return context.getString(R.string.progress_new_town_hall, day)
        val change = change(latest.done, earlier.done) ?: return context.getString(R.string.progress_same, day)
        return context.getString(R.string.progress_since, change, day)
    }

    // Whether [since] says it went up, for its colour.
    fun wentUp(latest: ProgressPoint, earlier: ProgressPoint?): Boolean =
        earlier != null && earlier.townHall == latest.townHall && tenths(latest.done) > tenths(earlier.done)

    private fun tenths(done: Float) = (done * 1000).roundToInt()
}
