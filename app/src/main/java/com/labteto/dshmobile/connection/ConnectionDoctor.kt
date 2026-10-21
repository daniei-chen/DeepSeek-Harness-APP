package com.labteto.dshmobile.connection

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** How one probe in a connection report ended. */
enum class DoctorStatus { OK, WARN, FAIL, SKIP }

/** One line of the connection report. [action] is the fix, already phrased for the user. */
data class DoctorStep(
    val label: String,
    val status: DoctorStatus,
    val detail: String,
    val action: String? = null,
)

/** Everything the doctor measured, in the order it was measured. */
data class DoctorReport(
    val target: String,
    val steps: List<DoctorStep>,
) {
    val failures: Int get() = steps.count { it.status == DoctorStatus.FAIL }
    val warnings: Int get() = steps.count { it.status == DoctorStatus.WARN }

    val summary: String
        get() = when {
            failures > 0 -> "连接失败：$failures 项未通过"
            warnings > 0 -> "可以连接，但有 $warnings 项需要注意"
            else -> "全部通过"
        }
}

/**
 * The connection doctor: an ordered, self-explaining probe of one DSH endpoint.
 *
 * The shell can fail in ways the page never reports — DNS, a closed port, a self-signed
 * certificate the WebView has not been told to trust, a gate that wants a login, a host plugin
 * that is missing — and each of them looks identical to the user ("it does not connect"). This
 * walks the same path the WebView walks and names the first thing that is wrong.
 *
 * It is deliberately read-only: no cookies are sent, no login is attempted, nothing is stored.
 * The TLS probe accepts any certificate because the point is to *report* the certificate, not to
 * decide whether to trust it — that decision stays with the WebView's per-host prompt.
 */
object ConnectionDoctor {

    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 8_000

    suspend fun run(
        url: String,
        relayPort: Int = 3443,
        wsConnected: Boolean? = null,
        runningOnPhone: Boolean = true,
    ): DoctorReport = withContext(Dispatchers.IO) {
        val steps = mutableListOf<DoctorStep>()
        val uri = runCatching { URI(url.trim()) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        val host = uri?.host
        val port = when {
            uri == null -> -1
            uri.port > 0 -> uri.port
            scheme == "https" -> 443
            else -> 80
        }

        // 1. Address shape ---------------------------------------------------
        steps += if (uri == null || host.isNullOrBlank() || (scheme != "http" && scheme != "https")) {
            DoctorStep("地址", DoctorStatus.FAIL, "不是可用的 http(s) 地址", "在设备设置里重新填写完整地址")
        } else {
            DoctorStep("地址", DoctorStatus.OK, "$scheme://$host:$port")
        }

        if (uri == null || host.isNullOrBlank()) return@withContext DoctorReport(url, steps)

        // 2. A loopback address on a phone points at the phone itself. -----------------
        val loopback = host == "127.0.0.1" || host == "localhost" || host == "::1" || host == "[::1]"
        if (loopback && runningOnPhone) {
            steps += DoctorStep(
                "回环地址",
                DoctorStatus.FAIL,
                "127.0.0.1 在手机上指手机自己，不是电脑",
                "改成电脑的局域网 IP（如 https://192.168.2.13:3443/relay/pair）",
            )
        }

        // 3. DNS --------------------------------------------------------------
        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
        steps += if (address == null) {
            DoctorStep("DNS", DoctorStatus.FAIL, "无法解析 $host", "检查 Wi-Fi / DNS，或直接填 IP")
        } else {
            DoctorStep("DNS", DoctorStatus.OK, address.hostAddress ?: host)
        }
        if (address == null) return@withContext DoctorReport(url, steps)

        // 4. TCP --------------------------------------------------------------
        val tcpStarted = System.nanoTime()
        val tcp = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = CONNECT_TIMEOUT_MS
            }
        }
        val tcpMs = ((System.nanoTime() - tcpStarted) / 1_000_000).toInt()
        steps += if (tcp.isSuccess) {
            DoctorStep("TCP", DoctorStatus.OK, "$host:$port 可连接 · $tcpMs ms")
        } else {
            DoctorStep(
                "TCP",
                DoctorStatus.FAIL,
                "端口 $port 连不上（${tcp.exceptionOrNull()?.javaClass?.simpleName ?: "失败"}）",
                "确认 DSH/中继在运行、防火墙放行，且手机与电脑在同一网络",
            )
        }
        if (tcp.isFailure) return@withContext DoctorReport(url, steps)

        // 5. TLS --------------------------------------------------------------
        if (scheme == "https") {
            val cert = runCatching {
                trustAllContext().socketFactory.createSocket(host, port).use { raw ->
                    (raw as SSLSocket).apply {
                        soTimeout = READ_TIMEOUT_MS
                        startHandshake()
                    }.session.peerCertificates.first() as X509Certificate
                }
            }
            steps += if (cert.isSuccess) {
                val x509 = cert.getOrThrow()
                val selfSigned = runCatching { x509.issuerDN.name == x509.subjectDN.name }.getOrDefault(false)
                val daysLeft = runCatching {
                    ((x509.notAfter.time - System.currentTimeMillis()) / 86_400_000).toInt()
                }.getOrDefault(0)
                if (selfSigned) {
                    DoctorStep(
                        "证书",
                        DoctorStatus.WARN,
                        "自签名证书（${x509.subjectDN.name}，剩余 $daysLeft 天）",
                        "首次连接时在 App 弹窗里点“信任”，或在网关换上受信任证书",
                    )
                } else {
                    DoctorStep("证书", DoctorStatus.OK, "${x509.issuerDN.name} · 剩余 $daysLeft 天")
                }
            } else {
                DoctorStep(
                    "证书",
                    DoctorStatus.FAIL,
                    "TLS 握手失败（${cert.exceptionOrNull()?.javaClass?.simpleName ?: "失败"}）",
                    "检查端口是否为 HTTPS，或证书是否过期/域名不匹配",
                )
            }
        } else {
            steps += DoctorStep("证书", DoctorStatus.SKIP, "明文 HTTP，无证书")
        }

