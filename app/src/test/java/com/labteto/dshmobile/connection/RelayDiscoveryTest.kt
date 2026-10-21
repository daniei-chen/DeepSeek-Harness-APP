package com.labteto.dshmobile.connection

import com.labteto.dshmobile.core.wire.resolveHarnessUrl
import org.junit.Assert.*
import org.junit.Test

class RelayDiscoveryTest {
    @Test fun `redirected relay discovery retains normalized proxy root for pairing`() {
        for (path in listOf("/other/dsh/", "/other/unused/../dsh/", "/other/team%20a/", "/")) {
            val origin = "https://gateway:8443$path"
            val found = RelayProbe(origin, "pin", hostRefused = true).discoveredHost("original", 3080)
            assertEquals("gateway", found.host)
            assertEquals(8443, found.port)
            assertTrue(found.useTls)
            assertTrue(found.isRelay)
            assertTrue(found.hostRefused)
            assertEquals("pin", found.fingerprint)
            assertEquals(parseHostInput(origin)!!.basePath, found.basePath)
            assertEquals("https://gateway:8443${found.basePath}/relay/claim",
                resolveHarnessUrl(found.baseUrl, "/relay/claim").toString())
        }
    }
}
