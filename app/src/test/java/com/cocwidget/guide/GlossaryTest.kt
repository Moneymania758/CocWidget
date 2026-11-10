package com.cocwidget.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GlossaryTest {
    private fun term(name: String, vararg aliases: String) =
        GlossaryTerm(name, "attack", "short", listOf("means"), "", aliases.toList(), emptyList(), emptyList(), emptyList())

    private val small = Glossary(
        listOf(
            term("Funnel", "funnel", "funnels"),
            term("Queen Charge", "Queen Charge"),
            term("Charge plan", "hero charge"),
            term("Clan Castle", "Clan Castle", "CC"),
            term("Town Hall", "TH#"),
            term("Mid-base", "mid-base"),
        ),
    )

    private fun found(text: String, skip: String? = null) = small.matches(text, skip).map { text.substring(it.start, it.end) to it.term.term }

    @Test
    fun findsATermAndWhereItIs() {
        val match = small.matches("Make a funnel first.").single()
        assertEquals("Funnel", match.term.term)
        assertEquals("funnel", "Make a funnel first.".substring(match.start, match.end))
    }

    @Test
    fun onlyMatchesWholeWords() {
        assertEquals(emptyList<Any>(), found("funnelling"))
        assertEquals(emptyList<Any>(), found("A superfunnel"))
    }

    @Test
    fun ignoresCaseUnlessTheAliasIsAllCapitals() {
        assertEquals(listOf("Funnel" to "Funnel"), found("Funnel it."))
        assertEquals(listOf("CC" to "Clan Castle"), found("Lure the CC."))
        assertEquals(emptyList<Any>(), found("Lure the cc."))
    }

    @Test
    fun marksEachTermOnlyTheFirstTimeInAText() {
        assertEquals(listOf("funnel" to "Funnel"), found("A funnel is a funnel, and funnels help."))
    }

    @Test
    fun differentTermsAreAllMarkedInOrder() {
        assertEquals(listOf("funnel" to "Funnel", "CC" to "Clan Castle"), found("Make a funnel, then lure the CC."))
    }

    @Test
    fun leavesOutTheTermWhosePageThisIs() {
        assertEquals(listOf("CC" to "Clan Castle"), found("A funnel and the CC.", skip = "Funnel"))
    }

    @Test
    fun readsTownHallNumbers() {
        assertEquals(listOf("TH18" to "Town Hall"), found("Strong at TH18."))
        assertEquals(emptyList<Any>(), found("Nothing like TH here."))
        assertEquals(emptyList<Any>(), found("Lowercase th18 isn't it."))
    }

    @Test
    fun keepsHyphenatedTerms() {
        assertEquals(listOf("mid-base" to "Mid-base"), found("Past mid-base."))
    }

    @Test
    fun anEmptyGlossaryFindsNothing() {
        assertEquals(emptyList<TermMatch>(), Glossary(emptyList()).matches("A funnel."))
    }

    // The real glossary, as players will meet it.
    private val guide = Guide.parse(
        File("src/main/assets/guide_content.json").readText(),
        File("src/main/assets/guide_facts.json").readText(),
    )
    private val units = GuideUnits.parse(File("src/main/assets/guide_units.json").readText())
    private val real = guide.glossary

    private fun realTerms(text: String) = real.matches(text).map { it.term.term }

    @Test
    fun eachTermIsMarkedOnceAcrossAScreensText() {
        val seen = HashSet<String>()
        assertEquals(listOf("Funnel"), small.matches("A funnel.", seen = seen).map { it.term.term })
        assertEquals(emptyList<String>(), small.matches("Another funnel.", seen = seen).map { it.term.term })
    }

    @Test
    fun aPlainVerbIsNotMarkedAsJargon() {
        for (line in listOf("Super Yetis carry the push.", "The Eagle merges into the Town Hall.")) {
            assertFalse(line, realTerms(line).any { it == "Carry" || it == "Merged defence" })
        }
    }

    // "Raid" alone means farming, but not inside the Clan Capital's names.
    @Test
    fun raidMedalsAreNotFarming() {
        assertEquals("Raid Medals", realTerms("Raid Medals go on ore first.").first())
        assertEquals(listOf("Raid Medals"), realTerms("Raid Weekend ends on Monday."))
        assertEquals(listOf("Farming"), realTerms("A quick raid for Gold."))
    }

    @Test
    fun explainsTheWordsThisWorkWasFor() {
        assertTrue(real.term("Funnel") != null)
        assertTrue(realTerms("Make a funnel first.").contains("Funnel"))
        assertTrue(realTerms("Great at TH18.").isNotEmpty())
        assertTrue(realTerms("Lure the Clan Castle.").isNotEmpty())
        for (word in listOf("a triple", "the back end", "an expo", "stacked defences", "Ranked Battles", "Regular Battles")) {
            assertTrue(word, realTerms(word).isNotEmpty())
        }
    }

    @Test
    fun plainEnglishIsNotMistakenForAPlan() {
        for (line in listOf("Giants walk to the nearest defence.", "He can walk or fly over walls.", "Recharges in 50s.")) {
            assertEquals(line, emptyList<String>(), realTerms(line))
        }
    }

    @Test
    fun noAliasBelongsToTwoTerms() {
        val aliases = real.terms.flatMap { term -> term.aliases.map { it.lowercase() to term.term } }
        val repeated = aliases.groupBy({ it.first }, { it.second }).filter { it.value.size > 1 }
        assertEquals(emptyMap<String, List<String>>(), repeated)
    }

    @Test
    fun officialNamesHaveTheirOwnPagesNotTerms() {
        // Some equipment is a term too: its page has the numbers, and the term says how armies use it.
        val names = units.values.filter { it.kind != "equipment" }.map { it.name.lowercase() }.toSet()
        val clashes = real.terms.flatMap { term -> term.aliases.filter { it.lowercase() in names }.map { "${term.term}: $it" } }
        assertEquals(emptyList<String>(), clashes)
    }

    @Test
    fun everyTermIsComplete() {
        val groups = setOf("attack", "army", "game", "slang")
        val problems = mutableListOf<String>()
        val unitNames = units.values.map { it.name }.toSet()
        val sources = guide.content.sources.map { it.key }.toSet()
        for (term in real.terms) {
            if (term.group !in groups) problems += "${term.term}: group ${term.group}"
            if (term.short.isBlank() || term.means.isEmpty() || term.aliases.isEmpty()) problems += "${term.term}: incomplete"
            for (other in term.see) if (real.term(other) == null || other == term.term) problems += "${term.term}: see $other"
            for (unit in term.units) if (unit !in unitNames && guide.threat(unit) == null) problems += "${term.term}: no unit called $unit"
            for (key in term.sources) if (key !in sources) problems += "${term.term}: unknown source $key"
        }
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun followsTheHouseStyle() {
        val banned = Regex("[\\u2013\\u2014;]|\\bdefense|\\bcolor|behavior|favorite|armor|\\b(?!(?:size|sizes|prize|prizes|seize)\\b)\\w+iz(e|ed|es|ing|ation)\\b")
        val problems = real.terms.flatMap { term ->
            (listOf(term.short, term.example) + term.means).mapNotNull { line -> banned.find(line)?.let { "${term.term}: ${it.value} in \"$line\"" } }
        }
        assertEquals(emptyList<String>(), problems)
        assertFalse(real.terms.isEmpty())
    }
}
