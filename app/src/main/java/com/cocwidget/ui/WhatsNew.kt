package com.cocwidget.ui

import com.cocwidget.R

// What each release added, newest first. Add the new version's notes here with every release:
// a unit test fails while the newest entry doesn't match versionName.
object WhatsNew {
    class Release(val version: String, val notes: Int)

    val RELEASES = listOf(
        Release("2.3", R.string.whats_new_2_3),
        Release("2.2", R.string.whats_new_2_2),
        Release("2.1", R.string.whats_new_2_1),
        Release("2.0", R.string.whats_new_2_0),
        Release("1.9.1", R.string.whats_new_1_9_1),
        Release("1.9", R.string.whats_new_1_9),
        Release("1.8.1", R.string.whats_new_1_8_1),
        Release("1.8", R.string.whats_new_1_8),
        Release("1.7", R.string.whats_new_1_7),
        Release("1.6", R.string.whats_new_1_6),
        Release("1.5", R.string.whats_new_1_5),
        Release("1.4", R.string.whats_new_1_4),
        Release("1.3", R.string.whats_new_1_3),
        Release("1.2", R.string.whats_new_1_2),
        Release("1.1", R.string.whats_new_1_1),
        Release("1.0", R.string.whats_new_1_0),
    )

    // The card shows once per update, until it's dismissed. A fresh install has nothing new to
    // show, which is told apart from an update from 1.6 or earlier (when nothing was recorded) by
    // there being a synced export.
    fun showCard(installed: String, lastSeen: String?, hasSynced: Boolean): Boolean =
        installed != lastSeen && (lastSeen != null || hasSynced)
}
