package com.labteto.dshmobile.connection

import org.junit.Assert.*
import org.junit.Test

class EndpointHistoryTest {
    @Test fun `remembered sessions isolate scheme and proxy root with legacy fallback`() {
        val a = HostConfig("a", "A", "gateway", 443, useTls = true, basePath = "/team-a")
        val b = a.copy(id = "b", basePath = "/team-b")
        val plain = a.copy(id = "plain", useTls = false)
        val saved = mutableMapOf("gateway:443" to "legacy")
        assertEquals("legacy", a.rememberedSession(saved))
        assertEquals("legacy", b.rememberedSession(saved))
        saved[a.baseUrl] = "session-a"
        saved[b.baseUrl] = "session-b"
        saved[plain.baseUrl] = "session-plain"
        assertEquals("session-a", a.rememberedSession(saved))
        assertEquals("session-b", b.rememberedSession(saved))
        assertEquals("session-plain", plain.rememberedSession(saved))
        assertEquals("legacy", saved["gateway:443"])
        assertNull(a.rememberedSession(emptyMap()))
        val ipv6 = a.copy(host = "::1")
        assertEquals("old-ipv6", ipv6.rememberedSession(mapOf("::1:443" to "old-ipv6")))
    }
}
