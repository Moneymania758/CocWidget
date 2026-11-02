package com.cocwidget

import android.content.Context

object Store {
    private const val KEY_EXPORT = "export"
    private const val KEY_BUILDER_MULTIPLIER = "builder_multiplier"
    private const val KEY_NAME = "name"
    private const val KEY_COLLAPSED = "collapsed_sections"

    private fun prefs(context: Context) = context.getSharedPreferences("village", Context.MODE_PRIVATE)

    fun saveExport(context: Context, json: String) {
        prefs(context).edit().putString(KEY_EXPORT, json).apply()
    }

    fun village(context: Context): Village? {
        val json = prefs(context).getString(KEY_EXPORT, null) ?: return null
        return VillageParser.parse(context, json, builderMultiplier(context))
    }

    fun builderMultiplier(context: Context): Int =
        prefs(context).getInt(KEY_BUILDER_MULTIPLIER, BUILDER_POTION_MULTIPLIER)

    fun setBuilderMultiplier(context: Context, multiplier: Int) {
        prefs(context).edit().putInt(KEY_BUILDER_MULTIPLIER, multiplier).apply()
    }

    fun collapsedSections(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_COLLAPSED, emptySet()).orEmpty()

    fun toggleSection(context: Context, section: String) {
        val collapsed = collapsedSections(context).toMutableSet()
        if (!collapsed.remove(section)) collapsed += section
        prefs(context).edit().putStringSet(KEY_COLLAPSED, collapsed).apply()
    }

    fun displayName(context: Context): String? = prefs(context).getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }

    fun setDisplayName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_NAME, name).apply()
    }
}
