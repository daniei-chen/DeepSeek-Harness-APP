package com.labteto.dshmobile.ui.screens.main

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import com.labteto.dshmobile.core.wire.dto.LlmConfigurableProvider
import com.labteto.dshmobile.ui.components.DsDialog
import com.labteto.dshmobile.BuildConfig
import com.labteto.dshmobile.R
import com.labteto.dshmobile.connection.ConnectionPhase
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.ToggleRow
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.screens.settings.LanguageOptions
import com.labteto.dshmobile.ui.screens.settings.SettingsViewModel
import com.labteto.dshmobile.ui.screens.connect.connectFailureBody
import com.labteto.dshmobile.core.wire.dto.AgentPresetListValue
import com.labteto.dshmobile.core.wire.dto.AgentPresetTrust
import com.labteto.dshmobile.core.wire.dto.SessionModelsValue
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.DsPill
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import kotlinx.coroutines.launch

/** Full-page model picker reached from Settings (not a bottom sheet). */
@Composable
fun ModelsPage(models: SessionModelsValue?, store: SessionStore, onBack: () -> Unit, showHeader: Boolean = true) {
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    var editingProvider by remember { mutableStateOf<LlmConfigurableProvider?>(null) }
    var providerOpen by remember { mutableStateOf(false) }
    var customProviderOpen by remember { mutableStateOf(false) }
    var addMethodOpen by remember { mutableStateOf(false) }
    var credentialStatus by remember { mutableStateOf<Map<String, Boolean?>>(emptyMap()) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (showHeader) PageHeader(title = stringResource(R.string.models_title), onBack = onBack)
        if (models == null) {
            Text(
                stringResource(R.string.common_loading),
                style = DsType.std14,
                color = colors.labelTertiary,
                modifier = Modifier.padding(DsSpacing.medium),
            )
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.tiny),
        ) {
            var query by remember { mutableStateOf("") }
            val totalModels = models.groups.sumOf { it.models.size }
            if (totalModels > 4 || query.isNotBlank()) ChatSearchField(query, { query = it })
            val groups = models.groups
                .map { group ->
                    group to group.models.filter { model ->
                        modelSearchMatches(query, "${group.id} ${group.name} ${model.id} ${model.name}")
                    }
                }
                .filter { it.second.isNotEmpty() }
            if (groups.isEmpty()) {
                Text(
                    stringResource(R.string.ux_b_no_models),
                    style = DsType.small13,
                    color = colors.labelTertiary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            groups.forEach { (group, groupModels) ->
                Text(
                    group.name,
                    style = DsType.xsmall12,
                    color = colors.labelTertiary,
                    modifier = Modifier.padding(top = DsSpacing.small, bottom = 2.dp),
                )
                groupModels.forEach { model ->
                    val selected = models.current.provider == group.id && models.current.model == model.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DsShapes.row)
                            .clickable { scope.launch { store.selectModel(group.id, model.id) } }
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            model.name,
                            style = DsType.std14,
                            color = colors.labelPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (selected) {
                        model.reasoning?.efforts?.forEach { effort ->
                            val effortSelected = models.current.reasoningEffort == effort.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(DsShapes.row)
                                    .clickable {
                                        scope.launch { store.selectModel(group.id, model.id, effort.id) }
                                    }
                                    .padding(start = 24.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    effort.name,
                                    style = DsType.small13,
                                    color = if (effortSelected) colors.accent else colors.labelSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (effortSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(DsSpacing.medium))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DsShapes.row)
                    .clickable { addMethodOpen = true }
                    .padding(vertical = DsSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.models_add_provider),
                        style = DsType.std14,
                        color = colors.labelPrimary,
                    )
                    Text(
                        stringResource(R.string.models_provider_choose_hint),
                        style = DsType.caption11,
                        color = colors.labelTertiary,
                    )
                }
                Text("+", style = DsType.large20, color = colors.accent)
            }
            if (providerOpen) {
                val configurable = remember { mutableStateOf<List<LlmConfigurableProvider>?>(null) }
                LaunchedEffect(Unit) { configurable.value = store.configurableProviders() }
                val providerList = configurable.value
                LaunchedEffect(providerList) {
                    val list = providerList.orEmpty()
                    credentialStatus = list.associate { provider ->
                        provider.provider to store.credentialConfigured(providerKeyRef(provider.provider))
                    }
                }
                when {
                    providerList == null -> Text(
                        stringResource(R.string.common_loading),
                        style = DsType.small13,
                        color = colors.labelTertiary,
                        modifier = Modifier.padding(vertical = DsSpacing.small),
                    )
                    providerList.isEmpty() -> Text(
                        stringResource(R.string.models_provider_note),
                        style = DsType.caption11,
                        color = colors.labelTertiary,
                        modifier = Modifier.padding(vertical = DsSpacing.small),
                    )
                    else -> providerList.forEach { provider ->
                        val configured = credentialStatus[provider.provider]
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, colors.borderL1, RoundedCornerShape(12.dp))
                                .clickable { editingProvider = provider }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (configured == true) colors.success else colors.error),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    provider.displayName,
                                    style = DsType.std14,
                                    color = colors.labelPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    stringResource(R.string.models_edit),
                                    style = DsType.small13,
                                    color = colors.accent,
                                )
                            }
                            if (provider.active) {
                                Text(
                                    stringResource(R.string.models_provider_active),
                                    style = DsType.caption11,
                                    color = colors.accent,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.models_provider_note),
                    style = DsType.caption11,
                    color = colors.labelCaption,
                    modifier = Modifier.padding(top = DsSpacing.tiny),
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }

    if (addMethodOpen) {
        DsDialog(
            title = stringResource(R.string.models_add_provider),
            onDismiss = { addMethodOpen = false },
        ) {
            Text(
                stringResource(R.string.models_add_method),
                style = DsType.xsmall12,
                color = DsTheme.colors.labelTertiary,
            )
            AddProviderMethod(
                title = stringResource(R.string.models_third_party),
                description = stringResource(R.string.models_third_party_desc),
            ) {
                addMethodOpen = false
                providerOpen = true
            }
            AddProviderMethod(
                title = stringResource(R.string.models_custom_provider),
                description = stringResource(R.string.models_custom_desc),
            ) {
                addMethodOpen = false
                customProviderOpen = true
            }
            DsButton(
                text = stringResource(R.string.common_cancel),
                onClick = { addMethodOpen = false },
                variant = DsButtonVariant.Ghost,
            )
        }
    }

    if (customProviderOpen) {
        ProviderEditorDialog(provider = null, store = store) { customProviderOpen = false }
    }

    editingProvider?.let { provider ->
        ProviderEditorDialog(provider = provider, store = store) { editingProvider = null }
    }
}

/** One "add provider" method row in the chooser dialog. */
@Composable
private fun AddProviderMethod(title: String, description: String, onClick: () -> Unit) {
    val colors = DsTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.borderL1, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, style = DsType.std14, color = colors.labelPrimary)
        Text(description, style = DsType.caption11, color = colors.labelTertiary)
    }
}

