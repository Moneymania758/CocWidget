package com.cocwidget.store

import com.cocwidget.game.GameData
import com.cocwidget.game.Ids
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Every picture the app can ask for must exist: bundled in assets/icons, or on ClashKing's host as
// listed in clashking_icons.txt. tools/build-coc-data.js rewrites that list with the game data, so
// an update that adds an item ClashKing has no picture for fails here, not as a blank on a phone.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IconCoverageTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private val clashKing: Set<String> by lazy {
        javaClass.getResource("/clashking_icons.txt")!!.readText().lines().filter { it.isNotBlank() }.toSet()
    }

    // No source has a picture for these yet. Each shows the placeholder. Remove one once it has a
    // picture, and the test below makes sure the list doesn't go stale.
    private val knownBlank = setOf(
        "buildings/builder-base/botos_shack/level_1.webp",
    )

    private fun exists(path: String) = path in clashKing || Icons.load(app, path) != null

    // Every item at every level it has, as the planner, Up next, the widgets and the guide ask.
    private fun everyPath(): Map<String, String> = buildMap {
        for (item in GameData.all(app).values) {
            for (level in 1..maxOf(1, item.levelCount)) {
                Icons.path(item, level)?.let { putIfAbsent(it, "${item.name} (${item.kind}, ${item.village}) level $level") }
            }
        }
        val townHalls = GameData.all(app)[Ids.TOWN_HALL]!!.levelCount
        for (level in 1..townHalls) put(Icons.townHallPath(level), "Town Hall $level")
    }

    @Test
    fun everyItemAtEveryLevelHasAPicture() {
        val missing = everyPath().filterKeys { !exists(it) && it !in knownBlank }
        assertTrue(
            "No picture on ClashKing or in assets/icons. Bundle one, named as Icons.fileName names it:\n" +
                missing.entries.joinToString("\n") { (path, what) -> "$what: ${Icons.fileName(path)}" },
            missing.isEmpty(),
        )
    }

    @Test
    fun theKnownBlanksAreStillBlank() {
        val paths = everyPath().keys
        for (path in knownBlank) {
            assertTrue("$path is no longer asked for, take it off the list", path in paths)
            assertTrue("$path has a picture now, take it off the list", !exists(path))
        }
    }

    // Sanity check on the list itself, so a broken refresh can't pass everything.
    @Test
    fun theClashKingListIsReal() {
        assertTrue(clashKing.size > 500)
        assertEquals(true, "buildings/home-village/cannon/level_1.webp" in clashKing)
    }
}
