package com.labteto.dshmobile.ui.screens.main

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.WireJson
import com.labteto.dshmobile.data.QuestionCardState
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.*
import com.labteto.dshmobile.ui.theme.*
import kotlinx.serialization.builtins.ListSerializer

@Composable
internal fun ModernQuestions(store: SessionStore, sessionId: String?, modifier: Modifier = Modifier) {
    val available by store.timedQuestionsAvailable.collectAsStateWithLifecycle()
    if (available == false) return
    val offline = stringResource(R.string.common_offline)
    val cards by store.questionSessions.cards.collectAsStateWithLifecycle()
    val prefs = LocalContext.current.getSharedPreferences("question-drafts", Context.MODE_PRIVATE)
    val sessionCards = cards.filter { it.sessionId == sessionId }
    var selected by remember(store.activeHostKey, sessionId) { mutableStateOf<String?>(null) }
    val newest = sessionCards.lastOrNull { it.state != "settled" && !it.hidden }?.key
    LaunchedEffect(newest) { if (newest != null) selected = newest }
    val shown = sessionCards.firstOrNull { it.key == selected && !it.hidden }
    if (sessionCards.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(horizontal = DsSpacing.medium), verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
        val otherCards = sessionCards.filter { it.key != shown?.key }
        if (otherCards.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
            otherCards.forEach { card ->
                val label = stringResource(if (card.state == "settled" || card.queued) R.string.ux_b_view_answers else R.string.ux_b_view_questions)
                DsButton(
                    "$label: ${card.questions.firstOrNull()?.question.orEmpty().take(60)}",
                    { store.questionSessions.hide(card.key, false); selected = card.key },
                    variant = DsButtonVariant.Ghost,
                )
            }
        }
        shown?.let { card ->
            key(store.activeHostKey, card.key) {
                val draftKey = "${store.activeHostKey}/${card.key}"
                val saved = remember(draftKey) {
                    runCatching { WireJson.decodeFromString(ListSerializer(QuestionDraft.serializer()), prefs.getString(draftKey, null)!!) }.getOrNull()
                }
                DisposableEffect(card.key) {
                    onDispose { store.questionSessions.focus(card.key, false) }
                }
                if (card.state == "settled" || card.queued) {
                    QuestionReplyCard(card, onHide = { store.questionSessions.hide(card.key, true) })
                } else {
                    QuestionsPanel(
                        requestKey = draftKey, questions = card.questions,
                        dismissIsLocal = true, enabled = card.ready, revision = "${card.state}/${card.ready}", initialDrafts = saved,
                        onDraftsChange = {
                            store.questionSessions.hold(card.key)
                            prefs.edit().putString(draftKey, WireJson.encodeToString(ListSerializer(QuestionDraft.serializer()), it)).apply()
                        },
                        onSubmit = { store.questionSessions.answer(card.key, it)?.let { message -> if (message == "Disconnected") offline else message } },
                        onDismiss = { store.questionSessions.hide(card.key, true); null },
                        onFocusChange = { store.questionSessions.focus(card.key, it) },
                        statusContent = { QuestionWaitHeader(card, onHold = { store.questionSessions.hold(card.key) }) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionWaitHeader(card: QuestionCardState, onHold: () -> Unit) {
    val status = questionWaitStatus(card)
    val seconds = countdownSeconds(card.remainingMs ?: 0)
    val locale = LocalConfiguration.current.locales[0]
    val label = when (status) {
        QuestionWaitStatus.Countdown -> pluralStringResource(R.plurals.ux_b_countdown_seconds, seconds, countdownNumber(seconds, locale))
        QuestionWaitStatus.Paused -> stringResource(R.string.ux_b_paused)
        QuestionWaitStatus.Held -> stringResource(R.string.ux_b_held)
        QuestionWaitStatus.Continued -> stringResource(R.string.ux_b_continued)
        QuestionWaitStatus.Queued -> stringResource(R.string.harness_reply_queued)
        QuestionWaitStatus.Settled -> stringResource(R.string.harness_answered)
        QuestionWaitStatus.Waiting -> stringResource(if (card.ready) R.string.ux_b_held else R.string.common_loading)
    }
    val announcement = if (status == QuestionWaitStatus.Countdown) stringResource(R.string.ux_b_countdown_description) else label
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
        // Countdown ticks stay visual. Only a meaningful status change alters this live region.
        Text(label, style = DsType.small13, color = DsTheme.colors.labelSecondary,
            modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = announcement; liveRegion = LiveRegionMode.Polite })
        if (card.remainingMs != null && !card.held && card.state != "continued") DsButton(
            stringResource(R.string.harness_take_time), onHold,
            variant = DsButtonVariant.Ghost, size = DsButtonSize.Small,
        )
    }
}

@Composable
private fun QuestionReplyCard(card: QuestionCardState, onHide: () -> Unit) {
    var expanded by remember(card.key) { mutableStateOf(false) }
    val state = stringResource(if (card.queued && card.state != "settled") R.string.harness_reply_queued else R.string.harness_answered)
    DsCard {
        Row(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                DsPill(state)
                Text(card.questions.firstOrNull()?.question.orEmpty(), style = DsType.small13,
                    color = DsTheme.colors.labelPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DsIconButton(Icons.Filled.Close, stringResource(R.string.questions_dismiss), onHide)
        }
        val firstAnswer = card.answers.orEmpty().firstOrNull { it.id == card.questions.firstOrNull()?.id }
        val summary = firstAnswer?.let { it.selected + listOfNotNull(it.custom?.takeIf(String::isNotBlank)) }.orEmpty()
        Text(if (summary.isEmpty()) stringResource(R.string.ux_b_skipped) else summary.joinToString(" · "),
            style = DsType.small13, color = DsTheme.colors.labelSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        DsButton(stringResource(R.string.ux_b_view_answers), { expanded = true }, variant = DsButtonVariant.Ghost)
    }
    if (expanded) DsBottomSheet(
        title = stringResource(R.string.ux_b_view_answers), subtitle = state, onDismiss = { expanded = false },
        trailing = { DsIconButton(Icons.Filled.Close, stringResource(R.string.common_close), { expanded = false }) },
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.medium)) {
            card.questions.forEach { question ->
                val answer = card.answers.orEmpty().firstOrNull { it.id == question.id }
                val values = answer?.let { it.selected + listOfNotNull(it.custom?.takeIf(String::isNotBlank)) }.orEmpty()
                DsCard {
                    question.header?.let { Text(it, style = DsType.caption11, color = DsTheme.colors.labelTertiary) }
                    Text(question.question, style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
                    question.detail?.takeIf(String::isNotBlank)?.let { MarkdownText(it) }
                    Text(stringResource(R.string.ux_b_answer), style = DsType.caption11, color = DsTheme.colors.labelTertiary)
                    Text(if (values.isEmpty()) stringResource(R.string.ux_b_skipped) else values.joinToString(" · "),
                        style = DsType.std14, color = DsTheme.colors.labelSecondary)
                }
            }
        }
    }
}
