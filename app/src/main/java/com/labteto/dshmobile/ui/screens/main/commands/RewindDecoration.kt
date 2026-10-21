package com.labteto.dshmobile.ui.screens.main.commands

import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.session.ConversationSnapshot
import com.labteto.dshmobile.core.session.UserMessageNode
import com.labteto.dshmobile.core.wire.dto.CommandDescriptor
import com.labteto.dshmobile.data.CommandOutcome
import com.labteto.dshmobile.ui.screens.main.technicalDisplay
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException

/**
 * The phone's port of `dsh-rewind-plugin/lib/client.js`'s picker.
 *
 * The web flow is: run the hidden `/rewind __candidates` subcommand, parse its text, list the
 * messages, and on pick offer *conversation only* / *conversation and code* — the second one
 * previewed through `/rewind preview @seq both`, and confirmed against the files it would restore
 * or delete before anything is touched. The actual rewind is always sent as
 * `/rewind @<seq> <mode>`; a bare `/rewind` is never submitted from the UI.
 *
 * Text formats are the plugin's own and private to it; this mirrors its parsers byte for byte:
 * candidates are `candidates=N` then `seq\ttime\tpreview` lines; the preview impact list is
 * `restore:<path>` / `delete:<path>` lines.
 */
object RewindDecoration : CommandDecoration {
    override val names = setOf("rewind", "undo")

    /**
     * The plugin's catalog: `rewind` beside `snapshot-auto-cleanup`, which `dsh-rewind-plugin` has
     * registered with it since 0.5 at least. Plenty of other plugins name a command `rewind` or
     * `undo` without speaking this one's grammar, and their bare invocation is theirs to keep.
     */
    override fun recognizes(catalog: List<CommandDescriptor>): Boolean =
        catalog.any { it.name == "rewind" } && catalog.any { it.name == CLEANUP_COMMAND }

    /**
     * Always, on this client. Upstream offers the picker only while the browser's chat holds a
     * reachable user message, and otherwise lets the bare command through to fail with "no user
     * messages". The phone holds one page of history, which a single long turn can fill, so that
     * test would hand a session that does have messages to the bare command — and the plugin's bare
     * path rewinds to the latest one without asking. The host's candidate list decides instead, and
     * an empty one says so in the sheet.
     */
    override fun available(conversation: ConversationSnapshot?): Boolean = true

    override val ui: CommandUiSpec = CommandUiSpec.PopupSelect(
        options = { ctx -> candidates(ctx).map { it.option() } },
        onSelect = { option, ctx ->
            val seq = option.id.toLongOrNull() ?: return@PopupSelect null
            modeStep(ctx, seq, option.label)
        },
        empty = res(R.string.rewind_no_candidates),
    )