/** Full-page agent-preset picker reached from Settings. */
@Composable
fun AgentPresetsPage(
    presets: AgentPresetListValue?,
    currentPreset: String?,
    sessionBlank: Boolean,
    store: SessionStore,
    onBack: () -> Unit,
) {
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf<Pair<String, String>?>(null) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PageHeader(title = stringResource(R.string.presets_title), onBack = onBack)
        if (!sessionBlank) {
            Text(
                stringResource(R.string.presets_locked),
                style = DsType.small13,
                color = colors.labelTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        val entries = presets?.presets.orEmpty()
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.presets_empty),
                style = DsType.small13,
                color = colors.labelTertiary,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            listOf(
                stringResource(R.string.presets_builtin) to entries.filter { it.trust == AgentPresetTrust.SYSTEM },
                stringResource(R.string.presets_custom) to entries.filter { it.trust == AgentPresetTrust.USER },
                stringResource(R.string.presets_other) to entries.filter { it.trust == AgentPresetTrust.UNKNOWN },
            ).forEach { (groupLabel, group) ->
                if (group.isEmpty()) return@forEach
                Text(
                    groupLabel,
                    style = DsType.xsmall12,
                    color = colors.labelTertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                group.forEach { entry ->
                val selected = entry.id == currentPreset
                val presetLabel = entry.displayName()
                val selectable = sessionBlank && entry.broken == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DsShapes.row)
                        .clickable(enabled = selectable) {
                            scope.launch { store.selectAgentPreset(entry.id) }
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                presetLabel,
                                style = DsType.std14Strong,
                                color = when {
                                    selected -> colors.accent
                                    selectable -> colors.labelPrimary
                                    else -> colors.labelTertiary
                                },
                            )
                            if (entry.isDefault) {
                                Spacer(Modifier.width(6.dp))
                                DsPill(text = stringResource(R.string.presets_default))
                            }
                        }
                        entry.displayDescription()?.let {
                            Text(it, style = DsType.small13, color = colors.labelTertiary)
                        }
                        entry.broken?.let {
                            Text(
                                stringResource(R.string.presets_broken, it),
                                style = DsType.caption11,
                                color = colors.warnLabel,
                            )
                        }
                        Text(
                            stringResource(R.string.presets_view_config),
                            style = DsType.small13,
                            color = colors.accent,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable {
                                    scope.launch {
                                        viewing = presetLabel to (store.agentPresetDocument(entry.id) ?: "")
                                    }
                                },
                        )
                    }
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                }
            }
            Spacer(Modifier.size(24.dp))
        }
    }

    viewing?.let { (name, yaml) ->
        DsDialog(
            title = stringResource(R.string.presets_config_title, name),
            onDismiss = { viewing = null },
        ) {
            Text(
                yaml,
                style = DsType.small13.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = DsTheme.colors.labelSecondary,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
fun GeneralSettingsPage(onBack: () -> Unit, onOpenLanguage: () -> Unit, showHeader: Boolean = true) {
    val colors = DsTheme.colors
    val store = rememberSessionStore()
    val scope = rememberCoroutineScope()
    var openResult by remember { mutableStateOf<Boolean?>(null) }
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (showHeader) PageHeader(title = stringResource(R.string.settings_general), onBack = onBack)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.home_me_appearance), style = DsType.xsmall12, color = colors.labelTertiary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
            ) {
                DsPill(
                    text = stringResource(R.string.settings_appearance_light),
                    selected = settings.themePreference == "light",
                    onClick = { viewModel.set { it.copy(themePreference = "light") } },
                )
                DsPill(
                    text = stringResource(R.string.settings_appearance_dark),
                    selected = settings.themePreference == "dark",
                    onClick = { viewModel.set { it.copy(themePreference = "dark") } },
                )
                DsPill(
                    text = stringResource(R.string.settings_appearance_system),
                    selected = settings.themePreference == "system",
                    onClick = { viewModel.set { it.copy(themePreference = "system") } },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DsShapes.row)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onOpenLanguage)
                    .heightIn(min = 48.dp)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_language), style = DsType.std14, color = colors.labelPrimary, modifier = Modifier.weight(1f))
                Text("›", style = DsType.large20, color = colors.labelTertiary)
            }
            ToggleRow(
                label = stringResource(R.string.settings_show_code_work_view),
                checked = settings.showCodeWorkView,
                hint = stringResource(R.string.settings_show_code_work_view_hint),
            ) { viewModel.set { it.copy(showCodeWorkView = !it.showCodeWorkView) } }
            ToggleRow(
                label = stringResource(R.string.settings_update_check),
                checked = settings.updateCheckEnabled,
                hint = stringResource(R.string.settings_update_check_hint),
            ) { viewModel.set { it.copy(updateCheckEnabled = !it.updateCheckEnabled) } }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_version),
                    style = DsType.std14,
                    color = colors.labelPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    BuildConfig.VERSION_NAME,
                    style = DsType.small13,
                    color = colors.labelTertiary,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(DsShapes.row)
                    .clickable {
                        scope.launch { openResult = store.openSettingsDocument() }
                    }
                    .padding(vertical = DsSpacing.small),
            ) {
                Text(
                    stringResource(R.string.settings_open_document),
                    style = DsType.std14,
                    color = colors.labelPrimary,
                )
                Text(
                    if (openResult == false) stringResource(R.string.settings_open_document_failed)
                    else stringResource(R.string.settings_open_document_hint),
                    style = DsType.caption11,
                    color = if (openResult == false) colors.error else colors.labelTertiary,
                )
            }
        }
    }
}

