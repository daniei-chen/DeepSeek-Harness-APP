package com.labteto.dshmobile.ui.screens.main.commands

import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.session.ChatBlock
import com.labteto.dshmobile.core.session.ConversationSnapshot
import com.labteto.dshmobile.core.session.UserMessageNode
import com.labteto.dshmobile.core.wire.dto.CommandDescriptor
import com.labteto.dshmobile.data.CommandOutcome
import com.labteto.dshmobile.ui.screens.main.technicalDisplay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The dsh-rewind picker against a scripted host: what it sends, in what order, and what it leaves
 * in the composer. The host's text formats are the plugin's own (`dsh-rewind-plugin` 0.15).
 */
class RewindDecorationTest {

    /** A host answering each command line from [answer], recording what it was sent. */
    private class ScriptedHost(
        var text: String = "",
        override val conversation: ConversationSnapshot?,
        private val answer: (String) -> CommandOutcome,
    ) : CommandContext {
        val sent = mutableListOf<String>()
        override val draft: String get() = text
        override suspend fun run(line: String): CommandOutcome = answer(line).also { sent += line }
        override fun setDraft(text: String) { this.text = text }
    }

    /** Longer than the candidate preview's 80 characters, and on two lines. */
    private val fullText = "Refactor the parser so it handles nested quotes,\nthen rerun every test in the module and report what failed."
    private val preview = "Refactor the parser so it handles nested quotes, then rerun every test in the m…"
    private val conversation = ConversationSnapshot(
        sessionId = "s",
        nodes = listOf(UserMessageNode(20, "m20", listOf(ChatBlock("text", fullText)), "user")),
    )

    private fun host(
        draft: String = "",
        impact: String = "Rewinding to seq 20\nimpact=2\nrestore:src/A.kt\ndelete:src/New.kt",
        rewind: CommandOutcome = CommandOutcome.Ok("Withdrawn seq 20 and everything after it."),
    ) = ScriptedHost(draft, conversation) { line ->
        when {
            line == "/rewind __candidates" -> CommandOutcome.Ok("candidates=1\n20\t1759900000000\t$preview")
            line.startsWith("/rewind preview ") -> CommandOutcome.Ok(impact)
            else -> rewind
        }
    }

    private val spec = RewindDecoration.ui as CommandUiSpec.PopupSelect

    /** Open the picker, pick the one message, and let its mode step load. */
    private suspend fun modes(ctx: CommandContext): SelectStep {
        val message = spec.options(ctx).single()
        val loading = spec.onSelect(message, ctx) ?: error("picking a message opens the mode step")
        assertFalse("both waits for the impact preview", loading.options.single { it.id == "both" }.enabled)
        return loading.load?.invoke() ?: error("the mode step loads the impact preview")
    }

    private fun SelectStep.option(id: String) = options.single { it.id == id }

    @Test
    fun `the plugin is recognised by its catalog, not by a command name alone`() {
        fun catalog(vararg names: String) = names.map { CommandDescriptor(name = it) }
        assertTrue(RewindDecoration.recognizes(catalog("rewind", "undo", "snapshot-auto-cleanup", "compact")))
        assertFalse(RewindDecoration.recognizes(catalog("rewind", "compact")))
        assertFalse(RewindDecoration.recognizes(catalog("undo", "snapshot-auto-cleanup")))
    }

    /** The bare path rewinds to the latest message without asking, so the picker always opens instead. */
    @Test
    fun `the picker opens whatever the loaded history holds`() {
        assertTrue(RewindDecoration.available(null))
        assertTrue(RewindDecoration.available(ConversationSnapshot(sessionId = "s")))
    }

    @Test
    fun `a conversation-only rewind refills the composer with the whole message`() = runTest {
        val ctx = host()
        val step = modes(ctx)
        assertNull(step.onSelect(step.option("chat")))
        assertEquals(listOf("/rewind __candidates", "/rewind preview @20 both", "/rewind @20 chat"), ctx.sent)
        assertEquals(fullText, ctx.text)
    }

    @Test
    fun `a refill never replaces what the composer already holds`() = runTest {
        val ctx = host(draft = "something else")
        val step = modes(ctx)
        step.onSelect(step.option("chat"))
        assertEquals("something else", ctx.text)
    }

    @Test
    fun `restoring files asks first and lists what it would touch`() = runTest {
        val ctx = host()
        val confirm = modes(ctx).let { it.onSelect(it.option("both")) } ?: error("both asks for confirmation")
        assertEquals(listOf("/rewind __candidates", "/rewind preview @20 both"), ctx.sent)
        assertEquals(
            UiText.Lines(listOf(
                res(R.string.rewind_impact_restore, technicalDisplay("src/A.kt")),
                res(R.string.rewind_impact_delete, technicalDisplay("src/New.kt")),
            )),
            confirm.note,
        )
        assertNull(confirm.onSelect(confirm.option("confirm")))
        assertEquals("/rewind @20 both", ctx.sent.last())
    }

    @Test
    fun `going back from the confirmation offers both modes again`() = runTest {
        val ctx = host()
        val confirm = modes(ctx).let { it.onSelect(it.option("both")) }!!
        val back = confirm.onSelect(confirm.option("back"))!!
        assertEquals(listOf("chat", "both"), back.options.map { it.id })
        assertEquals(2, ctx.sent.size)
    }

    @Test
    fun `no tracked changes withholds the file restore`() = runTest {
        val step = modes(host(impact = "Rewinding to seq 20\nNo changes\nimpact=0"))
        assertEquals(listOf("chat"), step.options.map { it.id })
        assertEquals(res(R.string.rewind_no_changes), step.note)
    }

    @Test
    fun `a failed rewind is reported and leaves the composer alone`() = runTest {
        val ctx = host(rewind = CommandOutcome.Failed("A rewind is already running for this session; please wait."))
        val step = modes(ctx)
        try {
            step.onSelect(step.option("chat"))
            fail("a failed rewind must not close the sheet")
        } catch (e: CommandUiException) {
            assertEquals("A rewind is already running for this session; please wait.".ui(), e.text)
        }
        assertEquals("", ctx.text)
    }

    /** The command vanished between listing and picking, e.g. the plugin was disabled meanwhile. */
    @Test
    fun `an unmatched rewind line is a failure, not a success`() = runTest {
        val ctx = host(rewind = CommandOutcome.Unknown("/rewind @20 chat"))
        val step = modes(ctx)
        try {
            step.onSelect(step.option("chat"))
            fail("an unmatched line must not read as done")
        } catch (e: CommandUiException) {
            assertEquals(res(R.string.err_command_unknown, "/rewind @20 chat"), e.text)
        }
        assertEquals("", ctx.text)
    }

    @Test
    fun `candidate parsing matches the plugin's`() {
        assertEquals(
            listOf(RewindDecoration.Candidate(20, 1759900000000, "fix it"), RewindDecoration.Candidate(31, 1759900060000, "")),
            RewindDecoration.parseCandidates("candidates=3\n20\t1759900000000\tfix it\nnot\ta\tseq\n31\t1759900060000\t"),
        )
        assertEquals(emptyList<RewindDecoration.Candidate>(), RewindDecoration.parseCandidates("Usage: /rewind"))
    }
}
