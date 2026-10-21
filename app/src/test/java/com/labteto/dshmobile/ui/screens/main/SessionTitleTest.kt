package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.data.SessionRow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Session-list title fallback: explicit title, else working-directory folder, else session id. */
class SessionTitleTest {

    private fun session(id: String, title: String? = null, cwd: String? = null) = SessionRow(
        sessionId = id,
        title = title,
        running = false,
        blank = false,
        parentSessionId = null,
        origin = null,
        cwd = cwd,
        agentPreset = null,
        updatedAt = 0L,
        pendingInteraction = null,
    )

    @Test
    fun `explicit title wins`() {
        assertEquals("Fix the sidebar", sessionTitle(session("s1", title = "Fix the sidebar", cwd = "/repo/other")))
    }

    @Test
    fun `blank title falls back to the working-directory folder`() {
        assertEquals("workspace", sessionTitle(session("s2", title = "   ", cwd = "/home/me/workspace")))
        assertEquals("repo", sessionTitle(session("s3", title = null, cwd = "/srv/repo/")))
    }

    @Test
    fun `no title and no cwd falls back to the session id`() {
        assertEquals("session-42", sessionTitle(session("session-42")))
        assertEquals("session-43", sessionTitle(session("session-43", title = "", cwd = "")))
    }
}
