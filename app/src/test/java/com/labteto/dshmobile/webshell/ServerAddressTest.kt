package com.labteto.dshmobile.webshell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Address parsing is the first thing a user touches, and a scheme is not something they should
 * have to know. These cases pin the guessing rules: https for a bare host, http as the one-shot
 * fallback, and exact paths preserved.
 */
class ServerAddressTest {

    @Test
    fun `a full https URL keeps its scheme and has no fallback`() {
        val address = parseServerAddress("https://dsh.example.com")!!
        assertEquals("https://dsh.example.com/", address.url)
        assertEquals(null, address.alternateUrl)
        assertEquals(443, address.port)
        assertEquals(true, address.useTls)
    }

    @Test
    fun `a bare host defaults to https and remembers http`() {
        val address = parseServerAddress("deepseek.kaogong.art")!!
        assertEquals("https://deepseek.kaogong.art/", address.url)
        assertEquals("http://deepseek.kaogong.art/", address.alternateUrl)
    }

    @Test
    fun `a bare host with a port keeps that port for both schemes`() {
        val address = parseServerAddress("192.168.1.5:3080")!!
        assertEquals("https://192.168.1.5:3080/", address.url)
        assertEquals("http://192.168.1.5:3080/", address.alternateUrl)
    }

    @Test
    fun `a non-root path is preserved without adding a slash`() {
        val address = parseServerAddress("https://dsh.example.com/ui/")!!
        assertEquals("https://dsh.example.com/ui", address.url)
        assertEquals("/ui", address.basePath)
    }

    @Test
    fun `an unknown scheme is refused rather than guessed`() {
        assertNull(parseServerAddress("ftp://dsh.example.com"))
        assertNull(parseServerAddress(""))
        assertNull(parseServerAddress("   "))
    }

    @Test
    fun `the default port is omitted from the authority`() {
        assertEquals("https://dsh.example.com/", parseServerAddress("dsh.example.com:443")!!.url)
        assertEquals("http://dsh.example.com/", parseServerAddress("http://dsh.example.com:80")!!.url)
    }
}
