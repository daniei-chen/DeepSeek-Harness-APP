package com.labteto.dshmobile.webshell

/** A parsed DSH address, with the scheme the shell should try first and its one-time fallback. */
internal data class ServerAddress(
    val host: String,
    val port: Int,
    val useTls: Boolean,
    val basePath: String,
    val url: String,
    /** The other scheme, tried once when the address was typed without one. */
    val alternateUrl: String? = null,
)

/**
 * Read an address the way people actually type it.
 *
 * `https://dsh.example.com`, `dsh.example.com`, `192.168.1.5:3080` and `dsh.example.com/ui` are
 * all valid. A missing scheme defaults to https — that is what a public host means — and the http
 * variant is remembered as the one-time fallback, because a bare LAN address is usually plain
 * HTTP. The shell switches over automatically if the first attempt fails, so the user never has
 * to know which one their deployment uses.
 */
internal fun parseServerAddress(raw: String): ServerAddress? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val explicitScheme = trimmed.contains("://")
    val candidate = if (explicitScheme) trimmed else "https://$trimmed"
    val uri = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
    val explicitPort = uri.port > 0
    val port = if (explicitPort) uri.port else if (scheme == "https") 443 else 80
    if (port !in 1..65535) return null
    val basePath = uri.path.orEmpty().trimEnd('/').takeIf { it.isNotEmpty() } ?: ""

    fun authorityFor(targetScheme: String, targetPort: Int): String = when {
        targetScheme == "https" && targetPort == 443 -> host
        targetScheme == "http" && targetPort == 80 -> host
        else -> "$host:$targetPort"
    }
    fun urlFor(targetScheme: String, targetPort: Int): String =
        "$targetScheme://${authorityFor(targetScheme, targetPort)}$basePath" +
            if (basePath.isBlank()) "/" else ""

    val alternateUrl = if (explicitScheme) {
        null
    } else {
        val alternateScheme = if (scheme == "https") "http" else "https"
        val alternatePort = if (explicitPort) port else if (alternateScheme == "https") 443 else 80
        urlFor(alternateScheme, alternatePort)
    }
    return ServerAddress(
        host = host,
        port = port,
        useTls = scheme == "https",
        basePath = basePath,
        // Preserve an exact non-root path (notably /relay/pair); only the origin gets a
        // trailing slash so the page's own relative URLs keep resolving.
        url = urlFor(scheme, port),
        alternateUrl = alternateUrl,
    )
}
