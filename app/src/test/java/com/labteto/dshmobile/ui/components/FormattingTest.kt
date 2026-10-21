package com.labteto.dshmobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Formatting helpers shared by the transcript, stats footer, and settings pages. */
class FormattingTest {

    @Test
    fun `duration covers null, sub-second, seconds, minutes, and hours`() {
        assertEquals("—", formatDurationMs(null))
        assertEquals("0ms", formatDurationMs(0))
        assertEquals("999ms", formatDurationMs(999))
        assertEquals("1.0s", formatDurationMs(1_000))
        assertEquals("4.2s", formatDurationMs(4_200))
        assertEquals("59.9s", formatDurationMs(59_900))
        assertEquals("1:00", formatDurationMs(60_000))
        assertEquals("12:45", formatDurationMs(765_000))
        assertEquals("1:00:00", formatDurationMs(3_600_000))
        assertEquals("2:03:04", formatDurationMs(2 * 3_600_000 + 3 * 60_000 + 4_000))
    }

    @Test
    fun `tokens abbreviate at thousand and million`() {
        assertEquals("—", formatTokens(null))
        assertEquals("0", formatTokens(0))
        assertEquals("999", formatTokens(999))
        assertEquals("1.0k", formatTokens(1_000))
        assertEquals("1.2k", formatTokens(1_234))
        assertEquals("1.0M", formatTokens(1_000_000))
        assertEquals("2.5M", formatTokens(2_500_000))
    }
}
