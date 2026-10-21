package com.labteto.dshmobile.ui.screens.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import com.labteto.dshmobile.ui.media.recognizeImageText
import com.labteto.dshmobile.ui.media.sampleSizeFor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labteto.dshmobile.core.wire.dto.AskUserQuestionAnswer
import com.labteto.dshmobile.core.wire.dto.AskUserQuestionAnswerItem
import com.labteto.dshmobile.core.wire.dto.AskUserQuestionOption
import com.labteto.dshmobile.core.wire.RpcResult
import com.labteto.dshmobile.core.wire.dto.CommandSubmitAttachment
import com.labteto.dshmobile.core.wire.dto.ImageLimitsView
import com.labteto.dshmobile.data.CommandOutcome
import com.labteto.dshmobile.data.PromptOutcome
import com.labteto.dshmobile.data.QuestionOutcome
import com.labteto.dshmobile.data.SessionStore
import com.labteto.dshmobile.ui.components.ApprovalPanel
import com.labteto.dshmobile.ui.components.ConnectionBanner
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.DsDialog
import com.labteto.dshmobile.ui.components.DsToastHost
import com.labteto.dshmobile.ui.components.PlanReviewPanel
import com.labteto.dshmobile.ui.components.planReviewOf
import com.labteto.dshmobile.ui.components.QuestionsPanel
import com.labteto.dshmobile.ui.components.rememberDsToast
import com.labteto.dshmobile.ui.AppLaunchAction
import com.labteto.dshmobile.ui.IncomingText
import com.labteto.dshmobile.ui.rememberSessionStore
import com.labteto.dshmobile.ui.screens.main.commands.CommandContext
import com.labteto.dshmobile.ui.screens.main.commands.CommandDecorations
import com.labteto.dshmobile.ui.screens.main.commands.CommandUiSpec
import com.labteto.dshmobile.ui.screens.main.commands.PopupSelectSheet
import com.labteto.dshmobile.ui.screens.main.commands.SlashMenu
import com.labteto.dshmobile.ui.screens.main.commands.SlashRow
import com.labteto.dshmobile.ui.screens.main.commands.activeSlash
import com.labteto.dshmobile.ui.theme.DsAnimations
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.screens.settings.SettingsViewModel
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType
import androidx.compose.ui.res.stringResource
import com.labteto.dshmobile.R
import java.util.UUID
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.labteto.dshmobile.core.session.readImageBounded
import com.labteto.dshmobile.core.wire.dto.ImageRejection

/**
 * The chat surface: chrome, transcript or trajectory, the persistent docks, and the composer.
 *
 * Everything below the tabs stays outside the tab swap on purpose — you can keep typing, and keep
 * answering an approval, while reading the trajectory, and the keyboard-attached surface never
 * animates out from under the cursor.
 */
