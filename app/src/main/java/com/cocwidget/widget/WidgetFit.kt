package com.cocwidget.widget

// How much of the big widget's list is cut off, for a hint in its footer. Its rows can't scroll (see
// WidgetLayoutTest), so anything too tall is clipped at the bottom. This only ever counts: 2.7.3 used
// it to leave rows out, and normal sized widgets lost most of their upgrades.
object WidgetFit {
    // The fixed height of a row in widget_row.xml.
    const val ROW_DP = 38

    // Launchers round their sizes, so keep a little back rather than miss a clipped row.
    private const val SAFETY_DP = 4

    // A section's upgrades, and whether it's folded down to its header.
    class Section(val rows: Int, val folded: Boolean)

    // [hidden] upgrades don't fully show, starting in the section at position [section].
    class Hint(val hidden: Int, val section: Int)

    // The height left for headers and rows in a widget [height] tall, from widget.xml.
    // [text] gives the height in dp of a line of text at a size in sp.
    fun roomForRows(height: Int, viewButtons: Boolean, helpers: Boolean, text: (Int) -> Int): Int {
        val titleRow = maxOf(text(18), maxOf(text(14), 16) + 10)
        val buttons = if (viewButtons) 6 + text(11) + 6 else 0
        val helperLine = if (helpers) 4 + text(11) else 0
        val footer = 4 + maxOf(text(11), 2 + text(12) + 2)
        return height - 2 * 12 - titleRow - buttons - helperLine - footer - SAFETY_DP
    }

    // A section header in widget_section.xml, as tall as it gets with a boost chip showing.
    fun headerDp(text: (Int) -> Int): Int = 8 + 4 + maxOf(text(14), 2 + 2 + maxOf(text(12), 16))

    // Null when everything fits, or when the height isn't known. The list runs in order, so once
    // one thing doesn't fit, nothing after it shows either.
    fun hint(sections: List<Section>, available: Int?, headerDp: Int, rowDp: Int = ROW_DP): Hint? {
        if (available == null) return null
        var left: Int = available
        var hidden = 0
        var first = -1
        for ((index, section) in sections.withIndex()) {
            val headerFits = left >= headerDp
            left = if (headerFits) left - headerDp else -1
            val shown = when {
                section.folded -> if (headerFits) section.rows else 0
                headerFits -> minOf(section.rows, left / rowDp)
                else -> 0
            }
            if (!section.folded) left -= shown * rowDp
            if (shown < section.rows) {
                hidden += section.rows - shown
                if (first < 0) first = index
                left = -1
            }
        }
        return if (hidden > 0) Hint(hidden, first) else null
    }
}
