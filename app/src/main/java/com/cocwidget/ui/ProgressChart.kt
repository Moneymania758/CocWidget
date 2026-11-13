package com.cocwidget.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.cocwidget.guide.ChartTouch
import com.cocwidget.plan.Progress
import com.cocwidget.plan.ProgressPoint
import kotlin.math.abs

// Upgrade time done over time, one point a day, on the same panel as LevelChart. Points sit by
// date, as imports come at uneven times. Each Town Hall is its own stretch of line, labelled where
// it starts, as a new one starts lower. A tap or drag picks a point.
@SuppressLint("ViewConstructor")
class ProgressChart(
    context: Context,
    private val points: List<ProgressPoint>,
    // The Light skin's white panel instead of the dark one.
    light: Boolean,
    private val onPick: (Int) -> Unit,
) : View(context) {
    var selected = points.lastIndex
        set(value) {
            field = value
            invalidate()
        }

    private val panelColour = if (light) 0xFFFFFFFF.toInt() else LevelChart.SURFACE
    private val density = resources.displayMetrics.density
    private fun sp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val axisText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (light) 0xFF645B53.toInt() else LevelChart.AXIS_TEXT; textSize = sp(11f) }
    private val grid = Paint().apply { color = if (light) 0xFFECE5DA.toInt() else LevelChart.GRID; strokeWidth = density }
    private val cross = Paint().apply { color = if (light) 0xFF8A8580.toInt() else LevelChart.CROSSHAIR; strokeWidth = density }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = LevelChart.COLOURS[0]
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panel = Paint().apply { color = panelColour; isAntiAlias = true }
    private val path = Path()
    private val bounds = RectF()

    private val ticks = Progress.ticks(points.minOf { it.done }, points.maxOf { it.done })
    private val tickLabels = ticks.map { "$it%" }
    private val axisWidth = tickLabels.maxOf { axisText.measureText(it) }
    private val first = points.first().exportedAtMillis
    private val span = (points.last().exportedAtMillis - first).coerceAtLeast(1)
    private val stretchStarts = Progress.stretches(points).drop(1).map { points.indexOf(it.first()) }

    private val plotLeft get() = PAD * density + axisWidth + 6 * density
    private val plotRight get() = width - 14 * density
    // Room above for a Town Hall label.
    private val plotTop get() = PAD * density + axisText.textSize + 6 * density
    private val plotBottom get() = height - PAD * density - axisText.textSize - 6 * density

    private val touch = ChartTouch(ViewConfiguration.get(context).scaledTouchSlop.toFloat())

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val wanted = (PLOT_HEIGHT * density + 2 * PAD * density + 2 * axisText.textSize + 12 * density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), resolveSize(wanted, heightSpec))
    }

    private fun x(index: Int) =
        if (points.size <= 1) (plotLeft + plotRight) / 2 else plotLeft + (plotRight - plotLeft) * (points[index].exportedAtMillis - first) / span
    private fun y(done: Float): Float {
        val percent = done * 100
        return plotBottom - (plotBottom - plotTop) * (percent - ticks.first()) / (ticks.last() - ticks.first())
    }

    override fun onDraw(canvas: Canvas) {
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, 10 * density, 10 * density, panel)

        axisText.textAlign = Paint.Align.RIGHT
        for ((index, tick) in ticks.withIndex()) {
            val tickY = y(tick / 100f)
            canvas.drawLine(plotLeft, tickY, plotRight, tickY, grid)
            canvas.drawText(tickLabels[index], plotLeft - 6 * density, tickY + axisText.textSize / 3, axisText)
        }
        drawDates(canvas)
        if (selected in points.indices) canvas.drawLine(x(selected), plotTop, x(selected), plotBottom, cross)

        // Each stretch drawn on its own, so the drop to a new Town Hall isn't a line.
        path.reset()
        for (index in points.indices) {
            val pointY = y(points[index].done)
            if (index == 0 || index in stretchStarts) path.moveTo(x(index), pointY) else path.lineTo(x(index), pointY)
        }
        canvas.drawPath(path, line)
        drawTownHalls(canvas)

        drawDot(canvas, points.lastIndex, 4f)
        if (selected in points.indices) drawDot(canvas, selected, 5f)
    }

    // The first and last day, at the ends, so they never collide.
    private fun drawDates(canvas: Canvas) {
        val baseline = height - PAD * density
        axisText.textAlign = Paint.Align.LEFT
        canvas.drawText(TimeText.day(first), plotLeft, baseline, axisText)
        if (points.size > 1) {
            axisText.textAlign = Paint.Align.RIGHT
            canvas.drawText(TimeText.day(points.last().exportedAtMillis), plotRight, baseline, axisText)
        }
    }

    // "TH17" over the first point of each new Town Hall, with a short tick down to it.
    private fun drawTownHalls(canvas: Canvas) {
        axisText.textAlign = Paint.Align.CENTER
        for (index in stretchStarts) {
            val label = "TH" + points[index].townHall
            val half = axisText.measureText(label) / 2
            val labelX = x(index).coerceIn(plotLeft + half, plotRight - half)
            canvas.drawText(label, labelX, PAD * density + axisText.textSize, axisText)
            canvas.drawLine(x(index), plotTop - 2 * density, x(index), y(points[index].done) - 6 * density, cross)
        }
    }

    // A dot with a ring in the panel's colour, so it stays clear where it crosses a line.
    private fun drawDot(canvas: Canvas, index: Int, radius: Float) {
        val pointY = y(points[index].done)
        dot.color = panelColour
        canvas.drawCircle(x(index), pointY, (radius + 2) * density, dot)
        dot.color = LevelChart.COLOURS[0]
        canvas.drawCircle(x(index), pointY, radius * density, dot)
    }

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
        val index = points.indices.minByOrNull { abs(x(it) - touchX) } ?: return
        if (index != selected) onPick(index)
    }

    private companion object {
        const val PLOT_HEIGHT = 140f
        const val PAD = 10f
    }
}
