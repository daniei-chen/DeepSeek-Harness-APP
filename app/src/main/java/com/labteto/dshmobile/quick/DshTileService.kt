package com.labteto.dshmobile.quick

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.labteto.dshmobile.webshell.WebShellActivity
import com.labteto.dshmobile.R
import com.labteto.dshmobile.webshell.AppLaunchAction

/**
 * Fork: a Quick Settings tile that opens the app on the voice prompt.
 *
 * A tile rather than a toggle: DSH has no single on/off state the phone can own, and pretending
 * otherwise would leave the tile's state lying after the app is killed.
 */
class DshTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.app_name)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_notification_whale)
            tile.updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, WebShellActivity::class.java).apply {
            action = AppLaunchAction.ACTION_VOICE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this,
                7,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

