package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.data.HarnessFeature
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.core.wire.dto.*
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.components.*
import com.labteto.dshmobile.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun PluginManagerScreen(onClose: () -> Unit, onDraftOpened: () -> Unit = onClose) {
    val store = rememberSessionStore()
    val available by store.pluginManagementAvailable.collectAsStateWithLifecycle()
    val inventory by store.plugins.collectAsStateWithLifecycle()
    if (available == false) {
        inventory?.let { com.labteto.dshmobile.ui.screens.settings.PluginsSheet(it, onDismiss = onClose) }
        LaunchedEffect(inventory) { if (inventory == null) onClose() }
        return
    }
    val connection by store.connectionState.collectAsStateWithLifecycle()
    val host = store.activeHostKey
    val scope = rememberCoroutineScope()
    val operation by store.pluginOperations.state.collectAsStateWithLifecycle()
    val pending by store.pluginOperations.pending.collectAsStateWithLifecycle()
    val presets by store.agentPresets.collectAsStateWithLifecycle()
    var inventoryOpen by remember { mutableStateOf(false) }
    var bundles by remember(host) { mutableStateOf<List<PluginBundle>>(emptyList()) }
    var plugins by remember(host) { mutableStateOf<List<ManagedPlugin>>(emptyList()) }
    var registries by remember(host) { mutableStateOf<PluginRegistries?>(null) }
    var spec by remember(host) { mutableStateOf("") }
    var registry by remember(host) { mutableStateOf("") }
    var search by remember(host) { mutableStateOf("") }
    var installOpen by remember(host) { mutableStateOf(false) }
    var operationOpen by remember(host) { mutableStateOf(false) }
    var installedVersion by remember(host) { mutableStateOf<String?>(null) }
    var inspection by remember(host) { mutableStateOf<PluginInspection?>(null) }
    var confirm by remember(host) { mutableStateOf<PluginInstallSubject?>(null) }
    var remove by remember(host) { mutableStateOf<PluginBundle?>(null) }
    var loading by remember(host) { mutableStateOf(false) }
    var loaded by remember(host) { mutableStateOf(false) }
    var error by remember(host) { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    val busy = loading || pending.any { it.host == host && it.busy }
    val offline = stringResource(R.string.common_offline)
    val creator = stringResource(R.string.harness_creator_draft)
    val current = operation.takeIf { it.host == host }
    fun run(block: suspend (DshApiClient) -> Unit) { scope.launch {
        loading = true; error = null
        try { block(store.apiForHost(host) ?: error(offline)) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: offline }
        finally { loading = false }
    } }
    LaunchedEffect(host, connection.phase, revision, current?.result, current?.unknown) { run { api ->
        val next = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.pluginBundles() }.featureValue()
        val rows = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.managedPlugins() }.featureValue()
        val sources = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.pluginRegistries() }.featureValue()
        if (store.activeHostKey == host) { bundles = next; plugins = rows; registries = sources; loaded = true }
    } }
    LaunchedEffect(host) { store.hostEvents.collect { if (it.first == "plugin-manager/changed") revision++ } }
    @Composable fun Failure() {
        error?.let { DsCard {
            Text(stringResource(R.string.ux_a_action_error), color = DsTheme.colors.error, style = DsType.std14)
            FeatureAction(stringResource(R.string.common_retry), !busy) { revision++ }
            if (inventory != null) FeatureAction(stringResource(R.string.settings_plugins_inventory)) { inventoryOpen = true }
            FeatureDiagnostics(it)
        } }
    }
    @Composable fun PluginToggle(plugin: ManagedPlugin) {
        ToggleRow(technicalDisplay(plugin.moduleName), plugin.enabled, plugin.readOnlyReason?.let { stringResource(pluginReason(it)) },
            enabled = !busy && plugin.readOnlyReason == null) { run { api ->
            val result = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.setPluginEnabled(plugin.entryId, !plugin.enabled) }.featureValue()
            if (host != null) store.pluginOperations.changed(host, result); revision++
        } }
    }
    @Composable fun Operations() {
        // Ongoing operations are compact subject cards; terminal output stays behind disclosure.
        pending.filter { it.host == host }.forEach { state -> DsCard {
            Text(state.displaySubject()?.let(::technicalDisplay) ?: stringResource(R.string.ux_a_previous_install), style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
            if (state.busy) {
                FeatureProgress()
                Text(stringResource(pluginPhase(state.phase)), style = DsType.small13, color = DsTheme.colors.labelSecondary)
                FeatureAction(stringResource(R.string.common_cancel)) { if (host != null && state.requestId != null) store.pluginOperations.cancel(host, state.requestId) }
            }
            if (state.unknown) {
                Text(stringResource(R.string.ux_a_unknown), style = DsType.small13, color = DsTheme.colors.warnText)
                FeatureAction(stringResource(R.string.ux_a_check_status)) { if (host != null) store.pluginOperations.resume(host, state.requestId) }
                FeatureAction(stringResource(R.string.harness_dismiss_unknown)) { if (host != null && state.requestId != null) store.pluginOperations.dismissUnknown(host, state.requestId) }
            }
            FeatureDiagnostics(listOfNotNull(state.requestId, state.phase, state.error, state.log.takeIf { it.isNotBlank() }).joinToString("\n"))
        } }
        current?.let { state -> state.result?.let { result -> DsCard {
            Text(state.displaySubject()?.let(::technicalDisplay) ?: stringResource(R.string.ux_a_previous_install), style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
            result.version?.let { Text(technicalDisplay(it), style = DsType.small13, color = DsTheme.colors.labelSecondary) }
            Text(stringResource(when (result.application) {
                "applied" -> R.string.harness_applied; "restart-required" -> R.string.harness_restart
                "overridden" -> R.string.harness_overridden; "cancelled" -> R.string.harness_cancelled; else -> R.string.harness_failed
            }), style = DsType.std14, color = if (result.application == "restart-required") DsTheme.colors.warnText else DsTheme.colors.labelSecondary)
            result.error?.let { Text(stringResource(pluginReason(it.code)), color = DsTheme.colors.error, style = DsType.small13) }
            state.approvalSubject()?.let { approval ->
                FeatureAction(stringResource(R.string.ux_a_approve_retry), !busy, DsButtonVariant.Primary) { confirm = approval }
            }
            FeatureDiagnostics(listOfNotNull(state.requestId, result.error?.code, result.error?.diagnostic,
                result.warnings.joinToString("\n").ifBlank { null }, result.packageResult?.output?.takeLast(8192),
                result.packageResult?.takeIf { it.truncated }?.logPath, state.log.ifBlank { null }).joinToString("\n"))
        } } }
    }
    FeaturePage(stringResource(R.string.settings_plugins), onClose, onRefresh = { if (!busy) revision++ }) {
        FeatureField(search, { search = it }, R.string.plugins_search_hint, singleLine = true)
        FeatureAction(stringResource(R.string.harness_install_update), !busy, DsButtonVariant.Primary) {
            spec = ""; registry = ""; inspection = null; installedVersion = null; installOpen = true
        }
        if (presets?.presets?.any { it.id == "cordis" && it.broken == null } == true) {
            FeatureAction(stringResource(if (loading) R.string.common_loading else R.string.harness_create_plugin), !busy) { run {
                if (host != null && store.stageFeatureDraft(host, creator, "cordis")) onDraftOpened() else error(offline)
            } }
        }
        if (loading) FeatureProgress()
        Failure()
        (pending.filter { it.host == host } + listOfNotNull(current?.takeIf { it.result != null })).forEach { state ->
            DsCard(onClick = { operationOpen = true }) {
                Text(state.displaySubject()?.let(::technicalDisplay) ?: stringResource(R.string.ux_a_previous_install), style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
                Text(stringResource(when {
                    state.unknown -> R.string.ux_a_check_status
                    state.busy -> pluginPhase(state.phase)
                    state.result?.application == "restart-required" -> R.string.harness_restart
                    state.result?.application == "applied" -> R.string.harness_applied
                    state.result?.application == "cancelled" -> R.string.harness_cancelled
                    else -> R.string.harness_failed
                }), style = DsType.small13, color = if (state.unknown || state.result?.application == "restart-required") DsTheme.colors.warnText else DsTheme.colors.labelSecondary)
                if (state.busy) FeatureProgress()
            }
        }
        val bundledEntries = bundles.flatMap { it.rows }.mapNotNull { it.entryId }.toSet()
        val standalone = plugins.filter { it.entryId !in bundledEntries && it.moduleName.contains(search, true) }
        val matching = bundles.filter { it.name.contains(search, true) || it.description.orEmpty().contains(search, true) || it.rows.any { row -> row.moduleName.contains(search, true) } }
        if (loaded && matching.isEmpty() && standalone.isEmpty()) EmptyHero(
            stringResource(if (search.isBlank()) R.string.plugins_empty else R.string.harness_automation_no_matches), null)
        listOf(true, false).forEach { installed ->
            val group = matching.filter { (it.availability?.let { location -> location != "missing" } ?: it.installed) == installed }
            if (group.isNotEmpty() || installed && standalone.isNotEmpty()) SectionHeader(stringResource(if (installed) R.string.ux_a_installed else R.string.ux_a_available))
            if (installed) standalone.forEach { plugin -> DsCard { PluginToggle(plugin) } }
            group.forEach { bundle -> DsCard {
                Text(technicalDisplay(bundle.name), style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
                bundle.version?.let { Text(technicalDisplay(it), style = DsType.small13, color = DsTheme.colors.labelSecondary) }
                bundle.description?.let { Text(it, style = DsType.std14, color = DsTheme.colors.labelSecondary) }
                bundle.error?.let { Text(stringResource(pluginReason(it.code)), color = DsTheme.colors.error, style = DsType.small13); FeatureDiagnostics(it.diagnostic) }
                ToggleRow(technicalDisplay(bundle.name), bundle.enabled, bundle.readOnlyReason?.let { stringResource(pluginReason(it)) },
                    enabled = !busy && bundle.readOnlyReason == null) {
                    if (!bundle.enabled && bundle.availability == "missing" && bundle.installTarget != null) {
                        confirm = bundle.catalogInstall(enabled = true)
                    } else run { api ->
                    val result = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.setBundleEnabled(bundle.name, !bundle.enabled) }.featureValue()
                    if (host != null) store.pluginOperations.changed(host, result); revision++
                } }
                bundle.rows.forEach { row ->
                    val plugin = plugins.firstOrNull { it.entryId == row.entryId }
                    if (plugin != null) PluginToggle(plugin)
                    else Text(technicalDisplay(row.moduleName), style = DsType.small13, color = DsTheme.colors.labelSecondary)
                }
                 if (bundle.availability != "installation" && (bundle.installed || bundle.installTarget != null)) FeatureAction(stringResource(if (bundle.availability == "missing") R.string.harness_install_update else R.string.ux_a_update), !busy) {
                    val catalog = bundle.catalogInstall(enabled = bundle.enabled)
                    if (catalog != null) confirm = catalog
                    else { spec = bundle.source ?: bundle.name; registry = ""; inspection = null; installedVersion = bundle.version; installOpen = true }
                }
                if (bundle.removable && bundle.readOnlyReason == null) FeatureAction(stringResource(R.string.common_remove), !busy) { remove = bundle }
            } }
        }
    }
    if (operationOpen) FeaturePage(stringResource(R.string.harness_install_update), { operationOpen = false }) { Operations() }
    if (installOpen) FeaturePage(stringResource(R.string.harness_install_update), { installOpen = false }) {
        FeatureField(spec, { spec = it; inspection = null }, R.string.harness_package, !busy, singleLine = true, technical = true)
        FeatureField(registry, { registry = it; inspection = null }, R.string.harness_registry, !busy, singleLine = true, technical = true)
        installedVersion?.let { Text(stringResource(R.string.ux_a_installed_version, technicalDisplay(it)), style = DsType.small13, color = DsTheme.colors.labelSecondary) }
        FeatureDiagnostics(registries?.let { (listOfNotNull(it.registry ?: it.resolved) + it.fallbackRegistries).distinct().joinToString("\n", transform = ::technicalDisplay) })
        FeatureAction(stringResource(R.string.harness_inspect), !busy && spec.isNotBlank()) { run { api ->
            inspection = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.inspectPlugin(spec.trim(), registry.trim().ifBlank { null }) }.featureValue()
        } }
        inspection?.let { inspected -> DsCard {
            Text(listOfNotNull(inspected.name, inspected.version).joinToString(" · ", transform = ::technicalDisplay), style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
            inspected.description?.let { Text(it, style = DsType.std14, color = DsTheme.colors.labelSecondary) }
            Text(stringResource(if (inspected.status == "accepted") R.string.ux_a_inspected else R.string.ux_a_inspect_notice), style = DsType.small13, color = DsTheme.colors.labelSecondary)
            if (inspected.problem != null || inspected.reason != null) Text(stringResource(pluginReason(inspected.problem)), style = DsType.small13, color = DsTheme.colors.error)
            FeatureDiagnostics(listOfNotNull(inspected.status, inspected.problem, inspected.reason).joinToString("\n"))
        } }
        FeatureAction(stringResource(R.string.harness_install_update), !busy && spec.isNotBlank(), DsButtonVariant.Primary) {
            confirm = PluginInstallSubject(spec.trim(), registry.trim().ifBlank { null })
        }
        if (loading) FeatureProgress()
        Failure()
    }
    if (inventoryOpen) inventory?.let { com.labteto.dshmobile.ui.screens.settings.PluginsSheet(it, onDismiss = { inventoryOpen = false }) }
    confirm?.let { subject -> DsDialog(stringResource(if (subject.approvedBuilds == null) R.string.harness_install_update else R.string.ux_a_approve_retry), { confirm = null }) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
        Text(technicalDisplay(subject.target), style = DsType.std14Strong, color = DsTheme.colors.labelPrimary)
        subject.registry?.let { Text(technicalDisplay(it), style = DsType.small13, color = DsTheme.colors.labelSecondary) }
        subject.approvedBuilds?.forEach { Text(technicalDisplay(it), style = DsType.small13, color = DsTheme.colors.labelPrimary) }
        }
        FeatureAction(stringResource(if (subject.approvedBuilds == null) R.string.harness_install_update else R.string.ux_a_approve_retry), variant = DsButtonVariant.Primary) {
            confirm = null; installOpen = false; operationOpen = true
            if (host != null) store.pluginOperations.install(host, subject.target, subject.registry, subject.approvedBuilds, subject.enabled, subject.saveExact)
        }
        FeatureAction(stringResource(R.string.common_cancel)) { confirm = null }
    } }
    remove?.let { bundle -> DsDialog(stringResource(R.string.common_remove), { remove = null }) {
        Text(technicalDisplay(bundle.name), style = DsType.std14, color = DsTheme.colors.labelPrimary)
        FeatureAction(stringResource(R.string.common_remove), variant = DsButtonVariant.Danger) { remove = null; run { api ->
            val result = store.featureCall(HarnessFeature.PLUGIN_MANAGEMENT, api) { api.removePluginBundle(bundle.name) }.featureValue()
            if (host != null) store.pluginOperations.changed(host, result); revision++
        } }
        FeatureAction(stringResource(R.string.common_cancel)) { remove = null }
    } }
}

