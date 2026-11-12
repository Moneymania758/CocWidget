package com.cocwidget.bases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// How links tapped inside a base site are read. The links are real ones from the two sites.
class BaseLinksTest {
    private val clashCodes = "https://link.clashofclans.com/en/?action=OpenLayout&id=TH18%3AWB%3AAAAARQAAAAJPxPsGYjO2h85K_bNFQl17"
    private val layouts = "https://link.clashofclans.com/en?action=OpenLayout&id=TH18%3AWB%3AAAAAVwAAAAH8TxV5EzM9EJPUDyIO5UM5"
    private val homeLayout = "https://link.clashofclans.com/pt/?action=OpenLayout&id=TH9%3AHV%3AAAAADgAAAALp3JYU6r8-K_6ukc5H1Yy4"

    @Test
    fun layoutLinksFromBothSitesAreRecognised() {
        for (link in listOf(clashCodes, layouts, homeLayout)) assertTrue(link, BaseLinks.isLayout(link))
    }

    @Test
    fun theTownHallComesFromTheStartOfTheId() {
        assertEquals(18, BaseLinks.townHall(clashCodes))
        assertEquals(18, BaseLinks.townHall(layouts))
        assertEquals(9, BaseLinks.townHall(homeLayout))
        // An id that's already decoded reads the same.
        assertEquals(17, BaseLinks.townHall("https://link.clashofclans.com/en/?action=OpenLayout&id=TH17:WB:AAAA"))
        assertEquals(0, BaseLinks.townHall("https://link.clashofclans.com/en/?action=OpenLayout&id=AAAA"))
        assertEquals(0, BaseLinks.townHall("https://clashcodes.com/bases/th18"))
    }

    @Test
    fun otherGameLinksAndLookalikesAreNotLayouts() {
        assertFalse(BaseLinks.isLayout("https://link.clashofclans.com/en/?action=OpenPlayerProfile&tag=%23TEST0000"))
        assertFalse(BaseLinks.isLayout("https://link.clashofclans.com/en/?action=OpenLayout"))
        assertFalse(BaseLinks.isLayout("https://link.clashofclans.com.evil.example/en/?action=OpenLayout&id=TH18%3AWB%3AAAAA"))
        assertFalse(BaseLinks.isLayout("not a link at all"))
    }

    @Test
    fun linksGoToTheGameTheSiteTheBrowserOrNowhere() {
        val site = BaseSite.CLASH_CODES
        assertEquals(Route.LAYOUT, BaseLinks.route(clashCodes, site))
        assertEquals(Route.STAY, BaseLinks.route("https://clashcodes.com/top-10-latest-th18-base-designs", site))
        assertEquals(Route.STAY, BaseLinks.route("https://www.clashcodes.com/bases/th17", site))
        // The other site's pages are somewhere else while this one is open.
        assertEquals(Route.BROWSER, BaseLinks.route("https://clashofclans-layouts.com/plans/th_18/", site))
        assertEquals(Route.BROWSER, BaseLinks.route("https://googleads.g.doubleclick.net/aclk?x=1", site))
        assertEquals(Route.BROWSER, BaseLinks.route("https://notclashcodes.com/", site))
        for (link in listOf("intent://scan/#Intent;scheme=zxing;end", "market://details?id=x", "javascript:alert(1)", "file:///sdcard/x")) {
            assertEquals(link, Route.DROP, BaseLinks.route(link, site))
        }
    }

    @Test
    fun eachSiteHasAPagePerTownHall() {
        assertEquals("https://clashcodes.com/bases/th16", BaseSite.CLASH_CODES.page(16))
        assertEquals("https://clashofclans-layouts.com/plans/th_16/", BaseSite.LAYOUTS.page(16))
        // Below a site's first Town Hall, its first.
        assertEquals("https://clashofclans-layouts.com/plans/th_3/", BaseSite.LAYOUTS.page(2))
    }

    @Test
    fun anUnknownStoredSiteFallsBackToClashCodes() {
        assertEquals(BaseSite.CLASH_CODES, BaseSite.named(null))
        assertEquals(BaseSite.CLASH_CODES, BaseSite.named("GONE"))
        assertEquals(BaseSite.LAYOUTS, BaseSite.named("LAYOUTS"))
    }

    @Test
    fun aBaseLinkIsFoundInSharedText() {
        assertEquals(clashCodes, BaseLinks.find("Try my base! $clashCodes"))
        assertEquals(layouts, BaseLinks.find("$layouts."))
        assertEquals(homeLayout, BaseLinks.find("(see https://clashcodes.com/bases/th9 or $homeLayout)"))
        assertEquals(null, BaseLinks.find("https://link.clashofclans.com/en/?action=OpenPlayerProfile&tag=%23TEST0000"))
        assertEquals(null, BaseLinks.find("no link here"))
        assertEquals(null, BaseLinks.find(null))
    }

    @Test
    fun aSitePageSaysWhichTownHallItLists() {
        assertEquals(16, BaseSite.CLASH_CODES.townHallOf("https://clashcodes.com/bases/th16"))
        assertEquals(9, BaseSite.LAYOUTS.townHallOf("https://www.clashofclans-layouts.com/plans/th_9/"))
        assertEquals(null, BaseSite.CLASH_CODES.townHallOf("https://clashcodes.com/top-10-latest-th18-base-designs"))
        assertEquals(null, BaseSite.CLASH_CODES.townHallOf("https://example.com/bases/th16"))
        assertEquals(null, BaseSite.CLASH_CODES.townHallOf("not a url at all"))
    }
}
