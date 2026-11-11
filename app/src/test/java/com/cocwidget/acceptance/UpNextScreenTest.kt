package com.cocwidget.acceptance

import com.cocwidget.MainActivity
import com.cocwidget.TestData
import com.cocwidget.game.Section
import com.cocwidget.store.Store
import com.cocwidget.widget.WidgetRenderer
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// The main screen's status tiles and the one stream under them.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpNextScreenTest {
    private val app get() = RuntimeEnvironment.getApplication()

    // Exported just now, so everything in it still runs.
    private fun fresh(): String {
        val json = JSONObject(TestData.th18ExportJson()).put("timestamp", System.currentTimeMillis() / 1000).toString()
        Store.setSelected(app, Store.saveExport(app, json))
        return json
    }

    private fun names(json: String, section: Section) =
        TestData.th18Village(json).upgrades.filter { it.section == section }.map { WidgetRenderer.upgradeName(it).toString() }

    @Test
    fun aTileForEachSection() {
        fresh()
        val root = Screens.root(Screens.open<MainActivity>())
        for (tile in listOf("Builders", "Lab", "Pets")) assertNotNull(tile, Screens.tappable(root, tile))
    }

    @Test
    fun aTileCutsTheStreamDownToItsSectionAndShowAllBringsItBack() {
        val json = fresh()
        val builders = names(json, Section.BUILDERS)
        val lab = names(json, Section.LAB)
        assertTrue("the fixture has both", builders.isNotEmpty() && lab.isNotEmpty())
        val main = Screens.open<MainActivity>()

        Screens.tappable(Screens.root(main), "Lab")!!.performClick()
        var texts = Screens.texts(Screens.root(main))
        assertTrue(lab.all { it in texts })
        assertTrue(builders.none { it in texts })
        assertTrue("Showing Lab only" in texts)
        assertTrue(Screens.tappable(Screens.root(main), "Lab")!!.isSelected)

        Screens.tappable(Screens.root(main), "Show all")!!.performClick()
        texts = Screens.texts(Screens.root(main))
        assertTrue(builders.all { it in texts })
        assertFalse("Showing Lab only" in texts)
    }

    @Test
    fun tappingTheChosenTileAgainShowsEverything() {
        val json = fresh()
        val main = Screens.open<MainActivity>()
        Screens.tappable(Screens.root(main), "Builders")!!.performClick()
        Screens.tappable(Screens.root(main), "Builders")!!.performClick()
        val texts = Screens.texts(Screens.root(main))
        assertTrue(names(json, Section.LAB).all { it in texts })
    }

    @Test
    fun theStreamIsGroupedByTime() {
        fresh()
        val texts = Screens.texts(Screens.root(Screens.open<MainActivity>()))
        val groups = listOf("Now", "Next hour", "Later today", "Tomorrow", "This week", "Later").filter { it in texts }
        assertTrue(groups.size >= 2)
    }

    @Test
    fun anOldExportGetsAWarningAndAFreshOneDoesNot() {
        fresh()
        assertFalse(Screens.texts(Screens.root(Screens.open<MainActivity>())).any { it.contains("import again") })
        Store.saveExport(app, TestData.th18ExportJson())
        assertTrue(Screens.texts(Screens.root(Screens.open<MainActivity>())).any { it.contains("import again") })
    }

    // Laid out at the phone's real width, as Screens.layOut uses a fixed one.
    private fun layOutAtScreenWidth(main: MainActivity): View {
        val root = Screens.root(main)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(main.resources.displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return root
    }

    // The rows holding the tiles, by the Builders and Builder Base tiles' rows.
    private fun tileRows(root: View): Pair<Any?, Any?> =
        Screens.tappable(root, "Builders")!!.parent to Screens.tappable(root, "Builder Base")!!.parent

    @Test
    @Config(qualifiers = "w448dp-h900dp-xxhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun onAWidePhoneTheTilesShareOneRow() {
        fresh()
        val root = layOutAtScreenWidth(Screens.open<MainActivity>())
        val (builders, builderBase) = tileRows(root)
        assertEquals(builders, builderBase)
    }

    // At about 360 dp four tiles can't fit their words, and row titles must keep their level.
    @Test
    @Config(qualifiers = "w358dp-h780dp-xxhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun onANarrowPhoneTheTilesWrapAndNothingIsCutOff() {
        fresh()
        val main = Screens.open<MainActivity>()
        val root = layOutAtScreenWidth(main)
        val (builders, builderBase) = tileRows(root)
        assertTrue("two rows of tiles", builders != builderBase)
        val name = Store.label(app, Store.selected(app)!!)
        val cut = Screens.views<TextView>(root).filter { view ->
            val layout = view.layout ?: return@filter false
            view.visibility == View.VISIBLE && view.ellipsize == TextUtils.TruncateAt.END && view.text.toString() != name &&
                (0 until layout.lineCount).any { layout.getEllipsisCount(it) > 0 }
        }.map { it.text.toString() }
        assertEquals(emptyList<String>(), cut)
    }
}
