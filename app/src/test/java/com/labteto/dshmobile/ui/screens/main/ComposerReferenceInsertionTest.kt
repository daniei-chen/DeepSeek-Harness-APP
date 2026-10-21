package com.labteto.dshmobile.ui.screens.main

import org.junit.Assert.*
import org.junit.Test

class ComposerReferenceInsertionTest {
    @Test fun `reference after Thai word survives another insertion and an edit`() {
        val draft = ComposerDraft(ComposerKey("host", "session"))
        draft.text = "ทดสอบข้อความต่อท้าย"
        val cursor = "ทดสอบข้อความ".length
        draft.insertReference(cursor, cursor, "@preview.md", "preview.md", "file", "preview.md")
        assertEquals("ทดสอบข้อความ @preview.md ต่อท้าย", draft.text)
        assertEquals(cursor + 1, draft.references.single().start)
        assertEquals(draft.references.single().end + 1, draft.selection)
        draft.insertReference(draft.selection, draft.selection, "@folder/", "folder", "folder", "folder")
        draft.text += " เพิ่มเติม"
        assertReferences(draft, listOf("@preview.md", "@folder/"))
    }

    @Test fun `reference at end of Thai draft keeps both chips on next edit`() {
        val draft = ComposerDraft(ComposerKey("host", "session"))
        draft.text = "ภาษาไทย"
        draft.insertReference(draft.text.length, draft.text.length, "@one", "one", "file", "one")
        assertEquals("ภาษาไทย @one ", draft.text)
        assertEquals(draft.text.length, draft.selection)
        draft.insertReference(draft.selection, draft.selection, "@two", "two", "session", "two")
        draft.text = "อ่าน " + draft.text
        assertReferences(draft, listOf("@one", "@two"))
    }

    @Test fun `typed at query replaces token without adding a second separator`() {
        val draft = ComposerDraft(ComposerKey("host", "session"))
        draft.text = "ภาษาไทย @pre"
        val query = activeReference(draft.text, draft.text.length)!!
        draft.insertReference(query.start, query.end, "@preview.md", "preview.md", "file", "preview.md")
        assertEquals("ภาษาไทย @preview.md ", draft.text)
        draft.text += "@"
        val next = activeReference(draft.text, draft.text.length)!!
        draft.insertReference(next.start, next.end, "@folder/", "folder", "folder", "folder")
        draft.text += "อ่าน"
        assertReferences(draft, listOf("@preview.md", "@folder/"))
    }

    @Test fun `typed at at start of empty draft needs no leading space`() {
        val draft = ComposerDraft(ComposerKey("host", "session"))
        draft.text = "@"
        val query = activeReference(draft.text, draft.selection.coerceAtLeast(1))!!
        draft.insertReference(query.start, query.end, "@one", "one", "file", "one")
        assertEquals("@one ", draft.text)
        assertEquals(0, draft.references.single().start)
        assertEquals(draft.text.length, draft.selection)
    }

    private fun assertReferences(draft: ComposerDraft, mentions: List<String>) {
        assertEquals(mentions, draft.references.map { it.mention })
        draft.references.forEach {
            assertEquals(it.mention, draft.text.substring(it.start, it.end))
            assertTrue(it.start == 0 || draft.text[it.start - 1].isWhitespace())
            assertTrue(it.end == draft.text.length || draft.text[it.end].isWhitespace())
        }
    }
}
