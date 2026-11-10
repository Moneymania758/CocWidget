package com.cocwidget.widget

import com.cocwidget.game.Section

// The widget's quick views. Each draws only its own sections, and folding one of them by hand
// keeps the view. [folded] is what a view folded up to 2.12.1, when views were only folds.
enum class WidgetView(val shows: Set<Section>, val folded: Set<Section>) {
    HOME(setOf(Section.BUILDERS, Section.LAB, Section.PETS), setOf(Section.BUILDER_BASE)),
    BUILDER_BASE(setOf(Section.BUILDER_BASE), setOf(Section.BUILDERS, Section.LAB, Section.PETS)),
    ALL(Section.entries.toSet(), emptySet());

    companion object {
        // The view to show when none has been picked since 2.12.1: the one whose folds match, so
        // a view picked before the update stays. [shown] is the sections on the widget, so a fold
        // on a section that isn't there (pets before the Pet House) doesn't stop a view matching.
        fun matching(collapsed: Set<String>, shown: Set<Section>): WidgetView? {
            val folded = shown.filter { it.name in collapsed }.toSet()
            return entries.firstOrNull { view -> shown.filter { it in view.folded }.toSet() == folded }
        }
    }
}
