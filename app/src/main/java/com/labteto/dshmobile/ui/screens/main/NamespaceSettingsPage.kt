package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * The plugin-contributed settings pages the web client renders from `settings/describe`.
 *
 * The host exposes each one as a namespace with a section value; this build edits that value as
 * JSON rather than reconstructing five bespoke forms whose schemas differ per deployment. It is
 * the same data the web pages write, just a lower-level surface.
 */
@Composable
internal fun NamespaceSettingsPage(
    title: String,
    namespaces: List<String>,
    store: SessionStore,
    onBack: () -> Unit,
) {
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    var ns by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableStateOf<Long?>(null) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val pretty = remember { Json { prettyPrint = true } }

    suspend fun load() {
        var found: com.labteto.dshmobile.core.wire.dto.SettingsNamespaceView? = null
        for (candidate in namespaces) {
            found = store.settingsNamespace(candidate)
            if (found != null) break
        }
        if (found == null) {
            error = "Host 没有暴露 ${namespaces.joinToString(" / ")} 命名空间。"
            return
        }
        ns = found.ns
        revision = found.revision
        text = pretty.encodeToString(JsonObject.serializer(), (found.user ?: found.value) as? JsonObject ?: JsonObject(emptyMap()))
    }
    LaunchedEffect(namespaces) { load() }

    com.labteto.dshmobile.ui.components.DsDialog(title = title, onDismiss = onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.small),
        ) {
            Text(
                stringResource(R.string.advanced_settings_json_hint),
                style = DsType.caption11,
                color = colors.labelTertiary,
            )
            TextField(
                value = text,
                onValueChange = { text = it; saved = false },
                minLines = 10,
                maxLines = 24,
                textStyle = DsType.small13.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, style = DsType.caption11, color = colors.error) }
            if (saved) {
                Text(stringResource(R.string.advanced_settings_saved), style = DsType.caption11, color = colors.success)
            }
            val currentNs = ns
            com.labteto.dshmobile.ui.components.DsButton(
                text = if (saving) stringResource(R.string.common_saving) else stringResource(R.string.common_save),
                enabled = currentNs != null && !saving,
                onClick = {
                    error = null
                    saving = true
                    scope.launch {
                        val parsed = runCatching { pretty.parseToJsonElement(text) as? JsonObject }.getOrNull()
                        if (parsed == null) {
                            saving = false
                            error = "JSON 格式不正确。"
                            return@launch
                        }
                        val target = currentNs ?: return@launch
                        val ok = store.replaceSettings(target, parsed, revision)
                        saving = false
                        if (ok == true) {
                            saved = true
                            load()
                        } else {
                            error = "保存失败，请确认 Host 设置是否可写。"
                        }
                    }
                },
                variant = DsButtonVariant.Info,
            )
            DsButton(
                text = stringResource(R.string.common_close),
                onClick = onBack,
                variant = DsButtonVariant.Ghost,
            )
        }
    }
}

