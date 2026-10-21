package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.dto.PermissionSelect
import com.labteto.dshmobile.ui.theme.DshTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ComposerNarrowLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val longDraft = (1..12).joinToString("\n") { "บรรทัดที่ $it ทดสอบภาษาไทยและ draft" }
    private val references = listOf("preview.md", "folder/", "another session").mapIndexed { index, name ->
        DraftReference(index, index + 1, "@$name", name, listOf("file", "folder", "session")[index], name)
    }
    private var pixelsPerDp = 1f

    @Test fun multilineThaiDraftWithThreeReferencesKeepsControlsOnNarrowPhone() {
        render(360.dp, 560.dp, 220.dp)
        assertControlsHaveHeight()
    }

    @Test fun multilineThaiDraftWithThreeReferencesKeepsControlsInLandscape() {
        render(640.dp, 360.dp, 180.dp, running = true)
        assertControlsHaveHeight()
        val stop = compose.onNodeWithContentDescription(context.getString(R.string.chat_composer_stop))
            .fetchSemanticsNode().boundsInRoot
        assertTrue("stop must retain its height", stop.height >= 40f * pixelsPerDp - 1f)
    }

    @Test fun unboundedHeightKeepsEditorAndReferenceActionsUsable() {
        val opened = mutableListOf<String>()
        var additions = 0
        render(360.dp, 560.dp, null, onOpenReference = { opened.add(it.target) }, onAddReference = { additions++ })
        assertControlsHaveHeight()
        // Match the named action; the visual label now contains display-only bidi isolates.
        compose.onNodeWithContentDescription(context.getString(R.string.ux_b_open_reference,
            context.getString(R.string.ux_b_file), "preview.md")).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.ux_b_open_reference,
            context.getString(R.string.ux_b_session), "another session")).performScrollTo().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.harness_add_reference)).performClick()
        compose.runOnIdle {
            assertEquals(listOf("preview.md", "another session"), opened)
            assertEquals(1, additions)
        }
    }

    private fun render(
        width: Dp, height: Dp, composerHeight: Dp?, running: Boolean = false,
        onOpenReference: (DraftReference) -> Unit = {}, onAddReference: () -> Unit = {},
    ) {
        compose.setContent {
            BoxWithConstraints {
                // Fit both device shapes on the assigned emulator without rotating or changing it.
                val density = Density(minOf(LocalDensity.current.density,
                    constraints.maxWidth / width.value, constraints.maxHeight / height.value))
                SideEffect { pixelsPerDp = density.density }
                CompositionLocalProvider(LocalDensity provides density) {
                    DshTheme {
                        Box(Modifier.size(width, height)) {
                            Column(if (composerHeight == null) Modifier.verticalScroll(rememberScrollState()) else Modifier) {
                                if (composerHeight != null) Spacer(Modifier.height(height - composerHeight))
                                Composer(
                                    draft = longDraft, onDraftChange = {}, attachments = emptyList(),
                                    onRemoveAttachment = {}, onRetryAttachment = {},
                                    permissions = PermissionSelect(currentValue = "custom"), pendingPermission = null, onPermissionPick = {},
                                    contextBreakdown = null, contextPressure = null,
                                    running = running, enabled = true, onOpenSheet = {}, onSend = {}, onStop = {},
                                    references = references, onOpenReference = onOpenReference, onAddReference = onAddReference,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun assertControlsHaveHeight() {
        compose.waitForIdle()
        val send = compose.onNodeWithContentDescription(context.getString(R.string.chat_composer_send))
            .fetchSemanticsNode().boundsInRoot
        val field = compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        assertTrue("send was vertically squeezed: $send", send.height >= 40f * pixelsPerDp - 1f)
        assertTrue("editor must keep visible height: $field", field.height > 0f)
    }
}
