package com.cocwidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.cocwidget.store.Store
import com.cocwidget.ui.TimeText

// Quick Settings tile, so syncing works from the notification shade without leaving the game.
class SyncTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val village = Store.selected(this)?.let { Store.village(this, it) }
        tile.state = Tile.STATE_INACTIVE
        tile.subtitle = if (village == null) {
            getString(R.string.tile_never)
        } else {
            getString(R.string.exported_at, TimeText.exported(village.exportedAtMillis, System.currentTimeMillis()))
        }
        tile.updateTile()
    }

    // The PendingIntent overload only exists from Android 14, so 12 and 13 still need the Intent one.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, SyncActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
