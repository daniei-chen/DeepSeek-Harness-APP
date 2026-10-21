package com.labteto.dshmobile.ui.screens.main.commands

import com.labteto.dshmobile.core.wire.dto.CommandDescriptor
import com.labteto.dshmobile.core.wire.dto.SkillEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When the `/` menu opens above the composer, and what it lists for a typed token. */
class SlashMenuTest {

    @Test
    fun `a slash token leading the draft opens the menu`() {
        assertEquals(SlashQuery(""), activeSlash("/", 1))
        assertEquals(SlashQuery("rew"), activeSlash("/rew", 4))
        assertEquals(SlashQuery("rewind"), activeSlash("/rewind ", 7))
    }

    @Test
    fun `a slash mid-text, a cursor inside the token, or an argued line does not`() {
        assertNull(activeSlash("see /tmp/x", 10))
        assertNull(activeSlash("/rewind", 3))
        assertNull(activeSlash("/rewind @20", 11))
    }

    @Test
    fun `names match by prefix first, then substring, and never by description`() {
        val commands = listOf(
            CommandDescriptor(name = "hypercompact", description = "Compact on request"),
            CommandDescriptor(name = "prewarm", description = "Warm the caches"),
            CommandDescriptor(name = "rewind", description = "Rewind the conversation"),
        )
        val skills = listOf(SkillEntry(name = "rewrite", description = "Rewrite a passage", modelInvocable = true))
        assertEquals(listOf("rewind", "rewrite", "prewarm"), slashRows("rew", commands, skills).map { it.name })
        assertEquals(4, slashRows("", commands, skills).size)
    }
}
