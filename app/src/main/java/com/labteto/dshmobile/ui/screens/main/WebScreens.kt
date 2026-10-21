package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.BuildConfig
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsDialog
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.components.WhaleMark
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType as Type

/**
 * Web-replica top-level pages. The chat page is the app; this is the full-screen Sessions list
 * (the web sidebar expanded) plus a two-item bottom bar. Projects/tasks are gone: workspaces are
 * rows in this list, and plugins/models/agents live in Settings.
 */
@Composable
fun SessionsScreen(
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    listState: LazyListState,
) {
    val colors = DsTheme.colors
    Column(Modifier.fillMaxSize().background(colors.bgBase)) {
        Box(Modifier.weight(1f)) {
            ChatListDrawer(
                onClose = onOpenChat,
                onOpenSettings = onOpenSettings,
                fullScreen = true,
                listState = listState,
            )
        }
        WebBottomBar(selected = 0, onChat = {}, onSettings = onOpenSettings)
    }
}

@Composable
fun SettingsHomeScreen(
    onOpenChat: () -> Unit,
) {
    val store = rememberSessionStore()
    val models by store.models.collectAsStateWithLifecycle()
    val presets by store.agentPresets.collectAsStateWithLifecycle()
    val sessions by store.sessions.collectAsStateWithLifecycle()
    val connection by store.connectionState.collectAsStateWithLifecycle()
    val usage by store.tokenUsage.collectAsStateWithLifecycle()
    val stats by store.sessionStats.collectAsStateWithLifecycle()
    val colors = DsTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    // The settings destinations the desktop client keeps in its sidebar, as native bottom tabs.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var subPage by remember { mutableIntStateOf(0) }
    var aboutOpen by remember { mutableStateOf(false) }
    val currentSession = sessions.firstOrNull { it.sessionId == store.currentSessionId.value }
    androidx.activity.compose.BackHandler(enabled = subPage != 0) { subPage = 0 }
    val currentModelName = models?.let { value ->
        value.groups.firstOrNull { it.id == value.current.provider }
            ?.models?.firstOrNull { it.id == value.current.model }?.name
            ?: value.current.model
    }
    val connectionLabel = connection.host?.baseUrl ?: stringResource(R.string.common_offline)
    val connectionStateLabel = stringResource(
        when (connection.phase) {
            com.labteto.dshmobile.connection.ConnectionPhase.CONNECTED -> R.string.common_connected
            com.labteto.dshmobile.connection.ConnectionPhase.RECONNECTING -> R.string.common_reconnecting
            else -> R.string.common_offline
        },
    )

    Column(Modifier.fillMaxSize().background(colors.bgBase)) {
        Box(Modifier.weight(1f)) {
            when {
                subPage == 3 -> GeneralSettingsPage(
                    onBack = { subPage = 0 },
                    onOpenLanguage = { subPage = 8 },
                )
                subPage == 5 -> NotificationsSettingsPage(onBack = { subPage = 0 })
                subPage == 6 -> ConnectionSettingsPage(onBack = { subPage = 0 })
                subPage == 7 -> ArchivedSessionsPage(onBack = { subPage = 0 })
                subPage == 8 -> LanguageSettingsPage(onBack = { subPage = 0 })
                else -> when (tab) {
                    0 -> AccountSettingsTab(
                        connectionLabel = connectionLabel,
                        connectionStateLabel = connectionStateLabel,
                        currentModelName = currentModelName,
                        stats = stats,
                        usage = usage,
                        onOpenBrowser = {
                            val url = connection.host?.baseUrl ?: "https://"
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url),
                                    ),
                                )
                            }
                        },
                    )
                    1 -> ModelsPage(
                        models = models,
                        store = store,
                        onBack = { tab = 0 },
                        showHeader = false,
                    )
                    2 -> PluginsSettingsPage(onBack = { tab = 0 })
                    3 -> AgentPresetsPage(
                        presets = presets,
                        currentPreset = currentSession?.agentPreset,
                        sessionBlank = currentSession?.blank ?: true,
                        store = store,
                        onBack = { tab = 0 },
                    )
                    else -> MoreSettingsTab(
                        onOpenGeneral = { subPage = 3 },
                        onOpenNotifications = { subPage = 5 },
                        onOpenConnection = { subPage = 6 },
                        onOpenLanguage = { subPage = 8 },
                        onOpenArchive = { subPage = 7 },
                        onOpenAbout = { aboutOpen = true },
                    )
                }
            }
        }
        SettingsBottomBar(selected = tab, onSelect = { tab = it; subPage = 0 })
    }

    if (aboutOpen) {
        DsDialog(title = stringResource(R.string.home_me_about), onDismiss = { aboutOpen = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
                Text(stringResource(R.string.app_long_name), style = Type.std14Strong, color = colors.labelPrimary)
                Text(stringResource(R.string.home_me_version, BuildConfig.VERSION_NAME), style = Type.small13, color = colors.labelSecondary)
                Text(connectionLabel, style = Type.caption11, color = colors.labelCaption)
                DsButton(text = stringResource(R.string.common_close), onClick = { aboutOpen = false }, variant = DsButtonVariant.Ghost)
            }
        }
    }
}

