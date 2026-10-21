package com.labteto.dshmobile.ui.screens.main

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The desktop client's right sidebar, as a native panel: Start / Files / Details. */
internal enum class RightPanelTab { Start, Files, Details }

/**
 * The right panel in a modal shell for phones. Same structure as [RightPanelBody] so the docked and
 * modal forms cannot drift apart; only the shell and the entrance animation differ.
 */
@Composable
internal fun RightPanelModal(
    store: SessionStore,
    state: PanelState,
    tab: RightPanelTab,
    onTabChange: (RightPanelTab) -> Unit,
    onDismiss: () -> Unit,
    onReference: ((String) -> Unit)? = null,
    onOpenDetails: () -> Unit,
) {
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(200)
            onDismiss()
        }
    }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.bgBase) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            ) {
                RightPanelBody(
                    store = store,
                    state = state,
                    tab = tab,
                    onTabChange = onTabChange,
                    onDismiss = { close() },
                    onReference = onReference,
                    onOpenDetails = onOpenDetails,
                )
            }
        }
    }
}

/**
 * The docked form of the right panel. On a wide window it sits beside the conversation the way the
 * desktop client's sidebar does; on a phone [RightPanelModal] wraps this same body.
 */
@Composable
internal fun RightPanelBody(
    store: SessionStore,
    state: PanelState,
    tab: RightPanelTab,
    onTabChange: (RightPanelTab) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onReference: ((String) -> Unit)? = null,
    onOpenDetails: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(modifier.fillMaxSize().background(colors.bgBase)) {
        if (tab == RightPanelTab.Start) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DsIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onDismiss,
                    tint = colors.labelSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.right_panel_start),
                    style = DsType.std14Strong,
                    color = colors.labelPrimary,
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                RightPanelTab.Start -> StartPanel(
                    store = store,
                    onOpenFiles = {
                        state.browse(".")
                        onTabChange(RightPanelTab.Files)
                    },
                    onNewTerminal = {
                        state.section = 2
                        onTabChange(RightPanelTab.Files)
                    },
                    onOpenDiagnostics = onOpenDetails,
                )
                RightPanelTab.Files -> WorkspacePanelScaffold(
                    store = store,
                    state = state,
                    onDismiss = onDismiss,
                    onReference = onReference,
                )
                RightPanelTab.Details -> Unit
            }
        }
        RightPanelNav(tab = tab, onTabChange = { next ->
            if (next == RightPanelTab.Details) onOpenDetails() else onTabChange(next)
        })
    }
}

/** The panel's bottom navigation. Native pattern, matching the web sidebar's page switch. */
@Composable
private fun RightPanelNav(tab: RightPanelTab, onTabChange: (RightPanelTab) -> Unit) {
    val colors = DsTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.bgBase)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PanelNavItem(
            icon = FeatherIcons.Compass,
            label = stringResource(R.string.right_panel_start),
            selected = tab == RightPanelTab.Start,
            onClick = { onTabChange(RightPanelTab.Start) },
            modifier = Modifier.weight(1f),
        )
        PanelNavItem(
            icon = FeatherIcons.Folder,
            label = stringResource(R.string.panel_files),
            selected = tab == RightPanelTab.Files,
            onClick = { onTabChange(RightPanelTab.Files) },
            modifier = Modifier.weight(1f),
        )
        PanelNavItem(
            icon = FeatherIcons.Info,
            label = stringResource(R.string.chat_details_title),
            selected = tab == RightPanelTab.Details,
            onClick = { onTabChange(RightPanelTab.Details) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PanelNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val tint = if (selected) colors.accent else colors.labelTertiary
    Column(
        modifier = modifier
            .clip(DsShapes.row)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = DsType.caption11, color = tint, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** The "Start" page: the same five destinations the desktop sidebar offers. */
@Composable
private fun StartPanel(
    store: SessionStore,
    onOpenFiles: () -> Unit,
    onNewTerminal: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val colors = DsTheme.colors
    val context = LocalContext.current
    val connection by store.connectionState.collectAsState()
    val baseUrl = connection.host?.baseUrl
    val browserUrl = baseUrl ?: "https://"
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = DsSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.small),
    ) {
        item { Spacer(Modifier.size(DsSpacing.medium)) }
        item {
            StartCard(
                icon = FeatherIcons.Folder,
                title = stringResource(R.string.start_workspace_files),
                onClick = onOpenFiles,
            )
        }
        item {
            StartCard(
                icon = FeatherIcons.Terminal,
                title = stringResource(R.string.start_new_terminal),
                onClick = onNewTerminal,
            )
        }
        item {
            StartCard(
                icon = FeatherIcons.Globe,
                title = stringResource(R.string.start_browser),
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(browserUrl)))
                    }
                },
            )
        }
        item {
            StartCard(
                icon = FeatherIcons.Zap,
                title = stringResource(R.string.start_browser_skill),
                subtitle = stringResource(R.string.start_web_only),
                onClick = null,
            )
        }
        item {
            StartCard(
                icon = FeatherIcons.Layers,
                title = stringResource(R.string.start_session_diagnostics),
                onClick = onOpenDiagnostics,
            )
        }
        item { Spacer(Modifier.size(DsSpacing.large)) }
    }
}

@Composable
private fun StartCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)?,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.borderL1, RoundedCornerShape(16.dp))
            .background(colors.bgLayer1)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DsType.std14, color = colors.labelPrimary)
            subtitle?.let { Text(it, style = DsType.caption11, color = colors.labelTertiary) }
        }
    }
}



