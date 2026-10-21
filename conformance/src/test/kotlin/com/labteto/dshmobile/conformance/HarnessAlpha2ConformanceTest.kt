package com.labteto.dshmobile.conformance

import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.core.wire.dto.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class HarnessAlpha2ConformanceTest {
    private lateinit var harness: HarnessProcess
    private lateinit var client: HarnessClient
    private lateinit var model: MockModel

    @Before fun boot() {
        Assume.assumeTrue(System.getenv("DSH_LEGACY_CONFORMANCE") != "true")
        Assume.assumeTrue(HarnessProcess.available())
        model = MockModel.start(listOf("tool_call_success", "success"), toolName = "working_directory", toolArguments = """{"cd":"child"}""")
        harness = HarnessProcess.start(model)
        File(harness.workspacePath, "child").mkdirs()
        client = HarnessClient(harness)
    }
    @After fun close() {
        if (this::client.isInitialized) client.close()
        if (this::harness.isInitialized) harness.close()
        if (this::model.isInitialized) model.close()
    }

    @Test fun `directory changes publish current projection while session list retains original project`() = runBlocking {
        val sid = client.api.sessionCreate(SessionCreateRequest(cwd = harness.workspacePath)).featureValue().sessionId
        client.mux.start()
        withTimeout(10_000) { client.mux.awaitOpen() }
        val follow = client.mux.open("session/follow", buildJsonObject {
            put("request", WireJson.encodeToJsonElement(SessionFollowRequest.serializer(), SessionFollowRequest(SessionAddress.Session(sessionId = sid), maxMessages = 50)))
        })
        try {
            client.api.sessionPrompt(SessionPromptRequest(UUID.randomUUID().toString(), sid, "queue", listOf(PromptContentPart.Text("change directory")))).featureValue()
            val events = mutableListOf<SessionWireEvent>()
            withTimeout(90_000) {
                while (events.none { it.type == "turn/end" }) {
                    when (val frame = WireJson.decodeFromJsonElement(SessionFollowFrame.serializer(), follow.receive() ?: error("follow ended"))) {
                        is SessionFollowFrame.Snapshot -> events += frame.records.map { it.event }
                        is SessionFollowFrame.Entry -> events += frame.record.event
                        is SessionFollowFrame.AssistantStream -> Unit
                    }
                }
            }
            val change = events.lastOrNull { it.type == "working-directory/change" } ?: error("no directory change: $events")
            val expected = File(harness.workspacePath, "child").canonicalFile
            assertEquals(expected, File(change.data.jsonObject.getValue("cwd").jsonPrimitive.content).canonicalFile)
            val projection = requireNotNull(client.api.sessionProjections(sid).featureValue())
            assertEquals("sequenced", projection.kind)
            assertNotNull(projection.sequenced)
            assertEquals(expected, File(projection.values.getValue("workingDirectory").jsonPrimitive.content).canonicalFile)
            val row = client.api.sessionList(null).featureValue().items.single { it.sessionId == sid }
            assertEquals(File(harness.workspacePath).canonicalFile, File(requireNotNull(row.cwd)).canonicalFile)
            assertEquals("current", row.formatStatus)
            assertNull(client.api.sessionProjections("missing-alpha2-session").featureValue())
        } finally { follow.cancel() }
    }
}
