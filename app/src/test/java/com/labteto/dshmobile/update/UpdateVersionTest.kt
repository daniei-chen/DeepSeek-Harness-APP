package com.labteto.dshmobile.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which GitHub releases count as newer than the running build.
 *
 * The comparison decides whether someone is shown a dialog they did not ask for, so the failure
 * that matters most is offering an update to a version they already have — which a plain string
 * comparison would do constantly (`"0.10.0" < "0.9.0"` as text).
 */
class UpdateVersionTest {

    @Test
    fun `a higher patch is newer`() {
        assertTrue(isNewerVersion("0.2.1", "0.2.0"))
    }

    @Test
    fun `a higher minor is newer`() {
        assertTrue(isNewerVersion("0.3.0", "0.2.9"))
    }

    @Test
    fun `a higher major is newer`() {
        assertTrue(isNewerVersion("1.0.0", "0.9.9"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(isNewerVersion("0.2.0", "0.2.0"))
    }

    @Test
    fun `an older version is not newer`() {
        assertFalse(isNewerVersion("0.1.9", "0.2.0"))
    }

    /** The comparison is numeric, not lexicographic. */
    @Test
    fun `double-digit components compare by value`() {
        assertTrue(isNewerVersion("0.10.0", "0.9.0"))
        assertFalse(isNewerVersion("0.9.0", "0.10.0"))
    }

    @Test
    fun `a v prefix on the tag is ignored`() {
        assertTrue(isNewerVersion("v0.2.1", "0.2.0"))
        assertFalse(isNewerVersion("v0.2.0", "0.2.0"))
    }

    /** A pre-release of a version already installed is not an upgrade. */
    @Test
    fun `a pre-release suffix does not make the same version newer`() {
        assertFalse(isNewerVersion("0.2.0-rc.1", "0.2.0"))
        assertTrue(isNewerVersion("0.3.0-rc.1", "0.2.0"))
    }

    @Test
    fun `build metadata is ignored`() {
        assertFalse(isNewerVersion("0.2.0+build.7", "0.2.0"))
    }

    @Test
    fun `a shorter version compares as if zero-padded`() {
        assertFalse(isNewerVersion("0.2", "0.2.0"))
        assertTrue(isNewerVersion("0.3", "0.2.9"))
    }

    /** A tag nobody can parse must mean "no update", never a crash or a bogus prompt. */
    @Test
    fun `an unparseable tag is not newer`() {
        assertFalse(isNewerVersion("nightly", "0.2.0"))
        assertFalse(isNewerVersion("", "0.2.0"))
    }

    @Test
    fun `a release is still offered when the running version is unparseable`() {
        assertTrue(isNewerVersion("0.2.1", "unknown"))
    }

    /** Plugin releases share the repository but are not app versions. */
    @Test
    fun `plugin tags are not app versions`() {
        assertEquals("0.9.35", appVersionOf("v0.9.35"))
        assertEquals("0.9.35", appVersionOf("0.9.35"))
        assertEquals("0.9.35", appVersionOf("v0.9.35-rc1"))
        assertEquals(null, appVersionOf("app-plugin-v0.1.8"))
        assertEquals(null, appVersionOf("nightly"))
    }

    /**
     * The regression this guards: a plugin release published after an app release becomes
     * GitHub's "latest release", and the checker must still pick the app release.
     */
    @Test
    fun `a newer plugin release does not hide an app release`() {
        val releases = listOf(
            GithubRelease("v0.9.35", "https://example/0.9.35"),
            GithubRelease("app-plugin-v0.1.8", "https://example/plugin"),
            GithubRelease("v0.9.34", "https://example/0.9.34"),
        )
        val picked = pickLatestAppRelease(releases)
        assertEquals("v0.9.35", picked?.tagName)
    }

    @Test
    fun `only plugin releases means no app release`() {
        val releases = listOf(GithubRelease("app-plugin-v0.1.8", "https://example/plugin"))
        assertEquals(null, pickLatestAppRelease(releases))
    }
}
