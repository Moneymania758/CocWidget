package com.cocwidget.guide

// A word players use, and what it means in the game. Written by hand in guide_content.json.
class GlossaryTerm(
    val term: String,
    // Which list it's in: attack, army, game or slang.
    val group: String,
    val short: String,
    val means: List<String>,
    val example: String,
    val aliases: List<String>,
    val see: List<String>,
    val units: List<String>,
    val sources: List<String>,
)

// Where a term sits in a piece of text. [end] is exclusive.
class TermMatch(val start: Int, val end: Int, val term: GlossaryTerm)

class Glossary(val terms: List<GlossaryTerm>) {
    private val byName = terms.associateBy { it.term }

    // Case is ignored unless an alias is all capitals, so "CC" won't catch "cc" in a word list.
    private val byAlias = HashMap<String, GlossaryTerm>().apply {
        for (term in terms) for (alias in term.aliases) if (alias != TOWN_HALL_ALIAS) put(alias.lowercase(), term)
    }
    private val townHallTerm = terms.firstOrNull { TOWN_HALL_ALIAS in it.aliases }

    // Longest first, so "Queen Charge" wins over anything inside it.
    private val pattern: Regex? = terms.flatMap { it.aliases }
        .sortedByDescending { it.length }
        .map(::aliasPattern)
        .takeIf { it.isNotEmpty() }
        ?.let { Regex("(?<![\\p{L}\\p{N}])(?:${it.joinToString("|")})(?![\\p{L}\\p{N}])") }

    fun term(name: String): GlossaryTerm? = byName[name]

    // The first place each term appears in [text]. Marking every repeat would turn a line into a
    // wall of links. [skip] is the term whose own page this text is on. [seen] carries the terms
    // already marked, so a screen can mark each one once across all its text.
    fun matches(text: String, skip: String? = null, seen: MutableSet<String> = HashSet()): List<TermMatch> {
        val found = pattern?.findAll(text) ?: return emptyList()
        return found.mapNotNull { match ->
            val term = termFor(match.value)
            if (term == null || term.term == skip || !seen.add(term.term)) null
            else TermMatch(match.range.first, match.range.last + 1, term)
        }.toList()
    }

    private fun termFor(found: String): GlossaryTerm? =
        if (TOWN_HALL.matches(found)) townHallTerm else byAlias[found.lowercase()]

    private fun aliasPattern(alias: String): String = when {
        alias == TOWN_HALL_ALIAS -> TOWN_HALL.pattern
        isCapitals(alias) -> Regex.escape(alias)
        else -> "(?i:${Regex.escape(alias)})"
    }

    private fun isCapitals(alias: String) = alias.any(Char::isLetter) && alias == alias.uppercase()

    companion object {
        // Stands for TH followed by a number, as in TH18.
        const val TOWN_HALL_ALIAS = "TH#"
        private val TOWN_HALL = Regex("TH\\d+")
    }
}
