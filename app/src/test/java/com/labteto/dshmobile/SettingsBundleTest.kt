package com.labteto.dshmobile

import com.labteto.dshmobile.connection.AppSettings
import com.labteto.dshmobile.connection.ConnectMode
import com.labteto.dshmobile.connection.HostConfig
import com.labteto.dshmobile.data.BundleError
import com.labteto.dshmobile.data.BundleResult
import com.labteto.dshmobile.data.SettingsBundle
import com.labteto.dshmobile.webshell.ShellDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Settings export/import is deliberately pure, so the whole contract is testable on the JVM with
 * no Android runtime: round-trip fidelity, the two refusal cases, and the "one bad entry must not
 * cost the file" rule that the drop counters exist for.
 */
class SettingsBundleTest {

    @Test fun `round trip keeps every field`() {
        val original = sampleBundle()
        val json = SettingsBundle.encode(original)
        val decoded = ok(SettingsBundle.decode(json))
        assertEquals(original, decoded.bundle)
        assertEquals(0, decoded.dropped)
    }

    @Test fun `encoded bundle is indented json without credentials`() {
        val json = SettingsBundle.encode(sampleBundle())
        assertTrue("expected indented output, got $json", json.contains("\n  "))
        // The one promise of the format: no credential can ride along, because none of the records
        // it is built from hold one — the harness sign-in cookie and the relay bearer token live in
        // HarnessSessionStore and RelayCredentialStore instead. (`relayTokenExpiresAt` is the
        // token's expiry timestamp, not the token, so it legitimately survives.)
        listOf("bearer", "cookie", "password", "secret").forEach { word ->
            assertFalse("bundle must not carry a $word: $json", json.contains(word, ignoreCase = true))
        }
    }

    @Test fun `bad json is reported instead of thrown`() {
        invalid(SettingsBundle.decode(""), BundleError.NotJson)
        invalid(SettingsBundle.decode("not json at all"), BundleError.NotJson)
        // Valid JSON, but not the object the format is.
        invalid(SettingsBundle.decode("[1,2,3]"), BundleError.NotJson)
        // The object, minus the fields no bundle can be without.
        invalid(SettingsBundle.decode("""{"schema":1}"""), BundleError.NotJson)
    }

    @Test fun `unknown schema is refused`() {
        invalid(
            SettingsBundle.decode(
                """{"schema":2,"exportedAt":1,"appVersion":"9.9.9","devices":[],"hosts":[],"settings":{}}""",
            ),
            BundleError.SchemaMismatch,
        )
        invalid(
            SettingsBundle.decode(
                """{"schema":0,"exportedAt":1,"appVersion":"0.0.1","devices":[],"hosts":[],"settings":{}}""",
            ),
            BundleError.SchemaMismatch,
        )
        invalid(
            SettingsBundle.decode("""{"schema":"two","exportedAt":1,"appVersion":"9.9.9"}"""),
            BundleError.SchemaMismatch,
        )
    }

    @Test fun `a bundle without the schema key is read as the current one`() {
        val decoded = ok(SettingsBundle.decode("""{"exportedAt":1,"appVersion":"1.0.5"}"""))
        assertEquals(SettingsBundle.SCHEMA, decoded.bundle.schema)
        assertTrue(decoded.bundle.devices.isEmpty())
        assertTrue(decoded.bundle.hosts.isEmpty())
        assertEquals(AppSettings(), decoded.bundle.settings)
    }

    @Test fun `invalid devices and hosts are dropped and counted`() {
        val json = SettingsBundle.encode(
            SettingsBundle(
                exportedAt = 1L,
                appVersion = "1.0.5",
                devices = listOf(
                    device(id = "keep", url = "https://dsh.example.com"),
                    device(id = "drop-scheme", url = "ftp://dsh.example.com"),
                    device(id = "drop-bare", url = "dsh.example.com:3080"),
                    device(id = "", url = "https://nameless.example.com"),
                ),
                hosts = listOf(
                    host(id = "keep", port = 3080),
                    host(id = "drop-zero", port = 0),
                    host(id = "drop-high", port = 70_000),
                    host(id = "", port = 3080),
                ),
            ),
        )
        val decoded = ok(SettingsBundle.decode(json))
        assertEquals(listOf("keep"), decoded.bundle.devices.map { it.id })
        assertEquals(listOf("keep"), decoded.bundle.hosts.map { it.id })
        assertEquals(3, decoded.droppedDevices)
        assertEquals(3, decoded.droppedHosts)
        assertEquals(6, decoded.dropped)
    }

