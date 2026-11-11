package com.cocwidget.widget

import com.cocwidget.R

// How a big widget lays out its upgrades. Sections ticks every row and folds to make room. Timeline
// puts everything in one scrolling list in finish order: only its top card ticks, as countdowns
// inside a scrolling list stop on One UI, so the list shows hours and minutes and is redrawn each
// minute. Pages ticks every row and flips through them with arrows instead of scrolling.
enum class WidgetStyle(val key: String, val label: Int, val hint: Int) {
    SECTIONS("sections", R.string.widget_style_sections, R.string.widget_style_sections_hint),
    TIMELINE("timeline", R.string.widget_style_timeline, R.string.widget_style_timeline_hint),
    PAGES("pages", R.string.widget_style_pages, R.string.widget_style_pages_hint);

    companion object {
        fun of(key: String?): WidgetStyle? = entries.firstOrNull { it.key == key }
    }
}
