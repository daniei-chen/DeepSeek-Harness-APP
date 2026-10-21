package com.labteto.dshmobile.core.wire

import com.labteto.dshmobile.core.wire.dto.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class Harness021Test {
    @Test fun `all transport routes remain under a proxy prefix`() {
        for (route in listOf("/", "/api/host", "/api/remote-stream", "/api/file?x=1")) {
            assertEquals("https://example.com/harness/team" + route, resolveHarnessUrl("https://example.com/harness/team/?token=discard#fragment", route).toString())
        }
        assertEquals("http://[::1]:3080/api/host", resolveHarnessUrl("http://[::1]:3080", "/api/host").toString())
        assertThrows(IllegalArgumentException::class.java) { resolveHarnessUrl("https://host/prefix", "/../escape") }
        assertThrows(IllegalArgumentException::class.java) { resolveHarnessUrl("https://host/prefix", "//evil") }
    }
    @Test fun `calendar editing retains explicit zones and ISO weekdays`() {
        val weekly = automationTiming("weekly", "10:15:00", "Asia/Bangkok", "1,7")["weekly"]!!.jsonObject
        assertEquals("Asia/Bangkok", weekly["time_zone"]!!.jsonPrimitive.content)
        assertEquals(listOf(1,7), weekly["weekdays"]!!.jsonArray.map { it.jsonPrimitive.int })
        assertThrows(IllegalArgumentException::class.java) { automationTiming("weekly", "10:00", "UTC", "1,1") }
        assertThrows(IllegalArgumentException::class.java) { automationTiming("every", "59", "", "") }
        assertThrows(Exception::class.java) { automationTiming("at", "2026-10-10T12:00:00", "", "") }
    }
    @Test fun `streamed arguments show complete and partial strings but ignore nested names`() {
        assertEquals("work/a", partialArgumentStrings("""{"path":"work/a""")["path"]!!.jsonPrimitive.content)
        assertEquals("hello\nworld", partialArgumentStrings("""{"description":"hello\nworld","nested":{"path":"bad"},"more":"x""")["description"]!!.jsonPrimitive.content)
        assertFalse(partialArgumentStrings("""{"nested":{"path":"bad"}}""").containsKey("path"))
        assertEquals("abc", partialArgumentStrings("""{"path":"abc\u0""")["path"]!!.jsonPrimitive.content)
    }
    @Test fun `expected schedule is the complete stored record without catalogue decorations`() {
        val task = AutomationTask("t", "weekly", "test", "prompt", "2099-01-01T00:00:00Z", sessionId = "s", time = "10:00:00.000", timeZone = "UTC", weekdays = listOf(1,3))
        val expected = task.expectedRecord()
        assertFalse(expected.containsKey("sessionId")); assertFalse(expected.containsKey("status")); assertFalse(expected.containsKey("lastDelivery"))
        assertEquals(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(3))), expected["weekdays"])
    }
    @Test fun `legacy question requests still decode without a wait`() {
        assertNull(WireJson.decodeFromString(AskUserQuestionRequestEvent.serializer(), """{"questions":[]}""").wait)
        assertEquals(QuestionWait("call", true), WireJson.decodeFromString(AskUserQuestionRequestEvent.serializer(), """{"questions":[],"wait":{"callId":"call","timed":true}}""").wait)
    }
}
