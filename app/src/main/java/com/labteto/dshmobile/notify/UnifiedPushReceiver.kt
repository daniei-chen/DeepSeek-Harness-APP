package com.labteto.dshmobile.notify

import android.content.Context
import android.widget.Toast
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * Optional UnifiedPush distributor endpoint.
 *
 * The app never ships a distributor or a public endpoint. If the user installs one (ntfy can act
 * as one) and configures it, this receiver turns incoming UnifiedPush messages into local
 * notifications. The host-side notify plugin can post to the endpoint shown by the distributor
 * using DSH_UNIFIEDPUSH_ENDPOINT.
 */
class UnifiedPushReceiver : MessagingReceiver() {
    override fun onMessage(context: Context, message: PushMessage, instance: String) {
        val text = runCatching { String(message.content, Charsets.UTF_8) }.getOrNull()?.trim().orEmpty()
        if (text.isEmpty()) return
        val notifications = DshNotifications(context)
        notifications.ensureChannels()
        notifications.postSession(
            channel = DshNotifications.CHANNEL_ACTION,
            id = (text.hashCode() and 0x7fffffff) % 100000 + 10000,
            title = "DeepSeek Harness",
            text = text,
            sessionId = null,
            actionLabel = context.getString(com.labteto.dshmobile.R.string.notif_open),
        )
    }

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
        UnifiedPushConfig.save(context, endpoint.url)
        Toast.makeText(context, "UnifiedPush endpoint registered", Toast.LENGTH_LONG).show()
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        Toast.makeText(context, "UnifiedPush registration failed: $reason", Toast.LENGTH_LONG).show()
    }

    override fun onUnregistered(context: Context, instance: String) {
        UnifiedPushConfig.clear(context)
    }


}
