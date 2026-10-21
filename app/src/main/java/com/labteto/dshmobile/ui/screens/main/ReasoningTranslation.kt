package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.ui.components.MarkdownText
import com.labteto.dshmobile.ui.media.LocalAttachmentScope
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.theme.DsTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Source stays visible until an explicit request completes; absent plugins add no controls. */
@Composable
internal fun ReasoningContent(text: String, streaming: Boolean) {
    val owner = LocalAttachmentScope.current
    key(owner, text) { ReasoningContentState(text, streaming) }
}

@Composable
private fun ReasoningContentState(text: String, streaming: Boolean) {
    val store = rememberSessionStore()
    val owner = LocalAttachmentScope.current
    val api = remember(owner) { store.apiForHost(owner?.first) }
    val scope = rememberCoroutineScope()
    val language = LocalConfiguration.current.locales[0].toLanguageTag()
    var limits by remember(owner) { mutableStateOf<TranslationLimits?>(null) }
    var translated by remember(owner, text) { mutableStateOf<String?>(null) }
    var showTranslation by remember(owner, text) { mutableStateOf(false) }
    var busy by remember(owner, text) { mutableStateOf(false) }
    var failed by remember(owner, text) { mutableStateOf(false) }
    LaunchedEffect(api) {
        try { limits = (api?.translationLimits() as? RpcResult.Ok)?.value }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { limits = null }
    }
    Column {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text((if (showTranslation) translated ?: text else text).trimEnd(),
                style = com.labteto.dshmobile.ui.theme.DsType.small13,
                color = DsTheme.colors.labelTertiary, softWrap = true)
        }
        val metadata = limits
        if (!streaming && api != null && owner != null && metadata != null &&
            metadata.maxTextChars >= 2 && metadata.preferences.provider in metadata.availableProviders && text.isNotBlank()) {
            TextButton(enabled = !busy, onClick = {
                if (translated != null) showTranslation = !showTranslation
                else scope.launch {
                    busy = true; failed = false
                    try {
                        val target = metadata.preferences.targetLanguage.takeUnless { it == "auto" } ?: language
                        val parts = mutableListOf<String>()
                        for (fragment in translationFragments(text, metadata.maxTextChars)) {
                            parts += api.translateReasoning(owner.second, fragment, target, metadata.preferences.provider).featureValue()
                        }
                        translated = parts.joinToString("")
                        showTranslation = true
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { failed = true }
                    finally { busy = false }
                }
            }) {
                Text(if (busy) stringResource(R.string.common_loading) else if (showTranslation)
                    stringResource(R.string.alpha2_original) else stringResource(R.string.alpha2_translate, metadata.preferences.provider))
            }
            if (failed) Text(stringResource(R.string.common_error), color = DsTheme.colors.error)
        }
    }
}
