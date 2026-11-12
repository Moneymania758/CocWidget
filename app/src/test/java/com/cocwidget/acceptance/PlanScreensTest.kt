package com.cocwidget.acceptance

import android.view.View
import com.cocwidget.GuidePetsActivity
import com.cocwidget.GuideUnitActivity
import com.cocwidget.PlannerActivity
import com.cocwidget.PotionsActivity
import com.cocwidget.TestData
import com.cocwidget.plan.Planner.Group
import com.cocwidget.store.Store
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.Skin
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Time to max's rows unfold into each level and open the guide, and the Potions screen.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlanScreensTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Before
    fun syncTh16() {
        Store.setSelected(app, Store.saveExport(app, TestData.exportJson()))
    }

    private fun described(root: View, description: String): View? =
        Screens.all(root).firstOrNull { it.contentDescription?.toString() == description && it.isClickable }

    private fun shown(root: View, text: String) = Screens.views<android.widget.TextView>(root).any { it.text.toString() == text && it.isShown }

    // The first item in [group] with a level still to reach, by the planner's own order.
    private fun firstWithSteps(group: Group) = TestData.plan().group(group)!!.items.first { it.steps.isNotEmpty() }

    private fun openGroup(screen: PlannerActivity, title: String) {
        Screens.tappable(Screens.root(screen), title)!!.performClick()
        Screens.layOut(screen)
    }

    @Test
    fun aRowUnfoldsIntoEachLevelLeft() {
        val screen = Screens.open<PlannerActivity>()
        openGroup(screen, "Defences")
        val item = firstWithSteps(Group.DEFENCES)
        val first = item.steps.first()
        val label = if (first.supercharge > 0) "Supercharge ${first.supercharge}" else if (first.level == 1) "Build" else "Level ${first.level}"
        assertFalse(shown(Screens.root(screen), label))

        val row = Screens.tappable(Screens.root(screen), item.name)!!
        assertEquals("Closed", row.stateDescription)
        row.performClick()
        Screens.layOut(screen)
        assertTrue(shown(Screens.root(screen), label))
        assertEquals("Open", row.stateDescription)

        row.performClick()
        Screens.layOut(screen)
        assertFalse(shown(Screens.root(screen), label))
        assertEquals("Closed", row.stateDescription)
    }

    // Without its Ricochet Cannons, a TH16 gets them by merging Cannons, not by building.
    @Test
    fun aMergedDefenceStartsWithMergeNotBuild() {
        val ricochet = TestData.gameData.entries.first { it.value.name == "Ricochet Cannon" }.key
        val root = JSONObject(TestData.exportJson())
        val buildings = root.getJSONArray("buildings")
        for (i in buildings.length() - 1 downTo 0) if (buildings.getJSONObject(i).optInt("data") == ricochet) buildings.remove(i)
        Store.setSelected(app, Store.saveExport(app, root.toString()))

        val screen = Screens.open<PlannerActivity>()
        openGroup(screen, "Defences")
        Screens.tappable(Screens.root(screen), "Ricochet Cannon")!!.performClick()
        Screens.layOut(screen)
        assertTrue(shown(Screens.root(screen), "Merge"))
        assertFalse(shown(Screens.root(screen), "Build"))
    }

    @Test
    fun aDefencePictureOpensItsGuidePage() {
        val screen = Screens.open<PlannerActivity>()
        openGroup(screen, "Defences")
        val item = firstWithSteps(Group.DEFENCES)
        described(Screens.root(screen), "${item.name} in the guide")!!.performClick()
        val started = shadowOf(screen).nextStartedActivity
        assertEquals(GuideUnitActivity::class.java.name, started.component?.className)
        assertEquals(item.id, started.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    @Test
    fun heroesTroopsAndPetsOpenTheGuideAndWallsDoNot() {
        val plan = TestData.plan()
        val screen = Screens.open<PlannerActivity>()
        val root = Screens.root(screen)
        assertNotNull(described(root, "${plan.group(Group.HEROES)!!.items.first().name} in the guide"))
        assertNotNull(described(root, "${plan.group(Group.LAB)!!.items.first().name} in the guide"))
        val pet = plan.group(Group.PETS)!!.items.first()
        described(root, "${pet.name} in the guide")!!.performClick()
        val started = shadowOf(screen).nextStartedActivity
        assertEquals(GuidePetsActivity::class.java.name, started.component?.className)
        assertEquals(pet.id, started.getIntExtra(GuideScreen.EXTRA_ID, 0))
        assertNull(described(root, "Wall in the guide"))
    }

    @Test
    fun bothScreensOpenInEverySkin() {
        for (skin in Skin.ALL) {
            Store.setSkin(app, skin.key)
            val planner = Screens.open<PlannerActivity>()
            for (title in listOf("Defences", "Heroes", "Walls")) openGroup(planner, title)
            val potions = Screens.open<PotionsActivity>()
            val texts = Screens.texts(Screens.root(potions))
            assertTrue(skin.key, texts.any { it.startsWith("Builder Potion") })
            assertTrue(skin.key, texts.any { it.startsWith("Research Potion") })
        }
    }
}
