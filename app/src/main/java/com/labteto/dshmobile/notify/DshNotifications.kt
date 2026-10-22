package com.labteto.dshmobile.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.labteto.dshmobile.R
import com.labteto.dshmobile.webshell.WebShellActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification channels + post helpers. Completion events come from the
 * core CompletionClassifier (turn end, goal complete/blocked, plan review,
 * approval/question requested).
 */
@Singleton
class DshNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_COMPLETIONS, context.getString(R.string.notif_channel_completions), NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTION, context.getString(R.string.notif_channel_action), NotificationManager.IMPORTANCE_HIGH),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONNECTION, context.getString(R.string.notif_channel_connection), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONNECTION_LIVE, context.getString(R.string.notif_channel_connection), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Post a completion/action notification that deep-links to a session.
     * [canPost] gates the POST_NOTIFICATIONS runtime permission (API 33+);
     * the lint suppression covers that checked gate.
     *
     * [replyEnabled] hangs a free-form RemoteInput reply action on the notification, so a turn that
     * has already ended can be answered from the shade ("queue this" rather than "open the app and
     * retype it"). [ReplyReceiver] does the sending. Approval notifications never get it: they are
     * waiting on a decision between two buttons, not on prose, and a text field there would invite
     * an answer the harness cannot accept.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun postSession(
        channel: String,
        id: Int,
        title: String,
        text: String,
        sessionId: String?,
        actionLabel: String? = null,
        approvalId: String? = null,
        replyEnabled: Boolean = false,
    ) {
        if (!canPost()) return
        val open = Intent(context, WebShellActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (sessionId != null) {
                data = android.net.Uri.parse("dshmobile://host/current/session/$sessionId")
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_whale)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(if (channel == CHANNEL_ACTION) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (approvalId != null && sessionId != null) {
            builder.addAction(
                0,
                context.getString(R.string.notif_approve),
                actionIntent(
                    action = NotificationActionReceiver.ACTION_APPROVE,
                    sessionId = sessionId,
                    approvalId = approvalId,
                    notificationId = id,
                    requestCode = id * 10 + 1,
                ),
            )
            builder.addAction(
                0,
                context.getString(R.string.notif_reject),
                actionIntent(
                    action = NotificationActionReceiver.ACTION_REJECT,
                    sessionId = sessionId,
                    approvalId = approvalId,
                    notificationId = id,
                    requestCode = id * 10 + 2,
                ),
            )
        } else {
            // Reply first so it is the primary action; "open" stays available beside it.
            if (replyEnabled && sessionId != null) {
                builder.addAction(replyAction(sessionId = sessionId, notificationId = id))
            }
            if (actionLabel != null) {
                builder.addAction(0, actionLabel, pending)
            }
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /**
     * The free-form reply action: one RemoteInput, one explicit broadcast.
     *
     * The PendingIntent has to be **mutable** on Android 12+: the system writes the typed text into
     * the broadcast as extras, and an immutable PendingIntent cannot receive them (the reply is
     * silently dropped). It is explicit — an app-internal component — so nothing else can hijack
     * it, and the mutable flag simply does not exist before API 31, where PendingIntents are
     * mutable by default.
     */
    private fun replyAction(
        sessionId: String,
        notificationId: Int,
    ): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(ReplyReceiver.RESULT_KEY)
            .setLabel(context.getString(R.string.notif_reply_hint))
            .build()
        val intent = Intent(context, ReplyReceiver::class.java).apply {
            action = ReplyReceiver.ACTION_REPLY
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(ReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val replyPending = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + REPLY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification_whale,
            context.getString(R.string.notif_reply_label),
            replyPending,
        )
            .addRemoteInput(remoteInput)
            .build()
    }

    private fun actionIntent(
        action: String,
        sessionId: String,
        approvalId: String,
        notificationId: Int,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(DshNotifications.EXTRA_SESSION_ID, sessionId)
            putExtra(NotificationActionReceiver.EXTRA_APPROVAL_ID, approvalId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_COMPLETIONS = "completions"
        const val CHANNEL_ACTION = "needs_action"
        const val CHANNEL_CONNECTION = "connection"
        const val CHANNEL_CONNECTION_LIVE = "connection_live"
        const val EXTRA_SESSION_ID = "dsh_session_id"

        /**
         * Request-code slot on one notification id: 1 approve, 2 reject, 3 reply.
         *
         * Approve and reject are minted as `id * 10 + 1` / `+ 2`; the reply field takes the next
         * slot on the same grid rather than its own numbering, so no two PendingIntents the app
         * derives from one notification id can ever collide.
         */
        private const val REPLY_REQUEST_CODE = 3
    }
}

