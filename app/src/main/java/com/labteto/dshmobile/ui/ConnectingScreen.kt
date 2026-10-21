package com.labteto.dshmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.WhaleMark
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType

/**
 * Fork: shown while the saved host is being reconnected on launch.
 *
 * Before this, a cold start drew the whole connect form for a beat and then replaced it with the
 * chat — the form flashing under a connection the user never asked to configure. This is the
 * branded "opening your harness" state instead, with a way out if the automatic attempt stalls.
 */
@Composable
fun ConnectingScreen(host: String?, onManual: () -> Unit) {
    val colors = DsTheme.colors
    Surface(modifier = Modifier.fillMaxSize(), color = colors.bgBase) {
        Column(
            modifier = Modifier.fillMaxSize().padding(DsSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WhaleMark(Modifier.size(68.dp))
            Spacer(Modifier.height(DsSpacing.medium))
            Text(
                stringResource(R.string.app_long_name),
                style = DsType.large20,
                color = colors.labelPrimary,
            )
            Spacer(Modifier.height(DsSpacing.small))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.accent,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(DsSpacing.small))
            Text(
                stringResource(R.string.connecting_title),
                style = DsType.small13,
                color = colors.labelTertiary,
            )
            if (!host.isNullOrBlank()) {
                Text(
                    host,
                    style = DsType.caption11,
                    color = colors.labelCaption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(DsSpacing.large))
            DsButton(
                text = stringResource(R.string.connecting_manual),
                onClick = onManual,
                variant = DsButtonVariant.Ghost,
            )
        }
    }
}
