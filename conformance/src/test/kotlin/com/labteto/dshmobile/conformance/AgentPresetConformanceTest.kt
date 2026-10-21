package com.labteto.dshmobile.conformance

import com.labteto.dshmobile.core.wire.RpcResult
import com.labteto.dshmobile.core.wire.dto.SessionCreateRequest
import com.labteto.dshmobile.core.wire.dto.SessionSummary
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** The preset chip's list-row source, checked against the harness rather than a hand-written row. */
class AgentPresetConformanceTest {

    private lateinit var harness: HarnessProcess
    private lateinit var client: HarnessClient

    @Before
    fun boot() {
        assumeTrue("no built harness checkout; see HarnessProcess", HarnessProcess.available())
        harness = HarnessProcess.start()
        client = HarnessClient(harness)
    }

    @After
    fun stop() {
        if (this::client.isInitialized) client.close()
        if (this::harness.isInitialized) harness.close()
    }

    private suspend fun row(sessionId: String): SessionSummary {
        val listed = client.api.sessionList()
        val items = (listed as? RpcResult.Ok)?.value?.items ?: error("session/list failed: $listed")
        return items.single { it.sessionId == sessionId }
    }

    @Test
    fun `a blank session reports its current agent preset only through the list projection`() = runBlocking {
        val listed = client.api.agentPresetList()
        val roster = (listed as? RpcResult.Ok)?.value ?: error("agentPresets/list failed: $listed")
        assumeTrue("agentPresets/list returned an empty roster", roster.presets.isNotEmpty())
        assumeTrue("agent preset mode selection is disabled on this harness", roster.modeSelectionEnabled)
        val presetIds = roster.presets.map { it.id }.toSet()

        val created = client.api.sessionCreate(SessionCreateRequest(cwd = harness.workspacePath))
        val sessionId = (created as? RpcResult.Ok)?.value?.sessionId
            ?: error("could not create a blank session: $created")

        // Issue #45 escaped mocks because none sent the projection-only shape session/list uses.
        val initial = row(sessionId)
        assertTrue("a newly created session must be blank", initial.blank)
        assertNull("session/list has no top-level agentPreset", initial.agentPreset)
        assertNotNull("the blank session must publish its effective preset", initial.agentPresetEffective)
        assertTrue("preset ${initial.agentPresetEffective} is not in $presetIds", initial.agentPresetEffective in presetIds)
        assertEquals(
            initial.projections?.values?.get("agentPreset")?.jsonPrimitive?.content,
            initial.agentPresetEffective,
        )

        // Changing a blank session advances the projection, not its immutable creation header.
        val alternative = roster.presets.firstOrNull { it.id != initial.agentPresetEffective && it.broken == null }
        if (alternative != null) {
            val selected = client.api.agentPresetSelect(sessionId, alternative.id)
            assertTrue("agentPresets/select failed: $selected", selected is RpcResult.Ok)
            val changed = row(sessionId)
            assertTrue("selecting a preset must leave the session blank", changed.blank)
            assertNull("selection must not introduce a top-level agentPreset", changed.agentPreset)
            assertEquals(alternative.id, changed.agentPresetEffective)
            assertEquals(alternative.id, changed.projections?.values?.get("agentPreset")?.jsonPrimitive?.content)
        }
    }
}
