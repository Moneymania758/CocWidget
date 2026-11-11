package com.cocwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PagingTest {
    @Test
    fun pagesRoundUpAndAnEmptyListStillHasOne() {
        assertEquals(3, Paging.count(9, 4))
        assertEquals(2, Paging.count(8, 4))
        assertEquals(1, Paging.count(0, 4))
    }

    @Test
    fun aPagePastTheEndComesBackToTheLast() {
        assertEquals(2, Paging.clamp(7, 3))
        assertEquals(0, Paging.clamp(-1, 3))
        assertEquals(0, Paging.clamp(5, 1))
    }

    @Test
    fun aSliceIsOnePage() {
        val rows = (1..9).toList()
        assertEquals(listOf(5, 6, 7, 8), Paging.slice(rows, 1, 4))
        assertEquals(listOf(9), Paging.slice(rows, 2, 4))
    }
}
