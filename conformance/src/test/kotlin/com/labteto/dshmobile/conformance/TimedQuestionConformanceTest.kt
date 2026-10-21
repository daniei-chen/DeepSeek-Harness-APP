package com.labteto.dshmobile.conformance

import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.core.wire.dto.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class TimedQuestionConformanceTest {
    @Test fun `a claimed question continues on timeout and accepts a later answer`() = runBlocking {
        Assume.assumeTrue("new surface excluded from legacy run", System.getenv("DSH_LEGACY_CONFORMANCE") != "true")
        Assume.assumeTrue(HarnessProcess.available())
        val model = MockModel.start(listOf("tool_call_success", "success"), toolName = "ask_user_question",
            toolArguments = """{"questions":[{"id":"q","question":"Choose?","options":[{"label":"Yes"}]}],"timeout":30}""")
        // Config patches replace the whole value, so retain the shipped preset and all its tools.
        val standard = java.io.File(HarnessProcess.checkout(), "packages/bundle/web-app/presets/standard.patch.yml")
            .readLines().dropWhile { it != "- insert:" }.drop(1).joinToString("\n") { it.removePrefix("    ") }
        val askRow = "        name: '@deepseek-ai/dsh-tool-ask-user'"
        check(standard.contains(askRow)) { "the standard preset no longer declares tool-ask-user" }
        val patch = standard.replace(askRow, "$askRow\n        config:\n          mode: timed\n          timeout: 30")
        val harness = HarnessProcess.start(model, patch)
        val client = HarnessClient(harness)
        try {
            client.mux.start(); withTimeout(10_000) { client.mux.awaitOpen() }
            val events = client.mux.open(REMOTE_EVENT_STREAM_ENDPOINT, JsonObject(emptyMap()))
            val ready = WireJson.decodeFromJsonElement(RemoteEventFrame.serializer(), withTimeout(10_000) { events.receive()!! }) as RemoteEventFrame.Ready
            val session = client.api.sessionCreate(SessionCreateRequest(cwd = harness.workspacePath)).featureValue().sessionId
            client.api.sessionPrompt(SessionPromptRequest(UUID.randomUUID().toString(), session, "queue", listOf(PromptContentPart.Text("ask")))).featureValue()
            val request = withTimeout(40_000) {
                var question: RemoteEventFrame.Waterfall? = null
                while (question == null) {
                    val frame = WireJson.decodeFromJsonElement(RemoteEventFrame.serializer(), events.receive() ?: error("events ended"))
                    if (frame is RemoteEventFrame.Waterfall && frame.event == USER_QUESTIONS_REQUEST_EVENT) question = frame
                }
                question
            }
            val body = WireJson.decodeFromJsonElement(AskUserQuestionRequestEvent.serializer(), request.request)
            assertEquals(true, body.wait?.timed)
            val call = body.wait!!.callId
            val claim = client.mux.open("userQuestions/attachWait", buildJsonObject { put("agentId", session); put("callId", call) })
            val wait = WireJson.decodeFromJsonElement(QuestionWaitFrame.serializer(), withTimeout(10_000) { claim.receive()!! })
            assertTrue(wait.remainingMs > 0)
            delay(wait.remainingMs)
            client.api.answerEvent(ready.clientId, request.eventId, RemoteEventOutcome.Rejected(error = RemoteEventRejection("UserQuestionError", "Test timeout", "ASK_TIMED_OUT"))).featureValue()
            claim.cancel()
            suspend fun projection(): UserQuestionsView? {
                val row = client.api.sessionList().featureValue().items.single { it.sessionId == session }
                val raw = row.projections?.values?.get("userQuestions") ?: return null
                return WireJson.decodeFromJsonElement(UserQuestionsView.serializer(), raw)
            }
            withTimeout(30_000) { while (projection()?.active?.none { it.callId == call && it.state == "continued" } != false) delay(100) }
            assertTrue(client.api.answerContinuedQuestion(session, call, AskUserQuestionAnswer(listOf(AskUserQuestionAnswerItem("q", listOf("Yes"))))).featureValue())
            val settled = withTimeout(30_000) {
                var answer: SettledQuestion? = null
                while (answer == null) { answer = projection()?.settled?.firstOrNull { it.callId == call }; if (answer == null) delay(100) }
                answer
            }
            assertEquals(listOf("Yes"), settled.answers.single().selected)
            events.cancel()
        } finally { client.close(); harness.close(); model.close() }
    }
}
