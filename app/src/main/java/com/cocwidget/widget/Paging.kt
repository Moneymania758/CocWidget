package com.cocwidget.widget

// A Pages widget's maths. A page kept from an earlier draw can be past the end once upgrades
// finish and are cleared, so it is pulled back to the last page.
object Paging {
    fun count(rows: Int, perPage: Int): Int = maxOf(1, (rows + perPage - 1) / maxOf(1, perPage))

    fun clamp(page: Int, pages: Int): Int = page.coerceIn(0, maxOf(0, pages - 1))

    fun <T> slice(rows: List<T>, page: Int, perPage: Int): List<T> = rows.drop(page * perPage).take(perPage)
}
