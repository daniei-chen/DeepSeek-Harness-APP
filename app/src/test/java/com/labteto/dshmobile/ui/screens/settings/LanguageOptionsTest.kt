package com.labteto.dshmobile.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The language picker is data, not layout: a missing label or a duplicate tag would show up as an
 * unnamed row or a selection that cannot be made. This guards the shipped list.
 */
class LanguageOptionsTest {

    @Test
    fun `system default is first and clears the override`() {
        assertEquals(LanguageOptions.first(), LanguageOption(null, null, com.labteto.dshmobile.R.string.settings_language_system))
        assertNull(LanguageOptions.first().tag)
    }

    @Test
    fun `every option has a display label and a unique tag`() {
        LanguageOptions.forEach { option ->
            val hasLabel = option.label?.isNotBlank() == true || (option.labelRes ?: 0) != 0
            assertTrue("language ${option.tag} has no label", hasLabel)
        }
        val tags = LanguageOptions.mapNotNull { it.tag }
        assertEquals("language tags must be unique", tags.size, tags.toSet().size)
    }

    @Test
    fun `shipped locales cover the app's 11 translations`() {
        val tags = LanguageOptions.mapNotNull { it.tag }.toSet()
        assertEquals(
            setOf("en", "zh", "hi", "es", "fr", "ar", "bn", "pt", "ru", "ur", "th"),
            tags,
        )
        assertNotNull(LanguageOptions.first { it.tag == "zh" }.label)
    }
}
