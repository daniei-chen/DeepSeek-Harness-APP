package com.labteto.dshmobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import com.labteto.dshmobile.ui.theme.DshTheme

/** Section title row with an optional right-aligned accent action. */
@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    /** Web session-list style: 11px tertiary label on a 32dp row. */
    compact: Boolean = false,
) {
    val colors = DsTheme.colors
    Row(
        modifier = if (compact) {
            Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
        } else {
            Modifier.fillMaxWidth()
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = if (compact) DsType.caption11Strong else DsType.std14Strong,
            color = if (compact) colors.labelTertiary else colors.labelSecondary,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                action,
                style = DsType.caption11Strong,
                color = colors.accent,
                modifier = if (onAction != null) {
                    Modifier.clickable(onClick = onAction).padding(4.dp)
                } else {
                    Modifier.padding(4.dp)
                },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SectionHeaderPreview() {
    DshTheme {
        SectionHeader(
            title = "Tool calls",
            action = "Clear",
            onAction = {},
        )
    }
}
