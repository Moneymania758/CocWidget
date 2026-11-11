package com.cocwidget.bases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// How saved bases are kept and which ones each account sees.
class MyBasesTest {
    private val link = "https://link.clashofclans.com/en/?action=OpenLayout&id=TH16%3AHV%3AAAAA"
    private val war = SavedBase("a", "#TEST0000", link, "War one", BaseKind.WAR, 100, 200)
    private val plain = SavedBase("b", "", "$link-2", "", null, 50)

    @Test
    fun basesComeBackAsSaved() {
        assertEquals(listOf(war, plain), MyBases.parse(MyBases.toJson(listOf(war, plain))))
        assertEquals(16, war.townHall)
    }

    @Test
    fun brokenOrUnknownEntriesAreSkippedNotFatal() {
        assertTrue(MyBases.parse(null).isEmpty())
        assertTrue(MyBases.parse("not json").isEmpty())
        val read = MyBases.parse("""[{"id":"x","link":"$link","kind":"gone"},{"id":"","link":"$link"},{"id":"y"},7]""")
        assertEquals(listOf("x"), read.map { it.id })
        assertNull(read.single().kind)
    }

    @Test
    fun eachAccountSeesItsOwnAndOnesSavedBeforeAnyAccount() {
        val other = war.copy(id = "c", account = "#TEST0001")
        val all = listOf(war, plain, other)
        assertEquals(listOf("a", "b"), MyBases.forAccount(all, "#TEST0000").map { it.id })
        assertEquals(listOf("b", "c"), MyBases.forAccount(all, "#TEST0001").map { it.id })
        assertEquals(listOf("b"), MyBases.forAccount(all, null).map { it.id })
    }

    @Test
    fun putReplacesInPlaceOrAddsAtTheTop() {
        val renamed = war.copy(name = "Renamed")
        assertEquals(listOf(renamed, plain), MyBases.put(listOf(war, plain), renamed))
        val added = war.copy(id = "new")
        assertEquals(listOf(added, war, plain), MyBases.put(listOf(war, plain), added))
        assertEquals(war, MyBases.matching(listOf(plain, war), "#TEST0000", link))
        assertNull(MyBases.matching(listOf(war), "#TEST0001", link))
    }
}
