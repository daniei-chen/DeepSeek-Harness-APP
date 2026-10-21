package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.dto.LlmConfigurableProvider
import com.labteto.dshmobile.core.wire.dto.SettingsNamespaceView
import com.labteto.dshmobile.core.wire.dto.SettingsPathOpView
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsDialog
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** One model row in the provider editor. */
private data class EditableModel(var id: String = "", var name: String = "")

private val PROTOCOL_IDS = listOf("openai-completions", "openai-responses", "anthropic-messages")

private fun JsonElement?.atPath(path: List<String>): JsonElement? {
    var current = this
    for (segment in path) {
        current = current?.let { element ->
            when (element) {
                is JsonObject -> element[segment]
                is JsonArray -> segment.toIntOrNull()?.let { element.getOrNull(it) }
                else -> null
            }
        }
    }
    return current
}

/**
 * The official model-provider editor: provider basics (endpoint + protocol), the model catalogue,
 * and the write-only API key. Existing providers edit their stored profile in place; the custom
 * form is a create against `llm-pi-ai` under `providers.<route>`.
 */
@Composable
internal fun ProviderEditorDialog(
    provider: LlmConfigurableProvider?,
    store: SessionStore,
    onDismiss: (saved: Boolean) -> Unit,
) {
    val custom = provider == null
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    val ns = provider?.settingsNs ?: "llm-pi-ai"
    var route by remember(provider) { mutableStateOf(provider?.provider ?: "") }
    var displayName by remember(provider) { mutableStateOf(provider?.displayName ?: "") }
    var baseURL by remember(provider) { mutableStateOf("") }
    var protocol by remember(provider) { mutableStateOf(PROTOCOL_IDS.first()) }
    var apiKey by remember(provider) { mutableStateOf("") }
    var revision by remember(provider) { mutableStateOf<Long?>(null) }
    var inheritedKeyRef by remember(provider) { mutableStateOf<String?>(null) }
    val models = remember(provider) { mutableStateListOf<EditableModel>() }
    var loading by remember(provider) { mutableStateOf(true) }
    var saving by remember(provider) { mutableStateOf(false) }
    var error by remember(provider) { mutableStateOf<String?>(null) }
    val path = provider?.settingsPath?.takeIf { it.isNotEmpty() } ?: listOf("providers", route)

    LaunchedEffect(provider) {
        loading = true
        val view: SettingsNamespaceView? = store.settingsNamespace(ns)
        val profile = view?.let { runCatching { it.value.atPath(path) }.getOrNull() } as? JsonObject
        if (profile != null) {
            runCatching { profile["baseURL"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.let { baseURL = it }
            runCatching { profile["api"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.let { protocol = it }
            runCatching { profile["displayName"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.let { displayName = it }
            runCatching { profile["apiKeyEnv"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.let { inheritedKeyRef = it }
            runCatching {
                profile["models"]?.jsonArray?.forEach { row ->
                    val obj = row.jsonObject
                    models.add(
                        EditableModel(
                            id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        ),
                    )
                }
            }
        }
        revision = view?.revision
        loading = false
    }

    fun validate(): String? {
        if (custom && !Regex("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$").matches(route.trim())) {
            return "供应商 ID 需以小写字母开头，只能含小写字母、数字和单个连字符。"
        }
        val url = baseURL.trim()
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            return "API 地址需以 http:// 或 https:// 开头。"
        }
        if ((custom || ns == "llm-pi-ai") && models.none { it.id.isNotBlank() }) {
            return "至少添加一个模型。"
        }
        return null
    }

    fun save() {
        val problem = validate()
        if (problem != null) {
            error = problem
            return
        }
        saving = true
        error = null
        scope.launch {
            val modelJson = buildJsonArray {
                models.filter { it.id.isNotBlank() }.forEach { model ->
                    add(
                        buildJsonObject {
                            put("id", model.id.trim())
                            if (model.name.isNotBlank()) put("name", model.name.trim())
                        },
                    )
                }
            }
            val ops = mutableListOf<SettingsPathOpView>()
            if (custom) {
                val profile = buildJsonObject {
                    if (displayName.isNotBlank()) put("displayName", displayName.trim())
                    if (apiKey.isNotBlank()) put("apiKeyEnv", providerKeyRef(route.trim()))
                    put("api", protocol)
                    put("baseURL", baseURL.trim())
                    put("models", modelJson)
                }
                ops += SettingsPathOpView.Set(listOf("providers", route.trim()), profile)
            } else {
                val base = path
                if (baseURL.isNotBlank()) {
                    ops += SettingsPathOpView.Set(base + "baseURL", JsonPrimitive(baseURL.trim()))
                }
                if (displayName.isNotBlank()) {
                    ops += SettingsPathOpView.Set(base + "displayName", JsonPrimitive(displayName.trim()))
                }
                if (ns == "llm-pi-ai") {
                    ops += SettingsPathOpView.Set(base + "api", JsonPrimitive(protocol))
                    ops += SettingsPathOpView.Set(base + "models", modelJson)
                }
            }
            val written = store.mutateSettings(ns, ops, revision)
            if (written == false) {
                saving = false
                error = "保存失败，请检查 Host 设置是否可写。"
                return@launch
            }
            if (apiKey.isNotBlank()) {
                val ref = inheritedKeyRef ?: providerKeyRef(route.trim())
                val stored = store.saveProviderCredential(ref, apiKey.trim())
                if (stored == false) {
                    saving = false
                    error = "API 密钥保存失败。"
                    return@launch
                }
            }
            saving = false
            onDismiss(true)
        }
    }

    DsDialog(
        title = if (custom) stringResource(R.string.models_custom_provider) else displayName.ifBlank { provider?.displayName.orEmpty() },
        onDismiss = { if (!saving) onDismiss(false) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.small),
        ) {
            if (custom) {
                EditorField(
                    label = stringResource(R.string.provider_route),
                    value = route,
                    placeholder = "例如 my-gateway",
                    onValueChange = { route = it.trim() },
                )
                Text(
                    stringResource(R.string.provider_route_hint),
                    style = DsType.caption11,
                    color = colors.labelTertiary,
                )
            }
            EditorField(
                label = stringResource(R.string.provider_name),
                value = displayName,
                placeholder = null,
                onValueChange = { displayName = it },
            )
            EditorField(
                label = stringResource(R.string.provider_base_url),
                value = baseURL,
                placeholder = "https://…",
                onValueChange = { baseURL = it },
            )
            if (custom || ns == "llm-pi-ai") {
                Text(stringResource(R.string.provider_protocol), style = DsType.xsmall12, color = colors.labelTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PROTOCOL_IDS.forEach { id ->
                        val selected = protocol == id
                        Text(
                            text = when (id) {
                                "openai-completions" -> "OpenAI Completions"
                                "openai-responses" -> "OpenAI Responses"
                                else -> "Anthropic Messages"
                            },
                            style = DsType.caption11,
                            color = if (selected) colors.accent else colors.labelSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (selected) colors.accent else colors.borderL1,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { protocol = id }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.provider_models),
                        style = DsType.xsmall12,
                        color = colors.labelTertiary,
                        modifier = Modifier.weight(1f),
                    )
                    DsButton(
                        text = stringResource(R.string.provider_add_model),
                        onClick = { models.add(EditableModel()) },
                        variant = DsButtonVariant.Ghost,
                    )
                }
                models.forEachIndexed { index, model ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextField(
                            value = model.id,
                            onValueChange = { models[index] = model.copy(id = it) },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.provider_model_id), style = DsType.caption11) },
                            modifier = Modifier.weight(1.2f),
                        )
                        TextField(
                            value = model.name,
                            onValueChange = { models[index] = model.copy(name = it) },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.provider_model_name), style = DsType.caption11) },
                            modifier = Modifier.weight(1f),
                        )
                        DsIconButton(
                            icon = FeatherIcons.Info,
                            contentDescription = stringResource(R.string.common_remove),
                            onClick = { models.removeAt(index) },
                        )
                    }
                }
            }
            EditorField(
                label = stringResource(R.string.models_provider_key),
                value = apiKey,
                placeholder = stringResource(R.string.models_provider_key_hint),
                onValueChange = { apiKey = it },
            )
            error?.let {
                Text(it, style = DsType.caption11, color = colors.error)
            }
            if (loading) {
                Text(stringResource(R.string.common_loading), style = DsType.caption11, color = colors.labelTertiary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DsButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = { if (!saving) onDismiss(false) },
                    variant = DsButtonVariant.Ghost,
                )
                DsButton(
                    text = stringResource(R.string.common_save),
                    enabled = !saving && !loading,
                    onClick = { save() },
                    variant = DsButtonVariant.Info,
                )
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    placeholder: String?,
    onValueChange: (String) -> Unit,
) {
    val colors = DsTheme.colors
    Column {
        Text(label, style = DsType.xsmall12, color = colors.labelTertiary)
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = placeholder?.let { { Text(it, style = DsType.caption11) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Provider route -> credential reference (same derivation the web client uses). */
internal fun providerKeyRef(provider: String): String =
    provider.uppercase().replace(Regex("[^A-Z0-9]+"), "_") + "_API_KEY"

