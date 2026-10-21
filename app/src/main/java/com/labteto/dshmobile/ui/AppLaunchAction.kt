package com.labteto.dshmobile.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fork: one-shot launch intents from surfaces that are not the app itself — launcher shortcuts,
 * the Quick Settings tile, and the home-screen widget.
 *
 * Kept process-wide for the same reason as [IncomingText]: a surface may cold-start the process,
 * and the Compose tree the action is meant for does not exist yet when the intent arrives.
 */
object AppLaunchAction {
    /** Open the voice sheet once a session has a composer. */
    const val VOICE: String = "voice"

    /** The intent action the shortcuts/tile/widget put on [android.content.Intent]. */
    const val ACTION_VOICE: String = "com.labteto.dshmobile.action.VOICE"

    private val _pending = MutableStateFlow<String?>(null)

    /** The action waiting for the chat screen, or null. */
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun publish(action: String?) {
        if (!action.isNullOrBlank()) _pending.value = action
    }

    fun consume() {
        _pending.value = null
    }
}
