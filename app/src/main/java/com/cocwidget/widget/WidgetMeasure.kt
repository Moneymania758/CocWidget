package com.cocwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.cocwidget.R

// How much room one big widget has, measured from its real layouts so the phone's font size counts.
object WidgetMeasure {
    // Launchers round their sizes, so keep a little back rather than clip a row by a pixel.
    private const val SAFETY_DP = 4

    // Heights in dp. [rows] is what's left for headers, rows and more lines.
    class Room(val rows: Int, val header: Int, val more: Int)

    // Null when the launcher hasn't said how big the widget is, or the layouts can't be measured.
    // The widget then shows every row and leaves any overflow cut off, as it did before.
    fun of(context: Context, manager: AppWidgetManager, widgetId: Int, viewButtons: Boolean, helpers: Boolean, update: Boolean): Room? {
        val options = manager.getAppWidgetOptions(widgetId)
        // Portrait uses the smallest width and tallest height the launcher reports, landscape the opposite.
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (width <= 0 || height <= 0) return null

        return try {
            val density = context.resources.displayMetrics.density
            val widthPx = (width * density).toInt()
            val inflater = LayoutInflater.from(context)
            fun heightDp(view: View): Int {
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                )
                return (view.measuredHeight / density).toInt() + 1
            }

            val frame = inflater.inflate(R.layout.widget, null)
            frame.findViewById<View>(R.id.empty).visibility = View.GONE
            frame.findViewById<View>(R.id.views).visibility = if (viewButtons) View.VISIBLE else View.GONE
            frame.findViewById<View>(R.id.helpers).visibility = if (helpers) View.VISIBLE else View.GONE
            frame.findViewById<View>(R.id.update).visibility = if (update) View.VISIBLE else View.GONE
            frame.findViewById<View>(R.id.footer).visibility = View.VISIBLE

            // With a boost chip showing, the tallest a header gets.
            val header = inflater.inflate(R.layout.widget_section, null)
            header.findViewById<View>(R.id.badge_chip).visibility = View.VISIBLE
            header.findViewById<TextView>(R.id.label).text = context.getString(R.string.app_name)

            val more = inflater.inflate(R.layout.widget_more, null)
            Room(height - heightDp(frame) - SAFETY_DP, heightDp(header), heightDp(more))
        } catch (e: RuntimeException) {
            null
        }
    }
}
