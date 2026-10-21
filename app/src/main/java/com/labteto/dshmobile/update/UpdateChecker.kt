package com.labteto.dshmobile.update

import com.labteto.dshmobile.connection.HostsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** The subset of a GitHub release this app reads. */
@Serializable
internal data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
internal data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
)

/** One file attached to a release, by the name GitHub lists it under. */
data class ReleaseAsset(val name: String, val url: String)

/**
 * The repository's own update manifest (`latest.json` on `main`).
 *
 * Reading it removes two dependencies at once: GitHub's release ordering (which put a plugin
 * release ahead of an app release) and the unauthenticated API's rate limit. The manifest is a
 * plain static file, so it can be cached or mirrored by anything.
 */
@Serializable
internal data class UpdateManifest(
    val version: String = "",
    @SerialName("versionCode") val versionCode: Int = 0,
    val apk: String = "",
    val sha256: String = "",
    val page: String = "",
    @SerialName("minPluginVersion") val minPluginVersion: String = "",
    val notes: String = "",
)

/**
 * Only tags shaped like an app version are candidates: `v0.9.35`, `0.9.35`, `0.9.35-rc1`.
 *
 * The repository also publishes `app-plugin-v0.1.8`-style releases. GitHub's "latest release"
 * endpoint is ordered by creation time, not by version, so the plugin release published after an
 * app release becomes "latest" — and a checker that trusts it compares `0.1.8` with `0.9.35` and
 * reports "up to date" while an update is sitting right there.
 */
internal val APP_VERSION_TAG = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$")

/** The `x.y.z` core of an app release tag, or null for any other tag. */
internal fun appVersionOf(tag: String): String? {
    val match = APP_VERSION_TAG.matchEntire(tag.trim()) ?: return null
    return "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}"
}

/** The highest app-version release in [releases], ignoring plugin and other non-app tags. */
internal fun pickLatestAppRelease(releases: List<GithubRelease>): GithubRelease? {
    var best: GithubRelease? = null
    var bestVersion: String? = null
    for (release in releases) {
        val version = appVersionOf(release.tagName) ?: continue
        if (bestVersion == null || isNewerVersion(version, bestVersion)) {
            best = release
            bestVersion = version
        }
    }
    return best
}

/**
 * A release newer than the running build.
 *
 * [apk] and [checksums] are what "install via Termux" needs and are null when the release
 * carries no such files — the release page at [url] is always there.
 */
data class AvailableUpdate(
    val version: String,
    val url: String,
    val apk: ReleaseAsset? = null,
    val checksums: ReleaseAsset? = null,
)

/**
 * The APK to install and the checksum file to verify it with, out of a release's assets.
 *
 * The release workflow uploads `app-release.apk` and `SHA256SUMS.txt`; the APK is matched by
 * that name first and by extension second, so a renamed asset still installs. Only `https://`
 * downloads are considered: the URL is spliced into a shell command in Termux.
 */
internal fun pickReleaseAssets(assets: List<ReleaseAsset>): Pair<ReleaseAsset?, ReleaseAsset?> {
    val safe = assets.filter { it.url.startsWith("https://") && it.name.isNotBlank() }
    val apk = safe.firstOrNull { it.name == "app-release.apk" }
        ?: safe.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    val sums = safe.firstOrNull { it.name.equals("SHA256SUMS.txt", ignoreCase = true) }
    return apk to sums
}

/**
 * Is [candidate] a later version than [current]?
 *
 * Compares the numeric core only, so a `v` prefix and any `-rc1`/`-beta` suffix are ignored — which
 * also means a pre-release of a version already installed does not count as newer. Anything
 * unparseable in a component reads as 0 rather than throwing: a malformed tag should mean "no
 * update", never a crash on a screen the user did not ask for.
 *
 * A free function so the comparison can be tested without a network.
 */
internal fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(value: String): List<Int> = value.trim()
        .removePrefix("v")
        .substringBefore('-')
        .substringBefore('+')
        .split('.')
        .map { it.trim().toIntOrNull() ?: 0 }

    val a = parts(candidate)
    val b = parts(current)
    if (a.all { it == 0 }) return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(i) { 0 }
        val right = b.getOrElse(i) { 0 }
        if (left != right) return left > right
    }
    return false
}

/**
 * Asks GitHub whether a newer release exists.
 *
 * This is the only request the app itself makes to anything other than the harness the user
 * pointed it at, which is why it is behind a setting and why it never blocks anything: a failure
 * — offline, rate limited, no releases yet — leaves [available] null and is not reported. A
 * release the user has already declined stays declined until a later one appears. (Installing
 * through Termux downloads the APK too, but that is Termux fetching a file the user just asked
 * for, not this app phoning anywhere.)
 */
