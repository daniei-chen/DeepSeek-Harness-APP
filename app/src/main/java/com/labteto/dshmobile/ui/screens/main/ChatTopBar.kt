package com.labteto.dshmobile.ui.screens.main

import android.annotation.SuppressLint

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.dto.SessionModelsValue
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonSize
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.components.DsSegment
import com.labteto.dshmobile.ui.components.DsSegmented
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.components.StateDot
import com.labteto.dshmobile.ui.components.StateDotState
import com.labteto.dshmobile.ui.components.skeleton
import com.labteto.dshmobile.ui.theme.DsAnimations
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType

/** The two views of a session the harness offers. */
internal enum class ChatTab { Chat, Trajectory }

/**
 * The session chrome: a two-row bar plus the Chat / Trajectory tabs and the Workspace button.
 *
 * Row one carries the controls that belong to the *connection* — the drawer, the model, the live
 * status. Row two carries the ones that belong to the *session* — its title, its agent preset, its
 * subagents. Splitting them is what makes room for the model selector on the left without eliding
 * the session title down to nothing on a phone.
 *
 * The title and the chips share row two rather than stacking, and the row disappears entirely when
 * it would be empty: four stacked rows of chrome over a white page ate a third of a phone screen
 * before the first message, and the chip row kept its padding even with no chips to pad.
 */
@SuppressLint("ModifierParameter")
@Composable
internal fun ChatTopBar(
    title: String,
    subagentCount: Int,
    jobCount: Int,
    detailsOpen: Boolean,
    tab: ChatTab,
    showTrajectory: Boolean = true,
    onOpenTurns: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    onOpenSubagents: () -> Unit,
    onOpenDetails: () -> Unit,
    onTabChange: (ChatTab) -> Unit,
    onOpenWorkspace: () -> Unit,
    workspaceEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    Column(modifier.fillMaxWidth().background(colors.bgBase)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DsIconButton(
                icon = FeatherIcons.Menu,
                contentDescription = stringResource(R.string.chatlist_open),
                onClick = onOpenDrawer,
                tint = colors.labelSecondary,
                iconSize = 18.dp,
            )
            Text(
                title,
                style = DsType.std14Strong,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (subagentCount > 0) {
                HeaderChip(
                    icon = FeatherIcons.Users,
                    label = stringResource(R.string.chat_subagents_count, subagentCount),
                    onClick = onOpenSubagents,
                )
            }
            if (jobCount > 0) {
                HeaderChip(
                    icon = FeatherIcons.Activity,
                    label = stringResource(R.string.chat_jobs_count, jobCount),
                    onClick = onOpenDetails,
                )
            }
            Spacer(Modifier.weight(1f))
            DsIconButton(
                icon = FeatherIcons.Folder,
                contentDescription = stringResource(R.string.panel_files),
                onClick = onOpenWorkspace,
                enabled = workspaceEnabled,
                tint = if (workspaceEnabled) colors.labelSecondary else colors.labelCaption,
                iconSize = 18.dp,
            )
            if (!detailsOpen) {
                DsIconButton(
                    icon = Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.chat_details_title),
                    onClick = onOpenDetails,
                    tint = colors.labelSecondary,
                    iconSize = 20.dp,
                )
            }
        }

        ChatTabRow(tab, showTrajectory, onTabChange)
    }
}

/** A session-fact chip in the header: subagent count, background jobs. */
@Composable
private fun HeaderChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .clip(DsShapes.row)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(15.dp))
        Text(
            label,
            style = DsType.small13,
            color = colors.labelSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The model chip: display names, not wire ids.
 *
 * `session.models` returns `deepseek-official / deepseek-v4-pro / max`, which is not what anyone
 * calls it — the catalog's own names resolve that to `DeepSeek-V4-Pro Max`.
 *
 * Drawn as a filled pill rather than the harness's transparent trigger. That is not a style
 * preference: on the web the affordance is the hover state, and a touch screen has no hover, so
 * bare text over the transcript gave no sign the model was switchable at all.
 */
@Composable
internal fun ModelChip(
    models: SessionModelsValue?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    if (models == null) {
        Box(
            modifier
                .padding(horizontal = DsSpacing.small)
                .width(120.dp)
                .height(14.dp)
                .skeleton(colors.bgLayer2, colors.hover),
        )
        return
    }
    val current = models.current
    val group = models.groups.firstOrNull { it.id == current.provider }
    val model = group?.models?.firstOrNull { it.id == current.model }
    val effort = model?.reasoning?.efforts?.firstOrNull { it.id == current.reasoningEffort }
    val modelLabel = model?.name ?: technicalDisplay(current.model)

    Row(
        modifier = modifier
            .widthIn(max = 240.dp)
            .heightIn(min = 28.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = DsSpacing.compact, vertical = DsSpacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.tiny),
    ) {
        if (!models.routable) {
            StateDot(StateDotState.Warning, size = 6.dp)
        }
        Text(
            modelLabel,
            style = DsType.std14Strong,
            color = colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        effort?.let {
            Text(it.name, style = DsType.small13, color = colors.labelTertiary, maxLines = 1)
        }
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.labelSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * The tab strip, as a compact segmented control rather than underlined tabs.
 *
 * Two short labels sitting over a full-width underline read as a page heading and cost a row of
 * their own; a 28dp track wraps to the labels and lets the chrome end there.
 */
@Composable
private fun ChatTabRow(tab: ChatTab, showTrajectory: Boolean = true, onTabChange: (ChatTab) -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = DsSpacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Web-style text tabs: no bordered segmented control; the active tab is text + underline.
        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatTabLabel(
                label = stringResource(R.string.chat_tab),
                selected = tab == ChatTab.Chat,
            ) { onTabChange(ChatTab.Chat) }
            ChatTabLabel(
                label = stringResource(R.string.trajectory_title),
                selected = tab == ChatTab.Trajectory,
            ) { onTabChange(ChatTab.Trajectory) }
        }
    }
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(colors.borderL3),
    )
}

@Composable
private fun ChatTabLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = DsType.tabText,
            color = if (selected) colors.accent else colors.labelTertiary,
        )
        // The desktop client marks the active tab with a short accent rule under the label.
        Box(
            Modifier
                .padding(top = 7.dp)
                .height(2.dp)
                .width(if (selected) 18.dp else 0.dp)
                .background(if (selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

private const val TAB_CHAT = "chat"
private const val TAB_TRAJECTORY = "trajectory"









