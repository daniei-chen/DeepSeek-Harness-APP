package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonSize
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsCard
import com.labteto.dshmobile.ui.components.EmptyHero
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.components.relativeTime
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import kotlinx.coroutines.launch

/**
 * The archive: every session the reader has put away, searchable, each one a card that can be
 * restored to the drawer.
 *
 * A card reads top-down the way the drawer's rows do: what the session is about, when it was last
 * touched, which workspace it belongs to. The absolute path is the quiet third line — useful for
 * telling two same-named workspaces apart, not something to lead with.
 */
@Composable
internal fun ArchivedSessions(store: SessionStore) {
    val sessions by store.sessions.collectAsStateWithLifecycle()
    val archived by store.archivedSessionIds.collectAsStateWithLifecycle()
    val workspaces by store.workspaces.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = DsTheme.colors
    LaunchedEffect(store.activeHostKey, archived) { store.refreshSessions() }

    ChatSearchField(query) { query = it }

    val rows = sessions
        .filter { it.sessionId in archived }
        .filter { query.isBlank() || it.title.orEmpty().contains(query, true) || it.cwd.orEmpty().contains(query, true) }
        .sortedByDescending { it.updatedAt }

    if (failed) {
        Text(stringResource(R.string.panel_failed), style = DsType.small13, color = colors.error)
    }
    if (rows.isEmpty()) {
        EmptyHero(
            headline = stringResource(R.string.archived_empty),
            subtitle = null
        )
        return
    }

    Text(
        stringResource(R.string.archived_count, rows.size),
        style = DsType.caption11,
        color = colors.labelCaption,
        modifier = Modifier.padding(horizontal = DsSpacing.tiny),
    )

    rows.forEach { row ->
        val workspace = workspaces.firstOrNull { row.sessionId in it.sessionIds }
        val folder = workspace?.title?.ifBlank { null } ?: row.cwd?.let { basenameOf(it) }
        val path = workspace?.path ?: row.cwd
        val restoring = busy == row.sessionId

        DsCard(verticalArrangement = Arrangement.spacedBy(DsSpacing.xsmall)) {
            // Line 1: title, with the time it was last touched on the right.
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    row.title?.takeIf { it.isNotBlank() } ?: row.sessionId,
                    style = DsType.std14Strong,
                    color = colors.labelPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(DsSpacing.small))
                Text(
                    relativeTime(row.updatedAt),
                    style = DsType.caption11,
                    color = colors.labelCaption,
                    maxLines = 1,
                )
            }
            // Line 2: the workspace it lives in, with its path as the quiet detail.
            if (folder != null || path != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        FeatherIcons.Folder,
                        contentDescription = null,
                        tint = colors.labelTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(DsSpacing.xsmall))
                    Column(Modifier.weight(1f)) {
                        folder?.let {
                            Text(it, style = DsType.small13, color = colors.labelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        path?.takeIf { it != folder }?.let {
                            // The whole path, wrapped: it is what tells two same-named
                            // projects apart, so it is never cut short.
                            Text(
                                technicalDisplay(it),
                                style = DsType.caption11,
                                color = colors.labelCaption,
                                softWrap = true,
                            )
                        }
                    }
                }
            }
            // Line 3: the one action.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DsButton(
                    text = stringResource(if (restoring) R.string.common_loading else R.string.archived_restore),
                    onClick = {
                        busy = row.sessionId
                        failed = false
                        scope.launch {
                            try { failed = !store.unarchiveSession(row.sessionId) } finally { busy = null }
                        }
                    },
                    enabled = busy == null,
                    variant = DsButtonVariant.Outline,
                    size = DsButtonSize.Small,
                )
            }
        }
    }
}

/** Last path segment, so a card can name the project rather than print the whole path twice. */
private fun basenameOf(path: String): String =
    path.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifBlank { path }

