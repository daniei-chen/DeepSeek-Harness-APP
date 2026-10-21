package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import com.labteto.dshmobile.ui.components.*
import com.labteto.dshmobile.ui.theme.*
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.automationHistory
import com.labteto.dshmobile.core.wire.featureValue
import com.labteto.dshmobile.core.wire.dto.AutomationHistory
import com.labteto.dshmobile.core.wire.dto.AutomationTask
import com.labteto.dshmobile.core.wire.dto.DeliveryRecord
import com.labteto.dshmobile.data.HarnessFeature
import com.labteto.dshmobile.data.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Mounted only on the Records tab; leaving it cancels outstanding page requests. */
@Composable
internal fun AutomationRecords(store: SessionStore, host: String?, task: AutomationTask) {
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf<AutomationHistory?>(null) }
    var records by remember { mutableStateOf<List<DeliveryRecord>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var diagnostic by remember { mutableStateOf<String?>(null) }
    var retryBefore by remember { mutableStateOf<String?>(null) }
    val offline = stringResource(R.string.common_offline)
    val loadError = stringResource(R.string.harness_automation_history_error)
    val notFound = stringResource(R.string.harness_automation_history_not_found)
    val cursorError = stringResource(R.string.harness_automation_history_cursor_error)

    suspend fun load(before: String? = null) {
        if (busy) return
        busy = true
        error = null
        diagnostic = null
        retryBefore = before
        try {
            val api = store.apiForHost(host) ?: error(offline)
            val page = store.featureCall(HarnessFeature.AUTOMATION, api) {
                api.automationHistory(task.sessionId, task.id, before)
            }.featureValue()
            if (host != store.activeHostKey) return
            if (page.code != null) {
                // An expired cursor must restart pagination rather than repeat the invalid cursor.
                retryBefore = null
                error = when (page.code) {
                    "schedule_not_found" -> notFound
                    "delivery_cursor_not_found" -> cursorError
                    else -> loadError
                }
                diagnostic = page.code
            } else {
                records = (if (before == null) page.records else records + page.records).distinctBy { it.messageId }
                history = page
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = loadError
            diagnostic = e.message
        } finally {
            busy = false
        }
    }

    LaunchedEffect(host, task.sessionId, task.id) { load() }
    val formatter = scheduleFormatter()
    val locale = LocalConfiguration.current.locales[0]
    if (busy) FeatureProgress()
    error?.let { message ->
        DsCard {
            Text(message, color = DsTheme.colors.error, style = DsType.std14)
            FeatureAction(stringResource(if (message == cursorError) R.string.ux_a_refresh else R.string.common_retry), !busy) { scope.launch { load(retryBefore) } }
            FeatureDiagnostics(diagnostic)
        }
    }
    if (!busy && error == null && history != null && records.isEmpty()) {
        EmptyHero(stringResource(R.string.harness_automation_history_empty), null)
    }
    records.forEach { record ->
        DsCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.xsmall)) {
                Text(formatter.absolute(record.scheduledAt), style = DsType.small13, color = DsTheme.colors.labelSecondary)
                record.prompt?.let { Text(it, style = DsType.std14, color = DsTheme.colors.labelPrimary) }
            }
        }
    }
    history?.let { page ->
        page.nextBefore?.let { before ->
            if (error == null) FeatureAction(stringResource(R.string.harness_more), !busy) { scope.launch { load(before) } }
        }
        val days = (page.retention?.get("days") as? JsonPrimitive)?.intOrNull
        val count = (page.retention?.get("records") as? JsonPrimitive)?.intOrNull
        if (days != null && count != null) {
            Text(stringResource(R.string.ux_a_retention, formatter.amount(days.toLong(), ScheduleUnit.DAY),
                pluralStringResource(R.plurals.ux_a_records, count, java.text.NumberFormat.getIntegerInstance(locale).format(count))),
                style = DsType.small13, color = DsTheme.colors.labelSecondary)
        }
        if (!busy && error == null && page.nextBefore == null) {
            if (page.earlierRecordsUnavailable) Text(stringResource(R.string.harness_automation_history_unavailable), style = DsType.small13, color = DsTheme.colors.labelSecondary)
            if (page.earlierRecordsPruned) Text(stringResource(R.string.harness_automation_history_pruned), style = DsType.small13, color = DsTheme.colors.labelSecondary)
        }
    }
}

