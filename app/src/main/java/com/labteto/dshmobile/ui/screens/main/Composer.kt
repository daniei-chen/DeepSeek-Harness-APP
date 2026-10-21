package com.labteto.dshmobile.ui.screens.main

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.input.key.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.dto.ContextBreakdownView
import com.labteto.dshmobile.core.wire.dto.ContextPressureView
import com.labteto.dshmobile.core.wire.dto.FULL_ACCESS_PRESET
import com.labteto.dshmobile.core.wire.dto.EncodedImageAttachment
import com.labteto.dshmobile.core.wire.dto.FileAttachmentRef
import com.labteto.dshmobile.core.wire.dto.PermissionSelect
import com.labteto.dshmobile.core.wire.dto.displayPermissionPreset
import com.labteto.dshmobile.ui.components.ContextMeter
import com.labteto.dshmobile.ui.components.FeatherIcons
import com.labteto.dshmobile.ui.components.DsIconButton
import com.labteto.dshmobile.ui.components.skeleton
import com.labteto.dshmobile.ui.theme.DsAnimations
import com.labteto.dshmobile.ui.theme.DsShapes
import com.labteto.dshmobile.ui.theme.DsSpacing
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType

/**
 * Something picked and waiting to be sent with the next message.
 *
 * Two kinds, because the harness carries them two ways. An [Image] rides the prompt as bytes,
 * so it is held here fully encoded. A [File] is uploaded the moment it is picked — harness
 * 0.1.3 streams it to the host verbatim — and what the prompt carries is the receipt that upload
 * answered with, so the chip tracks the upload rather than the bytes.
 */
internal sealed interface PendingAttachment {
    /** Display name, when the picker had one. */
    val name: String?

    /**
     * A picked image, held with its decoded preview.
     *
     * [bytes], [width] and [height] are the encoded size and intrinsic dimensions the picker already
     * had to learn to admit the image; keeping them means the *next* pick can be measured against
     * the message's running totals without decoding everything already attached a second time.
     */
    data class Image(
        val mediaType: String,
        val base64: String,
        val preview: ImageBitmap?,
        val bytes: Int,
        val width: Int,
        val height: Int,
        override val name: String? = null,
    ) : PendingAttachment {
        /** The wire form `session/prompt` carries. */
        fun encoded(): EncodedImageAttachment = EncodedImageAttachment(mediaType, base64, name)
    }

    /** A picked file and the state of its upload. [id] is what a progress callback keys on. */
    data class File(
        val id: String,
        val uri: Uri,
        override val name: String,
        /** Size as the provider reported it; `-1` when it would not say. */
        val size: Long,
        val state: FileUploadState,
    ) : PendingAttachment {
        /** The receipt to cite, once the upload has one. */
        val receiptId: String? get() = (state as? FileUploadState.Ready)?.receiptId
    }
}

/** Where one file's upload stands. */
internal sealed interface FileUploadState {
    /** Bytes are still going up; [sent] is the running count. */
    data class Uploading(val sent: Long) : FileUploadState

    /** The host holds the file and minted a receipt for it. */
    data class Ready(val receiptId: String, val file: FileAttachmentRef) : FileUploadState

    /** The upload did not complete; the chip offers a retry. */
    data class Failed(val message: String) : FileUploadState
}

/**
 * The message composer, laid out like the harness's own: the `+` and the permission chip on the
 * left, the send affordance on the right.
 *
 * The model selector is deliberately *not* here — it moved to the top bar, which leaves this row
 * for the two controls you change mid-conversation and keeps the composer from wrapping on a
 * narrow phone.
 */