    @Test fun `empty lists import cleanly`() {
        val decoded = ok(SettingsBundle.decode(SettingsBundle.encode(sampleBundle().copy(devices = emptyList(), hosts = emptyList()))))
        assertTrue(decoded.bundle.devices.isEmpty())
        assertTrue(decoded.bundle.hosts.isEmpty())
        assertEquals(0, decoded.dropped)
    }

    @Test fun `whitespace around validated fields is trimmed`() {
        val decoded = ok(
            SettingsBundle.decode(
                SettingsBundle.encode(
                    SettingsBundle(
                        exportedAt = 1L,
                        appVersion = "1.0.5",
                        devices = listOf(device(id = " d1 ", url = "  https://dsh.example.com  ")),
                        hosts = listOf(host(id = " h1 ", host = " 192.168.1.5 ")),
                    ),
                ),
            ),
        )
        assertEquals("https://dsh.example.com", decoded.bundle.devices.single().url)
        assertEquals("192.168.1.5", decoded.bundle.hosts.single().host)
        assertEquals(0, decoded.dropped)
    }

    @Test fun `unknown keys are ignored`() {
        val json = """
            {
              "schema": 1,
              "exportedAt": 5,
              "appVersion": "1.0.5",
              "future": { "anything": true },
              "devices": [],
              "hosts": [],
              "settings": { "themePreference": "dark", "notYetASetting": 7 }
            }
        """.trimIndent()
        val decoded = ok(SettingsBundle.decode(json))
        assertEquals("dark", decoded.bundle.settings.themePreference)
    }

    private fun ok(result: BundleResult): BundleResult.Ok {
        assertTrue("expected Ok, got $result", result is BundleResult.Ok)
        return result as BundleResult.Ok
    }

    private fun invalid(result: BundleResult, expected: BundleError) {
        assertTrue("expected Invalid($expected), got $result", result is BundleResult.Invalid)
        assertEquals(expected, (result as BundleResult.Invalid).reason)
    }

    private fun device(id: String = "d1", url: String = "https://dsh.example.com") =
        ShellDevice(id = id, name = "Device $id", url = url, alternateUrl = "http://dsh.example.com")

    private fun host(id: String = "h1", host: String = "192.168.1.5", port: Int = 3080) =
        HostConfig(id = id, name = "Host $id", host = host, port = port)

    /**
     * Every [AppSettings] field deliberately set away from its default, so a field this test forgets
     * to send through the serializer shows up as a round-trip mismatch instead of passing silently.
     */
    private val customSettings = AppSettings(
        autoConnectLast = false,
        autoConnectLan = true,
        autoConnectLoopback = false,
        autoStartLoopbackHarness = true,
        autoConnectRelay = true,
        keepConnectedInBackground = true,
        notifyTurnComplete = false,
        notifyGoal = false,
        notifyNeedsAction = false,
        connectMode = ConnectMode.RELAY,
        themePreference = "dark",
        showCodeWorkView = false,
        chatRenderer = "web",
        localeOverride = "zh-CN",
        knownPorts = listOf(3080, 8443),
        updateCheckEnabled = true,
        dismissedUpdate = "1.0.6",
        glassCompat = "off",
        simpleMode = false,
    )

    private fun sampleBundle() = SettingsBundle(
        exportedAt = 1_700_000_000_000L,
        appVersion = "1.0.5",
        devices = listOf(device("d1", "https://dsh.example.com"), device("d2", "http://192.168.1.5:3080")),
        hosts = listOf(host("h1", "192.168.1.5", 3080), host("h2", "relay.example.com", 443)),
        settings = customSettings,
        ntfy = "https://ntfy.example.com/dsh",
        unifiedPush = "https://ntfy.example.com/up/endpoint",
    )
}
