package com.labteto.dshmobile.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.PromptOutcome
import com.labteto.dshmobile.data.SessionStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sends the text typed into a completion/question notification's reply field.
 *
 * The action is minted by [DshNotifications.postSession] with [ACTION_REPLY] and a mutable
 * PendingIntent (the system writes the typed text into the broadcast as RemoteInput results).
 *
 * Like [NotificationActionReceiver], this only works while the app process still owns a connected
 * [SessionStore]: the prompt is a plain session prompt over the live API client, and there is no
 * second network stack behind a notification action. If Android killed the process, the prompt
 * cannot be sent, so the action says so instead of pretending it succeeded — the notification
 * stays in the shade and tapping it still opens the app.
 *
 * The message is queued (`mode = "queue"`), not steered: a reply from the shade is composed after
 * the turn it answers has ended, so "send this next" is the only honest intent. A session that is
 * genuinely mid-turn queues it the same way the web composer does.
 *
 * **The Lead must add this declaration to `app/src/main/AndroidManifest.xml`** (next to
 * `.notify.NotificationActionReceiver`); this task is not allowed to edit the manifest:
 *
 * ```xml
 * <!-- Direct reply from a completion/question notification. The PendingIntent is explicit and
 *      mutable (the system fills in the RemoteInput results), so the receiver never needs to be
 *      exported or matched by an intent filter. -->
 * <receiver
 *     android:name=".notify.ReplyReceiver"
 *     android:exported="false" />
 * ```
 */
@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {

    @Inject lateinit var store: SessionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPLY) return
        val sessionId = intent.getStringExtra(DshNotifications.EXTRA_SESSION_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val text = replyText(intent).trim()
        // An empty field is nothing to send, not a failure to report: leave the notification
        // alone so the user can still open the session from it.
        if (text.isEmpty()) return

        val pending = goAsync()
        scope.launch {
            val outcome = runCatching {
                withTimeoutOrNull(REPLY_TIMEOUT_MS) {
                    store.prompt(text = text, mode = MODE_QUEUE, targetSessionId = sessionId)
                }
            }.getOrNull() ?: PromptOutcome.Failed("reply timed out")
            try {
                withContext(Dispatchers.Main) {
                    when (outcome) {
                        is PromptOutcome.Ok -> {
                            if (notificationId >= 0) {
                                NotificationManagerCompat.from(context).cancel(notificationId)
                            }
                            Toast.makeText(context, R.string.notif_reply_sent, Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(context, R.string.notif_action_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** What the user typed into the reply field, or `""` when the intent carries no results. */
    private fun replyText(intent: Intent): String =
        RemoteInput.getResultsFromIntent(intent)?.getCharSequence(RESULT_KEY)?.toString().orEmpty()

    companion object {
        /** RemoteInput result key: the text the user typed. */
        const val RESULT_KEY = "dsh_reply_text"

        /** Broadcast action of a reply tap; anything else is not ours and is ignored. */
        const val ACTION_REPLY = "com.labteto.dshmobile.notify.REPLY"

        /** Same key the approval actions use — both name "which notification am I acting on". */
        const val EXTRA_NOTIFICATION_ID = "dsh_notification_id"

        /** The prompt is one ordinary RPC; a tap must never leave the shade spinning. */
        private const val REPLY_TIMEOUT_MS = 8_000L

        /** Replies from the shade always queue; steering a finished turn makes no sense. */
        private const val MODE_QUEUE = "queue"
    }
}
