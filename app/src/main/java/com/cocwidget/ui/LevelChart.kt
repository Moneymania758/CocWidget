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
import com.cocwidget.guide.ChartScale
import com.cocwidget.guide.UnitChart
import kotlin.math.abs

// A chart of how a unit's stats grow with its level. It always sits on its own dark panel, so the
// checked line colours work the same in all three skins. A tap or drag picks a level.
@SuppressLint("ViewConstructor")
class LevelChart(
    context: Context,
    private val chart: UnitChart,
    private val levels: IntArray,
    // Levels past this position are out of reach at the player's Town Hall. -1 for no limit.
    private val capIndex: Int,
    // The player's own level, ringed on the line. -1 for none.
    private val myIndex: Int,
    private val onPick: (Int) -> Unit,
) : View(context) {
    var selected = levels.lastIndex
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private fun sp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val axisText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AXIS_TEXT; textSize = sp(11f) }
    private val endText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PRIMARY_TEXT; textSize = sp(12f); isFakeBoldText = true }
    private val grid = Paint().apply { color = GRID; strokeWidth = density }
    private val cross = Paint().apply { color = CROSSHAIR; strokeWidth = density }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panel = Paint().apply { color = SURFACE; isAntiAlias = true }
    private val path = Path()
    private val bounds = RectF()

    private val highest = chart.series.flatMap { it.values.filterNotNull() }.maxOrNull() ?: 0.0
    private val ticks = ChartScale.ticks(highest)
    private val axisMax = ticks.last()
    private val tickLabels = ticks.map { ChartScale.axisLabel(it, axisMax, chart.unit) }
    private val axisWidth = tickLabels.maxOf { axisText.measureText(it) }

    private val plotLeft get() = PAD * density + axisWidth + 6 * density
    private val plotRight get() = width - 14 * density
    // Room above for the last value's label.
    private val plotTop get() = PAD * density + endText.textSize + 6 * density
    private val plotBottom get() = height - PAD * density - axisText.textSize - 6 * density

    private var downX = 0f
    private var downY = 0f

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val wanted = (PLOT_HEIGHT * density + 2 * PAD * density + axisText.textSize + endText.textSize + 12 * density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), resolveSize(wanted, heightSpec))
    }

    private fun x(index: Int) = if (levels.size <= 1) plotLeft else plotLeft + (plotRight - plotLeft) * index / levels.lastIndex
    private fun y(value: Double) = plotBottom - ((plotBottom - plotTop) * (value / axisMax)).toFloat()

    override fun onDraw(canvas: Canvas) {
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, 10 * density, 10 * density, panel)

        axisText.textAlign = Paint.Align.RIGHT
        for ((index, tick) in ticks.withIndex()) {
            val tickY = y(tick)
            canvas.drawLine(plotLeft, tickY, plotRight, tickY, grid)
            canvas.drawText(tickLabels[index], plotLeft - 6 * density, tickY + axisText.textSize / 3, axisText)
        }
        drawLevelLabels(canvas)
        if (selected in levels.indices) canvas.drawLine(x(selected), plotTop, x(selected), plotBottom, cross)

        for ((index, series) in chart.series.withIndex()) drawSeries(canvas, series.values, COLOURS[index % COLOURS.size])
        drawEndLabels(canvas)
    }

    private fun drawLevelLabels(canvas: Canvas) {
        val widest = levels.maxOf { axisText.measureText(it.toString()) } + 10 * density
        val step = ChartScale.labelStep(levels.size, plotRight - plotLeft, widest)
        axisText.textAlign = Paint.Align.CENTER
        val baseline = height - PAD * density
        val last = levels.lastIndex
        var index = 0
        while (index <= last) {
            // The last level's label replaces a neighbour that would touch it.
            if (index == last || x(last) - x(index) >= widest) canvas.drawText(levels[index].toString(), x(index), baseline, axisText)
            index += step
        }
        if (last % step != 0) canvas.drawText(levels[last].toString(), x(last), baseline, axisText)
    }

    private fun drawSeries(canvas: Canvas, values: List<Double?>, colour: Int) {
        // Drawn twice: the part out of reach at this Town Hall recedes into grey.
        val reachEnd = if (capIndex in values.indices) capIndex else values.lastIndex
        if (reachEnd < values.lastIndex) {
            line.color = OUT_OF_REACH
            strokeBetween(canvas, values, reachEnd, values.lastIndex)
        }
        line.color = colour
        strokeBetween(canvas, values, 0, reachEnd)

        values.indices.lastOrNull { values[it] != null }?.let { last ->
            drawDot(canvas, last, values[last]!!, if (last > reachEnd) OUT_OF_REACH else colour, 4f)
        }
        if (myIndex in values.indices) values[myIndex]?.let { value ->
            dot.color = SURFACE
            canvas.drawCircle(x(myIndex), y(value), 7 * density, dot)
            dot.style = Paint.Style.STROKE
            dot.strokeWidth = 2 * density
            dot.color = PRIMARY_TEXT
            canvas.drawCircle(x(myIndex), y(value), 6 * density, dot)
            dot.style = Paint.Style.FILL
        }
        if (selected in values.indices) values[selected]?.let { value -> drawDot(canvas, selected, value, colour, 5f) }
    }

    private fun strokeBetween(canvas: Canvas, values: List<Double?>, from: Int, to: Int) {
        path.reset()
        var drawing = false
        for (index in from..to) {
            val value = values[index]
            if (value == null) {
                drawing = false
            } else {
                if (drawing) path.lineTo(x(index), y(value)) else path.moveTo(x(index), y(value))
                drawing = true
            }
        }
        canvas.drawPath(path, line)
    }

    // A dot with a ring in the panel's colour, so it stays clear where it crosses a line.
    private fun drawDot(canvas: Canvas, index: Int, value: Double, colour: Int, radius: Float) {
        dot.color = SURFACE
        canvas.drawCircle(x(index), y(value), (radius + 2) * density, dot)
        dot.color = colour
        canvas.drawCircle(x(index), y(value), radius * density, dot)
    }

    // Each line's top value beside its end, unless it would sit on another. The legend and the
    // readout above the chart carry the rest.
    private fun drawEndLabels(canvas: Canvas) {
        endText.textAlign = Paint.Align.RIGHT
        val used = mutableListOf<Float>()
        for (series in chart.series) {
            val value = series.values.lastOrNull { it != null } ?: continue
            val labelY = y(value) - 9 * density
            if (used.any { abs(it - labelY) < endText.textSize + 2 * density }) continue
            used += labelY
            canvas.drawText(ChartScale.value(value, chart.unit), plotRight + 8 * density, labelY.coerceAtLeast(endText.textSize + 2 * density), endText)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                pick(event.x)
            }
            MotionEvent.ACTION_MOVE -> {
                // A sideways drag belongs to the chart. Anything else scrolls the page as normal.
                if (abs(event.x - downX) > abs(event.y - downY)) parent?.requestDisallowInterceptTouchEvent(true)
                pick(event.x)
            }
            MotionEvent.ACTION_UP -> performClick()
        }
        return true
    }

    private fun pick(touchX: Float) {
        val index = ChartScale.nearest(touchX, plotLeft, plotRight, levels.size)
        if (index != selected) onPick(index)
    }

    companion object {
        private const val PLOT_HEIGHT = 120f
        private const val PAD = 10f

        // Checked with the data visualisation palette validator against SURFACE: colourblind
        // separation, normal vision and 3:1 contrast all pass for these three, in this order.
        val COLOURS = intArrayOf(0xFF3987E5.toInt(), 0xFFD95926.toInt(), 0xFF199E70.toInt())
        private const val SURFACE = 0xFF1A1A19.toInt()
        private const val PRIMARY_TEXT = 0xFFF0EFEC.toInt()
        private const val AXIS_TEXT = 0xFFC3C2B7.toInt()
        private const val GRID = 0xFF33332F.toInt()
        private const val CROSSHAIR = 0xFF8A8982.toInt()
        private const val OUT_OF_REACH = 0xFF65645E.toInt()
    }
}
