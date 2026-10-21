package com.labteto.dshmobile.ui.screens.main

import org.junit.Assert.assertEquals
import org.junit.Test

class TechnicalTextTest {

    @Test
    fun `technical text is isolated with unicode directional marks`() {
        assertEquals("\u2066app/build.gradle.kts\u2069", technicalDisplay("app/build.gradle.kts"))
        assertEquals("\u2066\u2069", technicalDisplay(""))
        // The original string is preserved verbatim, including non-ASCII path segments.
        assertEquals("\u2066项目/说明.md\u2069", technicalDisplay("项目/说明.md"))
    }
}
