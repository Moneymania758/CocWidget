package com.cocwidget.acceptance

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.view.View
import android.widget.EditText
import com.cocwidget.BedtimeActivity
import com.cocwidget.GuideActivity
import com.cocwidget.MainActivity
import com.cocwidget.MyBasesActivity
import com.cocwidget.PlannerActivity
import com.cocwidget.PotionsActivity
import com.cocwidget.SyncActivity
import com.cocwidget.TestData
import com.cocwidget.store.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowDialog

// The account button and its sheet at the top of the main screen, and the bar along the bottom.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigationTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private fun twoAccounts(): Pair<String, String> {
        val th16 = Store.saveExport(app, TestData.exportJson())
        val th18 = Store.saveExport(app, TestData.th18ExportJson())
        Store.setSelected(app, th16)
        return th16 to th18
    }

    private fun started(activity: Activity): Intent? = shadowOf(activity).nextStartedActivity

    private fun described(root: View, description: String): View? =
        Screens.all(root).firstOrNull { it.contentDescription?.toString() == description && it.isClickable }

    private fun openSheet(main: MainActivity): Dialog {
        described(Screens.root(main), "Switch account")!!.performClick()
        return ShadowDialog.getLatestDialog()
    }

    private fun sheetRoot(sheet: Dialog): View = sheet.window!!.decorView

    // The account button

    @Test
    fun theHeaderShowsTheChosenAccountAndItsTownHall() {
        val (th16, _) = twoAccounts()
        val texts = Screens.texts(Screens.root(Screens.open<MainActivity>()))
        assertTrue(Store.label(app, th16) in texts)
        val townHall = TestData.village().townHall
        assertTrue(texts.any { it.startsWith("Town Hall $townHall · ") })
    }

    @Test
    fun theAccountChipsAreGone() {
        val (_, th18) = twoAccounts()
        val texts = Screens.texts(Screens.root(Screens.open<MainActivity>()))
        assertFalse(Store.label(app, th18) in texts)
        assertFalse("+ Add account" in texts)
    }

    @Test
    fun theSheetListsEveryAccountWithWhatItNeeds() {
        val (th16, th18) = twoAccounts()
        val sheet = openSheet(Screens.open())
        assertTrue(sheet.isShowing)
        val texts = Screens.texts(sheetRoot(sheet))
        assertTrue(Store.label(app, th16) in texts)
        assertTrue(Store.label(app, th18) in texts)
        assertTrue("+ Add account" in texts)
        // Each row says what is idle, all busy, or that its export is too old to trust.
        val statuses = texts.filter { it.contains("idle") || it.startsWith("All ") || it.contains("import again") }
        assertEquals(2, statuses.size)
    }

    @Test
    fun pickingAnAccountSwitchesToItAndClosesTheSheet() {
        val (_, th18) = twoAccounts()
        val main = Screens.open<MainActivity>()
        val sheet = openSheet(main)
        Screens.tappable(sheetRoot(sheet), Store.label(app, th18))!!.performClick()
        assertEquals(th18, Store.selected(app))
        assertFalse(sheet.isShowing)
        val townHall = TestData.th18Village().townHall
        assertTrue(Screens.texts(Screens.root(main)).any { it.startsWith("Town Hall $townHall · ") })
    }

    @Test
    fun addAccountExplainsHow() {
        twoAccounts()
        val sheet = openSheet(Screens.open())
        Screens.tappable(sheetRoot(sheet), "+ Add account")!!.performClick()
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    @Test
    fun manyAccountsGetASearchThatFiltersTheList() {
        twoAccounts()
        val names = (1..6).map { "Alt $it" }
        for ((index, name) in names.withIndex()) {
            val json = TestData.exportJson().replace("#TEST0000", "#ALT000$index")
            Store.setDisplayName(app, Store.saveExport(app, json), name)
        }
        val sheet = openSheet(Screens.open())
        val search = Screens.views<EditText>(sheetRoot(sheet)).single()
        search.setText("alt 3")
        val texts = Screens.texts(sheetRoot(sheet))
        assertTrue("Alt 3" in texts)
        assertFalse("Alt 4" in texts)
    }

    @Test
    fun fewAccountsHaveNoSearch() {
        twoAccounts()
        val sheet = openSheet(Screens.open())
        assertTrue(Screens.views<EditText>(sheetRoot(sheet)).isEmpty())
    }

    @Test
    fun importSitsBesideTheAccountButton() {
        twoAccounts()
        val main = Screens.open<MainActivity>()
        described(Screens.root(main), "Import my village")!!.performClick()
        assertEquals(SyncActivity::class.java.name, started(main)?.component?.className)
    }

    @Test
    fun withNoVillageImportIsStillABigButton() {
        val main = Screens.open<MainActivity>()
        Screens.tappable(Screens.root(main), "Import my village")!!.performClick()
        assertEquals(SyncActivity::class.java.name, started(main)?.component?.className)
    }

    // The bottom bar

    private val tabs = listOf("Up next", "Plan", "Guide", "Bases")

    @Test
    fun theMainScreenHasTheBarWithUpNextChosen() {
        twoAccounts()
        val root = Screens.root(Screens.open<MainActivity>())
        for (tab in tabs) assertNotNull(tab, Screens.tappable(root, tab))
        assertTrue(Screens.tappable(root, "Up next")!!.isSelected)
        assertFalse(Screens.tappable(root, "Plan")!!.isSelected)
    }

    @Test
    fun theOldToolTilesAreGone() {
        twoAccounts()
        val texts = Screens.texts(Screens.root(Screens.open<MainActivity>()))
        assertFalse("Bedtime planner" in texts)
        assertFalse("Potions" in texts)
    }

    @Test
    fun eachTabOpensItsScreen() {
        twoAccounts()
        val screens = mapOf("Plan" to PlannerActivity::class.java, "Guide" to GuideActivity::class.java, "Bases" to MyBasesActivity::class.java)
        for ((tab, screen) in screens) {
            val main = Screens.open<MainActivity>()
            Screens.tappable(Screens.root(main), tab)!!.performClick()
            assertEquals(tab, screen.name, started(main)?.component?.className)
            assertFalse("the main screen stays underneath", main.isFinishing)
        }
    }

    @Test
    fun aTabReplacesTheTabBeforeIt() {
        twoAccounts()
        val guide = Screens.open<GuideActivity>()
        assertTrue(Screens.tappable(Screens.root(guide), "Guide")!!.isSelected)
        Screens.tappable(Screens.root(guide), "Bases")!!.performClick()
        assertEquals(MyBasesActivity::class.java.name, started(guide)?.component?.className)
        assertTrue(guide.isFinishing)
    }

    @Test
    fun upNextGoesBackToTheMainScreen() {
        twoAccounts()
        val bases = Screens.open<MyBasesActivity>()
        Screens.tappable(Screens.root(bases), "Up next")!!.performClick()
        assertEquals(MainActivity::class.java.name, started(bases)?.component?.className)
        assertTrue(bases.isFinishing)
    }

    @Test
    fun tappingTheChosenTabDoesNothing() {
        twoAccounts()
        val guide = Screens.open<GuideActivity>()
        Screens.tappable(Screens.root(guide), "Guide")!!.performClick()
        assertNull(started(guide))
        assertFalse(guide.isFinishing)
    }

    @Test
    fun planSwitchesBetweenTimeToMaxBedtimeAndPotions() {
        twoAccounts()
        val planner = Screens.open<PlannerActivity>()
        val root = Screens.root(planner)
        assertTrue(Screens.tappable(root, "Plan")!!.isSelected)
        assertTrue(Screens.tappable(root, "Time to max")!!.isSelected)
        Screens.tappable(root, "Bedtime")!!.performClick()
        assertEquals(BedtimeActivity::class.java.name, started(planner)?.component?.className)
        assertTrue(planner.isFinishing)

        val bedtime = Screens.open<BedtimeActivity>()
        Screens.tappable(Screens.root(bedtime), "Potions")!!.performClick()
        assertEquals(PotionsActivity::class.java.name, started(bedtime)?.component?.className)
    }

    @Test
    fun guideNoLongerHoldsTheBases() {
        twoAccounts()
        val texts = Screens.texts(Screens.root(Screens.open<GuideActivity>()))
        assertFalse("My bases" in texts)
        assertFalse("Find bases online" in texts)
    }

    // Every tab carries the account, so switching village works from wherever you are.
    @Test
    fun everyTabHasTheAccountButtonAndSwitchingRebuildsIt() {
        val (_, th18) = twoAccounts()
        for (screen in listOf(Screens.open<PlannerActivity>(), Screens.open<BedtimeActivity>(), Screens.open<PotionsActivity>(), Screens.open<GuideActivity>(), Screens.open<MyBasesActivity>())) {
            assertNotNull(screen.javaClass.simpleName, described(Screens.root(screen), "Switch account"))
            assertNotNull(screen.javaClass.simpleName, described(Screens.root(screen), "Import my village"))
        }
        val guide = Screens.open<GuideActivity>()
        described(Screens.root(guide), "Switch account")!!.performClick()
        val sheet = ShadowDialog.getLatestDialog()
        Screens.tappable(sheet.window!!.decorView, Store.label(app, th18))!!.performClick()
        assertEquals(th18, Store.selected(app))
        assertTrue(Screens.texts(Screens.root(guide)).contains("Town Hall 18"))
    }

    @Test
    fun findBasesAsATabHasTheBarAndTheAccountButFromATownHallItHasABackArrow() {
        twoAccounts()
        val tab = Screens.open<com.cocwidget.BasesActivity> { putExtra(com.cocwidget.BasesActivity.EXTRA_AS_TAB, true) }
        val root = Screens.root(tab)
        assertNotNull(described(root, "Switch account"))
        assertNull(described(root, "Back"))
        assertTrue(Screens.tappable(root, "Bases")!!.isSelected)
        assertTrue(Screens.tappable(root, "Find bases")!!.isSelected)
        Screens.tappable(root, "My bases")!!.performClick()
        assertEquals(MyBasesActivity::class.java.name, started(tab)?.component?.className)

        val fromGuide = Screens.root(Screens.open<com.cocwidget.BasesActivity> { putExtra(com.cocwidget.BasesActivity.EXTRA_TOWN_HALL, 12) })
        assertNotNull(described(fromGuide, "Back"))
        assertNull(described(fromGuide, "Switch account"))
        assertNull(Screens.tappable(fromGuide, "Up next"))
    }

    @Test
    fun settingsGroupsItsCardsUnderHeadings() {
        twoAccounts()
        val texts = Screens.texts(Screens.root(Screens.open<com.cocwidget.SettingsActivity>()))
        val headings = listOf("Look", "This account", "Events", "Get help")
        assertEquals(headings, texts.filter { it in headings })
    }

    @Test
    fun tabScreensHaveNoBackArrow() {
        twoAccounts()
        for (screen in listOf(Screens.open<PlannerActivity>(), Screens.open<GuideActivity>(), Screens.open<MyBasesActivity>())) {
            assertNull(screen.javaClass.simpleName, described(Screens.root(screen), "Back"))
        }
    }
}
