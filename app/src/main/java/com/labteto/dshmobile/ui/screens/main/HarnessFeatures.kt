package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.core.wire.dto.*
import com.labteto.dshmobile.data.HarnessFeature
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.components.*
import com.labteto.dshmobile.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
internal fun FeaturePage(title: String, onClose: () -> Unit, onRefresh: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    // Full page, not a sheet: automations and plugin management are destinations with their own
    // navigation, and a sheet cannot host their list → detail transitions.
    Surface(Modifier.fillMaxSize(), color = DsTheme.colors.bgBase) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = DsType.std14Strong,
                    color = DsTheme.colors.labelPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (onRefresh != null) {
                    DsIconButton(Icons.Default.Refresh, stringResource(R.string.ux_a_refresh), onRefresh)
                }
                DsIconButton(Icons.Default.Close, stringResource(R.string.common_close), onClose)
            }
            HorizontalDivider(Modifier.fillMaxWidth().height(0.5.dp), color = DsTheme.colors.borderL3)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = DsSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.medium),
                content = content,
            )
        }
    }
}

@Composable
internal fun FeatureField(value: String, onChange: (String) -> Unit, label: Int, enabled: Boolean = true, singleLine: Boolean = false, technical: Boolean = false) {
    TextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, enabled = enabled,
        singleLine = singleLine, textStyle = if (technical) DsType.std14.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr) else DsType.std14,
        colors = dialogTextFieldColors(), shape = DsShapes.block)
}

@Composable
internal fun FeatureProgress() = LinearProgressIndicator(Modifier.fillMaxWidth(), color = DsTheme.colors.accent, trackColor = DsTheme.colors.borderL1)

@Composable
internal fun FeatureAction(text: String, enabled: Boolean = true, variant: DsButtonVariant = DsButtonVariant.Outline, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = DsSpacing.touchTarget), contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
        DsButton(text, onClick, enabled = enabled, variant = variant, allowWrap = true)
    }
}

@Composable
internal fun FeatureDiagnostics(text: String?) {
    if (text.isNullOrBlank()) return
    var expanded by remember { mutableStateOf(false) }
    DisclosureRow(title = stringResource(R.string.ux_a_details), expanded = expanded, onToggle = { expanded = !expanded },
        modifier = Modifier.heightIn(min = DsSpacing.touchTarget)) {
        Text(text, style = DsType.caption11, color = DsTheme.colors.labelTertiary)
    }
}

@Composable
internal fun scheduleFormatter(): ScheduleFormatter {
    val resources = LocalContext.current.resources
    val locale = LocalConfiguration.current.locales[0]
    return remember(resources, locale) { ScheduleFormatter(locale, ZoneId.systemDefault(),
        { id, args -> resources.getString(id, *args.toTypedArray()) },
        { id, count, number -> resources.getQuantityString(id, count, number) }) }
}

@Composable
internal fun ScheduleSummary(task: AutomationTask) {
    val formatter = scheduleFormatter()
    Text(formatter.frequency(task), style = DsType.small13, color = DsTheme.colors.labelSecondary)
    formatter.runSummary(task)?.let { Text(it, style = DsType.caption11, color = DsTheme.colors.labelTertiary) }
    DsPill(stringResource(if (task.status == "active") R.string.harness_active else R.string.harness_inactive))
}

/** New tasks are drafted through the agent; the management API has no create endpoint. */
internal suspend fun SessionStore.stageFeatureDraft(host: String, text: String, preset: String? = null): Boolean {
    val api = apiForHost(host) ?: return false
    val id = api.sessionCreate(SessionCreateRequest()).featureValue().sessionId
    if (activeHostKey != host) return false
    if (preset != null) api.agentPresetSelect(id, preset).featureValue()
    if (activeHostKey != host) return false
    composers.get(ComposerKey(host, id)).text = text
    openSession(id)
    return activeHostKey == host && currentSessionId.value == id
}

