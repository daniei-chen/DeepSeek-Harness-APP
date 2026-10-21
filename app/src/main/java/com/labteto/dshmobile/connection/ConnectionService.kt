package com.labteto.dshmobile.connection

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.labteto.dshmobile.R
import com.labteto.dshmobile.notify.DshNotifications
import com.labteto.dshmobile.webshell.ShellDevices
import com.labteto.dshmobile.webshell.WebShellActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Foreground service that keeps the WebSocket connection alive while the app
 * is backgrounded (user opt-in via settings). The ConnectionManager owns the
 * actual connection; this service only pins the process with an ongoing
 * notification while it is running.
 */
@AndroidEntryPoint
class ConnectionService : Service() {

    @Inject lateinit var connectionManager: ConnectionManager
    @Inject lateinit var notifications: DshNotifications
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            connectionManager.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(connectionManager.state.value.phase))
        if (!observing) {
            observing = true
            serviceScope.launch {
                connectionManager.state.collect { state ->
                    NotificationManagerCompat.from(this@ConnectionService)
                        .notify(NOTIFICATION_ID, buildNotification(state.phase))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(phase: ConnectionPhase): Notification {
        val host = connectionManager.state.value.host
        val shellDevices = ShellDevices(this)
        val deviceName = shellDevices.load()
            .firstOrNull { it.id == shellDevices.currentId() }
            ?.name
            ?: host?.name
            ?: getString(R.string.common_connected)
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WebShellActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ConnectionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when (phase) {
            ConnectionPhase.CONNECTED -> getString(R.string.notif_connected_text, deviceName)
            ConnectionPhase.RECONNECTING -> getString(R.string.notif_reconnecting_text, deviceName)
            else -> getString(R.string.notif_connecting_text, deviceName)
        }
        val title = getString(R.string.notif_connected_title)
        val focusParams = JSONObject()
            .put("param_v2", JSONObject()
                .put("protocol", 1)
                .put("business", "deepseek.harness")
                .put("enableFloat", true)
                .put("updatable", true)
                .put("filterWhenNoPermission", false)
                .put("ticker", text)
                .put("aodTitle", title)
                .put("param_island", JSONObject()
                    .put("islandProperty", 1)
                    .put("bigIslandArea", JSONObject().put(
                        "imageTextInfoLeft",
                        JSONObject()
                            .put("type", 1)
                            .put("miui.focus.paramtextInfo", JSONObject()
                                .put("frontTitle", title)
                                .put("title", text)
                                .put("content", "")
                                .put("useHighLight", false)),
                    ))
                    .put("smallIslandArea", JSONObject()))
                .put("baseInfo", JSONObject()
                    .put("title", title)
                    .put("content", text)
                    .put("colorTitle", "#006EFF")
                    .put("type", 2)))
            .toString()
        val extras = Bundle().apply { putString("miui.focus.param", focusParams) }
        return NotificationCompat.Builder(this, DshNotifications.CHANNEL_CONNECTION_LIVE)
            .setSmallIcon(R.drawable.ic_notification_whale)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_disconnect), disconnectIntent)
            .addExtras(extras)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.labteto.dshmobile.connection.STOP"
        private const val NOTIFICATION_ID = 10
    }
}
