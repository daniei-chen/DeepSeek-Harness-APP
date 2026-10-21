package com.labteto.dshmobile.ui.screens.main

import org.junit.Assert.*
import org.junit.Test

class Harness021UiModelTest {
    @Test fun `references move with UTF16 edits and dissolve when edited`() {
        val text = "😀 @file end"
        val ref = DraftReference(3, 8, "@file", "file", "file", "file")
        assertEquals(5, adjustReferences(text, "hi" + text, listOf(ref)).single().start)
        assertEquals(emptyList<DraftReference>(), adjustReferences(text, "😀 @files end", listOf(ref)).filter { it.end > 8 })
        assertTrue(adjustReferences(text, "😀 @Xile end", listOf(ref)).isEmpty())
    }
    @Test fun `structured composer survives serialization and rejected plain drafts migrate`() {
        val draft = ComposerDraft(ComposerKey("host", "session"))
        draft.text = "Read "
        draft.insertReference(5, 5, "@\"a b\"", "a b", "file", "a b")
        val encoded = com.labteto.dshmobile.core.wire.WireJson.encodeToString(SavedComposer.serializer(), SavedComposer(draft.text, draft.references))
        val restored = com.labteto.dshmobile.core.wire.WireJson.decodeFromString(SavedComposer.serializer(), encoded)
        assertEquals(draft.references, ComposerDraft(draft.key, restored).references)
    }
    @Test fun `reference grammar excludes email and quotes whitespace`() {
        assertNull(activeReference("me@example.com", 14))
        assertEquals("src/", activeReference("read @src/", 10)?.query)
        assertEquals("@\"a b/\"", fileMention("a b", true))
        assertNull(fileMention("bad\npath", false))
    }
    @Test fun `safe YAML metadata is separated without hiding malformed source`() {
        val parsed = documentFrontmatter("---\ntitle: Hello\ntags: [one, two]\n---\n# Body")!!
        assertEquals("Hello", parsed.fields.first().second)
        assertEquals("# Body", parsed.body)
        assertNull(documentFrontmatter("---\na: [broken\n---\nbody"))
        assertNull(documentFrontmatter("---\na: !!java/object foo\n---\nbody"))
        assertNull(documentFrontmatter("---\na: &a [*a]\n---\nbody"))
        assertNull(documentFrontmatter("---\na: value"))
    }
    @Test fun `model search matches provider and model subsequences`() {
        assertTrue(modelSearchMatches("ds flsh", "DeepSeek deepseek-v4-flash"))
        assertFalse(modelSearchMatches("gemini", "DeepSeek deepseek-v4-flash"))
    }
}
