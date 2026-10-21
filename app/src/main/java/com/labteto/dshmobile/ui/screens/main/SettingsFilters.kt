package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.core.wire.dto.PluginInventoryEntry

/**
 * Plugin-inventory search: blank query means "everything", otherwise match either the module
 * specifier or the loader entry id, case-insensitively.
 *
 * Pure so the filter is testable without a host, and so the empty state and the count cannot
 * disagree with the rows.
 */
internal fun filterPluginEntries(
    entries: List<PluginInventoryEntry>,
    query: String,
): List<PluginInventoryEntry> {
    val needle = query.trim()
    if (needle.isEmpty()) return entries
    return entries.filter { entry ->
        entry.moduleName.contains(needle, ignoreCase = true) ||
            entry.entryId.contains(needle, ignoreCase = true)
    }
}
