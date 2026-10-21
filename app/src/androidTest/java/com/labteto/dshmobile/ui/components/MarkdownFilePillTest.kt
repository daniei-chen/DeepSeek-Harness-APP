package com.labteto.dshmobile.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.ui.theme.DshTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** A workspace link drawn as a pill opens from anywhere on the pill, not only its left half. */
class MarkdownFilePillTest {
    @get:Rule val compose = createComposeRule()

    private val files = mutableListOf<String>()
    private val folders = mutableListOf<String>()

    private fun render(markdown: String) {
        compose.setContent {
            DshTheme {
                CompositionLocalProvider(
                    LocalFileOpener provides { path: String -> files += path },
                    LocalFolderOpener provides { path: String -> folders += path },
                ) {
                    MarkdownText(markdown, Modifier.width(320.dp))
                }
            }
        }
    }

    @Test fun pillOpensFromItsLeftEdge() {
        render("See [Notes.md](<docs/Notes.md>) for details.")
        compose.onNodeWithText("Notes.md", useUnmergedTree = true).performTouchInput { click(centerLeft + Offset(2f, 0f)) }
        compose.runOnIdle { assertEquals(listOf("docs/Notes.md"), files) }
    }

    @Test fun pillOpensFromItsRightEdge() {
        render("See [Notes.md](<docs/Notes.md>) for details.")
        compose.onNodeWithText("Notes.md", useUnmergedTree = true).performTouchInput { click(centerRight - Offset(2f, 0f)) }
        compose.runOnIdle { assertEquals(listOf("docs/Notes.md"), files) }
    }

    @Test fun shortPillBesideALongOneOpensFromItsRightEdge() {
        render("Edited [a.kt](<src/a.kt>) and [VeryLongFileNameForTesting.kt](<src/VeryLongFileNameForTesting.kt>).")
        compose.onNodeWithText("a.kt", useUnmergedTree = true).performTouchInput { click(centerRight - Offset(2f, 0f)) }
        compose.runOnIdle { assertEquals(listOf("src/a.kt"), files) }
    }

    @Test fun longLabelStaysInsideTheText() {
        val label = "a/very/long/label/that/no/phone/line/could/ever/hold/in/one/piece/Notes.md"
        render("See [$label](<docs/Notes.md>).")
        val text = compose.onNodeWithText("See $label.").fetchSemanticsNode().boundsInRoot
        val pill = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("pill right ${pill.right} past text right ${text.right}", pill.right <= text.right + 1f)
    }

    @Test fun folderPillGoesToTheFolderOpener() {
        render("Everything lives in [src](<app/src/>).")
        compose.onNodeWithText("src", useUnmergedTree = true).performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(listOf("app/src/"), folders)
            assertEquals(emptyList<String>(), files)
        }
    }
}
