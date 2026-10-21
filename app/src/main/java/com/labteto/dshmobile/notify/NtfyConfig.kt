package com.labteto.dshmobile.notify

import android.content.Context
import java.net.URI

/** The user's own ntfy endpoint, stored locally. No project-hosted topic is ever shipped. */
data class NtfyEndpoint(
    val baseUrl: String,
    val topic: String,
) {
    val sseUrl: String get() = "$baseUrl/$topic/sse"
    val displayUrl: String get() = "$baseUrl/$topic"
}

object NtfyConfig {
    private const val PREFS = "ntfy_push"
    private const val KEY_URL = "url"

    fun raw(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, null)
            ?.takeIf { it.isNotBlank() }

    fun load(context: Context): NtfyEndpoint? = raw(context)?.let(::parse)

    /** Returns null when [value] is not a usable http(s) ntfy topic URL. */
    fun save(context: Context, value: String): NtfyEndpoint? {
        val endpoint = parse(value) ?: return null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, endpoint.displayUrl)
            .apply()
        return endpoint
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_URL).apply()
    }

    private fun parse(value: String): NtfyEndpoint? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
        val authority = if (uri.port > 0) "$host:${uri.port}" else host
        val segments = uri.path.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        val topic = segments.lastOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return NtfyEndpoint("$scheme://$authority", topic)
    }
}