@Composable
fun ChatScreen(
    onOpenDetails: () -> Unit,
    onOpenDrawer: () -> Unit,
    detailsOpen: Boolean,
) {
    val store = rememberSessionStore()
    val scope = rememberCoroutineScope()
    val colors = DsTheme.colors
    val context = LocalContext.current
    val toast = rememberDsToast()

    val conversation by store.currentConversation.collectAsStateWithLifecycle()
    val currentSessionId by store.currentSessionId.collectAsStateWithLifecycle()
    val sessions by store.sessions.collectAsStateWithLifecycle()
    val models by store.models.collectAsStateWithLifecycle()
    val skills by store.skills.collectAsStateWithLifecycle()
    val commands by store.commands.collectAsStateWithLifecycle()
    val commandsAvailable by store.commandsAvailable.collectAsStateWithLifecycle()
    val subagents by store.subagents.collectAsStateWithLifecycle()
    val jobs by store.jobs.collectAsStateWithLifecycle()
    val subagentConversation by store.subagentConversation.collectAsStateWithLifecycle()
    val subagentMode by store.subagentMode.collectAsStateWithLifecycle()
    val connectionError by store.connectionError.collectAsStateWithLifecycle()
    val loadingOlder by store.loadingOlder.collectAsStateWithLifecycle()
    val loadOlderFailed by store.loadOlderFailed.collectAsStateWithLifecycle()
    val pendingApproval by store.pendingApproval.collectAsStateWithLifecycle()
    val pendingQuestions by store.pendingQuestions.collectAsStateWithLifecycle()
    val permissions by store.permissions.collectAsStateWithLifecycle()
    val pendingPermission by store.pendingPermission.collectAsStateWithLifecycle()
    val agentPresets by store.agentPresets.collectAsStateWithLifecycle()
    val sessionStats by store.sessionStats.collectAsStateWithLifecycle()
    val tokenUsage by store.tokenUsage.collectAsStateWithLifecycle()
    val contextBreakdown by store.contextBreakdown.collectAsStateWithLifecycle()
    val contextPressure by store.contextPressure.collectAsStateWithLifecycle()
    val imageLimits by store.imageLimits.collectAsStateWithLifecycle()

    // Fork: one-shot speech input and read-aloud for the newest reply. The TTS engine is created
    // once per screen; the dialog owns the recognizer's lifecycle.
    var ttsReady by remember { mutableStateOf(false) }
    var readingAloud by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val tts = remember {
        TextToSpeech(context) { status ->
            mainHandler.post { ttsReady = status == TextToSpeech.SUCCESS }
        }
    }
    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { readingAloud = true }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { readingAloud = false }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { readingAloud = false }
            }
        })
        onDispose {
            runCatching { tts.stop() }
            runCatching { tts.shutdown() }
        }
    }
    LaunchedEffect(ttsReady) {
        if (ttsReady) runCatching { tts.language = Locale.getDefault() }
    }
    val lastAssistantText = conversation?.nodes
        ?.lastOrNull { it is com.labteto.dshmobile.core.session.AssistantMessageNode }
        ?.let { (it as com.labteto.dshmobile.core.session.AssistantMessageNode).plainText.trim() }
        ?.takeIf { it.isNotEmpty() }
    val onReadAloud: (() -> Unit)? = lastAssistantText?.takeIf { ttsReady }?.let { text ->
        {
            if (readingAloud) {
                runCatching { tts.stop() }
                readingAloud = false
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dsh-last-reply")
            }
        }
    }

    val currentSession = sessions.firstOrNull { it.sessionId == currentSessionId }
    val workingDirectory = com.labteto.dshmobile.core.session.effectiveWorkingDirectory(conversation?.projections, currentSession?.cwd)
    val title = currentSession?.title
        ?: currentSession?.cwd?.let { basename(it) }
        ?: currentSessionId.orEmpty()

    val connection by store.connectionState.collectAsStateWithLifecycle()
    val hostKey = connection.host?.let { "${it.baseUrl}|${it.id}" }.orEmpty()
    val composer = remember(hostKey, currentSessionId) {
        store.composers.get(ComposerKey(hostKey, currentSessionId.orEmpty()))
    }
    var referencePicker by remember(composer.key) { mutableStateOf<AtQuery?>(null) }
    var draft by composer::text
    var mode by composer::mode
    var tab by rememberSaveable { mutableStateOf(ChatTab.Chat) }
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val appSettings by settingsViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(appSettings.showCodeWorkView) {
        if (!appSettings.showCodeWorkView && tab == ChatTab.Trajectory) tab = ChatTab.Chat
    }
    var turnMenuOpen by remember { mutableStateOf(false) }
    var jumpToSeq by remember { mutableStateOf<Long?>(null) }
    val turnStarts = remember(conversation?.nodes) {
        conversation?.nodes?.filterIsInstance<com.labteto.dshmobile.core.session.TurnStartNode>().orEmpty()
    }
    val attachments = composer.attachments
    var voiceHelpOpen by remember { mutableStateOf(false) }
    var speechStartedAt by remember { mutableLongStateOf(0L) }

    // Fork: voice goes through the system's own recognizer app rather than an in-process
    // SpeechRecognizer. That removes the app's microphone-permission prompt entirely (the
    // recognizer holds the permission) and works on ROMs whose speech service is only exposed
    // through the ACTION_RECOGNIZE_SPEECH activity.
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!text.isNullOrBlank()) {
                val next = if (draft.isBlank()) text else draft.trimEnd() + " " + text
                draft = next
                composer.selection = next.length
            }
        } else if (System.currentTimeMillis() - speechStartedAt < 900L) {
            // Returning this fast with no text is usually the recognizer refusing on a permission
            // or engine problem, not the user reading the screen and cancelling.
            voiceHelpOpen = true
        }
    }

    fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceHelpOpen = true
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_title))
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            toast.second(context.getString(R.string.voice_unavailable))
        } else {
            speechStartedAt = System.currentTimeMillis()
            speechLauncher.launch(intent)
        }
    }

    // Fork: text shared into the app (share sheet or the selection menu). It waits in the
    // process-wide mailbox until a session is open, then lands in that session's composer.
    val incomingText by IncomingText.pending.collectAsStateWithLifecycle()
    var shareNoticeShown by remember { mutableStateOf(false) }
    LaunchedEffect(incomingText, currentSessionId) {
        val text = incomingText ?: return@LaunchedEffect
        if (currentSessionId == null) {
            if (!shareNoticeShown) {
                shareNoticeShown = true
                toast.second(context.getString(R.string.share_need_session))
                onOpenDrawer()
            }
            return@LaunchedEffect
        }
        val next = if (draft.isBlank()) text else draft.trimEnd() + "\n" + text
        draft = next
        composer.selection = next.length
        IncomingText.consume()
    }

    // Fork: the launcher shortcut / Quick Settings tile / widget can ask for the voice sheet.
    val launchAction by AppLaunchAction.pending.collectAsStateWithLifecycle()
    var launchNoticeShown by remember { mutableStateOf(false) }
    LaunchedEffect(launchAction, currentSessionId) {
        if (launchAction != AppLaunchAction.VOICE) return@LaunchedEffect
        if (currentSessionId == null) {
            if (!launchNoticeShown) {
                launchNoticeShown = true
                toast.second(context.getString(R.string.share_need_session))
                onOpenDrawer()
            }
            return@LaunchedEffect
        }
        startVoiceInput()
        AppLaunchAction.consume()
    }

    var panelKey by remember { mutableStateOf<ComposerKey?>(null) }
    var rightPanelTab by remember { mutableStateOf(RightPanelTab.Files) }
    // Desktop-style split: on a tablet or in landscape, dock the file panel beside the
    // conversation instead of covering it. Phones keep the full-screen sheet.
    val dockedPanel = panelKey != null && LocalConfiguration.current.screenWidthDp >= 720
    // In the docked layout the file panel is not a Dialog, so it needs its own back handler.
    androidx.activity.compose.BackHandler(enabled = dockedPanel) { panelKey = null }
    val referenceWorkspaceFile: (String) -> Unit = { path ->
        fileMention(path, directory = false)?.let { mention ->
            composer.insertReference(
                start = composer.selection,
                end = composer.selection,
                mention = mention,
                label = path,
                kind = "file",
                target = path,
            )
        }
        // The modal panel closes itself after its exit animation; the docked panel stays open,
        // the way the desktop client keeps the file pane beside the conversation.
    }
    var feedback by remember { mutableStateOf<Triple<ComposerKey, String, Boolean>?>(null) }
    var sheet by remember { mutableStateOf<ChatSheet?>(null) }
    // A decorated command's picker (the phone's `popupSelect`), keyed by the command it serves.
    var popup by remember(composer.key) { mutableStateOf<Pair<String, CommandUiSpec.PopupSelect>?>(null) }

    // Hoisted above the tab swap so each view keeps its own scroll position across switches.
    val chatListState = rememberLazyListState()
    val trajectoryListState = rememberLazyListState()

    val commandFailed = stringResource(R.string.err_command_failed)
    val unknownCommand = stringResource(R.string.err_command_unknown)

    fun report(outcome: CommandOutcome) {
        when (outcome) {
            is CommandOutcome.Ok -> outcome.text?.takeIf { it.isNotBlank() }?.let { toast.second(it) }
            is CommandOutcome.Unknown -> toast.second(unknownCommand.format(outcome.line))
            is CommandOutcome.Failed -> toast.second(commandFailed.format(outcome.message))
        }
    }

    val answerRefused = stringResource(R.string.questions_answer_refused)
    val answerUnsent = stringResource(R.string.questions_answer_unsent)

    /**
     * What to tell the user about a question response, or null when the harness took it.
     *
     * A refusal is worth naming rather than swallowing: the host's wait stays open and the tool
     * call that opened it stays blocked, so a card that quietly did nothing would leave the session
     * stuck with no explanation.
     */
    fun refusalOf(outcome: QuestionOutcome): String? = when (outcome) {
        is QuestionOutcome.Accepted -> null
        is QuestionOutcome.Refused -> answerRefused.format(outcome.reason)
        is QuestionOutcome.Unsent -> answerUnsent
    }

    /**
     * Fork: admit chosen images into the composer picked when the sheet opened.
     *
     * Shared by the gallery picker and the system camera so both go through the same size, count
     * and decode checks; [uris] are read off the main thread and the accepted ones are appended
     * to the target composer's attachment list.
     */
    fun ingestPickedImages(uris: List<Uri>) {
        val selection = store.composers.imagePickTarget
        val target = selection?.first
        store.composers.imagePickTarget = null
        if (target != null && uris.isNotEmpty()) {
            target.preparing = true
            val limits = selection.second
            store.composers.scope.launch {
                val failures = mutableListOf<String>()
                try {
                    val existing = target.attachments.filterIsInstance<PendingAttachment.Image>()
                    val accepted = withContext(Dispatchers.IO) {
                        val selected = mutableListOf<PendingAttachment.Image>()
                        val admission = com.labteto.dshmobile.core.session.PhotoBatchAdmission(limits, existing.map { it.bytes })
                        for (uri in uris) {
                            try {
                                if (admission.full) {
                                    failures.add(imageRejectionText(context, ImageRejection.TOO_MANY, limits))
                                    continue
                                }
                                val resolver = context.contentResolver
                                val mediaType = resolver.getType(uri)
                                val bytes = (resolver.openInputStream(uri) ?: throw java.io.IOException("Unreadable image" )).use {
                                    readImageBounded(it, limits.maxImageBytes.coerceIn(0, Int.MAX_VALUE.toLong() - 1))
                                }
                                if (bytes == null) {
                                    failures.add(imageRejectionText(context, ImageRejection.TOO_LARGE, limits))
                                    continue
                                }
                                val pick = decodePick(bytes)
                                val rejection = admission.accept(
                                    mediaType.orEmpty(), pick.detectedMediaType, bytes.size, pick.width, pick.height,
                                )
                                if (rejection != null) {
                                    failures.add(imageRejectionText(context, rejection, limits))
                                    continue
                                }
                                selected.add(PendingAttachment.Image(
                                    mediaType = mediaType.orEmpty(), base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                                    preview = pick.preview, bytes = bytes.size, width = pick.width, height = pick.height,
                                ))
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { failures.add(context.getString(R.string.err_attachment_failed)) }
                        }
                        selected
                    }
                    target.attachments.addAll(accepted)
                    if (failures.isNotEmpty()) toast.second(context.getString(
                        R.string.photos_skipped, failures.size, failures.distinct().joinToString("; "),
                    ))
                } finally { target.preparing = false }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        ingestPickedImages(uris)
    }

    /**
     * Fork: the system camera. The photo is captured into a private cache file and then goes
     * through the same admission/decode path as a gallery pick, so size and count limits apply
     * identically and the image upload pipeline needs no second branch.
     */
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    var pendingCameraWantsOcr by remember { mutableStateOf(false) }
    var cameraForOcr by remember { mutableStateOf(false) }
    var ocrBusy by remember { mutableStateOf(false) }

    fun runOcr(uri: Uri) {
        if (ocrBusy) return
        ocrBusy = true
        toast.second(context.getString(R.string.ocr_processing))
        scope.launch {
            val text = runCatching { recognizeImageText(context, uri) }.getOrNull()?.trim()
            ocrBusy = false
            if (text.isNullOrEmpty()) {
                toast.second(context.getString(R.string.ocr_empty))
                return@launch
            }
            val next = if (draft.isBlank()) text else draft.trimEnd() + "\n" + text
            draft = next
            composer.selection = next.length
        }
    }

    val ocrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runOcr(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        val forOcr = cameraForOcr
        cameraForOcr = false
        if (ok && uri != null) {
            if (forOcr) runOcr(uri) else ingestPickedImages(listOf(uri))
        } else {
            store.composers.imagePickTarget = null
            if (!ok) toast.second(context.getString(R.string.camera_capture_failed))
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraPermissionRequested = true
        } else {
            toast.second(context.getString(R.string.camera_permission_needed))
        }
    }

    fun takePhoto(ocr: Boolean = false) {
        if (!ocr && (composer.preparing || store.composers.imagePickTarget != null)) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingCameraWantsOcr = ocr
            cameraPermission.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val dir = java.io.File(context.cacheDir, "images").apply { mkdirs() }
            val file = java.io.File.createTempFile("dsh-photo-", ".jpg", dir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraUri = uri
            cameraForOcr = ocr
            if (!ocr) store.composers.imagePickTarget = composer to (imageLimits ?: ImageLimitsView())
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            store.composers.imagePickTarget = null
            toast.second(context.getString(R.string.camera_capture_failed))
        }
    }
    LaunchedEffect(cameraPermissionRequested) {
        if (cameraPermissionRequested) {
            cameraPermissionRequested = false
            val ocr = pendingCameraWantsOcr
            pendingCameraWantsOcr = false
            takePhoto(ocr)
        }
    }


    /** Replace one pending file by identity; a chip that was removed meanwhile is left removed. */
    fun updateFile(target: ComposerDraft, id: String, transform: (PendingAttachment.File) -> PendingAttachment.File) {
        val attachments = target.attachments
        val index = attachments.indexOfFirst { it is PendingAttachment.File && it.id == id }
        if (index >= 0) attachments[index] = transform(attachments[index] as PendingAttachment.File)
    }

    /**
     * Stream one picked file to the host and settle its chip.
     *
     * The upload starts the moment the file is picked, as the web client's does, so by the time
     * the message is sent the receipt is usually already there; the chip shows progress until it
     * is, and the send affordance waits for it.
     */
    fun startUpload(file: PendingAttachment.File, target: ComposerDraft = composer) {
        updateFile(target, file.id) { it.copy(state = FileUploadState.Uploading(0)) }
        store.composers.scope.launch {
            val result = store.uploadFile(
                name = file.name,
                targetSessionId = target.key.sessionId,
                targetHost = target.key.host,
                size = file.size,
                open = { runCatching { context.contentResolver.openInputStream(file.uri) }.getOrNull() },
                onProgress = { sent -> updateFile(target, file.id) { it.copy(state = FileUploadState.Uploading(sent)) } },
            )
            updateFile(target, file.id) {
                when (result) {
                    is RpcResult.Ok -> it.copy(state = FileUploadState.Ready(result.value.receiptId, result.value.file))
                    is RpcResult.Err -> it.copy(state = FileUploadState.Failed(result.error.message))
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = store.composers.filePickTarget
        store.composers.filePickTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        val (name, size) = describeDocument(context.contentResolver, uri)
        if (name == null) {
            toast.second(context.getString(R.string.err_file_read_failed))
            return@rememberLauncherForActivityResult
        }
        val file = PendingAttachment.File(
            id = UUID.randomUUID().toString(),
            uri = uri,
            name = name,
            size = size,
            state = FileUploadState.Uploading(0),
        )
        target.attachments.add(file)
        startUpload(file, target)
    }

    /** What a decoration gets to work with: the open session's conversation, a command runner, the draft. */
    fun commandContext(sessionId: String, host: String): CommandContext = object : CommandContext {
        override val conversation get() = store.currentConversation.value
        override val draft get() = composer.text
        override suspend fun run(line: String) = store.runCommand(line, emptyList(), sessionId, host)
        override fun setDraft(text: String) { composer.text = text; composer.selection = text.length }
    }

    /**
     * The phone's `commandUi.decorate` dispatch: a bare `/name` whose decoration is available
     * opens that decoration's UI and submits nothing — exactly what the web client does before it
     * would otherwise hand the line to `commands/execute`. Anything else returns false and takes
     * the ordinary path, and so does a same-named command from a plugin the decoration is not a
     * port of.
     */
    fun dispatchDecorated(text: String): Boolean {
        val trimmed = text.trim()
        if (!trimmed.startsWith("/") || trimmed.any { it.isWhitespace() }) return false
        val name = trimmed.drop(1)
        val decoration = CommandDecorations.forName(name) ?: return false
        if (commands.none { it.name == name } || !decoration.recognizes(commands)) return false
        if (!decoration.available(conversation)) return false
        when (val ui = decoration.ui) {
            is CommandUiSpec.PopupSelect -> popup = name to ui
            is CommandUiSpec.Action -> scope.launch {
                try {
                    ui.run(commandContext(composer.key.sessionId, composer.key.host))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toast.second(e.message ?: context.getString(R.string.panel_failed))
                }
            }
        }
        return true
    }

    fun send(text: String) {
        if (composer.preparing || composer.submitting) { draft = text; return }
        if (attachments.isEmpty() && dispatchDecorated(text)) return
        val delivery = mode
        val targetId = composer.key.sessionId
        val targetHost = composer.key.host
        val pending = attachments.toList()
        if (text.isBlank() && pending.isEmpty()) return
        val images = pending.filterIsInstance<PendingAttachment.Image>()
        val files = pending.filterIsInstance<PendingAttachment.File>()
        // A file without a receipt cannot be cited. The send affordance already waits for the
        // chips, but a keyboard send lands here too.
        if (files.any { it.state !is FileUploadState.Ready }) {
            draft = text
            toast.second(
                context.getString(
                    if (files.any { it.state is FileUploadState.Failed }) R.string.chat_attachment_upload_failed
                    else R.string.chat_attachment_still_uploading,
                ),
            )
            return
        }
        val receipts = files.mapNotNull { it.receiptId }
        // A slash line that names a registered command is not a message: `session/prompt` would
        // hand it to the model verbatim, so it has to be recognised here and written through the
        // command gateway. A miss falls through to the prompt path — that is how skills work.
        when (val submission = adjudicate(text, commands, pending.size, store.commandAttachmentsSupported)) {
            is Submission.Refused -> {
                // Nothing is sent and nothing is dropped. The composer clears the draft on its way
                // here, so put it back, and leave the attachments alone — a refusal the user cannot
                // act on without re-picking every one is not much of a refusal.
                draft = text
                val message = when (submission.reason) {
                    RefusalReason.COMMAND_TAKES_NO_ATTACHMENTS -> R.string.err_command_no_images
                    RefusalReason.HOST_TOO_OLD -> R.string.err_command_images_host
                }
                toast.second(context.getString(message, submission.command))
            }

            is Submission.Command -> {
                composer.submitting = true
                attachments.clear()
                val submitted = images.map { it.encoded().asSubmit() } +
                    receipts.map { CommandSubmitAttachment.File(it) }
                store.composers.scope.launch {
                    try {
                    val outcome = store.runCommand(submission.line, submitted, targetId, targetHost)
                    // An attachment-carrying command consumes its attachments only on success, as
                    // the harness client does: an error result is something to correct, and
                    // correcting it should not start with picking every file again. A plain
                    // command that fails keeps today's behaviour, because its whole submission was
                    // the line. The restore only lands in a composer nobody has touched meanwhile —
                    // the call is in flight while the user can still type and pick.
                    if (outcome is CommandOutcome.Failed) {
                        composer.restoreRejected(text, pending)
                    }
                    report(outcome)
                    } catch (e: kotlinx.coroutines.CancellationException) { composer.restoreRejected(text, pending); throw e }
                    catch (e: Exception) { composer.restoreRejected(text, pending); toast.second(e.message ?: context.getString(R.string.panel_failed)) }
                    finally { composer.submitting = false }
                }
            }

            is Submission.Prompt -> {
                composer.submitting = true
                attachments.clear()
                store.composers.scope.launch {
                    try {
                    // One call, whatever the count. The host admits a prompt's images as a single
                    // batch, and that batch is the only thing its per-message count and total-size
                    // bounds are measured against — sending one image per call made a single
                    // message into several and put both limits permanently out of reach. Files
                    // ride the same call as receipts; a receipt the host refuses stays staged,
                    // so restoring the chips is enough to try again.
                    val outcome = if (pending.isEmpty()) {
                        store.prompt(text, delivery, targetId, targetHost)
                    } else {
                        store.promptWithAttachments(text, delivery, images.map { it.encoded() }, receipts, targetId, targetHost)
                    }
                    if (outcome !is PromptOutcome.Ok) {
                        composer.restoreRejected(text, pending)
                        toast.second(
                            if (outcome is PromptOutcome.Rejected) imageRejectionText(context, outcome.rejection, imageLimits, outcome.reason)
                            else (outcome as PromptOutcome.Failed).message,
                        )
                    }
                    } catch (e: kotlinx.coroutines.CancellationException) { composer.restoreRejected(text, pending); throw e }
                    catch (e: Exception) { composer.restoreRejected(text, pending); toast.second(e.message ?: context.getString(R.string.panel_failed)) }
                    finally { composer.submitting = false }
                }
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.labteto.dshmobile.ui.media.LocalAttachmentScope provides (composer.key.host to composer.key.sessionId),
        com.labteto.dshmobile.ui.components.LocalFileOpener provides { path: String ->
            store.panels.get(composer.key).open(path); panelKey = composer.key
        },
        com.labteto.dshmobile.ui.components.LocalFolderOpener provides { path: String ->
            store.panels.get(composer.key).browse(path); panelKey = composer.key
        },
    ) {
    Surface(modifier = Modifier.fillMaxSize(), color = colors.bgBase) {
        Row(Modifier.fillMaxSize()) {
        // The activity draws edge to edge, so every top-level surface has to consume the insets
        // itself or the chrome ends up underneath the status bar. safeDrawing covers the status
        // bar, the gesture area and the keyboard in one modifier.
        Column(modifier = Modifier.weight(1f).fillMaxSize().safeDrawingPadding()) {
            ChatTopBar(
                title = title ?: stringResource(R.string.app_name),
                subagentCount = subagents.size,
                jobCount = jobs.size,
                detailsOpen = detailsOpen,
                tab = tab,
                showTrajectory = appSettings.showCodeWorkView,
                onOpenTurns = if (turnStarts.isNotEmpty()) ({ turnMenuOpen = true }) else null,
                onOpenDrawer = onOpenDrawer,
                onOpenSubagents = { sheet = ChatSheet.Subagents },
                onOpenDetails = onOpenDetails,
                onTabChange = { tab = it },
                onOpenWorkspace = { rightPanelTab = RightPanelTab.Files; panelKey = composer.key },
                workspaceEnabled = currentSessionId != null,
            )

            connectionError?.let {
                ConnectionBanner(
                    message = stringResource(R.string.connection_interrupted),
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { store.retryConnection() },
                )
            }
            if (conversation?.gap == true) {
                ConnectionBanner(
                    message = stringResource(R.string.common_reconnecting),
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { store.retryConnection() },
                )
            }

            val journalTimes = remember(conversation?.journal) {
                conversation?.journal?.associate { it.seq to it.time }.orEmpty()
            }
            val nodeContext = ChatNodeContext(
                nodes = conversation?.nodes ?: emptyList(),
                eventTimes = journalTimes,
                running = conversation?.running == true,
                cwd = workingDirectory,
                onOpenSubagent = { childId ->
                    scope.launch { store.openSubagentTranscript(childId) }
                    sheet = ChatSheet.Subagents
                },
                onBranchFrom = { seq -> scope.launch { currentSessionId?.let { store.forkSession(it, seq) } } },
                onFeedback = { seq, positive ->
                    conversation?.nodes?.filterIsInstance<com.labteto.dshmobile.core.session.AssistantMessageNode>()
                        ?.firstOrNull { it.seq == seq }?.messageId?.let { feedback = Triple(composer.key, it, positive) }
                },
                onReadAloud = { text ->
                    if (ttsReady) {
                        runCatching {
                            tts.stop()
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dsh-message")
                        }
                    } else {
                        toast.second(context.getString(R.string.voice_unavailable))
                    }
                },
            )

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // Keep transcript/question space even with a long draft. On very short screens,
                // reserve enough for reference chips, a line of text and the fixed action row.
                val composerMaxHeight = (maxHeight * 0.6f).coerceAtLeast(180.dp)
                Column(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (
                            slideInHorizontally { width -> if (forward) width / 6 else -width / 6 } +
                                fadeIn(DsAnimations.fade)
                            )
                            .togetherWith(fadeOut(DsAnimations.fade)) using SizeTransform(clip = false)
                    },
                    modifier = Modifier.weight(1f),
                    label = "chatTab",
                ) { current ->
                    when (current) {
                        ChatTab.Chat -> ChatTranscript(
                            conversation = conversation,
                            loading = conversation == null && currentSessionId != null,
                            loadingOlder = loadingOlder,
                            loadOlderFailed = loadOlderFailed,
                            context = nodeContext,
                            listState = chatListState,
                            onLoadOlder = { scope.launch { store.loadOlder() } },
                            jumpToSeq = jumpToSeq,
                            onJumpHandled = { jumpToSeq = null },
                        )
                        ChatTab.Trajectory -> TrajectoryTab(
                            conversation = conversation,
                            stats = sessionStats,
                            usage = tokenUsage,
                            cwd = workingDirectory,
                            listState = trajectoryListState,
                        )
                    }
                }

                conversation?.let { conv ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        parseTodos(conv.projections["todos"])?.let { TodoDock(it) }
                        parseGoal(conv.projections["goal"])?.let { GoalBar(it, store) }
                        QueueDock(conv.queue, store)
                    }
                }

                // Server-initiated requests take over the bottom of the screen: they block the turn,
                // so burying them behind a scroll would strand the session.
                val approval = pendingApproval
                if (approval != null && approval.sessionId == currentSessionId) {
                    // A refusal is said out loud here rather than swallowed, for the reason [refusalOf]
                    // gives: the host's wait — and the tool call behind it — stays open, and a panel
                    // that reported nothing would read as two buttons that do nothing.
                    fun decide(allow: Boolean) = scope.launch {
                        refusalOf(store.respondApproval(approval.sessionId, approval.approvalId, allow))
                            ?.let { toast.second(it) }
                    }
                    ApprovalPanel(
                        toolName = approval.toolName,
                        reason = approval.reason,
                        onAllow = { decide(true) },
                        onReject = { decide(false) },
                    )
                }
                val questions = pendingQuestions
                if (questions != null && questions.sessionId == currentSessionId) {
                    var planBusy by remember(questions.rpcId) { mutableStateOf(false) }
                    // A plan review rides the question channel but is a different decision, so it gets
                    // the card built for it. The narrowing decides which — and hands back anything the
                    // card could not answer in full, because the card answers one question and the host
                    // refuses an answer batch shorter than the request it resolves.
                    val review = remember(questions.rpcId) { planReviewOf(questions.items) }
                    if (review != null) {
                        fun settle(block: suspend () -> QuestionOutcome) {
                            planBusy = true
                            scope.launch {
                                refusalOf(block())?.let {
                                    planBusy = false
                                    toast.second(it)
                                }
                            }
                        }
                        fun decide(option: AskUserQuestionOption) = settle {
                            store.answerQuestions(
                                questions.sessionId,
                                AskUserQuestionAnswer(
                                    listOf(AskUserQuestionAnswerItem(review.id, listOf(option.label))),
                                ),
                            )
                        }
                        PlanReviewPanel(
                            review = review,
                            busy = planBusy,
                            onApprove = { decide(review.approve) },
                            onDecline = { review.decline?.let { decide(it) } },
                            // Wanting to talk it over first is not one of the options the asker stated,
                            // so it ends the request rather than answering it with the refusal.
                            onDiscuss = {
                                draft = ""
                                settle { store.dismissQuestions(questions.sessionId) }
                            },
                        )
                    } else {
                        QuestionsPanel(
                            requestKey = questions.rpcId,
                            questions = questions.items,
                            onSubmit = { answer ->
                                refusalOf(store.answerQuestions(questions.sessionId, answer))
                            },
                            onDismiss = { refusalOf(store.dismissQuestions(questions.sessionId)) },
                        )
                    }
                }

                // Weighted questions are measured after the composer, so long questions cannot
                // consume its controls. The question area scrolls within its allotted height.
                ModernQuestions(store, currentSessionId, Modifier.weight(1f, fill = false))

                // The `/` autocomplete sits where the web's does: directly above the composer,
                // only while the draft is a slash token being typed.
                activeSlash(draft, composer.selection)?.let { slash ->
                    SlashMenu(
                        query = slash,
                        commands = if (commandsAvailable) commands else emptyList(),
                        skills = skills,
                        onPick = { row ->
                            when (row) {
                                is SlashRow.Command -> if (row.descriptor.input == null) {
                                    draft = ""
                                    send(row.descriptor.line)
                                } else {
                                    draft = row.descriptor.draftPrefix
                                    composer.selection = draft.length
                                }
                                is SlashRow.Skill -> {
                                    draft = "/${row.entry.name} "
                                    composer.selection = draft.length
                                }
                            }
                        },
                    )
                }

                Composer(
                    modifier = Modifier.heightIn(max = composerMaxHeight),
                    references = composer.references,
                    onVoiceInput = { startVoiceInput() },
                    onAttachFile = {
                        if (!composer.preparing && store.composers.filePickTarget == null) {
                            store.composers.filePickTarget = composer
                            filePicker.launch(arrayOf("*/*"))
                        }
                    },
                    modelSlot = models?.let { value ->
                        { ModelChip(models = value, onClick = { sheet = ChatSheet.Models }) }
                    },
                    onOpenReference = { reference ->
                        if (reference.kind == "session") scope.launch { store.openSession(reference.target) }
                        else {
                            val panel = store.panels.get(composer.key)
                            if (reference.kind == "folder") {
                                panel.directory = reference.target
                                panel.section = 0
                                // Reopening the panel must load this target, not reuse the previous
                                // directory's cached rows. Its existing launch effect loads null listings.
                                panel.listing = null
                            } else panel.open(reference.target)
                            panelKey = composer.key
                        }
                    },
                    draft = draft,
                    onDraftChange = { draft = it },
                    cursor = composer.selection,
                    onCursorChange = { composer.selection = it; if (draft.take(it).endsWith("@")) referencePicker = activeReference(draft, it) },
                    attachments = attachments,
                    onRemoveAttachment = { index -> attachments.removeAt(index) },
                    onRetryAttachment = { index ->
                        (attachments.getOrNull(index) as? PendingAttachment.File)?.let { startUpload(it) }
                    },
                    permissions = permissions,
                    pendingPermission = pendingPermission,
                    onPermissionPick = { value -> scope.launch { report(store.setPermissionPreset(value)) } },
                    contextBreakdown = contextBreakdown,
                    contextPressure = contextPressure,
                    running = conversation?.running == true,
                    deliveryMode = mode,
                    onDeliveryModeChange = { mode = it },
                    enabled = currentSessionId != null && !composer.submitting,
                    preparing = composer.preparing,
                    onOpenSheet = { sheet = ChatSheet.Commands },
                    // A lambda, not `::send`. The composer holds this through rememberUpdatedState,
                    // which keeps what it has when the new value is equal to it, and a reference to a
                    // local function equals every other reference to that function whatever it
                    // captured. Each session's `::send` compared equal to the first and was dropped, so
                    // the button went on sending with the attachment list of whichever session was
                    // open when this screen first composed. A lambda is rebuilt when what it captures
                    // changes and compares by identity, so the composer always holds the current one.
                    onSend = { text -> send(text) },
                    onStop = { scope.launch { store.cancelTurn() } },
                )

                StatsFooter(stats = sessionStats, usage = tokenUsage, contextPressure = contextPressure)
                }
            }
        }
        if (dockedPanel) {
            Box(Modifier.fillMaxHeight().width(0.5.dp).background(colors.borderL3))
            panelKey?.let { key ->
                RightPanelBody(
                    store = store,
                    state = store.panels.get(key),
                    tab = rightPanelTab,
                    onTabChange = { rightPanelTab = it },
                    onDismiss = { panelKey = null },
                    onReference = referenceWorkspaceFile,
                    onOpenDetails = {
                        panelKey = null
                        onOpenDetails()
                    },
                    modifier = Modifier.width(380.dp).fillMaxHeight(),
                )
            }
        }
        }
        DsToastHost(toast, modifier = Modifier.fillMaxWidth())
    }

    }
    referencePicker?.let { ReferencePicker(store, composer, it, onClose = { referencePicker = null }) }
    if (voiceHelpOpen) {
        DsDialog(title = stringResource(R.string.voice_title), onDismiss = { voiceHelpOpen = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.small)) {
                Text(
                    stringResource(R.string.voice_engine_help),
                    style = DsType.std14,
                    color = DsTheme.colors.labelPrimary,
                )
                DsButton(
                    text = stringResource(R.string.voice_open_settings),
                    onClick = {
                        voiceHelpOpen = false
                        runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                    },
                    variant = DsButtonVariant.Info,
                )
                DsButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = { voiceHelpOpen = false },
                    variant = DsButtonVariant.Ghost,
                )
            }
        }
    }
    popup?.let { (name, spec) ->
        PopupSelectSheet(
            command = name,
            spec = spec,
            context = commandContext(composer.key.sessionId, composer.key.host),
            onDismiss = { popup = null },
        )
    }
    if (!dockedPanel) panelKey?.let { key ->
        RightPanelModal(
            store = store,
            state = store.panels.get(key),
            tab = rightPanelTab,
            onTabChange = { rightPanelTab = it },
            onDismiss = { panelKey = null },
            onReference = referenceWorkspaceFile,
            onOpenDetails = {
                panelKey = null
                onOpenDetails()
            },
        )
    }
    if (turnMenuOpen) {
        DsDialog(title = stringResource(R.string.chat_turn_navigation), onDismiss = { turnMenuOpen = false }) {
            turnStarts.asReversed().forEachIndexed { index, node ->
                Text(
                    stringResource(R.string.chat_turn_n, turnStarts.size - index),
                    style = DsType.std14,
                    color = DsTheme.colors.labelPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { jumpToSeq = node.seq; turnMenuOpen = false }
                        .padding(vertical = 12.dp),
                )
            }
        }
    }

    feedback?.let { (key, id, positive) -> FeedbackDialog(store, key, id, positive) { feedback = null } }
    when (sheet) {
        ChatSheet.Commands -> CommandSheet(
            commands = commands,
            commandsAvailable = commandsAvailable,
            skills = skills,
            mode = mode,
            running = conversation?.running == true,
            canAttach = currentSessionId != null && !composer.preparing && !composer.submitting,
            onModeChange = { mode = it },
            onAttach = {
                if (!composer.preparing && store.composers.imagePickTarget == null) {
                    store.composers.imagePickTarget = composer to (imageLimits ?: ImageLimitsView())
                    imagePicker.launch("image/*")
                }
            },
            onAttachCamera = { takePhoto() },
            onOcrCamera = { takePhoto(ocr = true) },
            onOcrGallery = { ocrPicker.launch("image/*") },
            onAttachFile = { store.composers.filePickTarget = composer; filePicker.launch(arrayOf("*/*")) },
            onOpenWorkspace = {
                sheet = null
                rightPanelTab = RightPanelTab.Files
                panelKey = composer.key
            },
            onAddReference = {
                sheet = null
                referencePicker = activeReference(draft, composer.selection)
                    ?: AtQuery(composer.selection, composer.selection, "")
            },
            // The sheet only auto-runs commands that take no input at all, and a command that
            // takes no input takes no attachments either — so a pending attachment refuses here
            // for the same reason it refuses at the composer, rather than being silently dropped.
            onRunCommand = { line ->
                val name = line.removePrefix("/").substringBefore(' ')
                if (attachments.isEmpty()) {
                    if (!dispatchDecorated(line)) scope.launch { report(store.runCommand(line)) }
                } else {
                    toast.second(context.getString(R.string.err_command_no_images, name))
                }
            },
            onPrefillDraft = { prefix -> draft = prefix },
            onDismiss = { sheet = null },
        )
        ChatSheet.Models -> ModelsSheet(models = models, store = store, onDismiss = { sheet = null })
        ChatSheet.Presets -> PresetsSheet(
            presets = agentPresets,
            currentPreset = currentSession?.agentPreset,
            sessionBlank = currentSession?.blank ?: false,
            store = store,
            onDismiss = { sheet = null },
        )
        ChatSheet.Subagents -> SubagentsSheet(
            store = store,
            entries = subagents,
            conversation = subagentConversation,
            mode = subagentMode,
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

/** Which sheet, if any, is open over the chat surface. */
private enum class ChatSheet { Commands, Models, Presets, Subagents }

/**
 * A picked document's display name and size, as its provider reports them.
 *
 * The name falls back to the last path segment when the provider offers none, and the size to
 * `-1` — the upload route accepts a chunked body, so an unknown length costs only the progress
 * ring. A null name means the provider answered nothing at all, which is a read failure.
 */
private fun describeDocument(resolver: android.content.ContentResolver, uri: android.net.Uri): Pair<String?, Long> {
    var name: String? = null
    var size = -1L
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
    }
    return (name ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }) to size
}


/**
 * What a bounds pass over the picked bytes tells us: the image's intrinsic size, the media type
 * its bytes actually are, and a thumbnail for the composer strip.
 *
 * A [width] of zero means the bytes did not parse as an image at all.
 */
internal data class DecodedPick(
    val width: Int,
    val height: Int,
    val detectedMediaType: String?,
    val preview: ImageBitmap?,
)

/**
 * Measure and thumbnail a picked image in one pass.
 *
 * The bounds pass was always here for the thumbnail's sample size; it also answers the two
 * questions the host's admission asks — how large is this, and is it really the type its provider
 * claims — so the picker can refuse an image before spending a round trip on it rather than after.
 */
internal fun decodePick(bytes: ByteArray): DecodedPick {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds) }
    val width = bounds.outWidth.coerceAtLeast(0)
    val height = bounds.outHeight.coerceAtLeast(0)
    val preview = if (width <= 0) {
        null
    } else {
        runCatching {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(maxOf(width, height), PREVIEW_WIDTH_PX)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
        }.getOrNull()
    }
    return DecodedPick(if (preview == null) 0 else width, if (preview == null) 0 else height, bounds.outMimeType, preview)
}

/** The composer thumbnail is 56dp; decoding much past that is wasted memory. */
private const val PREVIEW_WIDTH_PX = 224














