package com.labteto.dshmobile.conformance

import com.labteto.dshmobile.core.wire.*
import com.labteto.dshmobile.core.wire.dto.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*

class NativeTlsConformanceTest {
    @Test fun `native HTTPS authenticates unary calls and the mux using verified TLS`() = runBlocking {
        Assume.assumeTrue(System.getenv("DSH_LEGACY_CONFORMANCE") != "true")
        Assume.assumeTrue(HarnessProcess.available())
        HarnessProcess.start(tls = true).use { harness ->
            assertTrue(harness.baseUrl.startsWith("https://"))
            HarnessClient(harness).use { client ->
                client.api.sessionList(null).featureValue()
                assertTrue(client.anonymous().sessionList(null) is RpcResult.Err)
                client.mux.start()
                withTimeout(10_000) { client.mux.awaitOpen() }
                val events = client.mux.open(REMOTE_EVENT_STREAM_ENDPOINT, JsonObject(emptyMap()))
                try {
                    val frame = WireJson.decodeFromJsonElement(RemoteEventFrame.serializer(), withTimeout(10_000) { events.receive()!! })
                    assertTrue(frame is RemoteEventFrame.Ready)
                } finally { events.cancel() }
            }
        }
    }
}
