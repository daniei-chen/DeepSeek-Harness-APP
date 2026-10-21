package com.labteto.dshmobile.ui.screens.main.commands

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.session.ChatBlock
import com.labteto.dshmobile.core.session.ConversationSnapshot
import com.labteto.dshmobile.core.session.UserMessageNode
import com.labteto.dshmobile.data.CommandOutcome
import com.labteto.dshmobile.ui.theme.DshTheme
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The shared popup-select sheet driving the dsh-rewind port against a scripted host: the steps it
 * shows, what each tap sends, and that a failure is retried in place rather than closing the sheet.
 */
class PopupSelectSheetTest {
    @get:Rule val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)

    private class ScriptedHost(
        override val conversation: ConversationSnapshot?,
        private val answer: (String) -> CommandOutcome,
    ) : CommandContext {
        val sent = CopyOnWriteArrayList<String>()
        @Volatile var text = ""
        override val draft: String get() = text
        override suspend fun run(line: String): CommandOutcome = answer(line).also { sent += line }
        override fun setDraft(text: String) { this.text = text }
    }

    private val fullText = "Refactor the parser so it handles nested quotes,\nthen rerun every test in the module."
    private val preview = "Refactor the parser so it handles nested quotes, then rerun every test in the m…"
    private val conversation = ConversationSnapshot(
        sessionId = "s",
        nodes = listOf(UserMessageNode(20, "m20", listOf(ChatBlock("text", fullText)), "user")),
    )

    private fun host(
        candidates: () -> CommandOutcome = { CommandOutcome.Ok("candidates=1\n20\t1759900000000\t$preview") },
        rewind: () -> CommandOutcome = { CommandOutcome.Ok("Withdrawn seq 20 and everything after it.") },
    ) = ScriptedHost(conversation) { line ->
        when {
            line == "/rewind __candidates" -> candidates()
            line.startsWith("/rewind preview ") -> CommandOutcome.Ok("impact=2\nrestore:src/A.kt\ndelete:src/New.kt")
            else -> rewind()
        }
    }

    /** Show the sheet the way the chat screen does; the returned probe says whether it closed itself. */
    private fun open(host: ScriptedHost): () -> Boolean {
        var open by mutableStateOf(true)
        compose.setContent {
            DshTheme {
                if (open) PopupSelectSheet("rewind", RewindDecoration.ui as CommandUiSpec.PopupSelect, host) { open = false }
            }
        }
        return { !open }
    }

    private fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun conversationOnlyRewindRefillsTheWholeMessage() {
        val host = host()
        val closed = open(host)
        waitForText(preview)
        compose.onNodeWithText(preview).performClick()
        waitForText(text(R.string.rewind_mode_both_hint))
        compose.onNodeWithText(text(R.string.rewind_mode_chat)).performClick()
        compose.waitUntil(5_000) { closed() }
        assertEquals(listOf("/rewind __candidates", "/rewind preview @20 both", "/rewind @20 chat"), host.sent.toList())
        assertEquals(fullText, host.text)
    }

    @Test
    fun fileRestoreListsItsImpactAndWaitsForConfirmation() {
        val host = host()
        val closed = open(host)
        waitForText(preview)
        compose.onNodeWithText(preview).performClick()
        waitForText(text(R.string.rewind_mode_both_hint))
        compose.onNodeWithText(text(R.string.rewind_mode_both)).performClick()
        waitForText("src/New.kt", substring = true)
        compose.onNodeWithText("src/A.kt", substring = true).assertIsDisplayed()
        assertEquals("nothing runs before the confirmation", 2, host.sent.size)

        compose.onNodeWithText(text(R.string.common_back)).performClick()
        waitForText(text(R.string.rewind_mode_both))
        compose.onNodeWithText(text(R.string.rewind_mode_both)).performClick()
        waitForText(text(R.string.rewind_confirm))
        compose.onNodeWithText(text(R.string.rewind_confirm)).performClick()
        compose.waitUntil(5_000) { closed() }
        assertEquals(listOf("/rewind __candidates", "/rewind preview @20 both", "/rewind @20 both"), host.sent.toList())
    }

    @Test
    fun aFailedCandidateListRetriesInPlace() {
        var calls = 0
        val host = host(candidates = {
            if (calls++ == 0) CommandOutcome.Failed("host busy")
            else CommandOutcome.Ok("candidates=1\n20\t1759900000000\t$preview")
        })
        open(host)
        waitForText("host busy")
        compose.onNodeWithText(text(R.string.common_retry)).performClick()
        waitForText(preview)
        compose.onNodeWithText(preview).assertIsDisplayed()
    }

    @Test
    fun aFailedRewindStaysOpenAndCanBeTappedAgain() {
        var calls = 0
        val host = host(rewind = {
            if (calls++ == 0) CommandOutcome.Failed("A rewind is already running for this session; please wait.")
            else CommandOutcome.Ok("Withdrawn seq 20 and everything after it.")
        })
        val closed = open(host)
        waitForText(preview)
        compose.onNodeWithText(preview).performClick()
        waitForText(text(R.string.rewind_mode_both_hint))
        compose.onNodeWithText(text(R.string.rewind_mode_chat)).performClick()
        waitForText("A rewind is already running", substring = true)
        compose.onNodeWithText(text(R.string.rewind_mode_chat)).performClick()
        compose.waitUntil(5_000) { closed() }
        assertEquals(2, host.sent.count { it == "/rewind @20 chat" })
        assertEquals(fullText, host.text)
    }

    @Test
    fun anEmptyCandidateListSaysSo() {
        open(host(candidates = { CommandOutcome.Ok("candidates=0") }))
        waitForText(text(R.string.rewind_no_candidates))
        compose.onNodeWithText(text(R.string.rewind_no_candidates)).assertIsDisplayed()
    }
}
