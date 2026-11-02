package com.cocwidget

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup

// Lays children out in rows, starting a new row when the next one doesn't fit. Used for chips.
// Only ever made in code, never from a layout file, so it needs no XML constructor.
@SuppressLint("ViewConstructor")
class FlowLayout(context: Context, private val hGap: Int, private val vGap: Int) : ViewGroup(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var rowHeight = 0
        var widest = 0
        for (child in children()) {
            measureChild(child, MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST), heightMeasureSpec)
            if (x > 0 && x + child.measuredWidth > maxWidth) {
                y += rowHeight + vGap
                x = 0
                rowHeight = 0
            }
            x += child.measuredWidth + hGap
            widest = maxOf(widest, x - hGap)
            rowHeight = maxOf(rowHeight, child.measuredHeight)
        }
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(widthMeasureSpec) else widest + paddingLeft + paddingRight
        setMeasuredDimension(width, y + rowHeight + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxWidth = r - l - paddingLeft - paddingRight
        var x = 0
        var y = 0
        // Children in a row are centred on the row's height.
        val rows = mutableListOf<MutableList<View>>(mutableListOf())
        val heights = mutableListOf(0)
        for (child in children()) {
            if (x > 0 && x + child.measuredWidth > maxWidth) {
                rows += mutableListOf<View>()
                heights += 0
                x = 0
            }
            rows.last() += child
            heights[heights.lastIndex] = maxOf(heights.last(), child.measuredHeight)
            x += child.measuredWidth + hGap
        }
        for ((index, row) in rows.withIndex()) {
            x = 0
            val rowHeight = heights[index]
            for (child in row) {
                val top = paddingTop + y + (rowHeight - child.measuredHeight) / 2
                child.layout(paddingLeft + x, top, paddingLeft + x + child.measuredWidth, top + child.measuredHeight)
                x += child.measuredWidth + hGap
            }
            y += rowHeight + vGap
        }
    }

    private fun children() = (0 until childCount).map { getChildAt(it) }.filter { it.visibility != View.GONE }
}
