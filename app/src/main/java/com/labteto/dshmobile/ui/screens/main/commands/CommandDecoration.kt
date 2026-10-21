package com.labteto.dshmobile.ui.screens.main.commands

import com.labteto.dshmobile.core.session.ConversationSnapshot
import com.labteto.dshmobile.core.wire.dto.CommandDescriptor
import com.labteto.dshmobile.data.CommandOutcome

/**
 * A native stand-in for the harness web client's `commandUi.decorate()`.
 *
 * In the browser a plugin ships JavaScript that registers a `CommandDecoration` for one of its host
 * commands: `available(session)` says whether the bare invocation gets a UI, and `ui` is either a
 * `popupSelect` (fetch options, show a list, run something on pick) or an `action`. That JavaScript
 * cannot run here, so each plugin that wants its picker on the phone gets a small Kotlin port of
 * the same shape. The shell — the option sheet, loading, failure/retry, search — is shared; the
 * decoration only says *what* to list and *what* a pick means.
 *
 * Keyed by the host command's name, exactly as upstream: a decoration for `rewind` fires when the
 * composer submits `/rewind` bare, whether typed or picked from the `/` menu.
 */
interface CommandDecoration {
    /** The host command names this decorates (without the slash). `rewind` also claims `undo`. */
    val names: Set<String>

    /**
     * Whether [catalog], the session's `commands/list`, comes from the plugin this decoration
     * ports. Upstream a decoration is registered by its plugin's own client bundle, so it never
     * exists without that plugin. A port built into the app has to recognise the plugin from the
     * catalog instead, or it would take over another plugin's command of the same name.
     */
    fun recognizes(catalog: List<CommandDescriptor>): Boolean

    /** Whether the bare invocation gets this UI right now; false falls through to the host. */
    fun available(conversation: ConversationSnapshot?): Boolean

    /** The bare-invocation UI. */
    val ui: CommandUiSpec
}

/** Everything a decoration needs from the screen to do its work. */
interface CommandContext {
    val conversation: ConversationSnapshot?

    /** The composer's current text. */
    val draft: String

    /** Run one complete command line against the open session and wait for its result. */
    suspend fun run(line: String): CommandOutcome

    /** Put text into the composer, replacing the draft. */
    fun setDraft(text: String)
}

sealed interface CommandUiSpec {
    /**
     * Load rows, show them, act on the pick. [onSelect] may return a follow-up step so a flow can
     * chain (rewind: pick a message, then pick a mode) without each decoration owning a sheet.
     */
    data class PopupSelect(
        val options: suspend (CommandContext) -> List<SelectOption>,
        val onSelect: suspend (SelectOption, CommandContext) -> SelectStep?,
        /** Shown in place of the rows when [options] lists none. */
        val empty: UiText? = null,
    ) : CommandUiSpec

    /** A bare invocation runs one callback and submits nothing. */
    data class Action(val run: suspend (CommandContext) -> Unit) : CommandUiSpec
}

/**
 * Text a decoration hands to the shell: either literal (host-provided, e.g. a message preview) or
 * a string resource the shell resolves, so a decoration stays a plain object with no Context.
 * A [Res] argument may itself be a [UiText].
 */
sealed interface UiText {
    data class Literal(val text: String) : UiText
    data class Res(@androidx.annotation.StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Several texts, one per line. */
    data class Lines(val lines: List<UiText>) : UiText
}

/** A failure a decoration words itself; the shell shows [text] where it would show the message. */
class CommandUiException(val text: UiText) : Exception()

fun String.ui(): UiText = UiText.Literal(this)
fun res(@androidx.annotation.StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

/** One option row of a popup-select shell. */
data class SelectOption(
    val id: String,
    val label: UiText,
    val detail: UiText? = null,
    /** Short marker beside the label, e.g. a time. */
    val badge: String? = null,
    /** Shown but not pickable — the web disables a row while its data loads. */
    val enabled: Boolean = true,
)

/**
 * What happens after a pick: either done, or another step with its own rows.
 *
 * [note] is explanatory text above the rows (rewind shows the impact list there); [busy] rows
 * are shown while [load] is still in flight.
 */
data class SelectStep(
    val title: UiText,
    val options: List<SelectOption> = emptyList(),
    val note: UiText? = null,
    val load: (suspend () -> SelectStep)? = null,
    val onSelect: suspend (SelectOption) -> SelectStep?,
)

/** Every decoration this build knows. Adding a plugin port means adding one line here. */
object CommandDecorations {
    val all: List<CommandDecoration> = listOf(RewindDecoration)

    fun forName(name: String): CommandDecoration? = all.firstOrNull { name in it.names }
}
