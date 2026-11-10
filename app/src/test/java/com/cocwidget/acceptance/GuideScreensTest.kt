package com.cocwidget.acceptance

import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView
import com.cocwidget.GuideActivity
import com.cocwidget.GuideArmyActivity
import com.cocwidget.GuideDefencesActivity
import com.cocwidget.GuideGlossaryActivity
import com.cocwidget.GuideSourcesActivity
import com.cocwidget.GuideTermActivity
import com.cocwidget.GuideThreatActivity
import com.cocwidget.GuideHeroesActivity
import com.cocwidget.GuidePairingsActivity
import com.cocwidget.GuidePetsActivity
import com.cocwidget.GuideRushActivity
import com.cocwidget.GuideTownHallActivity
import com.cocwidget.GuideUnitActivity
import com.cocwidget.TestData
import com.cocwidget.guide.Guide
import com.cocwidget.guide.GuideUnits
import com.cocwidget.store.Store
import com.cocwidget.ui.GuideScreen
import com.cocwidget.ui.LevelChart
import com.cocwidget.ui.Skin
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

// What a player sees and taps in the guide, on the real screens with the real data. These stand in
// for most of the clicking on an emulator. How things look still needs a device.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GuideScreensTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val units get() = GuideUnits.load(app)
    private val guide get() = Guide.load(app)
    private val skins = listOf(Skin.WIDGET, Skin.CLEAN, Skin.GAME)

    private fun unitId(name: String) = units.values.first { it.name == name }.id

    private fun openUnit(name: String) = Screens.open<GuideUnitActivity> { putExtra(GuideScreen.EXTRA_ID, unitId(name)) }

    private fun syncTh16() {
        val tag = Store.saveExport(app, TestData.exportJson())
        Store.setSelected(app, tag)
    }

    @Test
    fun everyGuideScreenOpensInEverySkin() {
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            Screens.open<GuideActivity>()
            Screens.open<GuideArmyActivity>()
            Screens.open<GuideHeroesActivity>()
            Screens.open<GuidePetsActivity>()
            Screens.open<GuidePairingsActivity>()
            for (townHall in Guide.FIRST_TOWN_HALL..18) {
                val page = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, townHall) }
                assertTrue("TH$townHall in ${skin.key}", Screens.texts(Screens.root(page)).contains("Town Hall $townHall"))
            }
        }
    }

    @Test
    fun everyTroopAndSpellPageOpensInEverySkinWithItsCharts() {
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            for (unit in units.values) {
                val intent = android.content.Intent(app, GuideUnitActivity::class.java).putExtra(GuideScreen.EXTRA_ID, unit.id)
                val controller = org.robolectric.Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup()
                val root = Screens.root(controller.get())
                val where = "${unit.name} in ${skin.key}"
                assertTrue(where, Screens.texts(root).contains(unit.name))
                val expected = unit.charts.size + unit.subs.sumOf { it.charts.size }
                assertEquals(where, expected, Screens.views<LevelChart>(root).size)
                controller.pause().stop().destroy()
            }
        }
    }

    // The bar sat on top of the last row. One UI draws it about 10 dp up from the edge, so the table
    // needs clear room below it, not just the bar's thickness.
    @Test
    fun everyTablesScrollBarSitsUnderTheLastRow() {
        val density = app.resources.displayMetrics.density
        for (unit in units.values) {
            val page = Screens.open<GuideUnitActivity> { putExtra(GuideScreen.EXTRA_ID, unit.id) }
            for (scroller in Screens.views<android.widget.HorizontalScrollView>(Screens.root(page))) {
                val room = (scroller.height - scroller.getChildAt(0).bottom) / density
                assertTrue("${unit.name}: $room dp under the table", room >= 16)
                assertEquals(unit.name, android.view.View.SCROLLBARS_OUTSIDE_OVERLAY, scroller.scrollBarStyle)
            }
        }
    }

    // Taps the highlighted [word] in any text on the screen, and returns what that opened.
    private fun tapWord(screen: GuideScreen, word: String): android.content.Intent {
        val view = Screens.views<TextView>(Screens.root(screen)).first { label ->
            val spanned = label.text as? Spanned ?: return@first false
            spanned.getSpans(0, spanned.length, ClickableSpan::class.java).any {
                spanned.subSequence(spanned.getSpanStart(it), spanned.getSpanEnd(it)).toString().equals(word, ignoreCase = true)
            }
        }
        val spanned = view.text as Spanned
        spanned.getSpans(0, spanned.length, ClickableSpan::class.java).first {
            spanned.subSequence(spanned.getSpanStart(it), spanned.getSpanEnd(it)).toString().equals(word, ignoreCase = true)
        }.onClick(view)
        return shadowOf(screen).nextStartedActivity
    }

    @Test
    fun tappingAHighlightedWordOpensWhatItMeans() {
        val started = tapWord(openUnit("Thrower"), "funnel")
        assertEquals(GuideTermActivity::class.java.name, started.component?.className)
        assertEquals("Funnel", started.getStringExtra(GuideScreen.EXTRA_TERM))
    }

    @Test
    fun wordsAreHighlightedInAttacksToo() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 18) }
        assertEquals("Funnel", tapWord(screen, "funnel").getStringExtra(GuideScreen.EXTRA_TERM))
    }

    @Test
    fun everyWordsPageOpensInEverySkinAndLinksOnward() {
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            for (term in guide.glossary.terms) {
                val screen = Screens.open<GuideTermActivity> { putExtra(GuideScreen.EXTRA_TERM, term.term) }
                val root = Screens.root(screen)
                val where = "${term.term} in ${skin.key}"
                assertTrue(where, Screens.texts(root).contains(term.short))
                for (other in term.see) {
                    Screens.tappable(root, other)!!.performClick()
                    assertEquals(where, other, shadowOf(screen).nextStartedActivity.getStringExtra(GuideScreen.EXTRA_TERM))
                }
                for (name in term.units) {
                    Screens.tappable(root, name)!!.performClick()
                    val started = shadowOf(screen).nextStartedActivity
                    if (guide.threat(name) != null) {
                        assertEquals(where, name, started.getStringExtra(GuideThreatActivity.EXTRA_THREAT))
                    } else {
                        assertEquals(where, unitId(name), started.getIntExtra(GuideScreen.EXTRA_ID, 0))
                    }
                }
            }
        }
    }

    @Test
    fun aWordsOwnPageDoesNotLinkToItself() {
        val term = guide.glossary.term("Funnel")!!
        val screen = Screens.open<GuideTermActivity> { putExtra(GuideScreen.EXTRA_TERM, term.term) }
        val own = Screens.views<TextView>(Screens.root(screen)).flatMap { label ->
            val spanned = label.text as? Spanned ?: return@flatMap emptyList()
            spanned.getSpans(0, spanned.length, ClickableSpan::class.java).map {
                spanned.subSequence(spanned.getSpanStart(it), spanned.getSpanEnd(it)).toString().lowercase()
            }
        }
        assertFalse(own.toString(), term.aliases.any { it.lowercase() in own })
    }

    @Test
    fun theWordListOpensEveryWord() {
        val screen = Screens.open<GuideGlossaryActivity>()
        val root = Screens.root(screen)
        for (term in guide.glossary.terms) {
            Screens.tappable(root, term.term)!!.performClick()
            assertEquals(term.term, shadowOf(screen).nextStartedActivity.getStringExtra(GuideScreen.EXTRA_TERM))
        }
    }

    @Test
    fun everySourceOpensInTheBrowser() {
        val screen = Screens.open<GuideSourcesActivity>()
        val root = Screens.root(screen)
        for (source in guide.content.sources) {
            val row = app.getString(com.cocwidget.R.string.guide_source_row, source.name, source.date)
            Screens.tappable(root, row)!!.performClick()
            assertEquals(source.name, source.url, shadowOf(screen).nextStartedActivity.dataString)
        }
    }

    @Test
    fun theGuideLeadsToTheWordList() {
        val screen = Screens.open<GuideActivity>()
        Screens.tappable(Screens.root(screen), app.getString(com.cocwidget.R.string.guide_glossary))!!.performClick()
        assertEquals(GuideGlossaryActivity::class.java.name, shadowOf(screen).nextStartedActivity.component?.className)
    }

    private fun string(id: Int, vararg args: Any) = app.getString(id, *args)

    @Test
    fun theRushPageOpensInEverySkinAndWithNoTownHallShowsEveryBand() {
        val rush = guide.content.rushing!!
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            val texts = Screens.texts(Screens.root(Screens.open<GuideRushActivity>()))
            assertTrue(skin.key, texts.contains(string(com.cocwidget.R.string.guide_rush_band, 3, 12)))
            assertTrue(skin.key, texts.contains(string(com.cocwidget.R.string.guide_rush_band_one, rush.writtenFor)))
            for (townHall in 16..18) assertTrue("TH$townHall", texts.contains(string(com.cocwidget.R.string.guide_rush_before, townHall)))
            assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_heroes, 8)))
            assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_equipment)))
            assertFalse(texts.any { it.contains("You: level") })
            assertFalse(texts.contains(string(com.cocwidget.R.string.guide_rush_stale, 18)))
        }
    }

    @Test
    fun aSyncedVillageSeesItsOwnTownHallAndHowFarEachHeroIs() {
        syncTh16()
        val texts = Screens.texts(Screens.root(Screens.open<GuideRushActivity>()))
        assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_at, 16)))
        assertFalse(texts.contains(string(com.cocwidget.R.string.guide_rush_band, 3, 12)))
        assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_before, 17)))
        assertFalse(texts.contains(string(com.cocwidget.R.string.guide_rush_before, 18)))
        assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_takes_in, 17, 7)))
        assertTrue(texts.any { it.contains("You: level") && it.contains("to go") })
        assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_closest)))
        // The export's Queen is 95, as far as Town Hall 16 goes. Her 8-day upgrades open at 17.
        assertTrue(texts.any { it.contains("You: level 95, maxed for Town Hall 16.") })
        assertFalse(texts.any { it.contains("already there") })
    }

    @Test
    fun aTownHallPageLeadsToRushingAtThatTownHall() {
        syncTh16()
        val page = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 15) }
        Screens.tappable(Screens.root(page), string(com.cocwidget.R.string.guide_rush_instead))!!.performClick()
        val started = shadowOf(page).nextStartedActivity
        assertEquals(GuideRushActivity::class.java.name, started.component?.className)
        assertEquals(15, started.getIntExtra(GuideScreen.EXTRA_VIEW_TOWN_HALL, 0))
        val texts = Screens.texts(Screens.root(Screens.open<GuideRushActivity> { putExtra(GuideScreen.EXTRA_VIEW_TOWN_HALL, 15) }))
        assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_at, 15)))
        assertTrue(texts.contains(string(com.cocwidget.R.string.guide_rush_before, 16)))
        // The player is TH16, so a TH15 page doesn't show their hero levels.
        assertFalse(texts.any { it.contains("You: level") })
    }

    @Test
    fun theGuideLeadsToRushingAndRushingToItsSource() {
        val guideScreen = Screens.open<GuideActivity>()
        Screens.tappable(Screens.root(guideScreen), string(com.cocwidget.R.string.guide_rush))!!.performClick()
        assertEquals(GuideRushActivity::class.java.name, shadowOf(guideScreen).nextStartedActivity.component?.className)
        val screen = Screens.open<GuideRushActivity>()
        Screens.tappable(Screens.root(screen), string(com.cocwidget.R.string.guide_rush_read))!!.performClick()
        val source = guide.content.sources.first { it.key == guide.content.rushing!!.sources.first() }
        assertEquals(source.url, shadowOf(screen).nextStartedActivity.dataString)
    }

    private fun openUnitAt(name: String, townHall: Int) = Screens.open<GuideUnitActivity> {
        putExtra(GuideScreen.EXTRA_ID, unitId(name))
        putExtra(GuideScreen.EXTRA_VIEW_TOWN_HALL, townHall)
    }

    private fun tipLine(text: String) = app.getString(com.cocwidget.R.string.guide_tip, text)

    @Test
    fun aLowTownHallSeesUsableTipsFirstAndLaterOnesLabelled() {
        val balloon = guide.content.unitTactics.getValue("Balloon")
        val later = balloon.use.first { it.from > 5 }
        val texts = Screens.texts(Screens.root(openUnitAt("Balloon", 5)))
        val label = tipLine(app.getString(com.cocwidget.R.string.guide_tip_from, later.from, later.text))
        assertTrue(texts.toString(), texts.contains(label))
        val first = balloon.use.first { !it.isLaterThan(5) && !it.isPast(5) }
        assertTrue(texts.indexOf(tipLine(first.text)) < texts.indexOf(label))
    }

    @Test
    fun aHighTownHallDoesNotSeeTipsItHasPassed() {
        val past = guide.content.unitTactics.getValue("Balloon").use.first { it.to in 1..17 }
        val texts = Screens.texts(Screens.root(openUnitAt("Balloon", 18)))
        assertFalse(texts.any { it.contains(past.text) })
    }

    @Test
    fun withNoTownHallKnownEveryTipSaysWhenItApplies() {
        val balloon = guide.content.unitTactics.getValue("Balloon")
        val texts = Screens.texts(Screens.root(openUnit("Balloon")))
        val past = balloon.use.first { it.to > 0 }
        assertTrue(texts.contains(tipLine(app.getString(com.cocwidget.R.string.guide_tip_to, past.to, past.text))))
    }

    @Test
    fun aChipFromATownHallPageKeepsThatTownHall() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 9) }
        val strategy = guide.townHall(9)!!.strategies.first()
        val name = strategy.army.first()
        Screens.tappable(Screens.root(screen), app.getString(com.cocwidget.R.string.guide_quantity, name, strategy.counts.getValue(name)))!!.performClick()
        assertEquals(9, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_VIEW_TOWN_HALL, 0))
    }

    @Test
    fun aPartnerThatOpensLaterSaysWhen() {
        val texts = Screens.texts(Screens.root(openUnit("Balloon")))
        val opens = units.values.first { it.name == "Lava Hound" }.townHall
        assertTrue(texts.contains(app.getString(com.cocwidget.R.string.guide_partner_later, "Lava Hound", opens)))
    }

    @Test
    fun aTroopSaysWhatItTargets() {
        val targets = guide.content.unitTactics.getValue("Giant").targets
        assertTrue(Screens.texts(Screens.root(openUnit("Giant"))).contains(app.getString(com.cocwidget.R.string.guide_goes_for, targets)))
    }

    @Test
    fun everyThreatPageOpensInEverySkin() {
        for (skin in skins) {
            Store.setSkin(app, skin.key)
            for (threat in guide.content.threats) {
                val texts = Screens.texts(Screens.root(Screens.open<GuideThreatActivity> { putExtra(GuideThreatActivity.EXTRA_THREAT, threat.name) }))
                assertTrue(threat.name, texts.contains(threat.tactics.role))
            }
        }
    }

    @Test
    fun aTownHallPageLeadsToWhatGuardsIt() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 18) }
        Screens.tappable(Screens.root(screen), "Logger")!!.performClick()
        assertEquals("Logger", shadowOf(screen).nextStartedActivity.getStringExtra(GuideThreatActivity.EXTRA_THREAT))
    }

    @Test
    fun theDefenceListLeadsToEveryThreat() {
        val screen = Screens.open<GuideDefencesActivity>()
        for (threat in guide.content.threats) {
            Screens.tappable(Screens.root(screen), threat.name)!!.performClick()
            assertEquals(threat.name, shadowOf(screen).nextStartedActivity.getStringExtra(GuideThreatActivity.EXTRA_THREAT))
        }
    }

    @Test
    fun aTownHallPageSaysWhatToUpgradeFirstAndHowFullEachArmyIs() {
        val texts = Screens.texts(Screens.root(Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 10) }))
        assertTrue(texts.contains(app.getString(com.cocwidget.R.string.guide_upgrade_first)))
        assertTrue(texts.any { it.contains("housing") })
    }

    @Test
    fun aHeroPageExplainsItsEquipment() {
        val hero = guide.facts.heroes.first { it.name == "Barbarian King" }
        val texts = Screens.texts(Screens.root(Screens.open<GuideHeroesActivity> { putExtra(GuideScreen.EXTRA_ID, hero.id) }))
        val tip = guide.content.equipmentTips.getValue("Spiky Ball").tip
        assertTrue(texts.any { it.contains(tip.text) })
        assertTrue(texts.contains(app.getString(com.cocwidget.R.string.guide_gear_other)))
    }

    @Test
    fun aMergedDefenceSaysWhatYoullMeet() {
        val texts = Screens.texts(Screens.root(openUnitAt("Cannon", 17)))
        assertTrue(texts.contains(app.getString(com.cocwidget.R.string.guide_meet)))
    }

    @Test
    fun defencesThatDontFitOneLineSayWhatTheyHit() {
        val expected = mapOf(
            "X-Bow" to "Hits ground, or air and ground",
            "Skeleton Trap" to "Set to chase ground or air",
            "Builder's Hut" to "Hits air and ground from TH14",
            "Air Sweeper" to "Pushes air troops back",
        )
        val plain = listOf(com.cocwidget.R.string.guide_hits_both, com.cocwidget.R.string.guide_hits_air, com.cocwidget.R.string.guide_hits_ground).map(app::getString)
        for ((name, chip) in expected) {
            val texts = Screens.texts(Screens.root(openUnit(name)))
            assertTrue(name, texts.contains(chip))
            assertEquals(name, emptyList<String>(), texts.filter { it in plain })
        }
    }

    @Test
    fun aPlainDefenceKeepsItsHitsChip() {
        assertTrue(Screens.texts(Screens.root(openUnit("Cannon"))).contains(app.getString(com.cocwidget.R.string.guide_hits_ground)))
        assertTrue(Screens.texts(Screens.root(openUnit("Air Defense"))).contains(app.getString(com.cocwidget.R.string.guide_hits_air)))
    }

    @Test
    fun aDefenceSaysHowToBeatItAndATroopHowToUseIt() {
        val defence = Screens.texts(Screens.root(openUnit("Inferno Tower")))
        assertTrue(defence.contains(app.getString(com.cocwidget.R.string.guide_how_to_beat)))
        assertFalse(defence.contains(app.getString(com.cocwidget.R.string.guide_how_to_use)))
        val troop = Screens.texts(Screens.root(openUnit("Giant")))
        assertTrue(troop.contains(app.getString(com.cocwidget.R.string.guide_how_to_use)))
        assertFalse(troop.contains(app.getString(com.cocwidget.R.string.guide_how_to_beat)))
    }

    // Defence pages once showed raw data in a chip: Prefers [{"h":17,"n":1,"l":2}].
    @Test
    fun noPageShowsRawData() {
        val raw = listOf("{", "}", "[", "]", "\"", "null", "undefined", "NaN")
        for (unit in units.values) {
            val intent = android.content.Intent(app, GuideUnitActivity::class.java).putExtra(GuideScreen.EXTRA_ID, unit.id)
            val controller = org.robolectric.Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup()
            for (text in Screens.texts(Screens.root(controller.get()))) {
                for (mark in raw) assertFalse("${unit.name} shows: $text", text.contains(mark))
            }
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun everyAttackChipOpensItsOwnPage() {
        for (page in guide.content.townHalls) {
            val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, page.townHall) }
            val root = Screens.root(screen)
            for (strategy in page.strategies) for (name in strategy.army) {
                val where = "TH${page.townHall} ${strategy.name}: $name"
                val chip = Screens.tappable(root, app.getString(com.cocwidget.R.string.guide_quantity, name, strategy.counts.getValue(name)))
                assertNotNull(where, chip)
                chip!!.performClick()
                val started = shadowOf(screen).nextStartedActivity
                assertEquals(where, GuideUnitActivity::class.java.name, started.component?.className)
                assertEquals(where, unitId(name), started.getIntExtra(GuideScreen.EXTRA_ID, 0))
            }
        }
    }

    @Test
    fun aHeroRowInAnAttackOpensThatHero() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 18) }
        Screens.tappable(Screens.root(screen), "Dragon Duke")!!.performClick()
        val started = shadowOf(screen).nextStartedActivity
        assertEquals(GuideHeroesActivity::class.java.name, started.component?.className)
        assertEquals(guide.facts.heroes.first { it.name == "Dragon Duke" }.id, started.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    @Test
    fun everyChipInTheTroopsAndSpellsListOpensItsPage() {
        val screen = Screens.open<GuideArmyActivity>()
        for (unit in units.values.filter { it.group !in setOf("defence", "trap") }) {
            val chip = Screens.tappable(Screens.root(screen), unit.name)
            assertNotNull(unit.name, chip)
            chip!!.performClick()
            assertEquals(unit.name, unit.id, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
        }
    }

    @Test
    fun everyChipInTheDefencesListOpensItsPage() {
        val screen = Screens.open<GuideDefencesActivity>()
        val defences = units.values.filter { it.group == "defence" || it.group == "trap" }
        assertTrue(defences.size > 25)
        for (unit in defences) {
            val chip = Screens.tappable(Screens.root(screen), unit.name)
            assertNotNull(unit.name, chip)
            chip!!.performClick()
            assertEquals(unit.name, unit.id, shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
        }
    }

    @Test
    fun aNewDefenceOnATownHallPageOpensItsPage() {
        val screen = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 10) }
        Screens.tappable(Screens.root(screen), "Inferno Tower ×2")!!.performClick()
        assertEquals(unitId("Inferno Tower"), shadowOf(screen).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    @Test
    fun aDefencePageShowsWhatAnAttackerNeeds() {
        val texts = Screens.texts(Screens.root(openUnit("Multi-Archer Tower")))
        assertTrue(texts.contains("How many, and how high"))
        assertTrue(texts.any { it.startsWith("TH16: 2 × Lv ") })
        assertTrue(texts.contains("Supercharges"))
        assertFalse(texts.contains("From the Multi-Archer Tower"))
        assertFalse(texts.contains("Walks"))
    }

    @Test
    fun aSyncedVillageShowsYourDefenceLevel() {
        syncTh16()
        val texts = Screens.texts(Screens.root(openUnit("Cannon")))
        assertTrue(texts.toString(), texts.any { it.startsWith("You: level ") && it.endsWith(" at TH16") })
    }

    @Test
    fun ruinWitchShowsHerRuinKnights() {
        val root = Screens.root(openUnit("Ruin Witch"))
        val texts = Screens.texts(root)
        assertTrue(texts.contains("Ruin Knight"))
        assertTrue(texts.any { it.contains("Ruin Knights summoned, at most: 10") })
    }

    @Test
    fun aSuperTroopLinksToItsBaseAndBack() {
        val superYeti = openUnit("Super Yeti")
        Screens.tappable(Screens.root(superYeti), "Yeti")!!.performClick()
        assertEquals(unitId("Yeti"), shadowOf(superYeti).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
        val yeti = openUnit("Yeti")
        Screens.tappable(Screens.root(yeti), "Super Yeti")!!.performClick()
        assertEquals(unitId("Super Yeti"), shadowOf(yeti).nextStartedActivity.getIntExtra(GuideScreen.EXTRA_ID, 0))
    }

    // Picking a level anywhere moves every chart, every readout and the Level card with it.
    @Test
    fun aTapOnAChartMovesTheWholePage() {
        val page = openUnit("Dragon")
        val root = Screens.root(page)
        val charts = Screens.views<LevelChart>(root)
        assertTrue(charts.size >= 2)
        assertTrue(Screens.texts(root).contains("Level 13"))

        val chart = charts.first()
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 0f, 50f)

        assertTrue(charts.all { it.selected == 0 })
        val texts = Screens.texts(root)
        assertTrue(texts.contains("Level 1"))
        assertTrue(texts.contains("Lv 1: 140"))
        assertTrue(texts.contains("Lv 1: 1,900"))
    }

    @Test
    fun aSidewaysDragPicksALevel() {
        val chart = Screens.views<LevelChart>(Screens.root(openUnit("Dragon"))).first()
        val right = chart.width.toFloat()
        assertTrue(right > 0)
        Screens.touch(chart, MotionEvent.ACTION_DOWN, right, 50f)
        Screens.touch(chart, MotionEvent.ACTION_MOVE, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 0f, 50f)
        assertEquals(0, chart.selected)
    }

    // From the emulator test: a scroll that starts on a chart used to change the level.
    @Test
    fun aVerticalScrollFromAChartLeavesTheLevelAlone() {
        val chart = Screens.views<LevelChart>(Screens.root(openUnit("Dragon"))).first()
        val before = chart.selected
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_MOVE, 2f, 200f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 2f, 200f)
        assertEquals(before, chart.selected)
    }

    @Test
    fun aRotationKeepsThePickedLevel() {
        val intent = android.content.Intent(app, GuideUnitActivity::class.java).putExtra(GuideScreen.EXTRA_ID, unitId("Dragon"))
        val controller = org.robolectric.Robolectric.buildActivity(GuideUnitActivity::class.java, intent).setup()
        Screens.layOut(controller.get())
        val chart = Screens.views<LevelChart>(Screens.root(controller.get())).first()
        Screens.touch(chart, MotionEvent.ACTION_DOWN, 0f, 50f)
        Screens.touch(chart, MotionEvent.ACTION_UP, 0f, 50f)
        controller.recreate()
        assertTrue(Screens.texts(Screens.root(controller.get())).contains("Level 1"))
    }

    @Test
    fun aSyncedVillageOpensOnYourLevelAndLocksWhatsAhead() {
        syncTh16()
        val witch = Screens.texts(Screens.root(openUnit("Ruin Witch")))
        assertTrue(witch.contains("Level 2"))
        assertTrue(witch.contains("You: level 2 of 2 at TH16"))
        assertTrue(Screens.texts(Screens.root(openUnit("Meteor Golem"))).contains("Unlocks at TH17"))
    }

    @Test
    fun everyTownHallButtonHasAPictureAndOpensItsPage() {
        for (townHall in Guide.FIRST_TOWN_HALL..18) {
            val hub = Screens.open<GuideActivity>()
            val button = Screens.tappable(Screens.root(hub), "TH$townHall")!!
            assertTrue("TH$townHall", Screens.views<android.widget.ImageView>(button).size == 1)
            button.performClick()
            val started = shadowOf(hub).nextStartedActivity
            assertEquals(GuideTownHallActivity::class.java.name, started.component?.className)
            assertEquals(townHall, started.getIntExtra(GuideScreen.EXTRA_TOWN_HALL, 0))
        }
    }

    @Test
    fun everyTownHallPageShowsItsOwnPicture() {
        for (townHall in Guide.FIRST_TOWN_HALL..18) {
            val page = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, townHall) }
            val picture = Screens.views<android.widget.ImageView>(Screens.root(page)).single { it.contentDescription == "Town Hall $townHall" }
            assertTrue("TH$townHall", picture.drawable != null)
        }
    }

    // They all fell back to the Town Hall's level 1 picture, the thatched hut, so the Inferno
    // Artillery looked like a Town Hall 1.
    @Test
    fun aTownHallWeaponShowsTheTownHallThatCarriesIt() {
        val guide = com.cocwidget.guide.Guide.load(app)
        val items = com.cocwidget.game.GameData.all(app)
        val picture = { name: String, viewing: Int -> guide.threatPicture(guide.threat(name)!!, items, viewing) }
        assertEquals(com.cocwidget.store.Icons.townHallPath(12), picture("Giga Tesla", 0))
        assertEquals(com.cocwidget.store.Icons.townHallPath(13), picture("Giga Inferno", 0))
        assertEquals(com.cocwidget.store.Icons.townHallPath(15), picture("Giga Inferno", 15))
        assertEquals(com.cocwidget.store.Icons.townHallPath(13), picture("Giga Inferno", 18))
        assertEquals(com.cocwidget.store.Icons.townHallPath(17), picture("Inferno Artillery", 16))
        assertEquals(com.cocwidget.store.Icons.townHallPath(18), picture("Town Hall 18", 0))
    }

    @Test
    fun aGuardianShowsItsOwnPicture() {
        val guide = com.cocwidget.guide.Guide.load(app)
        val items = com.cocwidget.game.GameData.all(app)
        for (name in listOf("Smasher", "Longshot", "Logger")) {
            assertEquals("guardians/${name.lowercase()}/icon.webp", guide.threatPicture(guide.threat(name)!!, items, 18))
        }
    }

    @Test
    fun noThreatFallsBackToAPictureOfSomethingElse() {
        val guide = com.cocwidget.guide.Guide.load(app)
        val items = com.cocwidget.game.GameData.all(app)
        for (threat in guide.content.threats) {
            assertFalse(threat.name, guide.threatPicture(threat, items, 0).endsWith("town_hall/level_1.webp"))
        }
    }

    @Test
    fun aMaxedBlacksmithStillHasAPicture() {
        val blacksmith = com.cocwidget.game.GameData.all(app).values.first { it.name == "Blacksmith" }
        assertEquals("buildings/home-village/blacksmith/level_9.webp", com.cocwidget.store.Icons.path(blacksmith, 10))
        assertEquals("buildings/home-village/blacksmith/level_3.webp", com.cocwidget.store.Icons.path(blacksmith, 3))
    }

    @Test
    fun aSyncedVillageMarksItsTownHall() {
        syncTh16()
        val hub = Screens.root(Screens.open<GuideActivity>())
        assertTrue(Screens.texts(hub).contains("Your Town Hall: TH16"))
        val mine = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 16) }
        assertTrue(Screens.texts(Screens.root(mine)).contains("You"))
        val other = Screens.open<GuideTownHallActivity> { putExtra(GuideScreen.EXTRA_TOWN_HALL, 15) }
        assertFalse(Screens.texts(Screens.root(other)).contains("You"))
    }

    @Test
    fun bestPetsGivesEachHeroADifferentPetForYourTownHall() {
        syncTh16()
        val texts = Screens.texts(Screens.root(Screens.open<GuidePairingsActivity>()))
        assertTrue(texts.contains("A set for TH16"))
        for (pet in listOf("Unicorn", "Electro Owl", "Spirit Fox", "Angry Jelly", "Frosty", "Phoenix")) {
            assertTrue(pet, texts.contains(pet))
        }
    }

    @Test
    fun theMainScreenOffersTheGuideWithOrWithoutAVillage() {
        val first = Screens.open<com.cocwidget.MainActivity>()
        Screens.tappable(Screens.root(first), "Guide")!!.performClick()
        assertEquals(GuideActivity::class.java.name, shadowOf(first).nextStartedActivity.component?.className)
        syncTh16()
        val synced = Screens.open<com.cocwidget.MainActivity>()
        assertNotNull(Screens.tappable(Screens.root(synced), "Guide"))
    }
}
