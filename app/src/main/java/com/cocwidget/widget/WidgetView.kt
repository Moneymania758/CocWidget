package com.cocwidget.widget

import com.cocwidget.game.Section

// The widget's quick views, each folding away the sections it doesn't show. Folding a section by
// hand afterwards still works, and then no view is lit.
enum class WidgetView(val folded: Set<Section>) {
    HOME(setOf(Section.BUILDER_BASE)),
    BUILDER_BASE(setOf(Section.BUILDERS, Section.LAB, Section.PETS)),
    ALL(emptySet());

    companion object {
        // [shown] is the sections on the widget, so a fold on a section that isn't there (pets
        // before the Pet House) doesn't stop a view matching.
        fun matching(collapsed: Set<String>, shown: Set<Section>): WidgetView? {
            val folded = shown.filter { it.name in collapsed }.toSet()
            return entries.firstOrNull { view -> shown.filter { it in view.folded }.toSet() == folded }
        }
    }
}