@Singleton
class UpdateChecker @Inject constructor(
    private val client: OkHttpClient,
    private val hostsStore: HostsStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<AvailableUpdate?>(null)

    /** The update to offer, or null when there is none, none wanted, or none confirmed yet. */
    val available: StateFlow<AvailableUpdate?> = _available.asStateFlow()

    /** When the automatic check last ran, so a warm reopen can decide whether to run it again. */
    @Volatile
    private var lastAutoCheckAt = 0L

    /**
     * The automatic check: on start, and again when the app comes back to the foreground after
     * [AUTO_CHECK_INTERVAL_MS]. The old once-per-process latch meant a phone that keeps the app in
     * memory for days never asked again, which is why a release published after the last cold
     * start was never offered.
     */
    suspend fun checkOnce(currentVersion: String) {
        val now = System.currentTimeMillis()
        if (now - lastAutoCheckAt < AUTO_CHECK_INTERVAL_MS) return
        val settings = runCatching { hostsStore.settingsOnce() }.getOrNull() ?: return
        if (!settings.updateCheckEnabled) return
        // Stamped only once a check really runs, so switching the check on is honoured at the
        // next resume rather than up to six hours later.
        lastAutoCheckAt = now
        check(currentVersion, ignoreDismissed = false)
    }

    /**
     * The manual check from Settings. Ignores the automatic-check switch (the user just asked) and
     * the dismissed version (asking again is withdrawing the dismissal), and reports every outcome
     * — including "up to date" and "could not reach GitHub" — because a button that says nothing
     * when there is nothing to offer looks broken.
     */
    suspend fun checkNow(currentVersion: String): CheckOutcome = check(currentVersion, ignoreDismissed = true)

    private suspend fun check(currentVersion: String, ignoreDismissed: Boolean): CheckOutcome {
        val release = fetchLatest() ?: return CheckOutcome.Unreachable
        val version = release.tagName.trim().removePrefix("v")
        if (version.isEmpty() || !isNewerVersion(version, currentVersion)) {
            _available.value = null
            return CheckOutcome.UpToDate(version.ifEmpty { currentVersion })
        }
        if (!ignoreDismissed) {
            val dismissed = runCatching { hostsStore.settingsOnce().dismissedUpdate }.getOrNull()
            if (dismissed == version) return CheckOutcome.Dismissed(version)
        }
        val (apk, checksums) = pickReleaseAssets(release.assets.map { ReleaseAsset(it.name, it.downloadUrl) })
        val update = AvailableUpdate(version, release.htmlUrl.ifBlank { RELEASES_URL }, apk, checksums)
        _available.value = update
        return CheckOutcome.Available(update)
    }

    /** Stop offering [version]; a later release will still be offered. */
    suspend fun dismiss(version: String) {
        _available.value = null
        runCatching { hostsStore.setDismissedUpdate(version) }
    }

    /**
     * The newest **app** release, not simply GitHub's "latest release".
     *
     * `/releases/latest` is ordered by creation time, and this repository also publishes host
     * plugin releases. A plugin release published after an app release therefore wins that
     * endpoint, and comparing `app-plugin-v0.1.8` against `0.9.35` reads as "up to date". Reading
     * the list and keeping only app-shaped tags removes the ordering dependency entirely.
     */
    private suspend fun fetchLatest(): GithubRelease? = withContext(Dispatchers.IO) {
        // The repository's own manifest wins when it is readable: it is a static file, so it has
        // no ordering rules and no API quota. The release API stays as a fallback for a clone or
        // a fork that has not published one yet.
        fetchManifest()?.let { manifest ->
            if (manifest.version.isNotBlank() && manifest.apk.startsWith("https://")) {
                return@withContext GithubRelease(
                    tagName = "v${manifest.version.trim().removePrefix("v")}",
                    htmlUrl = manifest.page.ifBlank { RELEASES_URL },
                    assets = listOf(GithubAsset("app-release.apk", manifest.apk)),
                )
            }
        }
        for (base in API_BASES) {
            val request = Request.Builder()
                .url("$base/repos/$REPO/releases?per_page=50")
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            val releases = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    json.decodeFromString<List<GithubRelease>>(body)
                }
            }.getOrNull()
            val newest = releases?.let(::pickLatestAppRelease)
            if (newest != null) return@withContext newest
        }
        // Older mirrors may not proxy the list endpoint; a direct single-release read still works
        // whenever GitHub's newest release happens to be an app one.
        for (base in API_BASES) {
            val request = Request.Builder()
                .url("$base/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            val release = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    json.decodeFromString(GithubRelease.serializer(), body)
                }
            }.getOrNull()
            if (release != null && appVersionOf(release.tagName) != null) return@withContext release
        }
        null
    }

    /** Read `latest.json` from the repository (or a mirror of it). */
    private fun fetchManifest(): UpdateManifest? {
        for (url in MANIFEST_URLS) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()
            val manifest = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    json.decodeFromString(UpdateManifest.serializer(), body)
                }
            }.getOrNull()
            if (manifest != null && manifest.version.isNotBlank()) return manifest
        }
        return null
    }

    /** What a check found. */
    sealed interface CheckOutcome {
        data class Available(val update: AvailableUpdate) : CheckOutcome
        data class UpToDate(val latest: String) : CheckOutcome
        data class Dismissed(val version: String) : CheckOutcome
        data object Unreachable : CheckOutcome
    }

    private companion object {
        /** Six hours: often enough to notice a release the same day, rare enough to be free. */
        const val AUTO_CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000
        const val REPO = "daniei-chen/DeepSeek-Harness-APP"
        val API_BASES = listOf(
            "https://api.github.com",
            "https://gh-proxy.com/https://api.github.com",
            "https://ghproxy.net/https://api.github.com",
        )
        const val MANIFEST = "https://raw.githubusercontent.com/$REPO/main/latest.json"
        val MANIFEST_URLS = listOf(
            MANIFEST,
            "https://gh-proxy.com/$MANIFEST",
            "https://cdn.jsdelivr.net/gh/$REPO@main/latest.json",
        )
        const val RELEASES_URL = "https://github.com/$REPO/releases/latest"
    }
}
