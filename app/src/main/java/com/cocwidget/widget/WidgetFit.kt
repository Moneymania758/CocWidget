package com.cocwidget.widget

// Which rows fit on the big widget. Its rows can't scroll (see WidgetLayoutTest), so without this
// anything too tall is cut off with no hint. Every section keeps its header, as that's how to get
// to its rows. Rows go to the sections from the top, and each section with rows left out gets a
// "more" line.
object WidgetFit {
    // The fixed height of a row in widget_row.xml.
    const val ROW_DP = 38

    // Launchers round their sizes, so keep a little back rather than clip a row by a pixel.
    private const val SAFETY_DP = 4

    // The height left for headers, rows and more lines in a widget [height] tall, from widget.xml.
    // [text] gives the height in dp of a line of text at a size in sp.
    fun roomForRows(height: Int, viewButtons: Boolean, helpers: Boolean, text: (Int) -> Int): Int {
        val titleRow = maxOf(text(18), maxOf(text(14), 16) + 10)
        val buttons = if (viewButtons) 6 + text(11) + 6 else 0
        val helperLine = if (helpers) 4 + text(11) else 0
        val footer = 4 + text(11)
        return height - 2 * 12 - titleRow - buttons - helperLine - footer - SAFETY_DP
    }

    // A section header in widget_section.xml, as tall as it gets with a boost chip showing.
    fun headerDp(text: (Int) -> Int): Int = 8 + 4 + maxOf(text(14), 2 + 2 + maxOf(text(12), 16))

    // A more line in widget_more.xml.
    fun moreDp(text: (Int) -> Int): Int = maxOf(26, text(13))

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
