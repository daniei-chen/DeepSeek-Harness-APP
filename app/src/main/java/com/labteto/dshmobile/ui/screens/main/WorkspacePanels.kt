package com.labteto.dshmobile.ui.screens.main

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.RpcResult
import com.labteto.dshmobile.core.wire.dto.*
import com.labteto.dshmobile.data.SessionStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonSize
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.components.DsPill
import com.labteto.dshmobile.ui.components.DsSegment
import com.labteto.dshmobile.ui.components.DsSegmented
import com.labteto.dshmobile.ui.components.EmptyHero
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsType
import androidx.compose.ui.unit.sp
import com.labteto.dshmobile.ui.components.DsCard
import com.labteto.dshmobile.ui.components.MarkdownText
import com.labteto.dshmobile.ui.theme.DsTheme
import kotlinx.coroutines.*
import java.io.File

internal fun <T> RpcResult<T>.requireValue(): T = when (this) {
    is RpcResult.Ok -> value
    is RpcResult.Err -> throw IllegalStateException("${error.code}: ${error.message}")
}

@Composable
internal fun WorkspacePanels(
    store: SessionStore,
    state: PanelState,
    onDismiss: () -> Unit,
    onReference: ((String) -> Unit)? = null,
) {
    val colors = DsTheme.colors
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    // Close through the exit animation; the Dialog disappears the moment the parent drops it.
    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(200)
            onDismiss()
        }
    }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.bgBase) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            ) {
                WorkspacePanelScaffold(
                    store,
                    state,
                    ::close,
                    onReference?.let { reference ->
                        { path ->
                            reference(path)
                            close()
                        }
                    },
                )
            }
        }
    }
}

/**
 * The panel without its modal shell, so a wide window can dock it beside the conversation the way
 * the desktop client does. On a phone this is only ever reached through [WorkspacePanels].
 */
@SuppressLint("ModifierParameter")
@Composable
internal fun WorkspacePanelScaffold(
    store: SessionStore,
    state: PanelState,
    onDismiss: () -> Unit,
    onReference: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val sessions by store.sessions.collectAsStateWithLifecycle()
    val cwd = sessions.firstOrNull { it.sessionId == state.key.sessionId }?.cwd
    Column(modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DsSpacing.small, vertical = DsSpacing.xsmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DsIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onDismiss,
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.panel_files), style = DsType.large20, color = colors.labelPrimary)
                cwd?.let {
                    Text(technicalDisplay(it), style = DsType.caption11, color = colors.labelCaption, softWrap = true)
                }
            }
        }
        WorkspacePanelBody(store, state, Modifier.fillMaxSize(), onReference = onReference)
    }
}

/**
 * The workspace browser itself — Files / Preview / Terminal — without the screen's header, which
 * [WorkspacePanels] draws around it.
 */
