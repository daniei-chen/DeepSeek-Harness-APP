package com.labteto.dshmobile.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.labteto.dshmobile.R
import com.labteto.dshmobile.webshell.WebShellActivity
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional self-hosted ntfy subscriber.
 *
 * The host-side `notify-plugin/` publishes task/approval events to the operator's ntfy topic.
 * This foreground service keeps an SSE subscription so the phone can receive those events while
 * the app is in the background. It is intentionally opt-in and never points at a project cloud.
 */
class NtfySubscriberService : Service() {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val nextId = AtomicInteger(7000)

    @Volatile
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, getString(R.string.ntfy_service_channel), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MESSAGE, getString(R.string.notif_channel_action), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val endpoint = NtfyConfig.load(this)
        if (endpoint == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, serviceNotification(endpoint))
        if (worker?.isAlive != true) {
            worker = Thread({ subscribe(endpoint) }, "dsh-ntfy").also { it.start() }
        }
        return START_STICKY
    }

    private fun subscribe(endpoint: NtfyEndpoint) {
        while (!Thread.currentThread().isInterrupted) {
            try {
                val request = Request.Builder()
                    .url(endpoint.sseUrl)
                    .header("Accept", "text/event-stream")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("ntfy HTTP ${response.code}")
                    val source = response.body?.source() ?: error("ntfy empty body")
                    while (!Thread.currentThread().isInterrupted && !source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data:")) {
                            handleMessage(line.removePrefix("data:").trim())
                        }
                    }
                }
            } catch (_: Throwable) {
                // Reconnect below; a self-hosted ntfy can restart independently of the app.
            }
            try {
                Thread.sleep(5_000)
            } catch (_: InterruptedException) {
                return
            }
        }
    }

    private fun handleMessage(data: String) {
        val json = runCatching { JSONObject(data) }.getOrNull() ?: return
        if (json.optString("event", "message") != "message") return
        val message = json.optString("message").trim()
        if (message.isEmpty()) return
        val title = json.optString("title").ifBlank { getString(R.string.app_name) }
        val open = PendingIntent.getActivity(
            this,
            nextId.get(),
            Intent(this, WebShellActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGE)
            .setSmallIcon(R.drawable.ic_notification_whale)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(nextId.incrementAndGet(), notification) }
    }

    private fun serviceNotification(endpoint: NtfyEndpoint): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WebShellActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification_whale)
            .setContentTitle(getString(R.string.ntfy_service_title))
            .setContentText(endpoint.displayUrl)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        worker?.interrupt()
        worker = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_SERVICE = "ntfy_service"
        const val CHANNEL_MESSAGE = "ntfy_messages"
        private const val NOTIFICATION_ID = 31
    }
}
