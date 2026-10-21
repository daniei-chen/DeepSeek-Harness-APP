package com.labteto.dshmobile.ui.screens.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.components.FeatherIcons
import androidx.hilt.navigation.compose.hiltViewModel
import com.labteto.dshmobile.ui.components.ToggleRow
import com.labteto.dshmobile.ui.screens.settings.SettingsViewModel
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import kotlinx.coroutines.launch

/** The four native destinations: chat, automations, a real file/browser tab, and settings. */
@Composable
internal fun MainBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = DsTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.bgBase)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem(0, FeatherIcons.MessageSquare, stringResource(R.string.nav_chat), selected, onSelect, Modifier.weight(1f))
        NavItem(1, FeatherIcons.Activity, stringResource(R.string.nav_automations), selected, onSelect, Modifier.weight(1f))
        NavItem(2, FeatherIcons.Folder, stringResource(R.string.nav_files), selected, onSelect, Modifier.weight(1f))
        NavItem(3, Icons.Filled.Settings, stringResource(R.string.nav_settings), selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun NavItem(
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
            .clickable { onSelect(index) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = DsType.caption11, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The Files tab: the open session's workspace plus a WebView. Tapping an `.html` file renders it
 * locally; the URL field browses any http(s) page. This is deliberately a real browser tab rather
 * than the old modal file panel.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun FileBrowserScreen() {
    val store = rememberSessionStore()
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    val sessionId by store.currentSessionId.collectAsStateWithLifecycle()
    var directory by remember { mutableStateOf(".") }
    var listing by remember { mutableStateOf<com.labteto.dshmobile.core.wire.dto.WorkspaceDirectoryListing?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var urlText by remember { mutableStateOf("") }
    var webUrl by remember { mutableStateOf<String?>(null) }
    var html by remember { mutableStateOf<String?>(null) }
    var showingWeb by remember { mutableStateOf(false) }
    val baseUrl = remember(store) { runCatching { store.connectionState.value.host?.baseUrl }.getOrNull() }

    fun load(path: String) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            listing = store.workspaceListing(path)
            if (listing == null) error = "无法读取目录"
            busy = false
        }
    }
    LaunchedEffect(sessionId, directory) { load(directory) }

    BackHandler(enabled = showingWeb) {
        showingWeb = false
        webUrl = null
        html = null
    }

    Column(Modifier.fillMaxSize().background(colors.bgBase)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.files_title), style = DsType.large20, color = colors.labelPrimary, modifier = Modifier.weight(1f))
            if (showingWeb) {
                DsButton(
                    text = stringResource(R.string.files_back_to_list),
                    onClick = { showingWeb = false; webUrl = null; html = null },
                    variant = DsButtonVariant.Ghost,
                )
            } else {
                DsIconButton(
                    icon = FeatherIcons.RefreshCw,
                    contentDescription = stringResource(R.string.common_retry),
                    onClick = { load(directory) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = urlText,
                onValueChange = { urlText = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.files_url_hint), style = DsType.small13) },
                modifier = Modifier.weight(1f),
            )
            DsButton(
                text = stringResource(R.string.files_open),
                onClick = {
                    val raw = urlText.trim()
                    if (raw.isNotEmpty()) {
                        val normalized = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                        html = null
                        webUrl = normalized
                        showingWeb = true
                    }
                },
            )
        }
        if (showingWeb) {
            val htmlPayload = html
            val loaded = htmlPayload ?: webUrl ?: "about:blank"
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        webViewClient = WebViewClient()
                    }
                },
                update = { view ->
                    if (view.tag != loaded) {
                        view.tag = loaded
                        if (htmlPayload != null) {
                            view.loadDataWithBaseURL(baseUrl, htmlPayload, "text/html", "utf-8", null)
                        } else {
                            view.loadUrl(loaded)
                        }
                    }
                },
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    directory,
                    style = DsType.caption11,
                    color = colors.labelTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (directory != ".") {
                    DsButton(
                        text = stringResource(R.string.files_up),
                        onClick = {
                            val parent = directory.substringBeforeLast('/', "")
                            directory = parent.ifBlank { "." }
                        },
                        variant = DsButtonVariant.Ghost,
                    )
                }
            }
            error?.let {
                Text(it, style = DsType.small13, color = colors.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            val entries = listing?.entries.orEmpty()
                .sortedWith(compareBy({ it.type != "directory" }, { it.name.lowercase() }))
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries, key = { it.name }) { entry ->
                    val isDir = entry.type == "directory"
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) {
                                if (isDir) {
                                    directory = if (directory == ".") entry.name else "$directory/${entry.name}"
                                } else if (entry.name.endsWith(".html", true) || entry.name.endsWith(".htm", true)) {
                                    scope.launch {
                                        val path = if (directory == ".") entry.name else "$directory/${entry.name}"
                                        val bytes = store.workspaceFileBytes(path)
                                        if (bytes != null) {
                                            html = String(bytes, Charsets.UTF_8)
                                            webUrl = null
                                            showingWeb = true
                                        } else {
                                            error = "无法读取 ${entry.name}"
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isDir) FeatherIcons.Folder else FeatherIcons.FileText,
                            contentDescription = null,
                            tint = if (isDir) colors.labelSecondary else colors.labelTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            entry.name,
                            style = DsType.std14,
                            color = if (isDir) colors.labelPrimary else colors.labelSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!busy && listing != null && entries.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.panel_empty_folder),
                            style = DsType.std14,
                            color = colors.labelTertiary,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** The Settings destination: account/DeepSeek entry first, then the settings pages. */
@Composable
internal fun MeScreen(onOpenChat: () -> Unit) {
    val store = rememberSessionStore()
    val models by store.models.collectAsStateWithLifecycle()
    val presets by store.agentPresets.collectAsStateWithLifecycle()
    val sessions by store.sessions.collectAsStateWithLifecycle()
    val connection by store.connectionState.collectAsStateWithLifecycle()
    val colors = DsTheme.colors
    val context = LocalContext.current
    var subPage by rememberSaveable { mutableIntStateOf(0) }
    var aboutOpen by remember { mutableStateOf(false) }
    val currentSession = sessions.firstOrNull { it.sessionId == store.currentSessionId.value }
    val connectionLabel = connection.host?.baseUrl ?: stringResource(R.string.common_offline)
    val webUrl = connection.host?.baseUrl ?: "https://harness.example.com"

    BackHandler(enabled = subPage != 0) { subPage = 0 }

    when (subPage) {
        1 -> GeneralSettingsPage(onBack = { subPage = 0 }, onOpenLanguage = { subPage = 8 })
        2 -> ModelsPage(models = models, store = store, onBack = { subPage = 0 })
        3 -> PluginsSettingsPage(onBack = { subPage = 0 })
        4 -> AgentPresetsPage(
            presets = presets,
            currentPreset = currentSession?.agentPreset,
            sessionBlank = currentSession?.blank ?: true,
            store = store,
            onBack = { subPage = 0 },
        )
        5 -> NotificationsSettingsPage(onBack = { subPage = 0 })
        6 -> ConnectionSettingsPage(onBack = { subPage = 0 })
        7 -> ArchivedSessionsPage(onBack = { subPage = 0 })
        8 -> LanguageSettingsPage(onBack = { subPage = 0 })
        10 -> NamespaceSettingsPage(stringResource(R.string.settings_shell_title), listOf("pwsh-sandbox", "bash-sandbox"), store, onBack = { subPage = 0 })
        11 -> NamespaceSettingsPage(stringResource(R.string.settings_agent_loop_title), listOf("agent-loop"), store, onBack = { subPage = 0 })
        12 -> NamespaceSettingsPage(stringResource(R.string.settings_subagent_title), listOf("subagent"), store, onBack = { subPage = 0 })
        13 -> NamespaceSettingsPage(stringResource(R.string.settings_web_search_title), listOf("web-search-deepseek"), store, onBack = { subPage = 0 })
        14 -> NamespaceSettingsPage(stringResource(R.string.settings_session_log_title), listOf("session-log-deepseek"), store, onBack = { subPage = 0 })
        else -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.size(20.dp))
                Text(stringResource(R.string.nav_settings), style = DsType.large20, color = colors.labelPrimary)

                Text(
                    connectionLabel,
                    style = DsType.caption11,
                    color = colors.labelTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                MeGroup(title = stringResource(R.string.me_account_section)) {
                    MeRow(FeatherIcons.Users, stringResource(R.string.settings_login_state), stringResource(R.string.settings_not_logged_in)) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
                    }
                    MeRow(FeatherIcons.Info, stringResource(R.string.settings_billing_balance), "—") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
                    }
                    MeRow(FeatherIcons.Info, stringResource(R.string.settings_grant_balance), "—") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
                    }
                }
                MeGroup(title = stringResource(R.string.me_app_group)) {
                    MeRow(Icons.Filled.Settings, stringResource(R.string.settings_general), null) { subPage = 1 }
                    MeRow(FeatherIcons.Cpu, stringResource(R.string.models_title), null) { subPage = 2 }
                    MeRow(FeatherIcons.Zap, stringResource(R.string.settings_plugins), null) { subPage = 3 }
                    MeRow(FeatherIcons.Layers, stringResource(R.string.presets_title), null) { subPage = 4 }
                }
                MeGroup(title = stringResource(R.string.settings_section_other)) {
                    MeRow(FeatherIcons.Activity, stringResource(R.string.home_me_notifications), null) { subPage = 5 }
                    MeRow(FeatherIcons.Server, stringResource(R.string.home_me_connection), null) { subPage = 6 }
                    MeRow(FeatherIcons.Archive, stringResource(R.string.home_me_archive), null) { subPage = 7 }
                    MeRow(FeatherIcons.Globe, stringResource(R.string.settings_language), null) { subPage = 8 }
                    MeRow(FeatherIcons.Info, stringResource(R.string.home_me_about), null) { aboutOpen = true }
                    MeRow(FeatherIcons.Terminal, stringResource(R.string.settings_shell_title), null) { subPage = 10 }
                    MeRow(FeatherIcons.Activity, stringResource(R.string.settings_agent_loop_title), null) { subPage = 11 }
                    MeRow(FeatherIcons.Users, stringResource(R.string.settings_subagent_title), null) { subPage = 12 }
                    MeRow(FeatherIcons.Globe, stringResource(R.string.settings_web_search_title), null) { subPage = 13 }
                    MeRow(FeatherIcons.Server, stringResource(R.string.settings_session_log_title), null) { subPage = 14 }
                }
                Spacer(Modifier.size(24.dp))
            }
        }
    }

    if (aboutOpen) {
        com.labteto.dshmobile.ui.components.DsDialog(
            title = stringResource(R.string.home_me_about),
            onDismiss = { aboutOpen = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
                Text(stringResource(R.string.app_long_name), style = DsType.std14Strong, color = colors.labelPrimary)
                Text(connectionLabel, style = DsType.caption11, color = colors.labelCaption)
                DsButton(
                    text = stringResource(R.string.common_close),
                    onClick = { aboutOpen = false },
                    variant = DsButtonVariant.Ghost,
                )
            }
        }
    }
}

@Composable
private fun MeGroup(title: String, content: @Composable () -> Unit) {
    val colors = DsTheme.colors
    Text(title, style = DsType.xsmall12, color = colors.labelTertiary, modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.borderL1, RoundedCornerShape(16.dp))
            .background(colors.bgLayer1),
    ) { content() }
}

@Composable
private fun MeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DsType.std14, color = colors.labelPrimary)
            subtitle?.let {
                Text(it, style = DsType.caption11, color = colors.labelTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Launch the user's browser for a URL; kept small so MeScreen stays readable. */
@Suppress("unused")
private fun openExternal(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}





