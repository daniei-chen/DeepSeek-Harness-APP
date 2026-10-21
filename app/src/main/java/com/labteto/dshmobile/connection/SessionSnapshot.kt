package com.labteto.dshmobile.connection

import kotlinx.serialization.Serializable

/**
 * One session as it looked at the last successful sync.
 *
 * The shell keeps the newest list per host so a phone that wakes up without a network can still
 * show what was running instead of a blank error page. It is deliberately a *snapshot*: nothing
 * here is actionable, and opening a row waits for the connection to come back.
 */
@Serializable
data class SessionSnapshot(
    val sessionId: String,
    val title: String,
    val updatedAt: Long,
    val running: Boolean = false,
    /** `approval` / `plan-review` / `question`, or null. */
    val pending: String? = null,
)
