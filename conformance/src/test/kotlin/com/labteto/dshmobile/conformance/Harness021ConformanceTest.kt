package com.labteto.dshmobile.conformance

import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.core.wire.dto.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

/** Actual host storage and gateway calls, in the disposable test profile. No paid model or user profile. */
class Harness021ConformanceTest {
    private lateinit var harness: HarnessProcess
    private lateinit var client: HarnessClient
    private lateinit var model: MockModel
    @Before fun boot() {
        Assume.assumeTrue("new surfaces excluded from the explicit legacy run", System.getenv("DSH_LEGACY_CONFORMANCE") != "true")
        Assume.assumeTrue("built checkout required", HarnessProcess.available())
        model = MockModel.start(listOf("tool_call_success", "success"), toolName = "schedule_create", toolArguments = """{"title":"check","prompt":"check","every_seconds":3600}""")
        harness = HarnessProcess.start(model)
        client = HarnessClient(harness)
    }
    @After fun close() {
        if (this::client.isInitialized) client.close()
        if (this::harness.isInitialized) harness.close()
        if (this::model.isInitialized) model.close()
    }
    @Test fun `management catalogues and absent install receipts decode`() = runBlocking {
        val bundles = client.api.pluginBundles().featureValue()
        assertTrue(bundles.isNotEmpty())
        assertTrue(client.api.managedPlugins().featureValue().isNotEmpty())
        client.api.pluginRegistries().featureValue()
        assertNull(client.api.waitForPluginInstall("never-started").featureValue())
        assertEquals("not-running", client.api.cancelPluginInstall("never-started").featureValue().status)
        assertFalse(client.api.setBundleEnabled("dsh-conformance-nonexistent", true).featureValue().changed)
        assertFalse(client.api.setPluginEnabled("dsh-conformance-nonexistent", true).featureValue().changed)
        assertFalse(client.api.removePluginBundle("dsh-conformance-nonexistent").featureValue().changed)
        val session = client.api.sessionCreate(SessionCreateRequest(cwd = harness.workspacePath)).featureValue().sessionId
        client.api.sessionReferenceCandidates(session, "").featureValue()
        assertFalse(client.api.answerContinuedQuestion(session, "absent-call", AskUserQuestionAnswer(emptyList())).featureValue())
    }
    @Test fun `automation edits compare the complete record and preserve calendar rules`() = runBlocking {
        val session = client.api.sessionCreate(SessionCreateRequest(cwd = harness.workspacePath)).featureValue().sessionId
        client.api.sessionPrompt(SessionPromptRequest(UUID.randomUUID().toString(), session, "queue", listOf(PromptContentPart.Text("schedule")))).featureValue()
        val original = withTimeout(90_000) {
            var task: AutomationTask? = null
            while (task == null) { task = client.api.automationCatalog().featureValue().firstOrNull { it.sessionId == session }; if (task == null) delay(200) }
            task
        }
        assertEquals("every", original.kind)
        assertEquals(original.id, client.api.automationList(session).featureValue().single().id)
        val updated = client.api.automationUpdate(AutomationUpdate(session, original.id, original.expectedRecord(), title = "daily", change = automationTiming("daily", "09:30:00", "Asia/Bangkok", ""))).featureValue()
        assertEquals(true, updated.updated)
        assertEquals("Asia/Bangkok", updated.record?.timeZone)
        val stale = client.api.automationUpdate(AutomationUpdate(session, original.id, original.expectedRecord(), title = "stale")).featureValue()
        assertEquals("schedule_conflict", stale.code)
        var current = updated.record!!
        for ((kind, value) in listOf("weekly" to "10:00:00", "cron" to "0 11 * * 1", "at" to "2099-01-01T00:00:00Z")) {
            val result = client.api.automationUpdate(AutomationUpdate(session, current.id, current.expectedRecord(), change = automationTiming(kind, value, "Asia/Bangkok", "1,3,5"))).featureValue()
            assertEquals("$kind: $result", true, result.updated)
            current = result.record!!
        }
        assertTrue(client.api.automationHistory(session, original.id).featureValue().records.isEmpty())
        assertEquals(true, client.api.automationDelete(session, original.id).featureValue().deleted)
        assertTrue(client.api.automationList(session).featureValue().isEmpty())
    }
    @Test fun `a local bundle can be inspected installed disabled updated and removed`() = runBlocking {
        val directory = java.io.File(harness.workspacePath, "bundle").apply { mkdirs() }
        val name = "@conformance/mobile-bundle"
        fun manifest(version: String) = java.io.File(directory, "package.json").writeText("""{"name":"$name","version":"$version","dsh":{"bundle":{"patch":"cordis.patch.yml"}}}""")
        manifest("1.0.0")
        java.io.File(directory, "cordis.patch.yml").writeText("""[{"insert":[{"id":"mobile-probe","name":"./plugin.mjs"}]}]""")
        java.io.File(directory, "plugin.mjs").writeText("export function apply(ctx) { ctx.provide('mobileConformanceProbe', true) }\n")
        val spec = "file:" + directory.absolutePath.replace('\\', '/')
        val inspected = client.api.inspectPlugin(spec).featureValue()
        assertEquals("$inspected", name, inspected.name)
        client.mux.start()
        withTimeout(10_000) { client.mux.awaitOpen() }
        val events = client.mux.open(REMOTE_EVENT_STREAM_ENDPOINT, JsonObject(emptyMap()))
        assertTrue(WireJson.decodeFromJsonElement(RemoteEventFrame.serializer(), withTimeout(10_000) { events.receive()!! }) is RemoteEventFrame.Ready)
        val requestId = UUID.randomUUID().toString()
        val installing = async { client.api.installPlugin(spec, PluginInstallOptions(enabled = false, requestId = requestId)).featureValue() }
        withTimeout(30_000) {
            while (true) {
                val frame = WireJson.decodeFromJsonElement(RemoteEventFrame.serializer(), events.receive() ?: error("events ended"))
                if (frame is RemoteEventFrame.Emit && frame.event == "plugin-manager/install-state" &&
                    frame.args.first().jsonObject["requestId"]?.jsonPrimitive?.content == requestId) break
            }
        }
        val cookie = client.cookie
        installing.cancelAndJoin()
        client.close()
        client = HarnessClient(harness, existingCookie = cookie)
        val installed = withTimeout(90_000) { client.api.waitForPluginInstall(requestId).featureValue() }
        assertNotNull("the reconnected client must recover the active install", installed)
        installed!!
        assertTrue("$installed", installed.changed)
        assertEquals("1.0.0", installed.version)
        assertFalse(client.api.pluginBundles().featureValue().single { it.name == name }.enabled)
        // The host forgets completed receipts: null is an unknown outcome, never a success receipt.
        assertNull(client.api.waitForPluginInstall(requestId).featureValue())
        assertTrue(client.api.setBundleEnabled(name, true).featureValue().changed)
        val plugin = client.api.pluginBundles().featureValue().single { it.name == name }.rows.single()
        val entryId = requireNotNull(plugin.entryId)
        assertTrue(client.api.managedPlugins().featureValue().single { it.entryId == entryId }.enabled)
        assertTrue(client.api.setPluginEnabled(entryId, false).featureValue().changed)
        assertFalse(client.api.managedPlugins().featureValue().single { it.entryId == entryId }.enabled)
        assertTrue(client.api.setPluginEnabled(entryId, true).featureValue().changed)
        assertTrue(client.api.managedPlugins().featureValue().single { it.entryId == entryId }.enabled)
        assertTrue(client.api.setBundleEnabled(name, false).featureValue().changed)
        val nextDirectory = java.io.File(harness.workspacePath, "bundle-v2").apply { mkdirs() }
        java.io.File(nextDirectory, "package.json").writeText(java.io.File(directory, "package.json").readText().replace("1.0.0", "1.0.1"))
        for (file in listOf("cordis.patch.yml", "plugin.mjs")) java.io.File(directory, file).copyTo(java.io.File(nextDirectory, file))
        val nextSpec = "file:" + nextDirectory.absolutePath.replace('\\', '/')
        val updated = withTimeout(90_000) { client.api.installPlugin(nextSpec, PluginInstallOptions(enabled = false, requestId = UUID.randomUUID().toString())).featureValue() }
        assertTrue("$updated", updated.changed)
        assertEquals("1.0.1", updated.version)
        assertFalse("updating preserves disabled state", client.api.pluginBundles().featureValue().single { it.name == name }.enabled)
        assertTrue(client.api.removePluginBundle(name).featureValue().changed)
        assertTrue(client.api.pluginBundles().featureValue().none { it.name == name })
    }
}
