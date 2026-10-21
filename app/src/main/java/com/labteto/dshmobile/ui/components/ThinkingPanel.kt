package com.labteto.dshmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType

/** Reasoning stays quiet and compact, independently of how many lines it contains. */
@Composable
fun ThinkingPanel(text: String, streaming: Boolean, expanded: Boolean, onToggle: () -> Unit,
                  content: (@Composable () -> Unit)? = null) {
    if (text.isBlank() && !streaming) return
    val colors = DsTheme.colors
    val expandedState = stringResource(R.string.common_expanded)
    val collapsedState = stringResource(R.string.common_collapsed)
    val expandAction = stringResource(R.string.common_expand)
    val collapseAction = stringResource(R.string.common_collapse)
    // The original client shows one line of the reasoning under the label, so the collapsed row
    // says what was thought about instead of repeating the word "thinking" for every turn.
    val preview = remember(text) {
        text.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    }
    // Web-style transcript: reasoning is a quiet inline row, not a bordered card.
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 32.dp)
            .semantics {
                stateDescription = if (expanded) expandedState else collapsedState
            }
            .clickable(
                role = Role.Button,
                onClickLabel = if (expanded) collapseAction else expandAction,
                onClick = onToggle,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(FeatherIcons.Layers, null, Modifier.size(14.dp), tint = colors.labelCaption)
            Text(
                stringResource(if (streaming) R.string.chat_thinking else R.string.chat_thought_process),
                style = DsType.small13,
                color = colors.labelSecondary,
            )
            if (preview.isNotEmpty()) {
                Text("·", style = DsType.small13, color = colors.labelCaption)
                Text(
                    preview,
                    style = DsType.small13,
                    color = colors.labelTertiary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (streaming) StateDot(StateDotState.Running, size = 6.dp)
            Icon(FeatherIcons.ChevronRight, null,
                Modifier.size(14.dp).graphicsLayer { rotationZ = if (expanded) 90f else 0f }, tint = colors.labelCaption)
        }
        if (expanded) {
            Box(Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)) {
                // Selectable text rather than full-size reply markdown: this is reasoning, not the answer.
                if (content != null) content() else androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(text.trimEnd(), style = DsType.small13, color = colors.labelTertiary, softWrap = true)
                }
            }
        }
    }
}
