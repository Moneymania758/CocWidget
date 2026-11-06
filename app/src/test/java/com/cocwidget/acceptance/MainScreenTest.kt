package com.cocwidget.acceptance

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.BedtimeActivity
import com.cocwidget.GuideActivity
import com.cocwidget.MainActivity
import com.cocwidget.PlannerActivity
import com.cocwidget.PotionsActivity
import com.cocwidget.R
import com.cocwidget.SyncActivity
import com.cocwidget.TestData
import com.cocwidget.VillageWidget
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin
import com.cocwidget.widget.WidgetRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainScreenTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private fun open(): MainActivity = Screens.open()

    private fun started(activity: MainActivity): Intent = shadowOf(activity).nextStartedActivity

    @Test
    fun everyTownHallHasABundledPicture() {
        for (level in 1..18) assertNotNull("TH$level", Icons.load(app, Icons.townHallPath(level)))
    }

    @Test
    fun theVillagesOwnTownHallIsShown() {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        val level = Store.village(app, tag)!!.townHall
        val picture = Screens.views<ImageView>(Screens.root(open()))
            .single { it.contentDescription == app.getString(R.string.home_town_hall, level) }
        assertNotNull(picture.drawable)
    }

    @Test
    fun noTownHallWithoutAVillage() {
        val root = Screens.root(open())
        assertTrue(app.getString(R.string.status_none) in Screens.texts(root))
        assertFalse(Screens.views<ImageView>(root).any { it.contentDescription?.startsWith("Town Hall") == true })
    }

    @Test
    fun everyUpgradeIsListedInEverySkin() {
        for (json in listOf(TestData.exportJson(), TestData.th18ExportJson())) {
            val tag = Store.saveExport(app, json)
            Store.setSelected(app, tag)
            val village = Store.village(app, tag)!!
            for (skin in Skin.ALL) {
                Store.setSkin(app, skin.key)
                val texts = Screens.texts(Screens.root(open()))
                for (upgrade in village.upgrades) {
                    val name = WidgetRenderer.upgradeName(upgrade).toString()
                    assertTrue("${skin.key}: $name", name in texts)
                }
            }
        }
    }

    @Test
    fun aFinishedUpgradeSaysDone() {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        val village = Store.village(app, tag)!!
        val now = System.currentTimeMillis()
        val finished = village.upgrades.count { village.endMillis(it) <= now }
        assertEquals(finished, Screens.texts(Screens.root(open())).count { it == app.getString(R.string.done) })
    }

    @Test
    fun theHelpersAndTheGoblinShowTheirFaces() {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        val village = Store.village(app, tag)!!
        val now = System.currentTimeMillis()
        val faces = Screens.views<ImageView>(Screens.root(open()))
        val onRows = village.upgrades.count { village.endMillis(it) > now && village.isHelped(it, now) }
        val onCard = listOf(village.helpers.apprenticeLevel, village.helpers.assistantLevel).count { it > 0 }
        assertTrue("the fixture has helpers", onCard > 0)
        assertEquals(onRows + onCard, faces.count { it.contentDescription == app.getString(R.string.helper) })
        assertEquals(village.upgrades.count { it.extra }, faces.count { it.contentDescription == app.getString(R.string.goblin) })
    }

    // Its own picture is far bigger, so a face that loses its size fills the row.
    @Test
    fun theGoblinsFaceKeepsItsSize() {
        Store.saveExport(app, TestData.exportJson())
        val goblins = Screens.views<ImageView>(Screens.root(open())).filter { it.contentDescription == app.getString(R.string.goblin) }
        assertTrue("the fixture has a Goblin", goblins.isNotEmpty())
        for (goblin in goblins) assertEquals(26, goblin.layoutParams.width)
    }

    @Test
    fun theButtonSaysImportMyVillageAndOpensTheImport() {
        val activity = open()
        Screens.tappable(Screens.root(activity), "Import my village")!!.performClick()
        assertEquals(SyncActivity::class.java.name, started(activity).component?.className)
    }

    @Test
    fun everyToolOpensFromTheMainScreen() {
        Store.saveExport(app, TestData.th18ExportJson())
        val tools = mapOf(
            R.string.planner_open to PlannerActivity::class.java,
            R.string.bedtime_open to BedtimeActivity::class.java,
            R.string.potions_open to PotionsActivity::class.java,
            R.string.guide_open to GuideActivity::class.java,
        )
        for ((label, screen) in tools) {
            val activity = open()
            Screens.tappable(Screens.root(activity), app.getString(label))!!.performClick()
            assertEquals(screen.name, started(activity).component?.className)
        }
    }

    // What's New keeps the words each old release went out with.
    @Test
    fun nothingCurrentStillSaysSync() {
        val strings = File("src/main/res/values/strings.xml").readText()
        val current = Regex("""<(string|item)[^>]*>[^<]*</\1>""").findAll(strings).map { it.value }.filter { "whats_new" !in it }
        val left = current.filter { Regex("""\bsync""", RegexOption.IGNORE_CASE).containsMatchIn(it.substringAfter('>')) }.toList()
        assertEquals(emptyList<String>(), left)
    }

    private fun bigWidget(tag: String): View {
        val manager = AppWidgetManager.getInstance(app)
        val id = shadowOf(manager).createWidget(VillageWidget::class.java, R.layout.widget)
        val village = Store.village(app, tag)!!
        return WidgetRenderer.build(app, id, tag, village, village.exportedAtMillis + 60_000).apply(app, FrameLayout(app))
    }

    @Test
    fun tappingTheBigWidgetOpensTheMainScreenOnItsAccount() {
        val tag = Store.saveExport(app, TestData.th18ExportJson())
        val root = bigWidget(tag)

        // A tap on a row isn't taken by the row, so it reaches the widget itself.
        val rows = root.findViewById<LinearLayout>(R.id.rows)
        var view: View? = Screens.all(rows).first { it.id == R.id.bar }
        while (view != null && view !== rows) {
            assertFalse(view.hasOnClickListeners())
            view = view.parent as? View
        }

        assertTrue(root.findViewById<View>(R.id.widget_root).performClick())
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(tag, intent.getStringExtra(MainActivity.EXTRA_ACCOUNT))
    }

    @Test
    fun theWidgetsOwnControlsKeepTheirTaps() {
        val root = bigWidget(Store.saveExport(app, TestData.th18ExportJson()))
        root.findViewById<View>(R.id.sync).performClick()
        assertEquals(SyncActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
        assertTrue(Screens.all(root).first { it.id == R.id.section }.hasOnClickListeners())
    }
}
