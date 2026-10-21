package com.labteto.dshmobile.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.NOT_PENDING
import com.labteto.dshmobile.data.QuestionOutcome
import com.labteto.dshmobile.data.SessionStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Runs the approve/reject actions on an approval notification.
 *
 * This only works while the app process still owns a connected [SessionStore]. That is the
 * honest boundary of a notification action without a second network stack: the action answers a
 * pending Remote Event waterfall, and the waterfall correlation id lives in the live store. If
 * Android killed the process, the tap still opens the app and the user can approve there; the
 * action reports that instead of pretending it succeeded.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var store: SessionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_APPROVE && action != ACTION_REJECT) return
        val sessionId = intent.getStringExtra(DshNotifications.EXTRA_SESSION_ID) ?: return
        val approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pending = goAsync()
        scope.launch {
            val outcome = runCatching {
                withTimeoutOrNull(8_000) {
                    store.respondApproval(
                        sessionId = sessionId,
                        approvalId = approvalId,
                        allow = action == ACTION_APPROVE,
                    )
                }
            }.getOrNull() ?: QuestionOutcome.Unsent
            withContext(Dispatchers.Main) {
                when (outcome) {
                    is QuestionOutcome.Accepted -> {
                        if (notificationId >= 0) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    }
                    is QuestionOutcome.Refused -> {
                        if (outcome.reason == NOT_PENDING && notificationId >= 0) {
                            // The request settled while the notification sat in the shade.
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        } else {
                            Toast.makeText(context, R.string.notif_action_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                    QuestionOutcome.Unsent -> {
                        Toast.makeText(context, R.string.notif_action_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_APPROVE = "com.labteto.dshmobile.notify.APPROVE"
        const val ACTION_REJECT = "com.labteto.dshmobile.notify.REJECT"
        const val EXTRA_APPROVAL_ID = "dsh_approval_id"
        const val EXTRA_NOTIFICATION_ID = "dsh_notification_id"
    }
}