@Composable
internal fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<PendingAttachment>,
    onRemoveAttachment: (Int) -> Unit,
    onRetryAttachment: (Int) -> Unit,
    permissions: PermissionSelect?,
    pendingPermission: String?,
    onPermissionPick: (String) -> Unit,
    contextBreakdown: ContextBreakdownView?,
    contextPressure: ContextPressureView?,
    running: Boolean,
    enabled: Boolean,
    onOpenSheet: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    deliveryMode: String = "queue",
    onDeliveryModeChange: (String) -> Unit = {},
    preparing: Boolean = false,
    cursor: Int = draft.length,
    onCursorChange: (Int) -> Unit = {},
    references: List<DraftReference> = emptyList(),
    onOpenReference: (DraftReference) -> Unit = {},
    /** Fork: opens the speech-recognizer sheet; null hides the microphone. */
    onVoiceInput: (() -> Unit)? = null,
    /** Fork: desktop-style paperclip next to the permission chip; null hides it. */
    onAttachFile: (() -> Unit)? = null,
    /** Fork: the model selector sits inside the action row, as on the desktop client. */
    modelSlot: (@Composable () -> Unit)? = null,
) {
    var editor by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(draft, androidx.compose.ui.text.TextRange(cursor))) }
    androidx.compose.runtime.LaunchedEffect(draft, cursor) {
        if (editor.text != draft || editor.selection.end != cursor) editor = editor.copy(text = draft, selection = androidx.compose.ui.text.TextRange(cursor.coerceIn(0, draft.length)))
    }
    val colors = DsTheme.colors
    val haptics = LocalHapticFeedback.current
    // A file that is still uploading has no receipt to cite yet, and one that failed never will;
    // the send affordance waits for the chips rather than sending a message that names neither.
    val attachmentsSettled = attachments.none { it is PendingAttachment.File && it.state !is FileUploadState.Ready }
    val canSend = enabled && !preparing && (draft.isNotBlank() || attachments.isNotEmpty()) && attachmentsSettled
    val currentDraft by rememberUpdatedState(draft)
    val currentOnDraftChange by rememberUpdatedState(onDraftChange)
    val currentOnSend by rememberUpdatedState(onSend)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.medium, vertical = DsSpacing.small)
            .animateContentSize(),
        shape = DsShapes.composer,
        color = colors.composerCard,
        shadowElevation = 2.dp,
    ) {
        BoxWithConstraints {
            val boundedHeight = constraints.hasBoundedHeight
            Column(
                Modifier.padding(horizontal = DsSpacing.medium, vertical = DsSpacing.small),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.small),
            ) {
                // Reserve the action row before measuring any draft content, including attachments.
                Column(
                    Modifier.then(if (boundedHeight) Modifier.weight(1f, fill = false) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.small),
                ) {
                if (references.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.small),
                    ) {
                        references.forEach { reference ->
                            val type = referenceKindLabel(reference.kind)
                            val openLabel = stringResource(R.string.ux_b_open_reference, type, reference.label)
                            Row(
                                Modifier.clip(DsShapes.cube).background(colors.hoverSolid),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    Modifier.heightIn(min = DsSpacing.touchTarget).widthIn(min = DsSpacing.touchTarget, max = 210.dp)
                                        .clickable { onOpenReference(reference) }
                                        .semantics(mergeDescendants = true) { contentDescription = openLabel }
                                        .padding(horizontal = DsSpacing.small),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.small),
                                ) {
                                    Icon(referenceKindIcon(reference.kind), null, Modifier.size(18.dp), tint = colors.labelSecondary)
                                    Text(technicalDisplay(reference.label), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = DsType.small13, color = colors.labelSecondary)
                                }
                                DsIconButton(Icons.Filled.Close, stringResource(R.string.ux_b_remove_reference, reference.label), {
                                    onDraftChange(draft.removeRange(reference.start, reference.end))
                                    onCursorChange(reference.start)
                                })
                            }
                        }
                    }
                }
                TextField(
                    value = editor,
                    onValueChange = {
                        val edited = atomicReferenceEdit(editor.text, it, references)
                        editor = edited
                        onDraftChange(edited.text)
                        onCursorChange(edited.selection.end)
                    },
                    visualTransformation = remember(references, colors.accent, colors.accentTertiary) {
                        ReferenceTransformation(references, androidx.compose.ui.text.SpanStyle(color = colors.accent, background = colors.accentTertiary))
                    },
                    // Measure the controls first, then give the editor the remaining height. With
                    // unbounded height, omit weight so the editor keeps its natural line height.
                    modifier = Modifier
                        .then(if (boundedHeight) Modifier.weight(1f, fill = false) else Modifier)
                        .fillMaxWidth().onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && (event.isCtrlPressed || event.isMetaPressed)) {
                            if (event.type == KeyEventType.KeyUp && canSend) {
                                val text = currentDraft
                                currentOnDraftChange("")
                                currentOnSend(text)
                            }
                            true
                        } else false
                    },
                    // No `keyboardActions` here. The field is multi-line, so it carries the default IME
                    // action and the on-screen return key inserts a newline — which means an
                    // `onSend` action could never fire, and the one that used to sit here never did.
                    // The send key is the button; Ctrl/Cmd+Enter above is the shortcut.
                    enabled = enabled,
                    placeholder = {
                        Text(
                            stringResource(R.string.chat_composer_hint),
                            style = DsType.std14,
                            color = colors.labelTertiary,
                        )
                    },
                    minLines = 1,
                    maxLines = 8,
                    textStyle = DsType.std14,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = colors.accent,
                        focusedTextColor = colors.labelPrimary,
                        unfocusedTextColor = colors.labelPrimary,
                    ),
                )

                if (preparing) Text(stringResource(R.string.photos_preparing), style = DsType.caption11)
                AnimatedVisibility(visible = attachments.isNotEmpty()) {
                    AttachmentStrip(attachments, onRemoveAttachment, onRetryAttachment)
                }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.compact),
                ) {
                    CircleAction(
                        icon = Icons.Filled.Add,
                        description = stringResource(R.string.chat_composer_commands),
                        size = 28,
                        background = colors.hoverSolid,
                        tint = colors.labelPrimary,
                        enabled = enabled,
                        onClick = onOpenSheet,
                    )

                    PermissionChip(
                        select = permissions,
                        pending = pendingPermission,
                        enabled = enabled,
                        onPick = onPermissionPick,
                    )

                    if (onAttachFile != null) {
                        CircleAction(
                            icon = Icons.Filled.AttachFile,
                            description = stringResource(R.string.chat_composer_attach),
                            size = 28,
                            background = colors.hoverSolid,
                            tint = colors.labelPrimary,
                            enabled = enabled,
                            onClick = onAttachFile,
                        )
                    }

                    if (running) {
                        Text(
                            text = stringResource(
                                if (deliveryMode == "steer") R.string.chat_delivery_steer else R.string.chat_delivery_queue,
                            ),
                            style = DsType.caption11,
                            color = if (deliveryMode == "steer") colors.accent else colors.labelTertiary,
                            modifier = Modifier
                                .clip(DsShapes.cube)
                                .clickable {
                                    onDeliveryModeChange(if (deliveryMode == "steer") "queue" else "steer")
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }

                    modelSlot?.let { slot ->
                        Box(Modifier.weight(1f, fill = false), contentAlignment = Alignment.CenterEnd) {
                            slot()
                        }
                    } ?: Spacer(Modifier.weight(1f))

                    if (onVoiceInput != null) {
                        CircleAction(
                            icon = Icons.Filled.Mic,
                            description = stringResource(R.string.chat_composer_voice),
                            size = 28,
                            background = colors.hoverSolid,
                            tint = colors.labelPrimary,
                            enabled = enabled,
                            onClick = onVoiceInput,
                        )
                    }

                    // One right-hand action, the way the mobile assistants do it: while a turn runs
                    // and the composer is empty it is Stop; the moment there is something to send it
                    // becomes Send (the host accepts queue/steer mid-turn); with nothing to send it is
                    // a dim Send rather than a second button.
                    val showStop = running && !canSend
                    CircleAction(
                        icon = if (showStop) null else Icons.Filled.ArrowUpward,
                        description = stringResource(
                            if (showStop) R.string.chat_composer_stop else R.string.chat_composer_send,
                        ),
                        size = 34,
                        background = when {
                            showStop -> colors.error
                            canSend -> colors.buttonInfoFill
                            else -> colors.buttonPrimaryDimmed
                        },
                        tint = if (showStop || canSend) Color.White else colors.labelTertiary,
                        enabled = showStop || canSend,
                        onClick = {
                            if (showStop) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStop()
                            } else if (canSend) {
                                val text = currentDraft
                                currentOnDraftChange("")
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnSend(text)
                            }
                        },
                    ) {
                        if (showStop) {
                            Box(
                                Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The permission preset chip.
 *
 * Renders nothing when the projection key is absent: that means the harness composes no permission
 * service at all, and a dead control would be worse than none. Labels come from the wire, because
 * the preset table is deployment-configurable — mapping ids to local strings would mislabel any
 * deployment that renamed one.
 */
@Composable
private fun PermissionChip(
    select: PermissionSelect?,
    pending: String?,
    enabled: Boolean,
    onPick: (String) -> Unit,
) {
    val colors = DsTheme.colors
    if (select == null) return
    var menuOpen by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<String?>(null) }
    val effective = pending ?: select.currentValue
    val option = select.options.firstOrNull { it.value == effective }
    val label = if (option != null) {
        displayPermissionPreset(option.value, option.name)
    } else {
        stringResource(R.string.permission_custom)
    }

    Box {
        Row(
            modifier = Modifier
                .clip(DsShapes.cube)
                .clickable(enabled = enabled && pending == null) { menuOpen = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .then(
                    if (pending != null) {
                        Modifier.skeleton(colors.bgLayer2, colors.hover, DsShapes.cube)
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // One icon only, as the desktop composer does: the permission preset is recognised by
            // the shield, and its name is the accessibility label rather than permanent text.
            Icon(
                Icons.Outlined.Shield,
                contentDescription = label,
                tint = if (effective == FULL_ACCESS_PRESET) colors.warnLabel else colors.labelSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        if (menuOpen) {
            PermissionMenu(
                select = select,
                current = effective,
                onDismiss = { menuOpen = false },
                onPick = { value ->
                    menuOpen = false
                    // Full access removes the approval prompt entirely, so it gets an explicit
                    // acknowledgement the way the desktop client does.
                    if (value == FULL_ACCESS_PRESET) confirming = value else onPick(value)
                },
            )
        }
    }

    confirming?.let { target ->
        FullAccessConfirmDialog(
            onDismiss = { confirming = null },
            onConfirm = {
                confirming = null
                onPick(target)
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Attachments
// ---------------------------------------------------------------------------

@Composable
private fun AttachmentStrip(
    attachments: List<PendingAttachment>,
    onRemove: (Int) -> Unit,
    onRetry: (Int) -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.small),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        attachments.forEachIndexed { index, attachment ->
            Box {
                when (attachment) {
                    is PendingAttachment.Image -> Box(
                        Modifier
                            .size(56.dp)
                            .clip(DsShapes.block)
                            .background(colors.bgModulePlatform),
                    ) {
                        attachment.preview?.let {
                            Image(
                                bitmap = it,
                                contentDescription = attachment.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    is PendingAttachment.File -> FileAttachmentChip(attachment, onRetry = { onRetry(index) })
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.toastBg)
                        .clickable { onRemove(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(
                            if (attachment is PendingAttachment.File) R.string.chat_composer_remove_file
                            else R.string.chat_composer_remove_image,
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * One file in the strip: name, then either its size, its upload progress, or the failure.
 *
 * The chip is the same height as an image tile so the strip does not step. A failed upload is
 * retried by tapping the chip itself — the remove affordance stays where it is on every kind.
 */
@Composable
private fun FileAttachmentChip(file: PendingAttachment.File, onRetry: () -> Unit) {
    val colors = DsTheme.colors
    val failed = file.state is FileUploadState.Failed
    Row(
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 120.dp, max = 200.dp)
            .clip(DsShapes.block)
            .background(colors.bgModulePlatform)
            .border(1.dp, if (failed) colors.error else colors.borderL3, DsShapes.block)
            .clickable(enabled = failed, onClick = onRetry)
            .padding(start = 10.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val state = file.state) {
            is FileUploadState.Uploading -> CircularProgressIndicator(
                progress = {
                    if (file.size > 0) (state.sent.toFloat() / file.size).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier.size(18.dp),
                color = colors.accent,
                trackColor = colors.borderL3,
                strokeWidth = 2.dp,
            )
            else -> Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = if (failed) colors.error else colors.labelSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                file.name,
                style = DsType.small13,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when (val state = file.state) {
                    is FileUploadState.Uploading -> stringResource(R.string.chat_attachment_uploading)
                    is FileUploadState.Ready -> fileSizeText(state.file.bytes)
                    is FileUploadState.Failed -> stringResource(R.string.chat_attachment_upload_failed)
                },
                style = DsType.caption11,
                color = if (failed) colors.error else colors.labelTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared circular affordance
// ---------------------------------------------------------------------------

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    description: String,
    size: Int,
    background: Color,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        // Described here rather than on the icon, because not every one of these has an icon: the
        // stop button draws a plain square through `content`, and while the description hung off
        // the icon that button announced nothing at all to a screen reader.
        modifier = Modifier.size(size.dp).semantics { this.contentDescription = description },
        enabled = enabled,
        shape = CircleShape,
        color = background,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            when {
                content != null -> content()
                icon != null -> Icon(
                    icon,
                    // The Surface above already carries it; repeating it here would have a screen
                    // reader say the label twice.
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size((size * 0.46f).dp),
                )
            }
        }
    }
}






