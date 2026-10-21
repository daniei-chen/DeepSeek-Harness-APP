package com.labteto.dshmobile.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fork: a session id carried by a notification tap.
 *
 * The notification's PendingIntent has always attached the session id, but nothing consumed it, so
 * tapping a completion/approval notification just opened whatever screen the app was on. This is
 * the missing hand-off: [MainActivity] publishes the id from the intent (cold start or
 * `onNewIntent`), and the main screen opens it once a connection exists.
 */
object IncomingSession {
    private val _pending = MutableStateFlow<String?>(null)

    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun publish(sessionId: String?) {
        if (!sessionId.isNullOrBlank()) _pending.value = sessionId
    }

    fun consume() {
        _pending.value = null
    }
}
