package com.labteto.dshmobile.webshell

import com.labteto.dshmobile.core.wire.PairingPayloadResult
import com.labteto.dshmobile.core.wire.RelayPairing
import com.labteto.dshmobile.core.wire.RelayPairingPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The QR the app draws must be the exact payload the app's own scanner accepts.
 *
 * These assertions guard the contract between the relay's invitation and the phone: if the
 * serializer drifts, a scanned code silently stops pairing, which is hard to notice from the UI.
 */
class RemoteAccessTest {

    private val payload = RelayPairingPayload(
        v = 1,
        kind = "dsh-relay-pair",
        url = "https://192.168.2.13:3443",
        plainUrl = "http://192.168.2.13:3444",
        fingerprint = "QdUjtyUYPQmgf4vdN9TzJvOi5lF1fezHMOBKp83M9oo=",
        code = "48192037",
        expiresAt = 1_792_598_696_597,
    )

    @Test
    fun `encoded invitation round-trips through the scanner parser`() {
        val text = RemoteAccess.encode(payload)
        val parsed = RelayPairing.parsePayload(text)
        assertTrue(parsed is PairingPayloadResult.Valid)
        val decoded = (parsed as PairingPayloadResult.Valid).payload
        assertEquals(payload, decoded)
        assertTrue(text.contains("\"kind\":\"dsh-relay-pair\""))
    }

    @Test
    fun `remaining time is minutes and seconds, never negative`() {
        val now = 1_000_000L
        assertEquals("5:00", RemoteAccess.remaining(now + 300_000, now))
        assertEquals("0:09", RemoteAccess.remaining(now + 9_400, now))
        assertEquals("0:00", RemoteAccess.remaining(now - 5_000, now))
    }
}
