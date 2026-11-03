package com.cocwidget.store

import android.content.SharedPreferences
import com.cocwidget.game.STUDY_SOUP_MULTIPLIER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Up to 1.9.1 the one account lived in the app-wide file. Losing it on update would mean
// everyone syncing again and losing their boost answers, so the move is tested here.
class AccountsTest {
    private val global = FakePrefs()
    private val files = mutableMapOf<String, FakePrefs>()
    private fun account(tag: String) = files.getOrPut(Store.fileName(tag)) { FakePrefs() }

    private fun oldInstall() {
        global.edit()
            .putString("export", """{"tag":"#TEST0000","timestamp":1800000000}""")
            .putInt("lab_multiplier", STUDY_SOUP_MULTIPLIER)
            .putString("name", "Ben")
            .putStringSet("notified_upgrades", setOf("a", "b"))
            .putString("skin", "game")
            .putBoolean("event_discount_on", true)
            .commit()
    }

    @Test
    fun movesTheOldAccountIntoItsOwnFile() {
        oldInstall()
        Store.migrate(global, ::account)

        val moved = account("#TEST0000")
        assertEquals("""{"tag":"#TEST0000","timestamp":1800000000}""", moved.getString("export", null))
        assertEquals(STUDY_SOUP_MULTIPLIER, moved.getInt("lab_multiplier", 0))
        assertEquals("Ben", moved.getString("name", null))
        assertEquals(setOf("a", "b"), moved.getStringSet("notified_upgrades", null))
        assertEquals("#TEST0000", global.getString("accounts", null))
        assertEquals("#TEST0000", global.getString("selected_account", null))
    }

    @Test
    fun appWideSettingsStayAndAccountKeysGo() {
        oldInstall()
        Store.migrate(global, ::account)

        assertEquals("game", global.getString("skin", null))
        assertTrue(global.getBoolean("event_discount_on", false))
        assertFalse(global.contains("export"))
        assertFalse(global.contains("name"))
        assertFalse(global.contains("lab_multiplier"))
    }

    @Test
    fun aFreshInstallHasNothingToMove() {
        Store.migrate(global, ::account)
        assertTrue(files.isEmpty())
        assertFalse(global.contains("accounts"))
    }

    // A crash after the account's file is written but before the old keys go runs it again.
    @Test
    fun runningTwiceDoesNotAddTheAccountTwice() {
        oldInstall()
        Store.migrate(global, ::account)
        oldInstall()
        Store.migrate(global, ::account)
        assertEquals("#TEST0000", global.getString("accounts", null))
    }

    @Test
    fun widgetsFallBackToTheFirstAccount() {
        val accounts = listOf("#AAA", "#BBB")
        assertEquals("#BBB", Store.resolve("#BBB", accounts))
        assertEquals("#AAA", Store.resolve(null, accounts))
        assertEquals("#AAA", Store.resolve("#REMOVED", accounts))
        assertNull(Store.resolve(null, emptyList()))
    }

    @Test
    fun exportsAreFiledByTheirTag() {
        assertEquals("#TEST0001", Store.tagOf("""{"tag":"#TEST0001"}"""))
        assertEquals("?", Store.tagOf("""{"timestamp":1}"""))
        assertEquals("?", Store.tagOf("not json"))
        assertEquals("account_TEST0001", Store.fileName("#TEST0001"))
        assertEquals(listOf("#A", "#B"), Store.added(listOf("#A", "#B"), "#A"))
    }

    // Enough of SharedPreferences for the store's own calls. Writes land straight away.
    private class FakePrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = values.toMap()
        override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: Set<String>?) = values[key] as Set<String>? ?: defValues
        override fun getInt(key: String, defValue: Int) = values[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as Boolean? ?: defValue
        override fun contains(key: String) = key in values
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clear = false

            override fun putString(key: String, value: String?) = apply { changes[key] = value }
            override fun putStringSet(key: String, values: Set<String>?) = apply { changes[key] = values?.toSet() }
            override fun putInt(key: String, value: Int) = apply { changes[key] = value }
            override fun putLong(key: String, value: Long) = apply { changes[key] = value }
            override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
            override fun remove(key: String) = apply { removed += key }
            override fun clear() = apply { clear = true }
            override fun commit(): Boolean {
                if (clear) values.clear()
                removed.forEach(values::remove)
                values.putAll(changes)
                return true
            }
            override fun apply() {
                commit()
            }
        }
    }
}
