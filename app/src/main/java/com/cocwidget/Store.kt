package com.cocwidget

import android.content.Context

object Store {
    private const val KEY_EXPORT = "export"
    private const val KEY_BUILDER_MULTIPLIER = "builder_multiplier"
    private const val KEY_NAME = "name"
    private const val KEY_COLLAPSED = "collapsed_sections"
    private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_NOTIFIED = "notified_upgrades"
    private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"

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

    fun lastUpdateCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_UPDATE_CHECK, 0)

    fun setLastUpdateCheck(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_LAST_UPDATE_CHECK, millis).apply()
    }

    fun latestVersion(context: Context): String? = prefs(context).getString(KEY_LATEST_VERSION, null)

    fun setLatestVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_LATEST_VERSION, version).apply()
    }

    fun notifiedUpgrades(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()

    fun setNotifiedUpgrades(context: Context, keys: Set<String>) {
        prefs(context).edit().putStringSet(KEY_NOTIFIED, keys).apply()
    }

    fun askedNotifications(context: Context): Boolean = prefs(context).getBoolean(KEY_ASKED_NOTIFICATIONS, false)

    fun setAskedNotifications(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
    }

    fun displayName(context: Context): String? = prefs(context).getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }

    fun setDisplayName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_NAME, name).apply()
    }
}
