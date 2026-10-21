package com.labteto.dshmobile.ui.screens.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolSummaryTest {

    @Test
    fun `summary and elapsed join on one line`() {
        assertEquals("app/build.gradle.kts · 11ms", toolSummaryWithDuration("app/build.gradle.kts", "11ms"))
    }

    @Test
    fun `missing or blank parts are omitted instead of leaving separators`() {
        assertEquals("11ms", toolSummaryWithDuration(null, "11ms"))
        assertEquals("11ms", toolSummaryWithDuration("   ", "11ms"))
        assertEquals("Read", toolSummaryWithDuration("Read", null))
        assertEquals("Read", toolSummaryWithDuration("Read", "  "))
        assertNull(toolSummaryWithDuration(null, null))
        assertNull(toolSummaryWithDuration("", ""))
    }
}