@Composable
internal fun AutomationScreen(sessionId: String? = null, onClose: () -> Unit, onUnsupported: () -> Unit = onClose) {
    val store = rememberSessionStore()
    val available by store.automationAvailable.collectAsStateWithLifecycle()
    // MainScreen owns the single unavailable toast and resets its open state.
    LaunchedEffect(available) { if (available == false) onUnsupported() }
    if (available == false) return
    val connection by store.connectionState.collectAsStateWithLifecycle()
    val host = store.activeHostKey
    val scope = rememberCoroutineScope()
    var tasks by remember(host, sessionId) { mutableStateOf<List<AutomationTask>>(emptyList()) }
    var selected by remember(host, sessionId) { mutableStateOf<AutomationTask?>(null) }
    var search by remember(host, sessionId) { mutableStateOf("") }
    var statusFilter by remember(host, sessionId) { mutableStateOf(AutomationStatusFilter.ALL) }
    val listModel = remember(tasks, search, statusFilter) { automationListModel(tasks, search, statusFilter) }
    var error by remember(host) { mutableStateOf<String?>(null) }
    var busy by remember(host) { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val offline = stringResource(R.string.common_offline)
    val newPrompt = stringResource(R.string.harness_automation_draft)
    suspend fun refresh() {
        val api = store.apiForHost(host) ?: error(offline)
        val result = store.featureCall(HarnessFeature.AUTOMATION, api) { api.automationCatalog() }.featureValue()
        if (host == store.activeHostKey) tasks = result.filter { sessionId == null || it.sessionId == sessionId }
    }
    fun run(block: suspend () -> Unit) { scope.launch {
        busy = true; error = null
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: offline }
        finally { busy = false }
    } }
    LaunchedEffect(host, sessionId, connection.phase, revision) { run { refresh() } }
    LaunchedEffect(host) { store.hostEvents.collect { if (it.first == "schedule/changed") revision++ } }
    FeaturePage(stringResource(R.string.harness_automations), onClose, onRefresh = { if (!busy) revision++ }) {
        FeatureAction(stringResource(R.string.harness_new), !busy, DsButtonVariant.Primary) { run {
            if (host != null && store.stageFeatureDraft(host, newPrompt)) onClose() else error(offline)
        } }
        FeatureField(search, { search = it }, R.string.harness_automation_search, singleLine = true)
        DsSegmented(AutomationStatusFilter.entries.map { filter -> DsSegment(filter.name, stringResource(when (filter) {
            AutomationStatusFilter.ALL -> R.string.harness_automation_all
            AutomationStatusFilter.ACTIVE -> R.string.harness_active
            AutomationStatusFilter.INACTIVE -> R.string.harness_inactive
        })) }, statusFilter.name, { statusFilter = AutomationStatusFilter.valueOf(it) }, stretch = true,
            minSegmentHeight = DsSpacing.touchTarget, maxLabelLines = Int.MAX_VALUE)
        if (busy) FeatureProgress()
        error?.let { diagnostic -> DsCard {
            Text(stringResource(R.string.ux_a_load_error), color = DsTheme.colors.error, style = DsType.std14)
            FeatureAction(stringResource(R.string.common_retry), !busy) { run { refresh() } }
            FeatureDiagnostics(diagnostic)
        } }
        if (!busy && error == null) listModel.emptyState?.let { empty ->
            EmptyHero(stringResource(when (empty) {
                AutomationEmptyState.NO_TASKS -> R.string.harness_automation_empty
                AutomationEmptyState.NO_MATCHES -> R.string.harness_automation_no_matches
                AutomationEmptyState.NO_INACTIVE_TASKS -> R.string.harness_automation_empty_inactive
            }), null)
        }
        listModel.tasks.forEach { task ->
            DsCard(onClick = { selected = task }) {
                Text(task.title, style = DsType.std14Strong, color = DsTheme.colors.labelPrimary,
                    textDecoration = if (task.status == "inactive") TextDecoration.LineThrough else null)
                Text(task.prompt, maxLines = 3, style = DsType.std14, color = DsTheme.colors.labelSecondary)
                ScheduleSummary(task)
            }
        }
    }
    selected?.let { task -> key(host, task.sessionId, task.id) {
        AutomationEditor(store, host, task, onClose = { selected = null; revision++ }, onSessionOpened = onClose)
    } }
}