@Composable
internal fun WorkspacePanelBody(
    store: SessionStore,
    state: PanelState,
    modifier: Modifier = Modifier,
    onReference: ((String) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val key = state.key
    // Local search over the loaded directory. The host list is truncated for huge folders, so this
    // is a convenience filter, not a filesystem-wide search.
    var fileQuery by remember(key) { mutableStateOf("") }
    val activeDocument = state.previews.getOrNull(state.selectedPreview)?.path
    fun listDirectory(path: String) {
        if (state.busy) return
        state.busy = true; state.error = null
        scope.launch {
            try {
                val api = store.apiForHost(key.host) ?: error(context.getString(R.string.common_offline))
                when (val result = api.workspaceFileList(key.sessionId, path)) {
                    is RpcResult.Ok -> { state.listing = result.value; state.directory = path }
                    // A reference guessed to be a folder (see isFolderReference) that names a
                    // file: show the file, and leave Files on the folder that holds it.
                    is RpcResult.Err -> if (result.error.code == "workspace-file/not-directory") {
                        state.directory = path.substringBeforeLast('/', ".").ifEmpty { "." }
                        state.listing = null
                        state.open(path)
                    } else result.requireValue()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.error = e.message }
            finally { state.busy = false }
        }
    }
    // Keyed on the directory and on a missing listing too: `browse()` clears the listing and may
    // set a new directory, and either must fetch even though the session is unchanged.
    LaunchedEffect(key, state.directory, state.listing == null) { if (state.listing == null) listDirectory(state.directory) }
    // Invalidate previews when the host reports a file observation. OS-only changes are checked
    // by stat each time a tab opens and by the explicit Refresh action.
    //
    // Harness 0.1.7 watches one named target per stream rather than the whole workspace, so each
    // open preview gets a watch of its own; the set follows the tabs. A 0.1.6 host refuses the
    // `path` argument and is watched workspace-wide instead, once.
    val mux = store.muxForHost(key.host)
    val watched = state.previews.map { it.path }.distinct()
    var workspaceWide by remember(key, mux) { mutableStateOf(false) }
    fun invalidate(raw: kotlinx.serialization.json.JsonElement) {
        val frame = com.labteto.dshmobile.core.wire.decodeFromJsonElement(WorkspaceFileWatchFrame.serializer(), raw)
        frame.change?.let { change ->
            state.previews.filter { it.stat?.absolutePath == change.absolutePath }.forEach {
                if (change.absent || it.stat?.version != change.version) { it.stat = null; it.bytes = null; it.text = null }
            }
        }
    }
    if (mux != null && !workspaceWide) {
        watched.forEach { path -> key(path) {
            LaunchedEffect(key, mux, path) {
                try {
                    mux.openStream("workspaceFiles/changes", kotlinx.serialization.json.buildJsonObject {
                        put("workspaceFileScopeId", kotlinx.serialization.json.JsonPrimitive(key.sessionId))
                        put("path", kotlinx.serialization.json.JsonPrimitive(path))
                    }).collect { invalidate(it) }
                } catch (e: CancellationException) { throw e }
                catch (e: com.labteto.dshmobile.core.wire.RemoteStreamException) {
                    if (e.error.code == "gateway/arguments-invalid") workspaceWide = true
                }
                catch (_: Exception) { /* File reads remain available without the optional observation feed. */ }
            }
        } }
    }
    if (mux != null && workspaceWide) {
        LaunchedEffect(key, mux) {
            try {
                mux.openStream("workspaceFiles/changes", kotlinx.serialization.json.buildJsonObject {
                    put("workspaceFileScopeId", kotlinx.serialization.json.JsonPrimitive(key.sessionId))
                }).collect { invalidate(it) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* As above: the feed is an optimisation, not a dependency. */ }
        }
    }
    // A link in a previewed document is relative to that document, folders as much as files.
    fun resolve(path: String) = activeDocument?.let { com.labteto.dshmobile.core.session.resolvePreviewReference(it, path) } ?: path
    CompositionLocalProvider(
        com.labteto.dshmobile.ui.components.LocalFileOpener provides { path: String -> state.open(resolve(path)) },
        com.labteto.dshmobile.ui.components.LocalFolderOpener provides { path: String -> state.browse(resolve(path)) },
    ) {
            val colors = DsTheme.colors
            Column(modifier.padding(horizontal = DsSpacing.medium), verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
                DsSegmented(
                    segments = listOf(
                        DsSegment("0", stringResource(R.string.panel_files)),
                        DsSegment("1", stringResource(R.string.panel_preview) + state.previews.size.takeIf { it > 0 }?.let { " · $it" }.orEmpty()),
                        DsSegment("2", stringResource(R.string.panel_terminal)),
                    ),
                    selectedKey = state.section.toString(),
                    onSelect = { state.section = it.toInt() },
                    role = Role.Tab,
                    stretch = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (state.section) {
                    0 -> {
                        // Toolbar: where you are, and the two moves that are always meaningful.
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.xsmall)) {
                            DsButton(
                                text = stringResource(R.string.panel_root),
                                icon = FeatherIcons.Folder,
                                onClick = { listDirectory(".") },
                                enabled = !state.busy && state.directory != ".",
                                variant = DsButtonVariant.Outline,
                                size = DsButtonSize.Small,
                            )
                            DsButton(
                                text = stringResource(R.string.panel_parent),
                                icon = FeatherIcons.CornerLeftUp,
                                onClick = { listDirectory(state.directory.substringBeforeLast('/', ".").ifEmpty { "." }) },
                                enabled = !state.busy && state.directory != ".",
                                variant = DsButtonVariant.Outline,
                                size = DsButtonSize.Small,
                            )
                            Spacer(Modifier.weight(1f))
                            DsIconButton(
                                icon = FeatherIcons.RefreshCw,
                                contentDescription = stringResource(R.string.common_retry),
                                onClick = { listDirectory(state.directory) },
                                enabled = !state.busy,
                            )
                        }
                        Text(
                            technicalDisplay(if (state.directory == ".") "/" else state.directory),
                            style = DsType.caption11,
                            color = colors.labelCaption,
                            softWrap = true,
                        )
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.borderL1)
                        state.error?.let { Text(it, style = DsType.small13, color = colors.error) }
                        if (state.listing?.truncated == true) {
                            Text(stringResource(R.string.panel_truncated), style = DsType.caption11, color = colors.warnLabel)
                        }
                        val allEntries = state.listing?.entries.orEmpty()
                        if (allEntries.size > 6 || fileQuery.isNotBlank()) {
                            ChatSearchField(fileQuery, { fileQuery = it })
                        }
                        val entries = allEntries
                            .filter { fileQuery.isBlank() || it.name.contains(fileQuery, ignoreCase = true) }
                            .sortedWith(compareBy({ it.type != "directory" }, { it.name.lowercase() }))
                        if (!state.busy && state.listing != null && entries.isEmpty() && state.error == null) {
                            Text(
                                stringResource(
                                    if (fileQuery.isNotBlank()) R.string.chatlist_search_empty else R.string.panel_empty_folder,
                                ),
                                style = DsType.std14,
                                color = colors.labelTertiary,
                                modifier = Modifier.padding(DsSpacing.medium),
                            )
                        }
                        LazyColumn(
                            Modifier.weight(1f),
                            state = state.directoryScroll.getOrPut(state.directory) { androidx.compose.foundation.lazy.LazyListState() },
                            verticalArrangement = Arrangement.spacedBy(DsSpacing.tiny),
                        ) {
                            items(entries, key = { it.name }) { entry ->
                                val directory = entry.type == "directory"
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(DsShapes.row)
                                        .clickable(enabled = !state.busy) {
                                            val path = if (state.directory == ".") entry.name else "${state.directory}/${entry.name}"
                                            if (directory) listDirectory(path) else state.open(path)
                                        }
                                        .padding(horizontal = DsSpacing.small, vertical = DsSpacing.xsmall),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.small),
                                ) {
                                    Icon(
                                        if (directory) FeatherIcons.Folder else FeatherIcons.FileText,
                                        null,
                                        Modifier.size(18.dp),
                                        tint = if (directory) colors.accent else colors.labelTertiary,
                                    )
                                    Text(
                                        technicalDisplay(entry.name),
                                        style = DsType.std14,
                                        color = colors.labelPrimary,
                                        modifier = Modifier.weight(1f),
                                        softWrap = true,
                                    )
                                    if (directory) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = colors.labelCaption)
                                    } else {
                                        entry.size?.let { Text(formatBytes(it), style = DsType.caption11, color = colors.labelCaption) }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (state.previews.isEmpty()) {
                            EmptyHero(headline = stringResource(R.string.panel_preview_empty), subtitle = stringResource(R.string.panel_preview_hint))
                        } else {
                            // Open files as pills; the live one is selected.
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.xsmall)) {
                                state.previews.forEachIndexed { i, preview ->
                                    DsPill(
                                        text = technicalDisplay(preview.path.substringAfterLast('/').substringAfterLast('\\')),
                                        selected = i == state.selectedPreview,
                                        onClick = { state.selectedPreview = i },
                                    )
                                }
                            }
                            val index = state.selectedPreview.coerceIn(0, state.previews.lastIndex)
                            val preview = state.previews[index]
                            key(preview) {
                                DocumentPreview(
                                    store, key, preview, Modifier.weight(1f),
                                    onClose = { state.previews.removeAt(index); state.selectedPreview = (index - 1).coerceAtLeast(0) },
                                    onDirectory = {
                                        state.previews.remove(preview); state.selectedPreview = (index - 1).coerceAtLeast(0)
                                        state.browse(preview.path)
                                    },
                                    onReference = onReference?.let { ref -> { ref(preview.path) } },
                                )
                            }
                        }
                    }
                    2 -> TerminalPanel(store, state, Modifier.weight(1f))
                }
            }
    }
}

