package com.labteto.dshmobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.wire.dto.AskUserQuestionItem
import com.labteto.dshmobile.ui.theme.DshTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class QuestionWindowFocusTest {
    @get:Rule val compose = createComposeRule()
    private val questions = listOf(AskUserQuestionItem(id = "answer", question = "Which approach?"))

    @Test fun inlineFocusReleasesWhenItsWindowLosesFocusAndReturnsOnForeground() {
        var foreground by mutableStateOf(true)
        var visible by mutableStateOf(true)
        var paused by mutableStateOf(false)
        compose.setContent {
            val realWindow = LocalWindowInfo.current
            val window = remember { object : WindowInfo by realWindow {
                override val isWindowFocused: Boolean get() = foreground
            } }
            DshTheme {
                CompositionLocalProvider(LocalWindowInfo provides window) {
                    if (visible) Box(Modifier.height(700.dp)) {
                        QuestionsPanel("inline", questions, { null }, { null },
                            onFocusChange = { paused = it }, statusContent = { Text(if (paused) "Paused" else "Running") })
                    }
                }
            }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().requestFocus()
        compose.waitUntil { paused }
        compose.runOnIdle { foreground = false }
        compose.waitUntil { !paused }
        compose.runOnIdle { foreground = true }
        compose.waitUntil { paused }
        compose.runOnIdle { visible = false }
        compose.waitUntil { !paused }
    }

    @Test fun expandedAnswerUsesDialogWindowInsteadOfUnfocusedParentWindow() {
        var paused by mutableStateOf(false)
        compose.setContent {
            val realWindow = LocalWindowInfo.current
            val unfocusedParent = remember { object : WindowInfo by realWindow {
                override val isWindowFocused: Boolean get() = false
            } }
            DshTheme {
                CompositionLocalProvider(LocalWindowInfo provides unfocusedParent) {
                    Box(Modifier.height(700.dp)) {
                        QuestionsPanel("sheet", questions, { null }, { null },
                            onFocusChange = { paused = it }, statusContent = { Text(if (paused) "Paused" else "Running") })
                    }
                }
            }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().requestFocus()
        compose.runOnIdle { assertFalse("An unfocused input window cannot pause the timer", paused) }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithContentDescription(context.getString(R.string.ux_b_view_questions)).performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().requestFocus()
        compose.waitUntil { paused }
        compose.onNodeWithContentDescription(context.getString(R.string.common_close)).performClick()
        compose.waitUntil { !paused }
    }
}