@Composable
private fun AutomationEditor(store: SessionStore, host: String?, task: AutomationTask, onClose: () -> Unit, onSessionOpened: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember(task) { mutableStateOf(task.title) }
    var prompt by remember(task) { mutableStateOf(task.prompt) }
    var timing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TimingEditorDraft.from(task)) }
    var tab by remember(task.sessionId, task.id) { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var diagnostics by remember { mutableStateOf<String?>(null) }
    val sessions by store.sessions.collectAsStateWithLifecycle()
    val archived by store.archivedSessionIds.collectAsStateWithLifecycle()
    val connection by store.connectionState.collectAsStateWithLifecycle()
    val linkMessage = when {
        connection.phase == com.labteto.dshmobile.connection.ConnectionPhase.DISCONNECTED -> R.string.common_offline
        connection.phase != com.labteto.dshmobile.connection.ConnectionPhase.CONNECTED -> R.string.common_loading
        task.sessionId in archived -> R.string.ux_a_session_archived
        sessions.none { it.sessionId == task.sessionId } -> R.string.ux_a_session_unavailable
        else -> null
    }
    val offline = stringResource(R.string.common_offline)
    val failure = stringResource(R.string.ux_a_action_error)
    val saveFailure = stringResource(R.string.ux_a_save_error)
    val deleteFailure = stringResource(R.string.ux_a_delete_error)
    val conflict = stringResource(R.string.harness_conflict)
    val notFound = stringResource(R.string.harness_automation_history_not_found)
    val active = task.status == "active"
    fun run(errorText: String = failure, block: suspend (DshApiClient) -> Unit) { scope.launch {
        busy = true; message = null; diagnostics = null
        try { block(store.apiForHost(host) ?: error(offline)) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = errorText; diagnostics = e.message }
        finally { busy = false }
    } }
    FeaturePage(task.title, onClose) {
        DsSegmented(listOf(DsSegment("0", stringResource(R.string.harness_automation_rules)), DsSegment("1", stringResource(R.string.harness_automation_records))),
            tab.toString(), { tab = it.toInt() }, enabled = !busy, stretch = true,
            minSegmentHeight = DsSpacing.touchTarget, maxLabelLines = Int.MAX_VALUE)
        if (tab == 1) AutomationRecords(store, host, task) else {
            FeatureField(title, { title = it }, R.string.harness_title, active && !busy)
            FeatureField(prompt, { prompt = it }, R.string.harness_prompt, active && !busy)
            ScheduleSummary(task)
            FeatureAction(stringResource(R.string.harness_open_session), !busy && linkMessage == null) { run {
                if (host == store.activeHostKey) {
                    store.openSession(task.sessionId)
                    if (host == store.activeHostKey && store.currentSessionId.value == task.sessionId) onSessionOpened()
                }
            } }
            linkMessage?.let { Text(stringResource(it), style = DsType.small13, color = DsTheme.colors.labelSecondary) }
            if (active) {
                ToggleRow(stringResource(R.string.harness_change_timing), timing, enabled = !busy) { timing = !timing }
                if (timing) AutomationTimingFields(draft, { draft = it }, !busy)
                FeatureAction(stringResource(R.string.common_save), !busy && title.trim().isNotEmpty() && title.trim().length <= 120 && prompt.trim().isNotEmpty() && (!timing || draft.valid), DsButtonVariant.Primary) { run(saveFailure) { api ->
                    val result = store.featureCall(HarnessFeature.AUTOMATION, api) { api.automationUpdate(AutomationUpdate(task.sessionId, task.id, task.expectedRecord(), title.trim(), prompt.trim(), if (timing) draft.change() else null)) }.featureValue()
                    if (result.code == null && result.record != null) onClose() else {
                        message = when (result.code) { "schedule_conflict" -> conflict; "schedule_not_found" -> notFound; else -> saveFailure }
                        diagnostics = listOfNotNull(result.code, result.message).joinToString("\n")
                    }
                } }
                FeatureAction(stringResource(R.string.common_delete), !busy, DsButtonVariant.Danger) { deleteConfirm = true }
            }
            if (busy) FeatureProgress()
            message?.let { DsCard {
                Text(it, color = DsTheme.colors.error, style = DsType.std14)
                FeatureAction(stringResource(R.string.ux_a_refresh), !busy, onClick = onClose)
                FeatureDiagnostics(diagnostics)
            } }
        }
    }
    if (deleteConfirm) DsDialog(stringResource(R.string.common_delete), { deleteConfirm = false }) {
        Text(task.title, style = DsType.std14, color = DsTheme.colors.labelPrimary)
        FeatureAction(stringResource(R.string.common_delete), variant = DsButtonVariant.Danger) { deleteConfirm = false; run(deleteFailure) { api ->
            val result = store.featureCall(HarnessFeature.AUTOMATION, api) { api.automationDelete(task.sessionId, task.id) }.featureValue()
            if (result.deleted == true) onClose() else { message = if (result.code == "schedule_not_found") notFound else deleteFailure; diagnostics = listOfNotNull(result.code, result.message).joinToString("\n") }
        } }
        FeatureAction(stringResource(R.string.common_cancel)) { deleteConfirm = false }
    }
}




