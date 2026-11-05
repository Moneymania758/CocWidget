package com.cocwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import kotlin.math.ceil

// How much room one big widget has. Rendering runs off the main thread, where Views can't be
// measured, so this works from the font's real metrics (which follow the phone's font size) and the
// layouts' fixed sizes in WidgetFit.
object WidgetMeasure {
    class Room(val rows: Int, val header: Int)

    // Null when the launcher hasn't said how tall the widget is, and the widget then gives no hint.
    fun of(context: Context, manager: AppWidgetManager, widgetId: Int, viewButtons: Boolean, helpers: Boolean): Room? {
        val options = manager.getAppWidgetOptions(widgetId)
        // Launchers report the tallest height for portrait and the shortest for landscape.
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (height <= 0) return null

        val metrics = context.resources.displayMetrics
        val paint = TextPaint().apply { typeface = Typeface.create("sans-serif-black", Typeface.NORMAL) }
        val text: (Int) -> Int = { sp ->
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics)
            val line = paint.fontMetricsInt
            ceil((line.bottom - line.top) / metrics.density).toInt()
        }
        return Room(WidgetFit.roomForRows(height, viewButtons, helpers, text), WidgetFit.headerDp(text))
    }
}