    data class Candidate(val seq: Long, val time: Long, val preview: String) {
        fun option() = SelectOption(
            id = seq.toString(),
            label = if (preview.isBlank()) res(R.string.rewind_no_text) else preview.ui(),
            badge = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(time)),
        )
    }

    private suspend fun candidates(ctx: CommandContext): List<Candidate> {
        val outcome = ctx.run("/rewind __candidates")
        outcome.failure()?.let { throw CommandUiException(it) }
        return parseCandidates((outcome as CommandOutcome.Ok).text.orEmpty())
    }

    /** Port of `rewindCandidatesFromHostText`. */
    fun parseCandidates(text: String): List<Candidate> {
        if (!text.startsWith(CANDIDATE_LIST_HEADER)) return emptyList()
        return text.lineSequence().drop(1).mapNotNull { line ->
            if (line.isEmpty()) return@mapNotNull null
            val parts = line.split('\t')
            if (parts.size != 3) return@mapNotNull null
            val seq = parts[0].toLongOrNull() ?: return@mapNotNull null
            val time = parts[1].toDoubleOrNull()?.toLong() ?: return@mapNotNull null
            Candidate(seq, time, parts[2])
        }.toList()
    }

    /** Port of `parseImpactList`. */
    fun parseImpact(text: String): Pair<List<String>, List<String>> {
        val restores = mutableListOf<String>()
        val deletes = mutableListOf<String>()
        for (line in text.lines()) {
            when {
                line.startsWith("restore:") -> restores.add(line.removePrefix("restore:"))
                line.startsWith("delete:") -> deletes.add(line.removePrefix("delete:"))
            }
        }
        return restores to deletes
    }

    /**
     * Step two: chat-only, or chat and files. The impact of the file restore is fetched while the
     * step is open, exactly as the web popover does, so the "both" row is offered only when there
     * is something to restore — or withheld, with the reason, when there is not.
     */
    private fun modeStep(ctx: CommandContext, seq: Long, label: UiText): SelectStep {
        val title = res(R.string.rewind_to, label)
        val chat = SelectOption(MODE_CHAT, res(R.string.rewind_mode_chat), res(R.string.rewind_mode_chat_hint))
        val checking = SelectOption(MODE_BOTH, res(R.string.rewind_mode_both), res(R.string.rewind_checking), enabled = false)
        val chatOnly: suspend (SelectOption) -> SelectStep? = { option ->
            if (option.id == MODE_CHAT) rewind(ctx, seq, MODE_CHAT) else null
        }
        return SelectStep(
            title = title,
            options = listOf(chat, checking),
            onSelect = chatOnly,
            load = {
                val impact = try {
                    ctx.run("/rewind preview @$seq both")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    CommandOutcome.Failed(e.message ?: e.toString())
                }
                val failure = impact.failure()
                val (restores, deletes) = parseImpact((impact as? CommandOutcome.Ok)?.text.orEmpty())
                when {
                    failure != null ->
                        SelectStep(title, listOf(chat), res(R.string.rewind_impact_failed, failure), onSelect = chatOnly)
                    restores.isEmpty() && deletes.isEmpty() ->
                        SelectStep(title, listOf(chat), res(R.string.rewind_no_changes), onSelect = chatOnly)
                    else -> {
                        val impactList = UiText.Lines(
                            restores.map { res(R.string.rewind_impact_restore, technicalDisplay(it)) } +
                                deletes.map { res(R.string.rewind_impact_delete, technicalDisplay(it)) },
                        )
                        val both = SelectOption(MODE_BOTH, res(R.string.rewind_mode_both), res(R.string.rewind_mode_both_hint))
                        fun modes(): SelectStep = SelectStep(title, listOf(chat, both), onSelect = { option ->
                            if (option.id == MODE_BOTH) confirmStep(ctx, seq, title, impactList, back = ::modes) else chatOnly(option)
                        })
                        modes()
                    }
                }
            },
        )
    }

    /**
     * Step three, for files only: what the restore would touch, before it touches anything. The web
     * popover asks the same — a file restore deletes and overwrites workspace files, and on a phone
     * the row for it sits right under the conversation-only one.
     */
    private fun confirmStep(ctx: CommandContext, seq: Long, title: UiText, impact: UiText, back: () -> SelectStep): SelectStep {
        val confirm = SelectOption(CONFIRM, res(R.string.rewind_confirm), res(R.string.rewind_mode_both_hint))
        val goBack = SelectOption(BACK, res(R.string.common_back))
        return SelectStep(title, listOf(confirm, goBack), impact, onSelect = { option ->
            if (option.id == CONFIRM) rewind(ctx, seq, MODE_BOTH) else back()
        })
    }

    /**
     * Run the rewind, then do what upstream does after one: put the withdrawn message back in the
     * composer so it can be edited and resent. That is the message's whole text — the candidate list
     * only previews it, cut to 80 characters with its line breaks folded — and only into an empty
     * composer. A message outside the loaded history is left alone, as upstream leaves one outside
     * its chat.
     */
    private suspend fun rewind(ctx: CommandContext, seq: Long, mode: String): SelectStep? {
        ctx.run("/rewind @$seq $mode").failure()?.let { throw CommandUiException(it) }
        val message = ctx.conversation?.nodes?.firstOrNull { it.seq == seq } as? UserMessageNode
        val text = message?.blocks?.filter { it.kind == "text" }?.joinToString("") { it.text.orEmpty() }
        if (!text.isNullOrEmpty() && ctx.draft.isBlank()) ctx.setDraft(text)
        return null
    }

    /** What went wrong with a command line, or null when the host ran it. */
    private fun CommandOutcome.failure(): UiText? = when (this) {
        is CommandOutcome.Ok -> null
        is CommandOutcome.Failed -> message.ui()
        is CommandOutcome.Unknown -> res(R.string.err_command_unknown, line)
    }

    private const val CANDIDATE_LIST_HEADER = "candidates="
    private const val CLEANUP_COMMAND = "snapshot-auto-cleanup"
    private const val MODE_CHAT = "chat"
    private const val MODE_BOTH = "both"
    private const val CONFIRM = "confirm"
    private const val BACK = "back"
}