@Composable
fun NotificationsSettingsPage(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PageHeader(title = stringResource(R.string.settings_notifications), onBack = onBack)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ToggleRow(
                stringResource(R.string.settings_notifications_turn),
                settings.notifyTurnComplete,
                stringResource(R.string.settings_notifications_turn_hint),
            ) { viewModel.set { it.copy(notifyTurnComplete = !it.notifyTurnComplete) } }
            ToggleRow(
                stringResource(R.string.settings_notifications_goal),
                settings.notifyGoal,
                stringResource(R.string.settings_notifications_goal_hint),
            ) { viewModel.set { it.copy(notifyGoal = !it.notifyGoal) } }
            ToggleRow(
                stringResource(R.string.settings_notifications_action),
                settings.notifyNeedsAction,
                stringResource(R.string.settings_notifications_action_hint),
            ) { viewModel.set { it.copy(notifyNeedsAction = !it.notifyNeedsAction) } }
        }
    }
}

@Composable
fun ConnectionSettingsPage(onBack: () -> Unit, showHeader: Boolean = true) {
    val colors = DsTheme.colors
    val viewModel: SettingsViewModel = hiltViewModel()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    var confirmDisconnect by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (showHeader) PageHeader(title = stringResource(R.string.settings_connection), onBack = onBack)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                connection.host?.baseUrl ?: stringResource(R.string.common_offline),
                style = DsType.std14Strong,
                color = colors.labelPrimary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                when (connection.phase) {
                    ConnectionPhase.CONNECTED -> stringResource(R.string.common_connected)
                    ConnectionPhase.RECONNECTING -> stringResource(R.string.common_reconnecting)
                    else -> stringResource(R.string.common_offline)
                },
                style = DsType.small13,
                color = colors.labelTertiary,
            )
            // The settings surface used to say only "offline"; the connect screen knew why.
            connection.failure?.let { failure ->
                Text(
                    connectFailureBody(failure, connection.host?.baseUrl),
                    style = DsType.small13,
                    color = colors.warnLabel,
                )
            }
            if (connection.host != null && connection.phase != ConnectionPhase.DISCONNECTED) {
                DsButton(
                    text = stringResource(R.string.settings_connection_disconnect),
                    onClick = { confirmDisconnect = true },
                    variant = DsButtonVariant.Outline,
                )
            }
        }
    }
    if (confirmDisconnect) {
        ConfirmDialog(
            title = stringResource(R.string.settings_connection_disconnect_confirm),
            body = stringResource(R.string.settings_connection_disconnect_message),
            confirmLabel = stringResource(R.string.settings_connection_disconnect),
            onDismiss = { confirmDisconnect = false },
            onConfirm = {
                confirmDisconnect = false
                viewModel.disconnect()
            },
        )
    }
}

