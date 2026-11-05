package com.cocwidget.widget

// Which rows fit on the big widget. Its rows can't scroll (see WidgetLayoutTest), so without this
// anything too tall is cut off with no hint. Every section keeps its header, as that's how to get
// to its rows. Rows go to the sections from the top, and each section with rows left out gets a
// "more" line.
object WidgetFit {
    // The fixed height of a row in widget_row.xml.
    const val ROW_DP = 38

    // How many rows to show in each section, given how many each has. A folded section has none.
    // [available] is the height for headers, rows and more lines, or null if it isn't known.
    fun rowsToShow(rows: List<Int>, available: Int?, headerDp: Int, moreDp: Int, rowDp: Int = ROW_DP): List<Int> {
        if (available == null) return rows
        val headers = rows.size * headerDp
        if (headers + rows.sum() * rowDp <= available) return rows

        // Down the sections, always keeping room for a more line for each one still to come.
        var left = available - headers
        return rows.mapIndexed { index, count ->
            val later = rows.drop(index + 1).count { it > 0 } * moreDp
            if (count * rowDp + later <= left) {
                left -= count * rowDp
                count
            } else {
                val shown = ((left - later - moreDp).coerceAtLeast(0) / rowDp).coerceAtMost(count - 1)
                left -= shown * rowDp + moreDp
                shown
            }
        }
    }
}
