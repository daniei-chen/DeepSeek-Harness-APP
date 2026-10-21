package com.labteto.dshmobile.core.wire

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/** What a launch-token exchange produced, or why it did not. */
sealed class SessionExchange {
    /** The `Cookie` header value to send on every later request to this authority. */
    data class Granted(val cookie: String) : SessionExchange()

    /**
     * The harness answered but issued no session.
     *
     * Almost always a token from an earlier harness process: the token rotates on every start and
     * is never persisted, so a startup URL copied yesterday is simply not the current one.
     */
    data class Refused(val status: Int) : SessionExchange()

    /** The exchange never reached a harness. */
    data class Unreachable(val kind: TransportFailure, val message: String?) : SessionExchange()
}

/**
 * The browser-session exchange harness 0.1.2 requires before any `/api` call.
 *
 * From 0.1.2 the harness authenticates its complete API — every Remote call, the mux upgrade, and
 * the session-log download — against a signed cookie, and answers 401 without one. The only way
 * to obtain that cookie is to present the launch token the harness prints once per process to its
 * *index* route: `GET /?token=…`. The token is deliberately refused on `/api` paths and in an
 * `Authorization` header, so there is no alternative to this exchange and no way to fold it into
 * an ordinary call.
 *
 * This is for **direct** connections. Behind `dsh-relay` the relay holds the harness session and
 * injects it upstream, and a phone that sent the host's cookie across the network would be
 * carrying a credential it has no business holding.
 *
 * Nothing here retries. A refusal means the token is not current, and asking again with the same
 * one produces the same answer.
 */
object HarnessSession {

    /** Cookie names the harness issues; the suffix is the normalized authority it is bound to. */
    private const val COOKIE_PREFIX = "dsh-auth-"

    /** Fork: the self-hosted gateway's access-code cookie. */
    private const val GATE_COOKIE_PREFIX = "dsh_gate="

    /**
     * Exchange [token] at [baseUrl] for a browser session.
     *
     * Redirects are not followed: a successful exchange answers 3xx to the clean `/`, and the
     * cookie is on *that* response. Letting the HTTP layer chase it would discard the header this
     * whole call exists to read.
     */
    suspend fun exchange(
        baseUrl: String,
        token: String,
        client: OkHttpClient,
        timeoutMs: Long = 10_000,
    ): SessionExchange = withContext(Dispatchers.IO) {
        val exchanger = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        val url = resolveHarnessUrl(baseUrl, "/").newBuilder().addQueryParameter("token", token).build()
        val request = Request.Builder().url(url).get().build()
        try {
            exchanger.newCall(request).execute().use { response ->
                val cookie = response.headers("Set-Cookie")
                    .firstOrNull { it.startsWith(COOKIE_PREFIX) }
                    // Only the name=value pair travels back; `Path`, `HttpOnly` and `SameSite`
                    // are instructions to a browser, and repeating them on a request would be
                    // sending the server its own directives.
                    ?.substringBefore(';')
                if (cookie != null) SessionExchange.Granted(cookie) else SessionExchange.Refused(response.code)
            }
        } catch (e: IOException) {
            SessionExchange.Unreachable(TransportFailures.classify(e), e.message)
        }
    }

    /**
     * Fork: exchange a self-hosted gateway's access code for its session cookie.
     *
     * The gateway in front of the harness (`https://harness.example.com`) exposes a small login
     * form: `POST /dsh-login` with the access code in the `p` field. A correct code answers 303
     * with `Set-Cookie: dsh_gate=…`, which every later request — including the `/api/remote.mux`
     * upgrade — must carry. The gateway then injects the harness's own browser session upstream,
     * so this app never holds the host's credential itself.
     *
     * Redirects are not followed, for the same reason as [exchange]: the cookie is on the 3xx
     * response and letting OkHttp chase it would only fetch the page behind it.
     */
    suspend fun exchangeGate(
        baseUrl: String,
        code: String,
        client: OkHttpClient,
        timeoutMs: Long = 10_000,
    ): SessionExchange = withContext(Dispatchers.IO) {
        val exchanger = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder()
            .url(resolveHarnessUrl(baseUrl, "/dsh-login"))
            .post(FormBody.Builder().add("p", code).build())
            .build()
        try {
            exchanger.newCall(request).execute().use { response ->
                val cookie = response.headers("Set-Cookie")
                    .firstOrNull { it.startsWith(GATE_COOKIE_PREFIX) }
                    ?.substringBefore(';')
                if (cookie != null) SessionExchange.Granted(cookie) else SessionExchange.Refused(response.code)
            }
        } catch (e: IOException) {
            SessionExchange.Unreachable(TransportFailures.classify(e), e.message)
        }
    }

    /**
     * Pull a launch token out of whatever the user pasted.
     *
     * People copy the whole startup line, the whole URL, or just the token. Accepting all three
     * costs one function and removes the one step where a working token gets rejected for having
     * a `?token=` in front of it.
     */
    fun tokenFrom(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val marker = trimmed.indexOf("token=")
        val candidate = if (marker >= 0) {
            trimmed.substring(marker + "token=".length).takeWhile { it != '&' && !it.isWhitespace() }
        } else {
            trimmed.takeWhile { !it.isWhitespace() }
        }
        return candidate.takeIf { it.isNotEmpty() }
    }
}
