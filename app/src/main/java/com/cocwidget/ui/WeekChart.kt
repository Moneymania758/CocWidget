package com.cocwidget.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.cocwidget.guide.ChartTouch
import com.cocwidget.plan.Progress
import com.cocwidget.plan.Week
import java.time.ZoneId
import kotlin.math.abs

// A bar a week on the same panel as ProgressChart. [values] holds each week's bar, null for a
// week no two imports span, drawn as a short dash so it doesn't read as nothing done. A tap or
// drag picks a week.
@SuppressLint("ViewConstructor")
class WeekChart(
    context: Context,
    private val weeks: List<Week>,
    internal val values: List<Float?>,
    private val tickLabel: (Int) -> String,
    light: Boolean,
    private val onPick: (Int) -> Unit,
) : View(context) {
    var selected = weeks.lastIndex
        set(value) {
            field = value
            invalidate()
        }

    private val panelColour = if (light) 0xFFFFFFFF.toInt() else LevelChart.SURFACE
    private val density = resources.displayMetrics.density
    private fun sp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val axisText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (light) 0xFF645B53.toInt() else LevelChart.AXIS_TEXT; textSize = sp(11f) }
    private val grid = Paint().apply { color = if (light) 0xFFECE5DA.toInt() else LevelChart.GRID; strokeWidth = density }
    private val highlight = Paint().apply { color = if (light) 0xFFF1ECE4.toInt() else 0x22FFFFFF }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LevelChart.COLOURS[0] }
    private val dash = Paint().apply { color = if (light) 0xFFB8B0A6.toInt() else LevelChart.CROSSHAIR; strokeWidth = 2 * density }
    private val panel = Paint().apply { color = panelColour; isAntiAlias = true }
    private val bounds = RectF()
    private val shape = RectF()

    private val ticks = Progress.barTicks(values.filterNotNull().maxOrNull() ?: 0f)
    private val tickLabels = ticks.map(tickLabel)
    private val axisWidth = tickLabels.maxOf { axisText.measureText(it) }

    private val plotLeft get() = PAD * density + axisWidth + 6 * density
    private val plotRight get() = width - 14 * density
    private val plotTop get() = PAD * density + axisText.textSize / 2
    private val plotBottom get() = height - PAD * density - axisText.textSize - 6 * density
    private val slot get() = (plotRight - plotLeft) / weeks.size

    private val touch = ChartTouch(ViewConfiguration.get(context).scaledTouchSlop.toFloat())

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val wanted = (PLOT_HEIGHT * density + 2 * PAD * density + 1.5f * axisText.textSize + 6 * density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), resolveSize(wanted, heightSpec))
    }

    private fun centre(index: Int) = plotLeft + slot * (index + 0.5f)
    private fun y(value: Float) = plotBottom - (plotBottom - plotTop) * value / ticks.last()

    override fun onDraw(canvas: Canvas) {
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, 10 * density, 10 * density, panel)

        if (selected in weeks.indices) {
            shape.set(centre(selected) - slot / 2, plotTop, centre(selected) + slot / 2, plotBottom)
            canvas.drawRoundRect(shape, 6 * density, 6 * density, highlight)
        }
        axisText.textAlign = Paint.Align.RIGHT
        for ((index, tick) in ticks.withIndex()) {
            val tickY = y(tick.toFloat())
            canvas.drawLine(plotLeft, tickY, plotRight, tickY, grid)
            canvas.drawText(tickLabels[index], plotLeft - 6 * density, tickY + axisText.textSize / 3, axisText)
        }

        // Thin bars with a 2 dp gap either side and rounded tops, square on the baseline.
        val half = minOf(slot / 2 - 2 * density, BAR_WIDTH * density / 2)
        val radius = 4 * density
        for ((index, value) in values.withIndex()) {
            val x = centre(index)
            if (value == null) {
                canvas.drawLine(x - half / 2, plotBottom - density, x + half / 2, plotBottom - density, dash)
                continue
            }
            val top = y(value)
            if (plotBottom - top < 0.5f) continue
            shape.set(x - half, top, x + half, plotBottom)
            canvas.drawRoundRect(shape, radius, radius, bar)
            shape.set(x - half, maxOf(top, plotBottom - radius), x + half, plotBottom)
            canvas.drawRect(shape, bar)
        }
        drawDates(canvas)
    }

    // The first and last week, under their bars, so they never collide.
    private fun drawDates(canvas: Canvas) {
        val baseline = height - PAD * density
        axisText.textAlign = Paint.Align.LEFT
        canvas.drawText(TimeText.day(startMillis(0)), plotLeft, baseline, axisText)
        if (weeks.size > 1) {
            axisText.textAlign = Paint.Align.RIGHT
            canvas.drawText(TimeText.day(startMillis(weeks.lastIndex)), plotRight, baseline, axisText)
        }
    }

    private fun startMillis(index: Int) = weeks[index].start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> touch.down(event.x, event.y)
            MotionEvent.ACTION_MOVE -> if (touch.move(event.x, event.y)) {
                // A sideways drag belongs to the chart. Anything else scrolls the page as normal.
                parent?.requestDisallowInterceptTouchEvent(true)
                pick(event.x)
            }
            MotionEvent.ACTION_UP -> {
                if (touch.up(event.x, event.y)) pick(event.x)
                performClick()
            }
        }
        return true
    }

    private fun pick(touchX: Float) {
        val index = weeks.indices.minByOrNull { abs(centre(it) - touchX) } ?: return
        if (index != selected) onPick(index)
    }

    private companion object {
        const val PLOT_HEIGHT = 110f
        const val PAD = 10f
        const val BAR_WIDTH = 18f
    }
}