@Composable
private fun DocumentPreview(
    store: SessionStore, key: ComposerKey, tab: PreviewTab, modifier: Modifier,
    onClose: () -> Unit, onDirectory: () -> Unit, onReference: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = DsTheme.colors
    // "Open on host" needs a desktop on the harness computer. A headless host (a server behind a
    // tunnel, a container) says so up front through `canOpenWorkspacePath`, and the button is
    // withheld rather than offered and failed. Null while the answer is still in flight.
    var canOpenOnHost by remember(key.host) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(key.host) {
        canOpenOnHost = runCatching {
            store.apiForHost(key.host)?.sessionCanOpenWorkspacePath()?.let { (it as? RpcResult.Ok)?.value } ?: false
        }.getOrDefault(false)
    }
    val openFailed = stringResource(R.string.panel_open_host_failed)
    val extension = tab.path.substringAfterLast('.', "").lowercase()
    val binary = extension in setOf("png", "jpg", "jpeg", "webp", "gif", "pdf")
    fun load(more: Boolean = false) {
        if (tab.busy) return
        tab.busy = true; tab.error = null
        scope.launch {
            try {
                val api = store.apiForHost(key.host) ?: error(context.getString(R.string.common_offline))
                val statResult = api.workspaceFileStat(key.sessionId, tab.path)
                // A reference guessed to be a file that names a folder (`.github`): browse it.
                if (statResult is RpcResult.Err && statResult.error.code == "workspace-file/not-regular-file" &&
                    (statResult.error.details as? kotlinx.serialization.json.JsonObject)?.get("kind")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } == "directory"
                ) { onDirectory(); return@launch }
                val stat = statResult.requireValue()
                val changed = stat.version != tab.stat?.version
                if (changed) { tab.text = null; tab.bytes = null; tab.nextLine = 1; tab.eof = false }
                tab.stat = stat
                if (binary && tab.bytes == null) {
                    if ((stat.bytes ?: 0) > 32L * 1024 * 1024) error(context.getString(R.string.panel_too_large))
                    val data = api.workspaceFileReadBytes(key.sessionId, tab.path).requireValue()
                    if (data.version != stat.version) error(context.getString(R.string.panel_changed))
                    if (data.data.size > 32 * 1024 * 1024) error(context.getString(R.string.panel_too_large))
                    tab.bytes = data.data
                    tab.eof = true
                } else if (!binary && (tab.text == null || more)) {
                    val data = api.workspaceFileRead(key.sessionId, tab.path, WorkspaceFileRange(tab.nextLine)).requireValue()
                    if (data.version != stat.version) error(context.getString(R.string.panel_changed))
                    if (tab.text.orEmpty().length + data.text.length > 4 * 1024 * 1024) error(context.getString(R.string.panel_too_large))
                    tab.text = if (more && !changed) tab.text.orEmpty() + "\n" + data.text else data.text
                    tab.nextLine = data.offset + data.lines
                    tab.eof = data.eof || data.lines == 0
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { tab.error = e.message }
            finally { tab.busy = false }
        }
    }
    LaunchedEffect(tab) { load() }
    LaunchedEffect(tab.stat) { if (tab.stat == null) load() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
        // File header: name, path and size on the left; the three actions on the right.
        DsCard(verticalArrangement = Arrangement.spacedBy(DsSpacing.tiny)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (binary) FeatherIcons.Image else FeatherIcons.FileText,
                    null,
                    Modifier.size(18.dp),
                    tint = colors.labelTertiary,
                )
                Spacer(Modifier.width(DsSpacing.small))
                Column(Modifier.weight(1f)) {
                    val name = tab.path.substringAfterLast('/').substringAfterLast('\\')
                    Text(technicalDisplay(name), style = DsType.std14Strong, color = colors.labelPrimary)
                    // The folder it is in, when it is in one; a root file would only repeat its name.
                    val folder = tab.path.replace('\\', '/').substringBeforeLast('/', "")
                    val meta = listOfNotNull(
                        folder.takeIf { it.isNotEmpty() }?.let { technicalDisplay("$it/") },
                        tab.stat?.bytes?.let { formatBytes(it) },
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        SelectionContainer {
                            Text(meta, style = DsType.caption11, color = colors.labelCaption, softWrap = true)
                        }
                    }
                }
                onReference?.let { ref ->
                    DsIconButton(
                        icon = FeatherIcons.AtSign,
                        contentDescription = stringResource(R.string.panel_reference),
                        onClick = ref,
                    )
                }
                if (canOpenOnHost == true) DsIconButton(
                    icon = FeatherIcons.ExternalLink,
                    contentDescription = stringResource(R.string.panel_open_host),
                    onClick = { scope.launch {
                        try { store.apiForHost(key.host)?.sessionOpenWorkspacePath(key.sessionId, tab.path)?.requireValue() }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { tab.error = openFailed }
                    } },
                )
                DsIconButton(
                    icon = FeatherIcons.RefreshCw,
                    contentDescription = stringResource(R.string.common_retry),
                    onClick = { tab.stat = null; tab.text = null; tab.bytes = null },
                    enabled = !tab.busy,
                )
                DsIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = onClose,
                )
            }
        }
        if (tab.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.borderL1)
        tab.error?.let { Text(it, style = DsType.small13, color = colors.error) }
        when {
            extension == "pdf" && tab.bytes != null -> PdfPreview(tab.bytes!!, Modifier.weight(1f))
            binary && tab.bytes != null -> {
                var image by remember(tab.bytes) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                LaunchedEffect(tab.bytes) {
                    image = withContext(Dispatchers.Default) {
                        val bytes = tab.bytes ?: return@withContext null
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        val options = BitmapFactory.Options().apply { inSampleSize = com.labteto.dshmobile.ui.media.sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), 2048) }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                    }
                    if (image == null) tab.error = context.getString(R.string.panel_failed)
                }
                image?.let { Image(it, tab.path, Modifier.fillMaxWidth().weight(1f)) }
            }
            extension in setOf("html", "htm", "svg") && tab.text != null -> {
                val base = android.net.Uri.Builder().scheme("https").authority("preview.invalid")
                    .path("/workspace/" + tab.path.replace('\\', '/').removePrefix("/")).build()
                AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { ctx ->
                    WebView(ctx).apply {
                        if (android.os.Build.VERSION.SDK_INT <= 30) setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = false; settings.allowContentAccess = false
                        settings.blockNetworkLoads = true
                        webViewClient = object : WebViewClient() {
                            private var totalBytes = 0L
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                                val denied = { WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0))) }
                                val uri = request?.url ?: return denied()
                                if (uri.scheme != "https" || uri.host != "preview.invalid") return denied()
                                val relative = uri.path?.let { com.labteto.dshmobile.core.session.relativePreviewResource(base.path.orEmpty(), it) }
                                    ?.takeIf { it.isNotBlank() } ?: return denied()
                                return try {
                                    val bytes = runBlocking(Dispatchers.IO) {
                                        withTimeout(10000) {
                                            val api = store.apiForHost(key.host) ?: return@withTimeout null
                                            val data = api.workspaceFileReadBytes(
                                                key.sessionId, relative, WorkspaceByteReadOptions(baseFile = tab.path),
                                            ).requireValue()
                                            if ((data.bytes ?: 0) > 4 * 1024 * 1024 || data.data.size > 4 * 1024 * 1024) return@withTimeout null
                                            data.data
                                        }
                                    } ?: return denied()
                                    synchronized(this) { totalBytes += bytes.size; if (totalBytes > 32 * 1024 * 1024) return denied() }
                                    val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(relative.substringAfterLast('.')) ?: "application/octet-stream"
                                    WebResourceResponse(mime, "utf-8", bytes.inputStream())
                                } catch (_: Exception) { denied() }
                            }
                        }
                    }
                }, update = { it.loadDataWithBaseURL(base.toString(), tab.text.orEmpty(), if (extension == "svg") "image/svg+xml" else "text/html", "utf-8", null) },
                    onRelease = { it.destroy() })
            }
            tab.text != null -> {
                // Markdown reads as a document; everything else is code, in the app's code font on
                // a code background with the file's own line breaks, scrollable both ways.
                val markdown = extension in setOf("md", "markdown")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(DsShapes.block)
                        .background(if (markdown) androidx.compose.ui.graphics.Color.Transparent else colors.bgLayer1)
                        .then(if (markdown) Modifier else Modifier.border(1.dp, colors.borderL2, DsShapes.block)),
                ) {
                    SelectionContainer(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(tab.scroll)
                            .then(if (markdown) Modifier else Modifier.horizontalScroll(rememberScrollState()))
                            .padding(DsSpacing.medium),
                    ) {
                        if (markdown) DocumentMarkdown(tab.text.orEmpty())
                        else Text(
                            tab.text.orEmpty(),
                            style = DsType.caption11.copy(fontFamily = DsType.codeFont, fontSize = 12.sp, lineHeight = 18.sp),
                            color = colors.labelPrimary,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        if (!tab.eof && tab.text != null) {
            DsButton(
                text = stringResource(R.string.panel_more),
                onClick = { load(true) },
                enabled = !tab.busy,
                variant = DsButtonVariant.Outline,
                size = DsButtonSize.Small,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PdfPreview(bytes: ByteArray, modifier: Modifier) {
    val context = LocalContext.current
    var page by remember(bytes) { mutableIntStateOf(0) }
    var count by remember(bytes) { mutableIntStateOf(0) }
    var failure by remember(bytes) { mutableStateOf(false) }
    var bitmap by remember(bytes, page) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(bytes, page) {
        bitmap = withContext(Dispatchers.IO) {
            val file = File.createTempFile("preview-", ".pdf", context.cacheDir)
            try {
                file.writeBytes(bytes)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { pdf ->
                        count = pdf.pageCount
                        if (count == 0) return@withContext null
                        pdf.openPage(page.coerceIn(0, count - 1)).use { p ->
                            val scale = minOf(2f, 2048f / maxOf(p.width, p.height))
                            val image = Bitmap.createBitmap((p.width * scale).toInt().coerceAtLeast(1), (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            image.eraseColor(android.graphics.Color.WHITE)
                            p.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            image.asImageBitmap()
                        }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failure = true; null }
            finally { file.delete() }
        }
    }
    Column(modifier) {
        if (failure) Text(stringResource(R.string.panel_failed))
        bitmap?.let { Image(it, null, Modifier.weight(1f).fillMaxWidth()) }
        Row {
            TextButton(onClick = { page-- }, enabled = page > 0) { Text(stringResource(R.string.common_back)) }
            Text("${page + 1} / $count", Modifier.padding(16.dp))
            TextButton(onClick = { page++ }, enabled = page + 1 < count) { Text(stringResource(R.string.panel_next)) }
        }
    }
}

/** `1.2 KB`, `3.4 MB` — a size a person can read beside a file name. */
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}




