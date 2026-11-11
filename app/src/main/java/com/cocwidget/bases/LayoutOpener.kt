package com.cocwidget.bases

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.cocwidget.R

// Opens a layout link in Clash of Clans, from a site or from My bases.
object LayoutOpener {
    // A base above the village's Town Hall gets a word first, as the game may not take it.
    // [onOpen] runs once the link has gone to the game.
    fun open(activity: Activity, url: String, myTownHall: Int, onOpen: () -> Unit = {}) {
        val baseTownHall = BaseLinks.townHall(url)
        if (myTownHall > 0 && baseTownHall > myTownHall) {
            AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(activity.getString(R.string.bases_too_high_title, baseTownHall))
                .setMessage(activity.getString(R.string.bases_too_high, baseTownHall, myTownHall))
                .setPositiveButton(R.string.bases_open_anyway) { _, _ -> if (inGame(activity, url)) onOpen() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else if (inGame(activity, url)) {
            onOpen()
        }
    }

    // Straight into the game. Without it, the link's own page, which offers the download.
    private fun inGame(activity: Activity, url: String): Boolean {
        val uri = Uri.parse(url)
        return try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(BaseLinks.GAME_PACKAGE))
            true
        } catch (_: ActivityNotFoundException) {
            outside(activity, Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        }
    }

    fun outside(activity: Activity, intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(activity, R.string.bases_cant_open, Toast.LENGTH_SHORT).show()
        false
    }
}
