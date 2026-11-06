package com.cocwidget

import com.cocwidget.guide.Threat
import com.cocwidget.ui.GuideScreen

// A Town Hall's own weapon or a Guardian: what it does, and how to beat it.
class GuideThreatActivity : GuideScreen() {
    private val threat: Threat? by lazy { guide.threat(intent.getStringExtra(EXTRA_THREAT).orEmpty()) }

    override fun buildGuide() {
        val threat = this.threat
        if (threat == null) {
            finish()
            return
        }
        content.addView(backHeader(threat.name))
        content.addView(ui.card().apply {
            addView(titleRow(threatPicture(threat, 44), threat.name, whenItAppears(threat)))
            addView(ui.text(threat.tactics.role, 14f).withTerms(), ui.matchWidth(8))
            addTactics(threat.tactics, defence = true, opensAt = threat.from)
        })
        addNote(getString(R.string.guide_tips_note, guide.content.checked))
    }

    private fun whenItAppears(threat: Threat) = getString(
        when (threat.kind) {
            "guardian" -> R.string.guide_threat_guardian
            "townhall" -> R.string.guide_threat_no_weapon
            else -> R.string.guide_threat_weapon
        },
        if (threat.from == threat.to) getString(R.string.town_hall_chip, threat.from) else getString(R.string.guide_threat_range, threat.from, threat.to),
    )

    companion object {
        const val EXTRA_THREAT = "threat"
    }
}
