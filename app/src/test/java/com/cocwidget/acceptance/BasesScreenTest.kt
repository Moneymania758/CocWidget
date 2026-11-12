package com.cocwidget.acceptance

import android.content.Intent
import android.webkit.WebView
import com.cocwidget.BasesActivity
import com.cocwidget.GuideTownHallActivity
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.bases.BaseLinks
import com.cocwidget.bases.BaseSite
import com.cocwidget.store.Store
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.Skin
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

// The Bases screen: which site page it opens, and where a tapped base link goes.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BasesScreenTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val th18Base = "https://link.clashofclans.com/en/?action=OpenLayout&id=TH18%3AWB%3AAAAARQAAAAJPxPsGYjO2h85K_bNFQl17"
    private val th16Base = "https://link.clashofclans.com/en/?action=OpenLayout&id=TH16%3AHV%3AAAAARQAAAAJPxPsGYjO2h85K_bNFQl17"

    private fun syncTh16() {
        Store.setSelected(app, Store.saveExport(app, TestData.exportJson()))
    }

    private fun loaded(screen: BasesActivity) = shadowOf(screen.web).lastLoadedUrl

    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    @Test
    fun opensClashCodesOnTheSyncedTownHall() {
        syncTh16()
        val screen = Screens.open<BasesActivity>()
        assertEquals("https://clashcodes.com/bases/th16", loaded(screen))
        assertTrue(Screens.texts(Screens.root(screen)).contains(app.getString(R.string.bases_my_town_hall, 16)))
    }

    @Test
    fun withNoVillageItOpensTheTopTownHall() {
        val screen = Screens.open<BasesActivity>()
        assertEquals("https://clashcodes.com/bases/th18", loaded(screen))
    }

    @Test
    fun aTownHallPageOpensItsOwnBases() {
        syncTh16()
        val screen = Screens.open<BasesActivity> { putExtra(BasesActivity.EXTRA_TOWN_HALL, 12) }
        assertEquals("https://clashcodes.com/bases/th12", loaded(screen))
    }

    @Test
    fun chipsPickTheTownHallAndTheSiteAndTheSiteIsRemembered() {
        syncTh16()
        val screen = Screens.open<BasesActivity>()
        val root = Screens.root(screen)
        Screens.tappable(root, "TH17")!!.performClick()
        assertEquals("https://clashcodes.com/bases/th17", loaded(screen))
        Screens.tappable(root, BaseSite.LAYOUTS.label)!!.performClick()
        assertEquals("https://clashofclans-layouts.com/plans/th_17/", loaded(screen))
        assertEquals(BaseSite.LAYOUTS.name, Store.basesSite(app))

        val again = Screens.open<BasesActivity>()
        assertEquals("https://clashofclans-layouts.com/plans/th_16/", loaded(again))
        // The other site starts at Town Hall 3, so it has no TH2 chip.
        assertNull(Screens.tappable(Screens.root(again), "TH2"))
    }

    @Test
    fun aBaseLinkOpensInTheGame() {
        syncTh16()
        val screen = Screens.open<BasesActivity>()
        nextStarted()
        assertTrue(screen.handle(th16Base))
        val intent = nextStarted()
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent!!.action)
        assertEquals(BaseLinks.GAME_PACKAGE, intent.`package`)
        assertEquals(th16Base, intent.dataString)
    }

    @Test
    fun aBaseAboveTheVillageAsksFirst() {
        syncTh16()
        val screen = Screens.open<BasesActivity>()
        nextStarted()
        assertTrue(screen.handle(th18Base))
        assertNull(nextStarted())
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog)
        assertEquals(app.getString(R.string.bases_too_high, 18, 16), shadowOf(dialog).message.toString())
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(th18Base, nextStarted()?.dataString)
    }

    @Test
    fun withNoVillageAnyBaseOpensStraightAway() {
        val screen = Screens.open<BasesActivity>()
        nextStarted()
        assertTrue(screen.handle(th18Base))
        assertEquals(BaseLinks.GAME_PACKAGE, nextStarted()?.`package`)
    }

    @Test
    fun theSitesOwnPagesStayAndEverythingElseLeavesOrIsDropped() {
        val screen = Screens.open<BasesActivity>()
        nextStarted()
        assertFalse(screen.handle("https://clashcodes.com/top-10-latest-th18-base-designs"))
        assertNull(nextStarted())
        assertTrue(screen.handle("https://example.com/advert"))
        val outside = nextStarted()
        assertNull(outside?.`package`)
        assertTrue(outside!!.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertTrue(screen.handle("intent://x#Intent;component=com.cocwidget/.SettingsActivity;end"))
        assertNull(nextStarted())
    }

    // A script sending the page to another site, with no tap, is stopped and opens nothing.
    @Test
    fun aRedirectWithNoTapStaysPut() {
        val screen = Screens.open<BasesActivity>()
        nextStarted()
        assertTrue(screen.handle("https://example.com/advert", tapped = false))
        assertNull(nextStarted())
        // A base link still opens in the game, as the sites open them from a tap's script.
        assertTrue(screen.handle(th16Base, tapped = false))
        assertNotNull(nextStarted())
    }

    @Test
    fun javascriptIsOnAndFilesAreOff() {
        val web: WebView = Screens.open<BasesActivity>().web
        assertTrue(web.settings.javaScriptEnabled)
        assertFalse(web.settings.allowFileAccess)
        assertFalse(web.settings.allowContentAccess)
    }

    @Test
    fun eachTownHallPageLeadsHere() {
        syncTh16()
        val page = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 13) }
        nextStarted()
        Screens.tappable(Screens.root(page), app.getString(R.string.bases_for_town_hall, 13))!!.performClick()
        val intent = nextStarted()
        assertEquals(BasesActivity::class.java.name, intent?.component?.className)
        assertEquals(13, intent?.getIntExtra(BasesActivity.EXTRA_TOWN_HALL, 0))
    }

    @Test
    fun opensInEverySkin() {
        for (skin in Skin.ALL) {
            Store.setSkin(app, skin.key)
            assertEquals("https://clashcodes.com/bases/th18", loaded(Screens.open<BasesActivity>()))
        }
    }

    // Back in the site to another Town Hall's list moves the chips with it.
    @Test
    fun theTownHallChipFollowsThePageShown() {
        val screen = Screens.open<BasesActivity> { putExtra(BasesActivity.EXTRA_TOWN_HALL, 16) }
        screen.visited("https://clashcodes.com/bases/th15")
        assertEquals(15, screen.townHall)
        assertTrue(Screens.tappable(Screens.root(screen), "TH15")!!.isSelected)
        screen.visited("https://clashcodes.com/some-article")
        assertEquals(15, screen.townHall)
    }
}
