package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.core.wire.dto.PluginInventoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsFiltersTest {

    private fun entry(module: String, id: String) = PluginInventoryEntry(
        entryId = id,
        moduleName = module,
        enabled = true,
        fiberPhase = null,
    )

    private val entries = listOf(
        entry("@deepseek-ai/dsh-client-ui-plan", "ui-plan"),
        entry("@deepseek-ai/dsh-client-ui-diff", "ui-diff"),
        entry("/local/plugins/markdown", "local-markdown"),
    )

    @Test
    fun `blank query keeps the whole inventory`() {
        assertEquals(entries, filterPluginEntries(entries, ""))
        assertEquals(entries, filterPluginEntries(entries, "   "))
    }

    @Test
    fun `matches module name or entry id case-insensitively`() {
        assertEquals(listOf("ui-plan"), filterPluginEntries(entries, "PLAN").map { it.entryId })
        assertEquals(listOf("ui-diff"), filterPluginEntries(entries, "ui-diff").map { it.entryId })
        assertEquals(listOf("local-markdown"), filterPluginEntries(entries, "markdown").map { it.entryId })
    }

    @Test
    fun `no match returns an empty list rather than everything`() {
        assertEquals(emptyList<PluginInventoryEntry>(), filterPluginEntries(entries, "nonexistent"))
    }
}
