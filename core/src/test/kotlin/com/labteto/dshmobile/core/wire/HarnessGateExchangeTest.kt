package com.labteto.dshmobile.core.wire

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fork: the access-code exchange of a self-hosted gateway, over a real socket.
 *
 * The gateway (nginx in front of a harness) answers `POST /dsh-login` with `p=<code>`. A correct
 * code answers 303 and `Set-Cookie: dsh_gate=…`; a wrong one answers 303 to the login page with no
 * cookie at all. Both are checked here, plus the form encoding, because a broken encoding would
 * reject an otherwise correct code and look exactly like a wrong password.
 */
class HarnessGateExchangeTest {

    @Test
    fun `a correct access code buys the gateway cookie`() = runBlocking {
        val forms = mutableListOf<String>()
        val paths = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/dsh-login") { exchange ->
            forms.add(exchange.requestBody.readBytes().toString(Charsets.UTF_8))
            paths.add(exchange.requestURI.toString())
            exchange.responseHeaders.add(
                "Set-Cookie",
                "dsh_gate=secret-value; Max-Age=31536000; Secure; HttpOnly; Path=/; SameSite=Lax",
            )
            exchange.sendResponseHeaders(303, -1)
            exchange.close()
        }
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            assertEquals(
                SessionExchange.Granted("dsh_gate=secret-value"),
                HarnessSession.exchangeGate(base, "p@ss word", OkHttpClient()),
            )
            assertEquals("p=p%40ss%20word", forms.single())
            assertEquals("/dsh-login", paths.single())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `a refused code issues nothing`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/dsh-login") { exchange ->
            exchange.responseHeaders.add("Location", "/dsh-login?e=1")
            exchange.sendResponseHeaders(303, -1)
            exchange.close()
        }
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            assertTrue(HarnessSession.exchangeGate(base, "wrong", OkHttpClient()) is SessionExchange.Refused)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `an unreachable gateway is reported as unreachable`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port = server.address.port
        server.stop(0)
        val outcome = HarnessSession.exchangeGate("http://127.0.0.1:$port", "x", OkHttpClient(), timeoutMs = 1_500)
        assertTrue(outcome is SessionExchange.Unreachable)
    }
}
