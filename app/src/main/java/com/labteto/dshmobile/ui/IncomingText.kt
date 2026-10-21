package com.labteto.dshmobile.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fork: text handed to the app by Android's share sheet or the selection ("Process text") menu.
 *
 * The share lands in [MainActivity] before any screen exists — the process may even be cold — so
 * the hand-off is a tiny process-wide mailbox rather than an intent extra the Compose tree would
 * have to rebuild itself around. The chat screen consumes it once an open session has a composer
 * to put it in; until then it simply stays here.
 */
object IncomingText {
    private val _pending = MutableStateFlow<String?>(null)

    /** The text waiting for a composer, or null. Screens collect this. */
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Publish freshly shared text, ignoring empty payloads. */
    fun publish(text: String?) {
        val trimmed = text?.trim()
        if (!trimmed.isNullOrEmpty()) _pending.value = trimmed
    }

    /** Drop what was pending, after a screen has written it into a draft. */
    fun consume() {
        _pending.value = null
    }
}
