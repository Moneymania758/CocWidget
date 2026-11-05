package com.cocwidget

import com.cocwidget.ui.Screen
import com.cocwidget.ui.WhatsNew

// Who helped, where the data comes from, and what each version added.
class AboutActivity : Screen() {
    override fun build() {
        content.addView(backHeader(R.string.about_title))
        val names = resources.getStringArray(R.array.thanks_names)
        if (names.isNotEmpty()) {
            content.addView(ui.card().apply {
                addView(ui.heading(getString(R.string.thanks_title), 16f, ui.skin.accent))
                addView(ui.text(getString(R.string.thanks_detail), 14f, ui.skin.dim), ui.matchWidth(6))
                addView(ui.text(names.joinToString("\n"), 15f), ui.matchWidth(10))
            }, ui.matchWidth(12))
        }
        content.addView(ui.card().apply {
            addView(ui.text(getString(R.string.version_line, installedVersion()), 15f, bold = true))
            addView(ui.text(getString(R.string.credit) + "\n\n" + getString(R.string.font_credit), 14f, ui.skin.dim), ui.matchWidth(10))
        }, ui.matchWidth(if (names.isEmpty()) 12 else 16))
        content.addView(ui.card().apply {
            addView(ui.heading(getString(R.string.fold_whats_new), 16f, ui.skin.accent))
            for (release in WhatsNew.RELEASES) {
                addView(ui.text(release.version, 15f, bold = true), ui.matchWidth(14))
                addView(ui.text(getString(release.notes), 14f, ui.skin.dim), ui.matchWidth(4))
            }
        }, ui.matchWidth(16))
    }
}