        val client = trustAllClient()

        // 6. HTTP -------------------------------------------------------------
        val http = runCatching {
            client.newCall(
                Request.Builder().url(url.trim()).header("Accept", "text/html").get().build(),
            ).execute().use { response ->
                Triple(response.code, response.header("Location"), response.body?.string().orEmpty())
            }
        }.getOrNull()
        steps += when {
            http == null -> DoctorStep("HTTP", DoctorStatus.FAIL, "请求没有完成", "重试；仍失败请检查代理/网络")
            http.first in 200..299 -> DoctorStep("HTTP", DoctorStatus.OK, "HTTP ${http.first}")
            http.first in 300..399 -> DoctorStep(
                "HTTP",
                DoctorStatus.WARN,
                "HTTP ${http.first} → ${http.second ?: "未知地址"}",
                "确认设备地址是否应包含该路径",
            )
            http.first == 401 || http.first == 403 -> DoctorStep(
                "HTTP",
                DoctorStatus.WARN,
                "HTTP ${http.first}：需要登录或网关验证",
                "在 App 页面完成登录；网关需要访问码时先输入访问码",
            )
            http.first >= 500 -> DoctorStep(
                "HTTP",
                DoctorStatus.FAIL,
                "HTTP ${http.first}：服务端错误",
                "查看 DSH 日志；确认反向代理指向正确的端口",
            )
            else -> DoctorStep("HTTP", DoctorStatus.WARN, "HTTP ${http.first}")
        }
        if (http != null && http.third.contains("dsh web authentication required")) {
            steps += DoctorStep(
                "DSH 登录",
                DoctorStatus.WARN,
                "网关要求先完成 DSH 登录",
                "在 App 里打开该地址，走一次登录流程",
            )
        }

        // 7. Host plugin -------------------------------------------------------
        val infoUrl = url.trim().trimEnd('/') + "/dsh-mobile/info"
        val info = runCatching {
            client.newCall(Request.Builder().url(infoUrl).header("Accept", "application/json").get().build())
                .execute().use { response -> response.code to response.body?.string().orEmpty() }
        }.getOrNull()
        steps += when {
            info == null -> DoctorStep("主机插件", DoctorStatus.WARN, "状态接口没有响应")
            info.first == 200 && info.second.contains("\"ok\":true") -> {
                val version = Regex("\"version\":\"([^\"]+)\"").find(info.second)?.groupValues?.get(1)
                DoctorStep("主机插件", DoctorStatus.OK, "已安装${version?.let { " · $it" } ?: ""}")
            }
            info.first == 404 -> DoctorStep(
                "主机插件",
                DoctorStatus.WARN,
                "未安装或版本过旧（404）",
                "在电脑上运行 setup 安装器并重启 DSH",
            )
            info.first == 401 || info.first == 403 -> DoctorStep(
                "主机插件",
                DoctorStatus.SKIP,
                "需要登录后才能检查",
            )
            else -> DoctorStep("主机插件", DoctorStatus.WARN, "HTTP ${info.first}")
        }

        // 8. Relay -------------------------------------------------------------
        val looksRelay = url.contains("/relay", ignoreCase = true)
        if (looksRelay) {
            val healthUrl = "${if (scheme == "https") "https" else "http"}://$host:$relayPort/relay/health"
            val health = runCatching {
                client.newCall(Request.Builder().url(healthUrl).header("Accept", "application/json").get().build())
                    .execute().use { response -> response.code to response.body?.string().orEmpty() }
            }.getOrNull()
            steps += when {
                health == null -> DoctorStep("中继", DoctorStatus.FAIL, "中继健康检查没有响应", "确认 dsh-relay 已启动")
                health.first == 200 && health.second.contains("dsh-relay") ->
                    DoctorStep("中继", DoctorStatus.OK, "dsh-relay 正常")
                health.first == 403 -> DoctorStep(
                    "中继",
                    DoctorStatus.WARN,
                    "中继拒绝了该 Host（403）",
                    "用安装器的 --public-hostname 加上这个域名，或改用局域网 IP",
                )
                else -> DoctorStep("中继", DoctorStatus.WARN, "HTTP ${health.first}")
            }
        }

        // 9. Live page connection ---------------------------------------------
        steps += when (wsConnected) {
            true -> DoctorStep("页面连接", DoctorStatus.OK, "已连接")
            false -> DoctorStep("页面连接", DoctorStatus.WARN, "页面未连接", "打开设备页面或点“重载”")
            null -> DoctorStep("页面连接", DoctorStatus.SKIP, "尚未建立")
        }

        DoctorReport(url, steps)
    }

    /** A client that only ever talks to the endpoint the user typed, for diagnostics. */
    private fun trustAllClient(): OkHttpClient {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .sslSocketFactory(context.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private fun trustAllContext(): SSLContext {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
    }
}