@Composable
fun PluginsSettingsPage(onBack: () -> Unit) {
    val colors = DsTheme.colors
    val store = com.labteto.dshmobile.ui.rememberSessionStore()
    val scope = rememberCoroutineScope()
    val plugins by store.plugins.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (store.plugins.value == null) store.loadPlugins()
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PageHeader(title = stringResource(R.string.settings_plugins), onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val entries = plugins?.entries.orEmpty()
            when {
                plugins == null -> Column {
                    Text(
                        stringResource(R.string.common_loading),
                        style = DsType.std14,
                        color = colors.labelTertiary,
                    )
                    DsButton(
                        text = stringResource(R.string.common_retry),
                        onClick = { scope.launch { store.loadPlugins() } },
                        variant = DsButtonVariant.Ghost,
                    )
                }
                entries.isEmpty() -> Text(
                    stringResource(R.string.plugins_empty),
                    style = DsType.std14,
                    color = colors.labelTertiary,
                )
                else -> {
                    Text(
                        stringResource(R.string.settings_plugins_inventory),
                        style = DsType.xsmall12,
                        color = colors.labelTertiary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    var pluginQuery by remember { mutableStateOf("") }
                    val filtered = filterPluginEntries(entries, pluginQuery)
                    if (entries.size > 8 || pluginQuery.isNotBlank()) ChatSearchField(pluginQuery, { pluginQuery = it })
                    if (filtered.isEmpty()) {
                        Text(
                            stringResource(R.string.plugins_empty),
                            style = DsType.small13,
                            color = colors.labelTertiary,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    filtered.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.moduleName.substringAfterLast('/'),
                                    style = DsType.std14,
                                    color = if (entry.enabled) colors.labelPrimary else colors.labelTertiary,
                                )
                                Text(
                                    entry.entryId,
                                    style = DsType.caption11,
                                    color = colors.labelCaption,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                entry.fiberPhase?.name?.lowercase().orEmpty(),
                                style = DsType.caption11,
                                color = colors.labelCaption,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSettingsPage(onBack: () -> Unit) {
    val colors = DsTheme.colors
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PageHeader(title = stringResource(R.string.settings_language), onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            LanguageOptions.forEach { option ->
                val label = option.label
                    ?: option.labelRes?.let { stringResource(it) }
                    ?: option.tag.orEmpty()
                val selected = settings.localeOverride == option.tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DsShapes.row)
                        .clickable {
                            viewModel.set { it.copy(localeOverride = option.tag) }
                        }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = DsType.std14,
                        color = if (selected) colors.accent else colors.labelPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedSessionsPage(onBack: () -> Unit) {
    val store = rememberSessionStore()
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        PageHeader(title = stringResource(R.string.chatlist_archived), onBack = onBack)
        Box(Modifier.weight(1f)) { ArchivedSessions(store) }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = colors.labelPrimary,
            )
        }
        Text(title, style = DsType.large20, color = colors.labelPrimary)
    }
}
















