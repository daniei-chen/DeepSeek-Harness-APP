package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.core.wire.dto.AutomationTask

internal enum class AutomationStatusFilter(val status: String?) {
    ALL(null), ACTIVE("active"), INACTIVE("inactive"),
}

internal enum class AutomationEmptyState { NO_TASKS, NO_MATCHES, NO_INACTIVE_TASKS }

internal data class AutomationListModel(
    val tasks: List<AutomationTask>,
    val emptyState: AutomationEmptyState?,
)

/** Filter the already session-scoped catalog, following the upstream empty-state precedence. */
internal fun automationListModel(
    tasks: List<AutomationTask>,
    search: String = "",
    statusFilter: AutomationStatusFilter = AutomationStatusFilter.ALL,
): AutomationListModel {
    val query = search.trim()
    val matches = tasks.filter { task ->
        (statusFilter.status == null || task.status == statusFilter.status) &&
            (task.title.contains(query, ignoreCase = true) ||
                task.prompt.contains(query, ignoreCase = true) ||
                task.sessionId.contains(query, ignoreCase = true))
    }
    val emptyState = when {
        matches.isNotEmpty() -> null
        statusFilter == AutomationStatusFilter.INACTIVE && query.isEmpty() -> AutomationEmptyState.NO_INACTIVE_TASKS
        tasks.isEmpty() -> AutomationEmptyState.NO_TASKS
        else -> AutomationEmptyState.NO_MATCHES
    }
    return AutomationListModel(matches, emptyState)
}
