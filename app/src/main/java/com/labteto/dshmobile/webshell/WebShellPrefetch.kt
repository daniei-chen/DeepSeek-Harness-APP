package com.labteto.dshmobile.webshell

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * One freshly fetched top-level document, ready to be handed to the WebView's first request.
 *
 * The deployment answers the SPA document with `Cache-Control: no-store`, so Chromium's own cache
 * can never satisfy a cold start. Fetching that single document in parallel with WebView startup
 * and serving it from [android.webkit.WebViewClient.shouldInterceptRequest] removes the first-byte
 * round trip from every launch, while the hashed bundles remain cached by the normal HTTP cache.
 */
internal data class PrefetchedDocument(
    val url: String,
    val mimeType: String?,
    val statusCode: Int,
    val reasonPhrase: String,
    val headers: Map<String, String>,
    val setCookies: List<String>,
    val body: ByteArray,
)

/** The preload client is separate from the harness RPC client: it only ever fetches one HTML page. */
internal fun newPrefetchClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .callTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

/**
 * Fetch the shell document without waiting for the WebView.
 *
 * Returns null on any failure; the WebView then simply performs the request itself, so a broken
 * preload never makes the app worse than a plain WebView.
 *
 * @param cookie the already-seeded `Cookie` header, so a gateway that remembers this device answers
 *   with the app shell rather than the access-code page.
 */
internal fun prefetchDocument(client: OkHttpClient, url: String, cookie: String?): PrefetchedDocument? {
    val request = Request.Builder()
        .url(url)
        .header("Accept", "text/html,application/xhtml+xml")
        .apply {
            if (!cookie.isNullOrBlank()) header("Cookie", cookie)
        }
        .build()
    return runCatching {
        client.newCall(request).execute().use { response ->
            val body = response.body?.bytes() ?: return null
            val contentType = response.header("Content-Type").orEmpty()
            val mime = contentType.substringBefore(';').trim().ifEmpty { "text/html" }
            val headers = LinkedHashMap<String, String>()
            response.headers.names().forEach { name ->
                when (name.lowercase()) {
                    // Body framing belongs to the WebView, not to this in-memory response.
                    "content-type", "content-length", "content-encoding", "transfer-encoding",
                    "connection", "set-cookie" -> Unit
                    else -> headers[name] = response.headers.values(name).joinToString(", ")
                }
            }
            PrefetchedDocument(
                url = response.request.url.toString(),
                mimeType = mime,
                statusCode = response.code,
                reasonPhrase = response.message,
                headers = headers,
                setCookies = response.headers.values("Set-Cookie"),
                body = body,
            )
        }
    }.getOrNull()
}
