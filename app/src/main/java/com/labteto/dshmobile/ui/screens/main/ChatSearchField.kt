package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.theme.DsType

/** Search keeps its focus when cleared and never grows into a multiline editor. */
@Composable
internal fun ChatSearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = { onChange(it.replace('\n', ' ').replace('\r', ' ')) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = DsType.std14,
        label = { Text(stringResource(R.string.common_search)) },
        colors = dialogTextFieldColors(),
        trailingIcon = if (query.isEmpty()) null else {
            { DsIconButton(Icons.Filled.Close, stringResource(R.string.ux_b_clear_search), { onChange("") }) }
        },
    )
}
