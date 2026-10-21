package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.components.MarkdownText
import com.labteto.dshmobile.ui.components.DsCard
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.constructor.SafeConstructor

internal data class Frontmatter(val fields: List<Pair<String, String>>, val body: String)

/** Safe YAML values only; malformed, oversized and unterminated headers stay visible as source. */
internal fun documentFrontmatter(text: String): Frontmatter? {
    val match = Regex("\\A\\uFEFF?---[ \\t]*\\r?\\n([\\s\\S]*?)\\r?\\n(?:---|\\.\\.\\.)[ \\t]*(?:\\r?\\n|$)").find(text) ?: return null
    val header = match.groupValues[1]
    if (header.length > 16_384) return null
    return runCatching {
        val options = LoaderOptions().apply { maxAliasesForCollections = 0; codePointLimit = 16_384; nestingDepthLimit = 16; isAllowDuplicateKeys = false }
        val value = Yaml(SafeConstructor(options)).load<Any?>(header) as? Map<*, *> ?: return null
        fun display(value: Any?, depth: Int = 0): String {
            require(depth <= 16)
            return when(value) {
                null -> "null"
                is Map<*, *> -> value.entries.joinToString("\n") { "${it.key}: ${display(it.value, depth + 1)}" }
                is List<*> -> value.joinToString(", ") { display(it, depth + 1) }
                else -> value.toString()
            }
        }
        Frontmatter(value.entries.map { it.key.toString() to display(it.value) }, text.substring(match.range.last + 1))
    }.getOrNull()
}

@Composable
internal fun DocumentMarkdown(text: String) {
    val frontmatter = remember(text) { documentFrontmatter(text) }
    var expanded by remember(text) { mutableStateOf(false) }
    Column {
        frontmatter?.let { metadata ->
            DsCard {
                DsButton(
                    stringResource(R.string.harness_frontmatter), { expanded = !expanded },
                    variant = DsButtonVariant.Ghost,
                    icon = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                )
                if (expanded) Column(
                    Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.small),
                ) {
                    metadata.fields.forEach { (key, value) ->
                        Column {
                            Text(key, style = DsType.caption11, color = DsTheme.colors.labelTertiary)
                            Text(value, style = DsType.std14, color = DsTheme.colors.labelPrimary)
                        }
                    }
                }
            }
        }
        MarkdownText(frontmatter?.body ?: text)
    }
}
