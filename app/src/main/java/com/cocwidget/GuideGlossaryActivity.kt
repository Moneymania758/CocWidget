package com.cocwidget

import android.content.Intent
import com.cocwidget.guide.GlossaryTerm
import com.cocwidget.ui.GuideScreen

// Every word the guide explains, by the kind of word it is.
class GuideGlossaryActivity : GuideScreen() {
    override fun buildGuide() {
        content.addView(backHeader(R.string.guide_glossary))
        addNote(getString(R.string.guide_glossary_intro), 13f)
        val groups = listOf(
            "attack" to R.string.guide_glossary_attack,
            "army" to R.string.guide_glossary_army,
            "game" to R.string.guide_glossary_game,
            "slang" to R.string.guide_glossary_slang,
        )
        for ((group, title) in groups) {
            val terms = guide.glossary.terms.filter { it.group == group }.sortedBy { it.term.lowercase() }
            if (terms.isEmpty()) continue
            content.addView(ui.card(0).apply {
                addView(ui.row().apply { addView(sectionTitle(getString(title))) })
                for (term in terms) {
                    addView(ui.divider())
                    addView(termRow(term))
                }
            }, ui.matchWidth(14))
        }
    }

    private fun termRow(term: GlossaryTerm) = ui.row().apply {
        addView(ui.text(term.term, 15f, bold = true))
        addView(ui.text(term.short, 13f, ui.skin.dim), ui.matchWidth(2))
        ui.pressable(this, 0f) { openTerm(term.term) }
    }
}

// One word: what it means here, an example, and where to go next.
class GuideTermActivity : GuideScreen() {
    private val term: GlossaryTerm? by lazy { guide.glossary.term(intent.getStringExtra(EXTRA_TERM).orEmpty()) }

    override val ownTerm: String? get() = term?.term

    override fun buildGuide() {
        val term = this.term
        if (term == null) {
            finish()
            return
        }
        content.addView(backHeader(term.term))
        content.addView(ui.card().apply {
            addView(ui.text(term.short, 15f, bold = true).withTerms())
            for (paragraph in term.means) addView(ui.text(paragraph, 14f).withTerms(), ui.matchWidth(10))
            if (term.example.isNotEmpty()) {
                addView(ui.text(getString(R.string.guide_term_example), 13f, bold = true), ui.matchWidth(12))
                addView(ui.text(term.example, 14f).withTerms(), ui.matchWidth(3))
            }
            if (term.units.isNotEmpty()) {
                addView(ui.text(getString(R.string.guide_term_units), 13f, bold = true), ui.matchWidth(12))
                addView(ui.flow().apply {
                    for (name in term.units) addView(unitChip(unitId(name), name))
                }, ui.matchWidth(4))
            }
            if (term.see.isNotEmpty()) {
                addView(ui.text(getString(R.string.guide_term_see), 13f, bold = true), ui.matchWidth(12))
                addView(ui.flow().apply {
                    for (name in term.see) {
                        addView(ui.chip(name).apply {
                            minHeight = ui.dp(40)
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            ui.pressable(this, 8f) { openTerm(name) }
                        })
                    }
                }, ui.matchWidth(4))
            }
            addSources(term.sources)
        })
        content.addView(ui.button(getString(R.string.guide_glossary), null, primary = false) {
            startActivity(Intent(this, GuideGlossaryActivity::class.java))
        }, ui.matchWidth(14))
    }
}
