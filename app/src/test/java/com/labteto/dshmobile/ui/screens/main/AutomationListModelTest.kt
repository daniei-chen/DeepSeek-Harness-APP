package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.core.wire.dto.AutomationTask
import org.junit.Assert.*
import org.junit.Test

class AutomationListModelTest {
    private val active = AutomationTask(
        id = "active", kind = "daily", title = "Morning report", prompt = "Summarize the weather",
        scheduledAt = "2026-10-07T08:00:00Z", sessionId = "Session-Alpha", status = "active",
    )
    private val inactive = AutomationTask(
        id = "inactive", kind = "at", title = "Evening digest", prompt = "Review the calendar",
        scheduledAt = "2026-10-06T20:00:00Z", sessionId = "Session-Beta", status = "inactive",
    )
    private val tasks = listOf(active, inactive)

    @Test fun `search matches title prompt and session case insensitively and trims whitespace`() {
        listOf("  MORNING  ", "WEATHER", "session-ALPHA").forEach { query ->
            assertEquals(listOf(active), automationListModel(tasks, query).tasks)
        }
        assertEquals(tasks, automationListModel(tasks, "  ").tasks)
        assertTrue(automationListModel(tasks, "missing").tasks.isEmpty())
    }

    @Test fun `all active and inactive filters retain only the requested statuses`() {
        assertEquals(tasks, automationListModel(tasks, statusFilter = AutomationStatusFilter.ALL).tasks)
        assertEquals(listOf(active), automationListModel(tasks, statusFilter = AutomationStatusFilter.ACTIVE).tasks)
        assertEquals(listOf(inactive), automationListModel(tasks, statusFilter = AutomationStatusFilter.INACTIVE).tasks)
        assertEquals(listOf(inactive), automationListModel(tasks, "CALENDAR", AutomationStatusFilter.INACTIVE).tasks)
        assertTrue(automationListModel(tasks, "CALENDAR", AutomationStatusFilter.ACTIVE).tasks.isEmpty())
    }

    @Test fun `populated results have no empty state`() {
        AutomationStatusFilter.entries.forEach { filter ->
            assertNull(automationListModel(tasks, statusFilter = filter).emptyState)
        }
    }

    @Test fun `empty catalog shows no tasks even with a search`() {
        assertEquals(AutomationEmptyState.NO_TASKS, automationListModel(emptyList()).emptyState)
        assertEquals(AutomationEmptyState.NO_TASKS, automationListModel(emptyList(), "report").emptyState)
        assertEquals(AutomationEmptyState.NO_TASKS,
            automationListModel(emptyList(), statusFilter = AutomationStatusFilter.ACTIVE).emptyState)
    }

    @Test fun `nonempty catalog with no search or status matches shows no matches`() {
        assertEquals(AutomationEmptyState.NO_MATCHES, automationListModel(tasks, "absent").emptyState)
        assertEquals(AutomationEmptyState.NO_MATCHES,
            automationListModel(listOf(inactive), statusFilter = AutomationStatusFilter.ACTIVE).emptyState)
        assertEquals(AutomationEmptyState.NO_MATCHES,
            automationListModel(listOf(active), "report", AutomationStatusFilter.INACTIVE).emptyState)
    }

    @Test fun `inactive filter with empty search has its own empty state including an empty catalog`() {
        listOf(emptyList(), listOf(active)).forEach { catalog ->
            assertEquals(AutomationEmptyState.NO_INACTIVE_TASKS,
                automationListModel(catalog, "  ", AutomationStatusFilter.INACTIVE).emptyState)
        }
        assertEquals(AutomationEmptyState.NO_TASKS,
            automationListModel(emptyList(), "report", AutomationStatusFilter.INACTIVE).emptyState)
    }
}