/** The account tab: server, model, connection and this session's usage. */
@Composable
private fun AccountSettingsTab(
    connectionLabel: String,
    connectionStateLabel: String,
    currentModelName: String?,
    stats: com.labteto.dshmobile.core.wire.dto.SessionStatsView?,
    usage: com.labteto.dshmobile.core.wire.dto.TokenUsageView?,
    onOpenBrowser: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
        Text(stringResource(R.string.home_tab_settings), style = Type.large20, color = colors.labelPrimary)
        Text(
            connectionLabel,
            style = Type.caption11,
            color = colors.labelTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        SettingsCardGroup(
            title = stringResource(R.string.settings_account_section),
            items = listOf(
                SettingsCardItem(
                    icon = FeatherIcons.Users,
                    title = stringResource(R.string.settings_login_state),
                    subtitle = stringResource(R.string.settings_not_logged_in),
                    onClick = onOpenBrowser,
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Info,
                    title = stringResource(R.string.settings_billing_balance),
                    subtitle = "—",
                    onClick = onOpenBrowser,
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Info,
                    title = stringResource(R.string.settings_grant_balance),
                    subtitle = "—",
                    onClick = onOpenBrowser,
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Globe,
                    title = stringResource(R.string.settings_open_web),
                    subtitle = connectionLabel,
                    onClick = onOpenBrowser,
                ),
            ),
        )
        SettingsCardGroup(
            title = stringResource(R.string.settings_section_notifications),
            items = listOf(
                SettingsCardItem(
                    icon = FeatherIcons.Server,
                    title = stringResource(R.string.settings_server),
                    subtitle = connectionLabel,
                    onClick = {},
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Activity,
                    title = stringResource(R.string.settings_connection_state),
                    subtitle = connectionStateLabel,
                    onClick = {},
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Cpu,
                    title = stringResource(R.string.settings_current_model),
                    subtitle = currentModelName ?: "—",
                    onClick = {},
                ),
            ),
        )
        if (stats != null || usage != null) {
            SettingsCardGroup(
                title = stringResource(R.string.settings_usage_section),
                items = buildList {
                    usage?.let { tokens ->
                        add(
                            SettingsCardItem(
                                icon = FeatherIcons.Activity,
                                title = stringResource(R.string.settings_usage_tokens),
                                subtitle = stringResource(
                                    R.string.settings_usage_in_out,
                                    com.labteto.dshmobile.ui.components.formatTokens(tokens.inputTokens),
                                    com.labteto.dshmobile.ui.components.formatTokens(tokens.outputTokens),
                                ),
                                onClick = {},
                            ),
                        )
                        tokens.cacheHitRatio?.let { ratio ->
                            add(
                                SettingsCardItem(
                                    icon = FeatherIcons.RefreshCw,
                                    title = stringResource(R.string.settings_usage_cache),
                                    subtitle = "${(ratio * 100).toInt()}%",
                                    onClick = {},
                                ),
                            )
                        }
                    }
                    stats?.let { value ->
                        add(
                            SettingsCardItem(
                                icon = FeatherIcons.Activity,
                                title = stringResource(R.string.settings_usage_turns),
                                subtitle = stringResource(R.string.chat_stats_turns, value.turns, value.steps),
                                onClick = {},
                            ),
                        )
                    }
                },
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
    }
}

/** The "More" tab: everything that does not deserve a primary tab. */
@Composable
private fun MoreSettingsTab(
    onOpenGeneral: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
        Text(stringResource(R.string.home_tab_settings), style = Type.large20, color = colors.labelPrimary)

        SettingsCardGroup(
            title = stringResource(R.string.home_tab_settings),
            items = listOf(
                SettingsCardItem(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.settings_general),
                    subtitle = null,
                    onClick = onOpenGeneral,
                ),
            ),
        )
        SettingsCardGroup(
            title = stringResource(R.string.settings_section_notifications),
            items = listOf(
                SettingsCardItem(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.home_me_notifications),
                    subtitle = null,
                    onClick = onOpenNotifications,
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Server,
                    title = stringResource(R.string.home_me_connection),
                    subtitle = null,
                    onClick = onOpenConnection,
                ),
            ),
        )
        SettingsCardGroup(
            title = stringResource(R.string.settings_section_other),
            items = listOf(
                SettingsCardItem(
                    icon = FeatherIcons.Globe,
                    title = stringResource(R.string.settings_language),
                    subtitle = null,
                    onClick = onOpenLanguage,
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Archive,
                    title = stringResource(R.string.home_me_archive),
                    subtitle = null,
                    onClick = onOpenArchive,
                ),
                SettingsCardItem(
                    icon = FeatherIcons.Info,
                    title = stringResource(R.string.home_me_about),
                    subtitle = stringResource(R.string.home_me_version, BuildConfig.VERSION_NAME),
                    onClick = onOpenAbout,
                ),
            ),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun SettingsBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = DsTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.bgBase)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SettingsTabItem(0, FeatherIcons.Users, stringResource(R.string.settings_tab_account), selected, onSelect, Modifier.weight(1f))
        SettingsTabItem(1, FeatherIcons.Cpu, stringResource(R.string.models_title), selected, onSelect, Modifier.weight(1f))
        SettingsTabItem(2, FeatherIcons.Zap, stringResource(R.string.settings_plugins), selected, onSelect, Modifier.weight(1f))
        SettingsTabItem(3, FeatherIcons.Layers, stringResource(R.string.settings_tab_presets), selected, onSelect, Modifier.weight(1f))
        SettingsTabItem(4, FeatherIcons.Menu, stringResource(R.string.settings_tab_more), selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun SettingsTabItem(
    index: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val tint = if (selected == index) colors.accent else colors.labelTertiary
    Column(
        modifier = modifier
            .clip(DsShapes.row)
            .clickable(role = Role.Button) { onSelect(index) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = Type.caption11, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun SettingsCardGroup(title: String, items: List<SettingsCardItem>) {
    val colors = DsTheme.colors
    Text(
        title,
        style = Type.xsmall12,
        color = colors.labelTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DsShapes.cube)
            .border(1.dp, colors.borderL1, DsShapes.cube)
            .background(colors.bgLayer1),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    Modifier.padding(start = 52.dp),
                    color = colors.borderL1,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(role = Role.Button, onClick = item.onClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(20.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = Type.std14, color = colors.labelPrimary)
                    item.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            subtitle,
                            style = Type.caption11,
                            color = colors.labelTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.labelCaption,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private data class SettingsCardItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit,
)

@Composable
internal fun WebBottomBar(selected: Int, onChat: () -> Unit, onSettings: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgLayer1)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WebBarItem(Icons.Outlined.ChatBubbleOutline, R.string.home_tab_chats, selected == 0, onChat)
        WebBarItem(Icons.Filled.Settings, R.string.home_tab_settings, selected == 1, onSettings)
    }
}

@Composable
private fun WebBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(DsShapes.row)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.labelTertiary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            stringResource(label),
            style = Type.caption11,
            color = if (selected) colors.accent else colors.labelTertiary,
        )
    }
}














