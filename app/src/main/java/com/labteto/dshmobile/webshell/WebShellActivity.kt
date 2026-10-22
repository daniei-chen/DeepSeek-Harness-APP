package com.labteto.dshmobile.webshell

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.labteto.dshmobile.BuildConfig
import com.labteto.dshmobile.R
import com.labteto.dshmobile.connection.HarnessSessionStore
import com.labteto.dshmobile.connection.HostConfig
import com.labteto.dshmobile.connection.HostsStore
import com.labteto.dshmobile.connection.ConnectionDoctor
import com.labteto.dshmobile.connection.DoctorReport
import com.labteto.dshmobile.connection.DoctorStatus
import com.labteto.dshmobile.connection.DoctorStep
import com.labteto.dshmobile.connection.SessionSnapshot
import com.labteto.dshmobile.connection.ConnectionManager
import com.labteto.dshmobile.connection.ConnectionPhase
import com.labteto.dshmobile.connection.ConnectionService
import com.labteto.dshmobile.connection.DiscoveryEngine
import com.labteto.dshmobile.connection.HarnessClientFactory
import com.labteto.dshmobile.connection.MdnsDiscovery
import com.labteto.dshmobile.connection.AppSettings
import com.labteto.dshmobile.core.wire.PairingPayloadResult
import com.labteto.dshmobile.core.wire.RelayPairing
import com.labteto.dshmobile.core.wire.RpcResult
import com.labteto.dshmobile.core.wire.dto.SessionSummary
import com.labteto.dshmobile.data.SessionRow
import com.labteto.dshmobile.data.SessionStore
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.labteto.dshmobile.notify.DshNotifications
import com.labteto.dshmobile.notify.NtfyConfig
import com.labteto.dshmobile.notify.NtfySubscriberService
import com.labteto.dshmobile.notify.UnifiedPushConfig
import com.labteto.dshmobile.update.UpdateChecker
import org.unifiedpush.android.connector.UnifiedPush
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The product surface: the harness web client, edge to edge, with a deliberately thin native shell.
 *
 * ## Why a WebView
 *
 * The deployment already ships `@dsh-android/dsh-client-ui-responsive`, a mobile adaptation layer
 * written for exactly this shell: below 768 CSS px it turns the left column into an off-screen
 * drawer, gives the right column a full-screen presentation, respects safe areas and the IME, and
 * publishes a `window.__dshBack()` layer stack for the Android back button. Re-implementing that UI
 * natively would be a second product that can never stay in step with the web build.
 *
 * ## What this class owns
 *
 * - **Boot**: one WebView, created immediately, plus a parallel preload of the SPA document so the
 *   first paint does not wait for a second round trip. The splash stays until the document commits.
 * - **Session**: the gateway cookie is seeded from the native credential store when this device has
 *   one, and any cookie obtained through the web login form is written back, so the native fallback
 *   and the web shell stay signed in together.
 * - **Native gaps**: file chooser (uploads), download manager, blob downloads, microphone for the
 *   voice-input plugin, external links, system bars/IME insets and the back button.
 *
 * Everything else — conversations, streaming, files, settings, automation — is the web client.
 */
@AndroidEntryPoint
@SuppressLint("SetJavaScriptEnabled")
class WebShellActivity : AppCompatActivity() {

    @Inject lateinit var hostsStore: HostsStore
    @Inject lateinit var harnessSessions: HarnessSessionStore
    @Inject lateinit var notifications: DshNotifications
    @Inject lateinit var connectionManager: ConnectionManager
    @Inject lateinit var discoveryEngine: DiscoveryEngine
    @Inject lateinit var mdnsDiscovery: MdnsDiscovery
    @Inject lateinit var harnessClientFactory: HarnessClientFactory
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var updateChecker: UpdateChecker

    private val cookieManager: CookieManager get() = CookieManager.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pageReady = AtomicBoolean(false)
    private val backAvailable = AtomicBoolean(false)
    private val failoverTried = mutableSetOf<String>()
    private var failoverInProgress = false
    private var pageHadError = false
    private var pendingSessionId: String? = null
    private var pendingNativeSpeech = false
    private var lanScanning = false
    private var sslPromptShowing = false
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var welcomeView: LinearLayout
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeSubtitle: TextView
    private val prefetchClient by lazy { newPrefetchClient() }
    private val chrome by lazy { WebShellChrome(this) }
    private val shellDevices by lazy { ShellDevices(this) }
    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf { it.isNotBlank() }?.let { handleScannedQr(it) }
    }

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var errorScroll: ScrollView
    private lateinit var errorTitle: TextView
    private lateinit var errorMessage: TextView

    @Volatile private var prefetchDeferred: Deferred<PrefetchedDocument?>? = null
    @Volatile private var interceptPrefetch = true
    private var targetHost: HostConfig? = null
    private var targetUrl: String = ""
    private var seededCookie: String? = null
    private var persistedCookie: String? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null
    private var insets = InsetsDp(0f, 0f, 0f)
    private var imeVisible = false
    private var glassCompat = "auto"
    private var inboxQuery = ""
    /**
     * Human-readable page lifecycle events, newest last.
     *
     * "It refreshed by itself" has several possible authors — the renderer crashing, a failed
     * main-frame load, a device failover, or the page's own JavaScript. The diagnostics page
     * shows this list so the next report names the author instead of guessing.
     */
    private val pageEvents = java.util.ArrayDeque<String>()
    private var relayPairingResult: kotlinx.coroutines.CompletableDeferred<String>? = null
    private var pageDark = false
    private var clearHistoryAfterLoad = false
    private var micPermissionAsked = false

    /** Wall-clock timestamps of main-document loads, used to break a WebView reload loop. */
    private val pageLoadTimes = java.util.ArrayDeque<Long>()
    /** Timestamps of renderer crashes; repeated crashes must not become an automatic reload loop. */
    private val rendererCrashTimes = java.util.ArrayDeque<Long>()
    private var reloadLoopNoticeShown = false

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted: Boolean ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (request == null) {
            if (pendingNativeSpeech) {
                pendingNativeSpeech = false
                if (granted) startNativeSpeech() else {
                    Toast.makeText(this, R.string.microphone_permission_denied, Toast.LENGTH_LONG).show()
                }
                return@registerForActivityResult
            }
            if (!granted) {
                Toast.makeText(
                    this@WebShellActivity,
                    R.string.microphone_permission_denied,
                    Toast.LENGTH_LONG,
                ).show()
            }
            return@registerForActivityResult
        }
        if (granted) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        } else {
            request.deny()
        }
    }

    private var pendingTestNotification = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted: Boolean ->
        val test = pendingTestNotification
        pendingTestNotification = false
        if (granted) {
            if (test) postTestNotificationInternal()
        } else {
            Toast.makeText(this@WebShellActivity, R.string.shell_settings_notification_disabled, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !pageReady.get() }
        enableEdgeToEdge()
        pendingSessionId = sessionIdFromIntent(intent)

        notifications.ensureChannels()
        if (NtfyConfig.load(this) != null) startNtfyService()
        buildContentView()
        configureWebView()
        installJavascriptInterfaces()
        installBackHandling()
        installInsetHandling()
        // Keep the glass-compat mode live: changing it in Settings re-marks the page immediately.
        lifecycleScope.launch {
            hostsStore.settings.collect { settings ->
                val mode = settings.glassCompat
                if (mode != glassCompat) {
                    glassCompat = mode
                    applyGlassCompat()
                }
            }
        }
        // Keep a read-only snapshot of the session list, so a phone that wakes up without a
        // network can still show what was running instead of an empty error page.
        lifecycleScope.launch {
            var lastSignature = ""
            sessionStore.sessions.collect { rows ->
                val hostId = targetHost?.id ?: return@collect
                val snapshot = rows.asSequence()
                    .filterNot { it.blank }
                    .sortedByDescending { it.updatedAt }
                    .take(30)
                    .map {
                        SessionSnapshot(
                            sessionId = it.sessionId,
                            title = it.title?.takeIf { title -> title.isNotBlank() } ?: it.sessionId,
                            updatedAt = it.updatedAt,
                            running = it.running,
                            pending = it.pendingInteraction,
                        )
                    }
                    .toList()
                val signature = snapshot.joinToString("|") {
                    "${it.sessionId}:${it.updatedAt}:${it.running}:${it.pending}"
                }
                if (signature != lastSignature) {
                    lastSignature = signature
                    hostsStore.saveSessionSnapshot(hostId, snapshot)
                }
            }
        }
        startLoading()
        if (shellDevices.load().isNotEmpty()) {
            mainHandler.postDelayed({ autoCheckForUpdate() }, 1800)
        }

        // A page that never commits (tunnel down, captive portal that never answers) must not keep
        // the splash on screen forever. The error screen, when one is deserved, is raised by the
        // WebViewClient callbacks; this is only the last-resort reveal.
        mainHandler.postDelayed({ markPageReady() }, SPLASH_TIMEOUT_MS)
    }

    // ---- layout -------------------------------------------------------------

    private fun buildContentView() {
        root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        webView = WebView(this)
        root.addView(
            webView,
            0,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        errorScroll = buildErrorView()
        root.addView(
            errorScroll,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        errorScroll.visibility = View.GONE
        welcomeView = buildWelcomeView()
        root.addView(
            welcomeView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        welcomeView.visibility = View.GONE
        setContentView(root)
        applyPageTheme(dark = isNightUi())
    }

    private fun buildWelcomeView(): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }
        column.addView(
            ImageView(this).apply {
                setImageResource(R.mipmap.ic_launcher)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(dp(82), dp(82))
            },
        )
        welcomeTitle = TextView(this).apply {
            text = getString(R.string.shell_welcome_title)
            textSize = 23f
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        }
        welcomeSubtitle = TextView(this).apply {
            text = getString(R.string.shell_welcome_subtitle)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(22))
        }
        column.addView(welcomeTitle)
        column.addView(welcomeSubtitle)
        column.addView(
            Button(this).apply {
                setText(R.string.shell_welcome_action)
                isAllCaps = false
                setOnClickListener { showSettings() }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        return column
    }

    private fun showWelcome() {
        if (!::welcomeView.isInitialized) return
        welcomeView.visibility = View.VISIBLE
        if (::errorScroll.isInitialized) errorScroll.visibility = View.GONE
    }

    private fun hideWelcome() {
        if (::welcomeView.isInitialized) welcomeView.visibility = View.GONE
    }

    private fun buildErrorView(): ScrollView {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(72), dp(28), dp(32))
        }
        errorTitle = TextView(this).apply {
            textSize = 20f
            setText(R.string.webshell_error_title)
        }
        errorMessage = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(12), 0, dp(24))
        }
        column.addView(errorTitle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(errorMessage, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(errorButton(R.string.webshell_reload) { reloadPage() })
        column.addView(errorButton(R.string.webshell_offline_snapshot) { showOfflineSnapshot() })
        column.addView(errorButton(R.string.webshell_clear_cache) { clearCacheAndReload() })
        column.addView(errorButton(R.string.shell_settings_title) { showSettings() })
        column.addView(errorButton(R.string.webshell_exit) { finishAffinity() })
        return ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            isFillViewport = true
            addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private fun errorButton(textRes: Int, action: () -> Unit): Button = Button(this).apply {
        setText(textRes)
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }
    }

    // ---- WebView ------------------------------------------------------------

    private fun configureWebView() {
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        webView.setBackgroundColor(Color.WHITE)
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
            // Honour the page's own `width=device-width` viewport instead of the legacy wide
            // viewport. Without this the harness client can be laid out against a virtual width
            // that no longer matches the window, and every height it derives from it is wrong.
            useWideViewPort = true
            loadWithOverviewMode = false
            setOffscreenPreRaster(true)
        }
        webView.webViewClient = shellClient
        webView.webChromeClient = shellChromeClient
        val currentUa = webView.settings.userAgentString ?: ""
        if (!currentUa.contains("DSHAndroidShell")) {
            webView.settings.userAgentString = "$currentUa DSHAndroidShell"
        }
        // Keep the renderer warm while the app is in the background; the page must keep its
        // WebSocket alive so returning to the app does not trigger a full reload.
        webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            onDownloadRequested(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun installJavascriptInterfaces() {
        webView.addJavascriptInterface(ShellBridge(), WebShellScripts.SHELL_BRIDGE)
        webView.addJavascriptInterface(BackBridge(), WebShellScripts.BACK_BRIDGE)
    }

    private fun installBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )
    }

    private fun installInsetHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val density = resources.displayMetrics.density.coerceAtLeast(0.1f)
            val status = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navigation = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val wasIme = imeVisible
            imeVisible = ime > 0
            insets = InsetsDp(status / density, navigation / density, ime / density)
            applyInsetsToPage()
            // A page measured while the keyboard was up can keep that height after it closes —
            // the chat then occupies a strip at the top with the page background below it. Wait
            // for the window to settle, then let the page re-measure against the full viewport.
            if (wasIme && !imeVisible) {
                mainHandler.postDelayed({ repairPageLayout() }, 250)
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    // ---- boot ---------------------------------------------------------------

    private fun startLoading() {
        lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) { resolveTarget() }
            if (resolved == null) {
                // First run: there is no device yet. Open Settings as the product's landing
                // surface instead of loading a fabricated personal URL.
                showWelcome()
                markPageReady()
                showSettings()
                return@launch
            }
            hideWelcome()
            targetHost = resolved.host
            targetUrl = resolved.url
            // Keep the native event/notification pipeline connected alongside the WebView. The
            // WebView owns the visible session; this connection is what lets NotificationObserver
            // turn approval/question waterfalls into Android notifications while the page is open
            // or in the background.
            launch { withContext(Dispatchers.IO) { runCatching { connectionManager.connect(resolved.host) } } }
            seededCookie = resolved.cookie
            if (!resolved.cookie.isNullOrBlank()) {
                cookieManager.setCookie(resolved.url, resolved.cookie)
                cookieManager.flush()
            }
            if (isFinishing || isDestroyed) return@launch
            interceptPrefetch = true
            val seededForPrefetch = cookieManager.getCookie(resolved.url)
            prefetchDeferred = lifecycleScope.async(Dispatchers.IO) {
                prefetchDocument(prefetchClient, resolved.url, seededForPrefetch)
            }
            webView.loadUrl(markAndroidUrl(resolved.url))
        }
    }

    private suspend fun resolveTarget(): ResolvedTarget? {
        val savedUrl = runCatching { hostsStore.shellUrl.first() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return null
        val host = runCatching { rememberHostForUrl(savedUrl) }.getOrNull() ?: return null
        val cookie = runCatching { harnessSessions.cookie(host.id) }.getOrNull()
        // A relay's /relay/pair route is exact; appending "/" turns it into a 404 page.
        val rootSuffix = if (host.basePath.isBlank()) "/" else ""
        return ResolvedTarget(host, host.baseUrl.trimEnd('/') + rootSuffix, cookie)
    }

    /**
     * Find (or remember) the endpoint a saved shell address names.
     *
     * The address is the shell's own fact, not a connection mode: it survives restarts even when the
     * person switches between the deployment and a plain LAN harness. A matching remembered host
     * keeps its gateway cookie; a new one simply starts signed out.
     */
    private suspend fun rememberHostForUrl(url: String, remembered: List<HostConfig>? = null): HostConfig? {
        val normalized = url.trimEnd('/')
        val hosts = remembered ?: hostsStore.hosts.first()
        hosts.firstOrNull { it.baseUrl.trimEnd('/') == normalized }?.let { return it }
        val uri = runCatching { java.net.URI(normalized) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host.orEmpty()
        if (host.isBlank()) return null
        val port = if (uri.port > 0) uri.port else if (scheme == "https") 443 else 80
        val basePath = uri.path.orEmpty().trimEnd('/').takeIf { it.isNotEmpty() } ?: ""
        return hostsStore.rememberHost(
            name = host,
            host = host,
            port = port,
            isLoopback = host == "127.0.0.1" || host == "localhost" || host == "::1",
            useTls = scheme == "https",
            basePath = basePath,
            isGateway = scheme == "https",
        )
    }

    /**
     * Detect a pathological main-document reload loop.
     *
     * A normal SPA load finishes once. Four document loads inside a minute means the page is
     * reloading itself (or the renderer is being torn down) faster than anyone can use it. Stop
     * hiding the error surface and tell the reader what happened instead of repainting the same
     * skeleton forever.
     */
    private fun recordPageLoad(url: String?): Boolean {
        val now = SystemClock.elapsedRealtime()
        synchronized(pageLoadTimes) {
            pageLoadTimes.addLast(now)
            while (pageLoadTimes.isNotEmpty() && now - pageLoadTimes.first() > 60_000L) {
                pageLoadTimes.removeFirst()
            }
            if (pageLoadTimes.size < 4) return false
        }
        Log.w(TAG, "web page reload loop detected: url=$url")
        if (!reloadLoopNoticeShown) {
            reloadLoopNoticeShown = true
            showError(
                getString(R.string.webshell_reload_loop_title),
                getString(R.string.webshell_reload_loop_message),
            )
        }
        return true
    }

    private fun markPageReady() {
        pageReady.set(true)
    }

    // ---- page callbacks -----------------------------------------------------

    private val shellClient = object : WebViewClient() {

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (!request.isForMainFrame || !interceptPrefetch) return null
            val requested = request.url.toString()
            if (requested.trimEnd('/') != targetUrl.trimEnd('/')) return null
            val deferred = prefetchDeferred ?: return null
            // shouldInterceptRequest runs on a WebView worker thread, so waiting here costs the page
            // nothing and saves the whole document round trip when the preload wins the race.
            val document = runBlocking {
                withTimeoutOrNull(PREFETCH_WAIT_MS) { deferred.await() }
            } ?: return null
            if (document.statusCode !in 200..299) return null
            // Only replay a document fetched for exactly this URL: a followed redirect would make
            // the page resolve its relative asset URLs against the wrong base.
            if (document.url.trimEnd('/') != requested.trimEnd('/')) return null
            document.setCookies.forEach { cookie -> cookieManager.setCookie(document.url, cookie) }
            interceptPrefetch = false
            return WebResourceResponse(
                document.mimeType ?: "text/html",
                null,
                document.statusCode,
                document.reasonPhrase,
                document.headers,
                ByteArrayInputStream(document.body),
            )
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            recordPageEvent("page started " + (url?.substringAfter("://")?.take(80) ?: ""))
            injectPageScripts()
        }

        override fun onPageCommitVisible(view: WebView, url: String?) {
            super.onPageCommitVisible(view, url)
            markPageReady()
            hideError()
            injectPageScripts()
            openPendingSession()
            applyInsetsToPage()
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            recordPageEvent("page finished")
            normalizeRelayDeviceUrl(url)
            val reloadLoop = recordPageLoad(url)
            if (!pageHadError) {
                failoverTried.clear()
                failoverInProgress = false
            }
            pageHadError = false
            markPageReady()
            if (!reloadLoop) hideError()
            injectPageScripts()
            openPendingSession()
            applyInsetsToPage()
            persistGatewayCookie()
            if (clearHistoryAfterLoad) {
                view.clearHistory()
                clearHistoryAfterLoad = false
            }
            ensureMicrophonePermission()
            // The client mounts its frame asynchronously; re-apply the insets once the frame exists.
            mainHandler.postDelayed({ applyInsetsToPage() }, 350)
            mainHandler.postDelayed({ applyInsetsToPage() }, 1200)
            // ...and once more after the shell has settled, so a root measured during the splash
            // cannot keep a short height.
            mainHandler.postDelayed({ repairPageLayout() }, 1600)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) {
                recordPageEvent("main-frame error ${error.errorCode} ${error.description}")
                pageHadError = true
                if (tryFailover()) return
                showError(getString(R.string.webshell_error_title), getString(R.string.webshell_error_network))
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            if (request.isForMainFrame && errorResponse.statusCode >= 500) {
                showError(
                    getString(R.string.webshell_error_title),
                    getString(R.string.webshell_error_http, errorResponse.statusCode),
                )
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            val authority = authorityOf(error.url)
            if (authority != null && isKnownDeviceAuthority(authority)) {
                if (shellDevices.isSelfSignedTrusted(authority)) {
                    handler.proceed()
                } else {
                    showSslTrustDialog(handler, authority, error)
                }
                return
            }
            handler.cancel()
            showError(getString(R.string.webshell_error_title), getString(R.string.webshell_error_ssl))
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            val scheme = uri.scheme?.lowercase() ?: return false
            // Custom schemes (baiduboxapp:, weixin:, alipays:, intent:, tel:, ...) must never be
            // loaded by a WebView; hand them to Android. This applies to sub-frames too: the
            // in-app browser workbench often opens such redirects inside its own frame.
            if (scheme != "http" && scheme != "https" && scheme != "blob" && scheme != "about" && scheme != "data") {
                openExternalUri(uri)
                return true
            }
            // Sub-frames are how the document preview loads HTML/PDF/Office blobs. Only the top
            // frame may be treated as a navigation/download decision; intercepting an iframe's
            // blob: URL here saved the preview as a file and left the preview pane blank.
            if (!request.isForMainFrame) return false
            if (scheme == "blob") {
                // A blob: navigation can only be a download; save it instead of showing binary.
                val quoted = org.json.JSONObject.quote(uri.toString())
                webView.evaluateJavascript(
                    "if(window.__dshShellSaveBlob){window.__dshShellSaveBlob($quoted,'download');}",
                    null,
                )
                return true
            }
            // http(s) stays inside the shell for the deployment itself and for the account
            // provider's own sign-in flow; every other site is handed to the system browser so an
            // arbitrary page never gains the shell's JavaScript bridge.
            if (scheme == "http" || scheme == "https") {
                val host = uri.host?.lowercase().orEmpty()
                val deployment = targetHost?.host?.lowercase().orEmpty()
                if (host == deployment || host == "deepseek.com" || host.endsWith(".deepseek.com")) {
                    return false
                }
                openExternalUri(uri)
                return true
            }
            return false
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recordPageEvent("renderer crash")
            val now = SystemClock.elapsedRealtime()
            synchronized(rendererCrashTimes) {
                rendererCrashTimes.addLast(now)
                while (rendererCrashTimes.isNotEmpty() && now - rendererCrashTimes.first() > 120_000L) {
                    rendererCrashTimes.removeFirst()
                }
                if (rendererCrashTimes.size >= 3) {
                    Log.e(TAG, "renderer crashed repeatedly; refusing another automatic reload")
                    showError(
                        getString(R.string.webshell_renderer_loop_title),
                        getString(R.string.webshell_renderer_loop_message),
                    )
                    return true
                }
            }
            recreateWebView()
            return true
        }
    }

    private val shellChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            val wantsAudio = request.resources.isEmpty() ||
                request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            if (!wantsAudio) {
                request.deny()
                return
            }
            if (ContextCompat.checkSelfPermission(
                    this@WebShellActivity,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            } else {
                pendingPermissionRequest?.deny()
                pendingPermissionRequest = request
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        override fun onShowFileChooser(
            webView: WebView,
            callback: ValueCallback<Array<Uri>>?,
            params: FileChooserParams?,
        ): Boolean {
            filePathCallback?.onReceiveValue(emptyArray())
            filePathCallback = callback
            val accepted = params?.acceptTypes
                ?.map { it.trim() }
                ?.filter { it.contains('/') }
                ?.toTypedArray()
                ?.takeIf { it.isNotEmpty() }
                ?: arrayOf("*/*")
            return runCatching {
                fileChooserLauncher.launch(accepted)
                true
            }.getOrElse {
                filePathCallback = null
                false
            }
        }
    }

    // ---- shell bridges ------------------------------------------------------

    private inner class ShellBridge {
        @android.webkit.JavascriptInterface
        fun toast(message: String) {
            runOnUiThread { Toast.makeText(this@WebShellActivity, message, Toast.LENGTH_SHORT).show() }
        }

        @android.webkit.JavascriptInterface
        fun openExternal(url: String) {
            runOnUiThread { openExternalUri(Uri.parse(url)) }
        }

        @android.webkit.JavascriptInterface
        fun saveFile(name: String, mime: String, base64: String) {
            lifecycleScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    WebShellDownloads.saveBase64(this@WebShellActivity, name, mime, base64)
                }
                if (isFinishing || isDestroyed) return@launch
                val message = if (saved != null) {
                    getString(R.string.webshell_download_saved, saved)
                } else {
                    getString(R.string.webshell_download_failed)
                }
                Toast.makeText(this@WebShellActivity, message, Toast.LENGTH_LONG).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun setDark(dark: Boolean) {
            runOnUiThread { applyPageTheme(dark) }
        }

        @android.webkit.JavascriptInterface
        fun startSpeechInput() {
            runOnUiThread { startNativeSpeech() }
        }

        /** Answer for [requestRelayPairing]; ignored when no request is outstanding. */
        @android.webkit.JavascriptInterface
        fun relayPairing(payload: String) {
            runOnUiThread { relayPairingResult?.complete(payload) }
        }
    }

    private inner class BackBridge {
        @android.webkit.JavascriptInterface
        fun setAvailable(available: Boolean) {
            backAvailable.set(available)
        }
    }

    // ---- page helpers -------------------------------------------------------

    private fun injectPageScripts() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(WebShellScripts.SHIMS, null)
        applyInsetsToPage()
        applyGlassCompat()
    }

    /** Re-evaluate the page for a glass theme that breaks fixed positioning. */
    private fun applyGlassCompat() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(WebShellScripts.glassCompat(glassCompat), null)
    }

    private fun applyInsetsToPage() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(WebShellScripts.insets(insets.top, insets.bottom, insets.ime), null)
    }

    /**
     * Make the page measure itself against the WebView again.
     *
     * A WebView that was resized while its surface was detached — the usual way a keyboard, a
     * rotation or a trip to the background ends — can keep the old frame: the app renders in a
     * strip at the top and the shell's own background shows below it. Nothing in the page will
     * fix that by itself, because nothing in the page knows the viewport changed. This re-asserts
     * the view's layout, asks the page to re-read the viewport, and (when explicitly requested)
     * detaches the hardware layer so the next frame is painted from scratch.
     */
    /**
     * @param forceRepaint the manual "repair layout" path. Only that one resizes the view and
     * detaches the hardware layer: both make the page re-render, and a silent pass must never do
     * that — a resize is what makes the harness remount the composer and re-request its model
     * list, which reads as "it refreshed by itself".
     */
    private fun repairPageLayout(forceRepaint: Boolean = false, notify: Boolean = false) {
        if (!::webView.isInitialized) return
        if (forceRepaint) {
            // A 1px resize is the only reliable way to make the renderer re-read the viewport:
            // `requestLayout()` with unchanged bounds is a no-op for the compositor, and that is
            // exactly how a WebView keeps painting a strip of page above an empty field.
            nudgeWebViewSize()
            val layer = webView.layerType
            webView.setLayerType(View.LAYER_TYPE_NONE, null)
            webView.post {
                if (!::webView.isInitialized) return@post
                webView.setLayerType(layer, null)
                webView.requestLayout()
            }
        }
        webView.evaluateJavascript(WebShellScripts.repairViewport(), null)
        if (notify) Toast.makeText(this, R.string.shell_diagnostics_repaired, Toast.LENGTH_SHORT).show()
    }

    /** Resize the WebView by one pixel and back, so `onSizeChanged` reaches the renderer. */
    private fun nudgeWebViewSize() {
        if (!::webView.isInitialized) return
        val params = webView.layoutParams as? FrameLayout.LayoutParams ?: return
        if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) return
        val height = webView.height
        if (height <= 1) return
        params.height = height - 1
        webView.layoutParams = params
        webView.post {
            if (!::webView.isInitialized) return@post
            val restore = webView.layoutParams as? FrameLayout.LayoutParams ?: return@post
            restore.height = ViewGroup.LayoutParams.MATCH_PARENT
            webView.layoutParams = restore
        }
    }

    private fun applyPageTheme(dark: Boolean) {
        pageDark = dark
        if (::root.isInitialized) root.setBackgroundColor(if (dark) DARK_BACKGROUND else Color.WHITE)
        if (::welcomeView.isInitialized) {
            welcomeView.setBackgroundColor(if (dark) DARK_BACKGROUND else Color.WHITE)
            if (::welcomeTitle.isInitialized) welcomeTitle.setTextColor(if (dark) DARK_TEXT else LIGHT_TEXT)
            if (::welcomeSubtitle.isInitialized) welcomeSubtitle.setTextColor(if (dark) DARK_SECONDARY else LIGHT_SECONDARY)
        }
        if (::webView.isInitialized) webView.setBackgroundColor(if (dark) DARK_BACKGROUND else Color.WHITE)
        if (::errorScroll.isInitialized) {
            errorScroll.setBackgroundColor(if (dark) DARK_BACKGROUND else Color.WHITE)
            errorTitle.setTextColor(if (dark) DARK_TEXT else LIGHT_TEXT)
            errorMessage.setTextColor(if (dark) DARK_SECONDARY else LIGHT_SECONDARY)
        }
        if (::webView.isInitialized) {
            val controller = WindowCompat.getInsetsController(window, webView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    private fun persistGatewayCookie() {
        val host = targetHost ?: return
        val raw = cookieManager.getCookie(targetUrl) ?: return
        val match = GATE_COOKIE.find(raw)?.groupValues?.getOrNull(1) ?: return
        if (match == seededCookie || match == persistedCookie) return
        persistedCookie = match
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { harnessSessions.rememberGatewayCookie(host.id, match) }
        }
    }

    private fun hideError() {
        if (::errorScroll.isInitialized) errorScroll.visibility = View.GONE
    }

    private fun showError(title: String, message: String) {
        markPageReady()
        if (!::errorScroll.isInitialized) return
        errorTitle.text = title
        errorMessage.text = message
        errorScroll.visibility = View.VISIBLE
    }

    /** Show the cached session list for the current host, without touching the network. */
    private fun showOfflineSnapshot() {
        val host = targetHost
        if (host == null) {
            Toast.makeText(this, R.string.shell_settings_no_device, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val rows = hostsStore.sessionSnapshot(host.id)
            if (isFinishing || isDestroyed) return@launch
            chrome.showOfflinePage(
                dark = pageDark,
                hostLabel = host.displayAddress,
                rows = rows,
                onRetry = { reloadPage() },
                onBack = { showSettings() },
            )
        }
    }

    // ---- navigation and recovery -------------------------------------------

    private fun reloadPage() {
        recordPageEvent("reload (requested)")
        failoverTried.clear()
        failoverInProgress = false
        pageHadError = false
        reloadLoopNoticeShown = false
        synchronized(pageLoadTimes) { pageLoadTimes.clear() }
        hideError()
        interceptPrefetch = false
        webView.loadUrl(markAndroidUrl(targetUrl))
    }

    private fun clearCacheAndReload() {
        webView.clearCache(true)
        Toast.makeText(this, R.string.webshell_cache_cleared, Toast.LENGTH_SHORT).show()
        reloadPage()
    }

    private fun recreateWebView() {
        if (isFinishing || isDestroyed) return
        recordPageEvent("webview recreated")
        val old = webView
        root.removeView(old)
        old.destroy()
        webView = WebView(this)
        root.addView(
            webView,
            0,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        configureWebView()
        installJavascriptInterfaces()
        applyPageTheme(pageDark)
        hideError()
        interceptPrefetch = false
        webView.loadUrl(markAndroidUrl(targetUrl))
    }

    private fun handleBack() {
        if (errorScroll.visibility == View.VISIBLE) {
            showDeviceManager()
            return
        }
        if (backAvailable.get()) {
            webView.evaluateJavascript(BACK_PROBE) { consumed ->
                if (consumed != "true") fallbackBack()
            }
            return
        }
        fallbackBack()
    }

    private fun fallbackBack() {
        if (!clearHistoryAfterLoad && webView.canGoBack()) {
            webView.goBack()
        } else {
            showSettings()
        }
    }

    /**
     * A dead current device should not strand the user when another profile is known. Try the
     * remaining profiles once per failed load; a successful page resets the tried set.
     */
    private fun tryFailover(): Boolean {
        if (failoverInProgress) return false
        val devices = shellDevices.load()
        if (devices.size <= 1) return false
        val currentUrl = targetUrl.trimEnd('/')
        val currentIndex = devices.indexOfFirst {
            it.id == shellDevices.currentId() || it.url.trimEnd('/') == currentUrl
        }
        if (currentIndex < 0) return false
        for (step in 1 until devices.size) {
            val next = devices[(currentIndex + step) % devices.size]
            if (!failoverTried.add(next.url.trimEnd('/'))) continue
            failoverInProgress = true
            Toast.makeText(this, R.string.shell_settings_failover, Toast.LENGTH_SHORT).show()
            shellDevices.setCurrent(next.id)
            applyServerUrl(next.url)
            return true
        }
        return false
    }

    /** The back-button surface: connection, notifications, background and about. */
    private fun showSettings() {
        val devices = shellDevices.load()
        val currentName = devices.firstOrNull { it.id == shellDevices.currentId() }?.name
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"
        val inboxRows = buildInboxRows()
        chrome.showSettingsHome(
            dark = pageDark,
            currentDeviceName = currentName,
            versionName = versionName,
            simpleMode = runCatching {
                runBlocking { hostsStore.settings.first().simpleMode }
            }.getOrDefault(true),
            inboxCount = inboxRows.count { it.action },
            notificationsEnabled = notifications.canPost() &&
                NotificationManagerCompat.from(this).areNotificationsEnabled(),
            backgroundEnabled = runCatching {
                runBlocking { hostsStore.settings.first().keepConnectedInBackground }
            }.getOrDefault(false),
            ntfyEnabled = NtfyConfig.load(this) != null,
            onConnection = { showConnectionPage() },
            onRemote = { showRemoteAccess() },
            onInbox = { showInbox() },
            onMobileSetup = { showMobileSetupDialog() },
            onDiagnostics = { showDiagnosticsDialog() },
            onNotifications = { showNotificationsPage() },
            onBackground = { showBackgroundPage() },
            onCheckUpdate = { checkForUpdate() },
            onSource = { openExternalUri(Uri.parse("https://github.com/daniei-chen/DeepSeek-Harness-APP")) },
            onRefresh = { reloadPage() },
            onExit = { finishAffinity() },
        )
    }

    /**
     * Native inbox for the current host: sessions that need a decision first, then running work,
     * then recent idle sessions. It reads the same live [SessionStore] the notification observer
     * uses, so opening it never starts a second connection or resumes an agent.
     */
    private fun buildInboxRows(): List<ShellInboxRow> {
        return sessionStore.sessions.value
            .filterNot { it.blank }
            .sortedWith(
                compareByDescending<SessionRow> { it.pendingInteraction != null }
                    .thenByDescending { it.running }
                    .thenByDescending { it.updatedAt },
            )
            .take(30)
            .map { session ->
                val action = session.pendingInteraction != null
                val running = session.running
                val status = when (session.pendingInteraction) {
                    "approval" -> getString(R.string.shell_inbox_status_approval)
                    "question" -> getString(R.string.shell_inbox_status_question)
                    "plan-review" -> getString(R.string.shell_inbox_status_plan)
                    else -> if (running) {
                        getString(R.string.shell_inbox_status_running)
                    } else {
                        getString(R.string.shell_inbox_status_idle)
                    }
                }
                val cwd = session.cwd
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('\\')
                    ?.takeIf { it.isNotBlank() }
                ShellInboxRow(
                    sessionId = session.sessionId,
                    title = session.title?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.shell_inbox_untitled, session.sessionId.take(8)),
                    subtitle = listOfNotNull(status, cwd).joinToString(" · "),
                    action = action,
                    running = running,
                )
            }
    }

    private fun showInbox(query: String = inboxQuery) {
        inboxQuery = query
        val connection = connectionManager.state.value
        val phase = when (connection.phase) {
            ConnectionPhase.CONNECTED -> getString(R.string.shell_inbox_state_connected)
            ConnectionPhase.CONNECTING, ConnectionPhase.RECONNECTING ->
                getString(R.string.shell_inbox_state_connecting)
            ConnectionPhase.DISCONNECTED -> getString(R.string.shell_inbox_state_disconnected)
        }
        val label = connection.host?.let { host ->
            getString(R.string.shell_inbox_connection, host.host, phase)
        } ?: getString(R.string.shell_inbox_disconnected)
        val all = buildInboxRows()
        val rows = if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true)
            }
        }
        chrome.showInboxPage(
            dark = pageDark,
            rows = rows,
            connectionLabel = label,
            query = query,
            onSearch = {
                chrome.showTextDialog(
                    dark = pageDark,
                    title = getString(R.string.shell_inbox_search),
                    description = getString(R.string.shell_inbox_search_hint),
                    initial = query,
                    hint = getString(R.string.shell_inbox_search_hint),
                    inputType = android.text.InputType.TYPE_CLASS_TEXT,
                    errorText = "",
                    onSubmit = { value ->
                        showInbox(value.trim())
                        true
                    },
                )
            },
            onClearSearch = { showInbox("") },
            onOpen = { row ->
                pendingSessionId = row.sessionId
                openPendingSession()
            },
            onDetails = { row -> loadSessionCockpit(row) },
            onRefresh = { showInbox() },
            onBack = { showSettings() },
        )
    }

    /** Load the native transcript first, then show the cockpit for that session. */
    private fun loadSessionCockpit(row: ShellInboxRow) {
        val progress = AlertDialog.Builder(this)
            .setMessage(R.string.shell_cockpit_loading)
            .setCancelable(false)
            .create()
        progress.show()
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { sessionStore.openSession(row.sessionId) }
            }
            progress.dismiss()
            if (isFinishing || isDestroyed) return@launch
            showSessionCockpit(row)
        }
    }

    private fun showSessionCockpit(row: ShellInboxRow) {
        val session = sessionStore.sessions.value.firstOrNull { it.sessionId == row.sessionId }
        val currentRow = buildInboxRows().firstOrNull { it.sessionId == row.sessionId } ?: row
        val usage = sessionStore.tokenUsage.value
        val stats = sessionStore.sessionStats.value
        val pressure = sessionStore.contextPressure.value
        val model = sessionStore.models.value?.current
        val contextWindow = pressure?.contextWindow
        val context = if (contextWindow != null) {
            val used = pressure?.projectedTokens ?: pressure?.pressureTokens ?: 0L
            val ratio = pressure?.usedRatio
            buildString {
                append(compactNumber(used))
                append(" / ")
                append(compactNumber(contextWindow))
                if (ratio != null) {
                    append(" (")
                    append((ratio * 100).roundToInt())
                    append("%)")
                }
            }
        } else {
            "—"
        }
        val details = listOf(
            getString(R.string.shell_cockpit_label_status) to currentRow.subtitle,
            getString(R.string.shell_cockpit_label_model) to (model?.let { "${it.provider} / ${it.model}" } ?: "—"),
            getString(R.string.shell_cockpit_label_input) to (usage?.inputTokens?.let(::compactNumber) ?: "—"),
            getString(R.string.shell_cockpit_label_output) to (usage?.outputTokens?.let(::compactNumber) ?: "—"),
            getString(R.string.shell_cockpit_label_cache) to (usage?.cacheHitRatio?.let { "${(it * 100).roundToInt()}%" } ?: "—"),
            getString(R.string.shell_cockpit_label_speed) to (stats?.tokensPerSecond?.let {
                String.format(java.util.Locale.US, "%.1f tok/s", it)
            } ?: "—"),
            getString(R.string.shell_cockpit_label_ttft) to (stats?.meanTtftMs?.let { "$it ms" } ?: "—"),
            getString(R.string.shell_cockpit_label_context) to context,
            getString(R.string.shell_cockpit_label_turns) to (stats?.let { "${it.turns} / ${it.steps}" } ?: "—"),
        )
        chrome.showSessionCockpit(
            dark = pageDark,
            title = currentRow.title,
            status = currentRow.subtitle,
            details = details,
            running = session?.running == true,
            onOpen = {
                pendingSessionId = row.sessionId
                openPendingSession()
            },
            onStop = { confirmStopSession(row) },
            onFork = {
                lifecycleScope.launch {
                    runCatching { sessionStore.forkSession(row.sessionId) }
                    Toast.makeText(this@WebShellActivity, R.string.shell_cockpit_forked, Toast.LENGTH_SHORT).show()
                    showSessionCockpit(row)
                }
            },
            onRefresh = { showSessionCockpit(row) },
            onBack = { showInbox() },
        )
    }

    private fun confirmStopSession(row: ShellInboxRow) {
        AlertDialog.Builder(this)
            .setTitle(R.string.shell_cockpit_stop_title)
            .setMessage(R.string.shell_cockpit_stop_message)
            .setPositiveButton(R.string.shell_cockpit_stop_confirm) { _, _ ->
                lifecycleScope.launch {
                    if (sessionStore.currentSessionId.value != row.sessionId) {
                        Toast.makeText(this@WebShellActivity, R.string.shell_cockpit_not_open, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    runCatching { sessionStore.cancelTurn() }
                    Toast.makeText(this@WebShellActivity, R.string.shell_cockpit_stopped, Toast.LENGTH_SHORT).show()
                    showSessionCockpit(row)
                }
            }
            .setNegativeButton(R.string.common_close, null)
            .show()
    }

    private fun compactNumber(value: Long): String = when {
        value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", value / 1_000.0)
        else -> value.toString()
    }

    private fun showConnectionPage() {
        val simple = runCatching {
            runBlocking { hostsStore.settings.first().simpleMode }
        }.getOrDefault(true)
        chrome.showConnectionPage(
            dark = pageDark,
            simpleMode = simple,
            onManageDevices = { showDeviceManager() },
            onScanQr = { openQrScanner() },
            onPairCode = { showPairingCodeDialog() },
            onScanLan = { scanLan() },
            onNetworkInfo = { showNetworkInfoDialog() },
            onFleet = { showFleet() },
            onFleetInbox = { showFleetInbox() },
            onDeviceHealth = { checkDeviceHealth() },
            onDoctor = { runConnectionDoctor() },
            onTestConnection = { testConnection() },
            onToggleSimple = {
                lifecycleScope.launch {
                    val current = hostsStore.settingsOnce().simpleMode
                    hostsStore.setSetting { it.copy(simpleMode = !current) }
                    showConnectionPage()
                }
            },
            onBack = { showSettings() },
        )
    }

    /**
     * Run the connection doctor against the current address and show every step.
     *
     * It reuses the same URL the WebView is on, so the report describes the path the user is
     * actually using — not a generic reachability test.
     */
    private fun runConnectionDoctor() {
        val url = targetUrl
        if (url.isBlank()) {
            Toast.makeText(this, R.string.shell_settings_no_device, Toast.LENGTH_SHORT).show()
            return
        }
        val loading = AlertDialog.Builder(this)
            .setMessage(R.string.shell_doctor_running)
            .setCancelable(false)
            .create()
        loading.show()
        lifecycleScope.launch {
            val report = runCatching {
                ConnectionDoctor.run(
                    url = url,
                    relayPort = targetHost?.takeIf { it.isRelay }?.port ?: 3443,
                    wsConnected = when (connectionManager.state.value.phase) {
                        ConnectionPhase.CONNECTED -> true
                        ConnectionPhase.DISCONNECTED -> null
                        else -> false
                    },
                )
            }.getOrNull()
            if (!isFinishing && !isDestroyed && loading.isShowing) loading.dismiss()
            if (isFinishing || isDestroyed) return@launch
            if (report == null) {
                Toast.makeText(this@WebShellActivity, R.string.shell_settings_test_connection_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            chrome.showDoctorPage(
                dark = pageDark,
                title = getString(R.string.shell_doctor_title),
                target = url,
                report = report,
                onRerun = { runConnectionDoctor() },
                onCopy = {
                    val text = buildString {
                        appendLine("DSH connection doctor")
                        appendLine(url)
                        appendLine(report.summary)
                        report.steps.forEach { step ->
                            append("- [${step.status}] ${step.label}: ${step.detail}")
                            if (!step.action.isNullOrBlank()) append(" -> ${step.action}")
                            appendLine()
                        }
                    }
                    copyToClipboard(text)
                    Toast.makeText(this@WebShellActivity, R.string.shell_doctor_copied, Toast.LENGTH_SHORT).show()
                },
                onBack = { showConnectionPage() },
            )
        }
    }

    private fun showMobileSetupDialog() {
        val command = "powershell -ExecutionPolicy Bypass -File setup/install.ps1 " +
            "-Profile desktop -FileRoot \"D:\\your-workspace\"\n\n" +
            "./setup/install.sh --profile desktop --file-root \"\$HOME/workspaces\""
        AlertDialog.Builder(this)
            .setTitle(R.string.shell_mobile_setup_title)
            .setMessage(getString(R.string.shell_mobile_setup_message, command))
            .setPositiveButton(R.string.shell_mobile_setup_copy) { _, _ ->
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("DSH host setup", command))
                Toast.makeText(this, R.string.shell_mobile_setup_copied, Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.shell_mobile_setup_docs) { _, _ ->
                openExternalUri(
                    Uri.parse("https://github.com/daniei-chen/DeepSeek-Harness-APP/blob/main/setup/README.md"),
                )
            }
            .setNegativeButton(R.string.common_close, null)
            .show()
    }

    private fun showDiagnosticsDialog() {
        val loads = synchronized(pageLoadTimes) { pageLoadTimes.size }
        val crashes = synchronized(rendererCrashTimes) { rendererCrashTimes.size }
        val webViewVersion = runCatching {
            WebView.getCurrentWebViewPackage()?.versionName
        }.getOrNull() ?: "unknown"
        val message = getString(
            R.string.shell_diagnostics_message,
            targetUrl.ifBlank { "(none)" },
            loads,
            crashes,
            connectionManager.state.value.phase.name.lowercase(),
            webViewVersion,
            "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        ) + "\n" + getString(R.string.shell_diagnostics_layout, layoutSummary()) +
            "\n\n" + getString(R.string.shell_diagnostics_events) + "\n" + pageEventSummary()
        chrome.showDiagnosticsPage(
            dark = pageDark,
            message = message,
            glassCompatLabel = when (glassCompat) {
                "force" -> getString(R.string.shell_glass_force)
                "off" -> getString(R.string.shell_glass_off)
                else -> getString(R.string.shell_glass_auto)
            },
            onCopy = {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("DSH diagnostics", message))
                Toast.makeText(this, R.string.shell_diagnostics_copied, Toast.LENGTH_SHORT).show()
            },
            onClearCache = { clearCacheAndReload() },
            onGlassCompat = {
                val next = when (glassCompat) {
                    "auto" -> "force"
                    "force" -> "off"
                    else -> "auto"
                }
                lifecycleScope.launch {
                    hostsStore.setSetting { it.copy(glassCompat = next) }
                    glassCompat = next
                    applyGlassCompat()
                    showDiagnosticsDialog()
                }
            },
            onRepair = { repairPageLayout(forceRepaint = true, notify = true) },
            onBack = { showSettings() },
        )
    }

    /**
     * The relay's pairing invitation, drawn on the phone.
     *
     * A pairing code may only be *issued* from the machine running the harness; the relay decides
     * that by the address the request arrives from. The phone therefore asks the host plugin —
     * which runs on that machine — for the live invitation, and draws the QR itself. Nothing is
     * stored: the code lives for the relay's pairing window and is replaced on refresh.
     */
    private fun showRemoteAccess(fresh: Boolean = false) {
        if (!::webView.isInitialized || targetUrl.isBlank() || targetHost == null) {
            Toast.makeText(this, R.string.shell_remote_no_host, Toast.LENGTH_SHORT).show()
            return
        }
        val loading = AlertDialog.Builder(this)
            .setMessage(R.string.shell_remote_loading)
            .setCancelable(false)
            .create()
        loading.show()
        lifecycleScope.launch {
            val raw = requestRelayPairing(fresh)
            if (!isFinishing && !isDestroyed && loading.isShowing) loading.dismiss()
            if (isFinishing || isDestroyed) return@launch
            val (invite, error) = if (raw == null) {
                null to getString(R.string.shell_remote_offline)
            } else {
                parseRemoteInvite(raw)
            }
            renderRemoteAccess(invite, error)
        }
    }

    /** Ask the page to fetch the invitation, so the relay's cookie and certificate are reused. */
    private suspend fun requestRelayPairing(fresh: Boolean): String? {
        val deferred = kotlinx.coroutines.CompletableDeferred<String>()
        relayPairingResult = deferred
        val query = if (fresh) "?new=1" else ""
        val script = """
            (function () {
              var done = function (value) {
                try { window.${WebShellScripts.SHELL_BRIDGE}.relayPairing(JSON.stringify(value)); } catch (e) {}
              };
              fetch('/dsh-mobile/relay/pair$query', {
                credentials: 'include',
                cache: 'no-store',
                headers: { accept: 'application/json' }
              })
                .then(function (response) {
                  return response.text().then(function (text) { done({ status: response.status, body: text }); });
                })
                .catch(function (error) { done({ status: 0, body: String((error && error.message) || error) }); });
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
        return withTimeoutOrNull(8_000) { deferred.await() }
    }

    private fun parseRemoteInvite(raw: String): Pair<RemoteInvite?, String?> {
        val envelope = runCatching { org.json.JSONObject(raw) }.getOrNull()
            ?: return null to getString(R.string.shell_remote_offline)
        val status = envelope.optInt("status", 0)
        val body = envelope.optString("body", "")
        if (status == 0) return null to getString(R.string.shell_remote_offline)
        // A 404/HTML answer means the host is still running a plugin from before this route
        // existed; saying "HTTP 404" would hide the one action that fixes it.
        if (status == 404) return null to getString(R.string.shell_remote_plugin_outdated)
        val root = runCatching { org.json.JSONObject(body) }.getOrNull()
            ?: return null to getString(R.string.shell_remote_plugin_outdated)
        if (!root.optBoolean("ok")) {
            val code = root.optString("error")
            val message = when (code) {
                "relay-unreachable" -> getString(R.string.shell_remote_offline)
                "relay-page-unreadable" -> getString(R.string.shell_remote_unreadable)
                else -> getString(R.string.shell_remote_failed, code.ifBlank { "HTTP $status" })
            }
            return null to message
        }
        val payloadJson = root.optJSONObject("payload")?.toString()
            ?: return null to getString(R.string.shell_remote_unreadable)
        val payload = runCatching {
            com.labteto.dshmobile.core.wire.WireJson.decodeFromString(
                com.labteto.dshmobile.core.wire.RelayPairingPayload.serializer(),
                payloadJson,
            )
        }.getOrNull() ?: return null to getString(R.string.shell_remote_unreadable)
        return RemoteInvite(
            payload = payload,
            pairPage = root.optString("pairPage").takeIf { it.isNotBlank() },
            devicesPage = root.optString("devicesPage").takeIf { it.isNotBlank() },
        ) to null
    }

    private fun renderRemoteAccess(invite: RemoteInvite?, error: String?) {
        val payload = invite?.payload
        val qr = payload?.let { RemoteAccess.qrBitmap(RemoteAccess.encode(it), dp(232)) }
        chrome.showRemoteAccessPage(
            dark = pageDark,
            qr = qr,
            code = payload?.code,
            expiry = payload?.let { RemoteAccess.remaining(it.expiresAt) },
            addresses = listOfNotNull(
                invite?.pairPage?.let { getString(R.string.shell_remote_pair_page) to it },
                payload?.plainUrl?.let { getString(R.string.shell_remote_plain) to it },
                invite?.devicesPage?.let { getString(R.string.shell_remote_devices) to it },
            ),
            note = getString(R.string.shell_remote_note),
            message = error,
            onRefresh = { showRemoteAccess(fresh = true) },
            onCopyCode = { payload?.let { copyToClipboard(it.code) } },
            onCopyLink = { payload?.let { copyToClipboard("${it.url}/relay/pair?code=${it.code}") } },
            onDevices = {
                val target = invite?.devicesPage
                if (target.isNullOrBlank()) {
                    Toast.makeText(this, R.string.shell_remote_offline, Toast.LENGTH_SHORT).show()
                } else {
                    webView.loadUrl(target)
                }
            },
            onBack = { showSettings() },
        )
    }

    private fun copyToClipboard(value: String) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("DSH", value))
        Toast.makeText(this, R.string.shell_remote_copied, Toast.LENGTH_SHORT).show()
    }

    private data class RemoteInvite(
        val payload: com.labteto.dshmobile.core.wire.RelayPairingPayload,
        val pairPage: String?,
        val devicesPage: String?,
    )

    /**
     * Notification self-check: the four things that silently turn a notification off.
     *
     * Nothing here is guessed — permission, the system switch, each channel's importance and the
     * battery-optimisation exemption are all readable from the OS, so a "no notifications" report
     * can end with a specific fix instead of "check your settings".
     */
    private fun runNotificationCheck() {
        val manager = getSystemService(NotificationManager::class.java)
        val steps = mutableListOf<DoctorStep>()
        val permission = notifications.canPost()
        steps += DoctorStep(
            getString(R.string.shell_notify_perm),
            if (permission) DoctorStatus.OK else DoctorStatus.FAIL,
            if (permission) getString(R.string.shell_settings_enabled) else getString(R.string.shell_settings_disabled),
            if (permission) null else getString(R.string.shell_notify_fix_permission),
        )
        val systemEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        steps += DoctorStep(
            getString(R.string.shell_notify_system),
            if (systemEnabled) DoctorStatus.OK else DoctorStatus.FAIL,
            if (systemEnabled) getString(R.string.shell_settings_enabled) else getString(R.string.shell_settings_disabled),
            if (systemEnabled) null else getString(R.string.shell_notify_fix_permission),
        )
        listOf(
            DshNotifications.CHANNEL_ACTION to getString(R.string.shell_notify_channel_action),
            DshNotifications.CHANNEL_COMPLETIONS to getString(R.string.shell_notify_channel_done),
        ).forEach { (id, label) ->
            val channel = manager?.getNotificationChannel(id)
            steps += when {
                channel == null -> DoctorStep(
                    label,
                    DoctorStatus.FAIL,
                    getString(R.string.shell_settings_disabled),
                    getString(R.string.shell_notify_fix_channel),
                )
                channel.importance >= NotificationManager.IMPORTANCE_DEFAULT -> DoctorStep(
                    label,
                    DoctorStatus.OK,
                    "importance=${channel.importance}",
                )
                else -> DoctorStep(
                    label,
                    DoctorStatus.WARN,
                    "importance=${channel.importance}",
                    getString(R.string.shell_notify_fix_channel),
                )
            }
        }
        val power = getSystemService(PowerManager::class.java)
        val exempt = power?.isIgnoringBatteryOptimizations(packageName) == true
        steps += DoctorStep(
            getString(R.string.shell_notify_background),
            if (exempt) DoctorStatus.OK else DoctorStatus.WARN,
            if (exempt) getString(R.string.shell_settings_enabled) else getString(R.string.shell_settings_background_off),
            if (exempt) null else getString(R.string.shell_notify_fix_background),
        )
        val push = NtfyConfig.load(this) != null || !UnifiedPushConfig.load(this).isNullOrBlank()
        steps += DoctorStep(
            getString(R.string.shell_notify_push),
            if (push) DoctorStatus.OK else DoctorStatus.SKIP,
            if (push) getString(R.string.shell_settings_enabled) else getString(R.string.shell_settings_disabled),
        )
        val report = DoctorReport("notifications", steps)
        chrome.showDoctorPage(
            dark = pageDark,
            title = getString(R.string.shell_notify_check_title),
            target = packageName,
            report = report,
            onRerun = { runNotificationCheck() },
            onCopy = {
                val text = buildString {
                    appendLine("DSH notification check")
                    appendLine(report.summary)
                    report.steps.forEach { step -> appendLine("- [${step.status}] ${step.label}: ${step.detail}") }
                }
                copyToClipboard(text)
            },
            onBack = { showNotificationsPage() },
        )
    }

    private fun showNotificationsPage() {
        val settings = runCatching { runBlocking { hostsStore.settings.first() } }.getOrDefault(AppSettings())
        chrome.showNotificationsPage(
            dark = pageDark,
            notificationsEnabled = notifications.canPost() &&
                NotificationManagerCompat.from(this).areNotificationsEnabled(),
            turnNotifications = settings.notifyTurnComplete,
            actionNotifications = settings.notifyNeedsAction,
            goalNotifications = settings.notifyGoal,
            ntfyEnabled = NtfyConfig.load(this) != null,
            unifiedPushEndpoint = UnifiedPushConfig.load(this),
            onNotificationSettings = { openNotificationSettings() },
            onTestNotification = { sendTestNotification() },
            onCheck = { runNotificationCheck() },
            onToggleTurn = { toggleNotificationSetting { it.copy(notifyTurnComplete = !it.notifyTurnComplete) } },
            onToggleAction = { toggleNotificationSetting { it.copy(notifyNeedsAction = !it.notifyNeedsAction) } },
            onToggleGoal = { toggleNotificationSetting { it.copy(notifyGoal = !it.notifyGoal) } },
            onConfigureNtfy = { configureNtfy() },
            onRegisterUnifiedPush = { registerUnifiedPush() },
            onCopyUnifiedPushEndpoint = { copyUnifiedPushEndpoint() },
            onBack = { showSettings() },
        )
    }

    private fun showBackgroundPage() {
        chrome.showBackgroundPage(
            dark = pageDark,
            backgroundEnabled = runCatching {
                runBlocking { hostsStore.settings.first().keepConnectedInBackground }
            }.getOrDefault(false),
            onToggleBackground = { toggleBackground() },
            onBatterySettings = { openBatterySettings() },
            onAutostart = { openAutostartSettings() },
            onBack = { showSettings() },
        )
    }

    private fun showAboutPage() {
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"
        chrome.showAboutPage(
            dark = pageDark,
            versionName = versionName,
            onCheckUpdate = { checkForUpdate() },
            onSource = { openExternalUri(Uri.parse("https://github.com/daniei-chen/DeepSeek-Harness-APP")) },
            onBack = { showSettings() },
        )
    }

    private fun scanLan() {
        if (lanScanning) return
        lanScanning = true
        val progress = AlertDialog.Builder(this)
            .setTitle(R.string.shell_settings_scan_lan)
            .setMessage(R.string.shell_settings_scanning)
            .setCancelable(false)
            .create()
        progress.setOnDismissListener { lanScanning = false }
        progress.show()

        lifecycleScope.launch {
            try {
            val settings = runCatching { hostsStore.settings.first() }.getOrDefault(AppSettings())
            val ports = (settings.knownPorts + 3080).distinct().take(8)
            val subnet = withContext(Dispatchers.IO) { discoveryEngine.localSubnetLabel() }
            // mDNS is best-effort and runs beside the sweep. A relay advertises the port and
            // pin it was configured with, so a hit here is both faster and more trustworthy
            // than a subnet knock; multicast-filtered Wi-Fi still falls back to the sweep.
            val advertised = async(Dispatchers.IO) {
                runCatching { mdnsDiscovery.browse(1_500) }.getOrDefault(emptyList())
            }
            val harnesses = withContext(Dispatchers.IO) {
                runCatching {
                    discoveryEngine.scan(
                        ports = ports,
                        onProgress = { probed, total ->
                            runOnUiThread {
                                if (!isFinishing && !isDestroyed && progress.isShowing) {
                                    progress.setMessage(
                                        getString(R.string.shell_settings_scan_progress, probed, total),
                                    )
                                }
                            }
                        },
                    )
                }.getOrDefault(emptyList())
            }
            // A relay is the only supported way to reach a Desktop host that binds loopback.
            // The sweep is the fallback when mDNS is unavailable; a trusted LAN harness still
            // gets any advertised relay added alongside it.
            val sweptRelays = if (harnesses.none { it.trusted }) {
                withContext(Dispatchers.IO) {
                    runCatching { discoveryEngine.scanForRelays() }.getOrDefault(emptyList())
                }
            } else {
                emptyList()
            }
            val relays = (advertised.await() + sweptRelays).distinctBy { it.authority }
            if (isFinishing || isDestroyed) return@launch
            progress.dismiss()

            val existing = shellDevices.load().map { it.url.trimEnd('/') }.toMutableSet()
            var added = 0
            harnesses.filter { it.trusted }.forEach { host ->
                val url = host.baseUrl.trimEnd('/') + "/"
                if (url.trimEnd('/') !in existing) {
                    shellDevices.add(url = url, name = "局域网 ${host.host}")
                    existing.add(url.trimEnd('/'))
                    added++
                }
            }
            relays.filter { it.host != "127.0.0.1" }.forEach { relay ->
                // The relay's default listener is TLS. The WebView asks for explicit trust on the
                // first visit; this address is remembered per host:port in ShellDevices.
                val url = relay.baseUrl.trimEnd('/') + "/relay/pair"
                if (url.trimEnd('/') !in existing) {
                    shellDevices.add(url = url, name = "中继 ${relay.host}")
                    existing.add(url.trimEnd('/'))
                    added++
                }
            }
            if (added > 0) {
                Toast.makeText(
                    this@WebShellActivity,
                    getString(R.string.shell_settings_scan_found_count, added),
                    Toast.LENGTH_SHORT,
                ).show()
                showDeviceManager()
                return@launch
            }
            // A scan that found the devices already in the list is a success, not an empty scan.
            val known = harnesses.count { it.trusted } + relays.size
            if (known > 0) {
                Toast.makeText(
                    this@WebShellActivity,
                    getString(R.string.shell_scan_all_added, known),
                    Toast.LENGTH_SHORT,
                ).show()
                showDeviceManager()
                return@launch
            }
            showScanEmptyDialog(subnet, ports, harnesses.count { !it.trusted }, relays.size)
            } catch (error: Throwable) {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@WebShellActivity,
                        getString(R.string.shell_settings_scan_failed, error.message ?: error.javaClass.simpleName),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } finally {
                if (progress.isShowing) progress.dismiss()
                lanScanning = false
            }
        }
    }

    private fun showScanEmptyDialog(
        subnet: String?,
        ports: List<Int>,
        harnessCount: Int,
        relayCount: Int,
    ) {
        val extra = buildString {
            if (harnessCount > 0) {
                append("\n\n")
                append(getString(R.string.shell_settings_scan_untrusted, harnessCount))
            }
            if (relayCount > 0) {
                append("\n\n")
                append(getString(R.string.shell_settings_scan_relay_unpaired, relayCount))
            }
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.shell_settings_scan_empty_title)
            .setMessage(
                getString(
                    R.string.shell_settings_scan_empty_detail,
                    subnet ?: getString(R.string.shell_settings_scan_unknown_subnet),
                    ports.joinToString(" / "),
                ) + extra,
            )
            .setPositiveButton(R.string.shell_settings_scan_help) { _, _ -> showMobileSetupDialog() }
            .setNeutralButton(R.string.device_add) { _, _ -> showAddDeviceDialog() }
            .setNegativeButton(R.string.common_close, null)
            .show()
    }

    private fun showNetworkInfoDialog() {
        lifecycleScope.launch {
            val addresses = withContext(Dispatchers.IO) { discoveryEngine.localIpv4s() }
            val subnet = withContext(Dispatchers.IO) { discoveryEngine.localSubnetLabel() }
            val ports = runCatching { hostsStore.settings.first().knownPorts + 3080 }
                .getOrDefault(listOf(3080))
                .distinct()
            if (isFinishing || isDestroyed) return@launch
            AlertDialog.Builder(this@WebShellActivity)
                .setTitle(R.string.shell_settings_network_info_title)
                .setMessage(
                    getString(
                        R.string.shell_settings_network_info_message,
                        addresses.ifEmpty { listOf(getString(R.string.shell_settings_network_none)) }
                            .joinToString("\n") { "• $it" },
                        subnet ?: getString(R.string.shell_settings_scan_unknown_subnet),
                        ports.joinToString(" / "),
                    ),
                )
                .setPositiveButton(R.string.common_close, null)
                .show()
        }
    }

    /** host:port key used for the per-device self-signed trust decision. */
    private fun authorityOf(url: String?): String? {
        val uri = url?.let { Uri.parse(it) } ?: return null
        val host = uri.host ?: return null
        val port = when {
            uri.port != -1 -> uri.port
            uri.scheme.equals("https", ignoreCase = true) -> 443
            else -> 80
        }
        return "$host:$port"
    }

    /** Only an address already in the device list may be offered the self-signed exception. */
    private fun isKnownDeviceAuthority(authority: String): Boolean =
        shellDevices.load().any { device ->
            authorityOf(device.url)?.equals(authority, ignoreCase = true) == true
        }

    private fun showSslTrustDialog(handler: SslErrorHandler, authority: String, error: SslError) {
        if (sslPromptShowing || isFinishing || isDestroyed) {
            handler.cancel()
            return
        }
        sslPromptShowing = true
        val issuer = error.certificate?.issuedTo?.cName?.takeIf { it.isNotBlank() } ?: authority
        AlertDialog.Builder(this)
            .setTitle(R.string.webshell_ssl_trust_title)
            .setMessage(getString(R.string.webshell_ssl_trust_message, authority, issuer))
            .setPositiveButton(R.string.webshell_ssl_trust_continue) { _, _ ->
                sslPromptShowing = false
                shellDevices.trustSelfSigned(authority)
                handler.proceed()
            }
            .setNegativeButton(R.string.webshell_ssl_trust_cancel) { _, _ ->
                sslPromptShowing = false
                handler.cancel()
                showError(getString(R.string.webshell_error_title), getString(R.string.webshell_error_ssl))
            }
            .setOnCancelListener {
                sslPromptShowing = false
                handler.cancel()
            }
            .show()
    }

    private fun openNotificationSettings() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }.onFailure { openAppDetailsSettings() }
    }

    /**
     * Multi-host overview: one `sessionList` read per saved device, in parallel. This is a
     * read-only fleet view; the active WebView connection is not touched until a row is tapped.
     */
    private fun showFleet() {
        val devices = shellDevices.load()
        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.shell_fleet_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val progress = AlertDialog.Builder(this)
            .setMessage(R.string.shell_fleet_loading)
            .setCancelable(false)
            .create()
        progress.show()
        lifecycleScope.launch {
            val rows = coroutineScope {
                devices.map { device -> async(Dispatchers.IO) { queryFleetHost(device) } }.awaitAll()
            }
            progress.dismiss()
            if (isFinishing || isDestroyed) return@launch
            chrome.showFleetPage(
                dark = pageDark,
                rows = rows,
                onSelect = { row ->
                    shellDevices.setCurrent(row.device.id)
                    applyServerUrl(row.device.url)
                },
                onRefresh = { showFleet() },
                onBack = { showConnectionPage() },
            )
        }
    }

    private suspend fun queryFleetHost(device: ShellDevice): ShellFleetHost {
        if (device.url.contains("/relay/")) {
            return ShellFleetHost(device, null, getString(R.string.shell_fleet_relay))
        }
        val hosts = runCatching { hostsStore.hosts.first() }.getOrDefault(emptyList())
        val base = device.url.substringBefore("/relay/").trimEnd('/')
        val host = hosts.firstOrNull {
            it.baseUrl.trimEnd('/') == device.url.trimEnd('/') || it.baseUrl.trimEnd('/') == base
        } ?: return ShellFleetHost(device, null, getString(R.string.shell_fleet_not_configured))
        if (host.isRelay) {
            return ShellFleetHost(device, null, getString(R.string.shell_fleet_relay))
        }
        val result = withTimeoutOrNull(6_000) {
            runCatching {
                val client = harnessClientFactory.clientFor(host)
                when (val list = client.sessionList()) {
                    is RpcResult.Ok -> {
                        val items = list.value.items
                        ShellFleetHost(
                            device = device,
                            online = true,
                            detail = getString(
                                R.string.shell_fleet_online,
                                items.size,
                                items.count { it.running },
                            ),
                            sessionCount = items.size,
                            runningCount = items.count { it.running },
                        )
                    }
                    is RpcResult.Err -> ShellFleetHost(
                        device,
                        false,
                        getString(R.string.shell_fleet_error, list.error.code),
                    )
                }
            }.getOrNull()
        }
        return result ?: ShellFleetHost(device, false, getString(R.string.shell_fleet_timeout))
    }

    /**
     * Cross-host inbox: only sessions that are running or have an active user question/plan
     * review. Idle history stays in each host's own inbox and is not pulled into this view.
     */
    private fun showFleetInbox() {
        val devices = shellDevices.load()
        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.shell_fleet_inbox_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val progress = AlertDialog.Builder(this)
            .setMessage(R.string.shell_fleet_loading)
            .setCancelable(false)
            .create()
        progress.show()
        lifecycleScope.launch {
            val rows = coroutineScope {
                devices.map { device -> async(Dispatchers.IO) { queryFleetSessions(device) } }.awaitAll()
            }.flatten()
                .sortedWith(compareBy<ShellFleetSession> { it.priority }.thenByDescending { it.updatedAt })
                .take(40)
            progress.dismiss()
            if (isFinishing || isDestroyed) return@launch
            chrome.showFleetInboxPage(
                dark = pageDark,
                rows = rows,
                onOpen = { row ->
                    shellDevices.setCurrent(row.device.id)
                    pendingSessionId = row.sessionId
                    applyServerUrl(row.device.url)
                },
                onRefresh = { showFleetInbox() },
                onBack = { showConnectionPage() },
            )
        }
    }

    private suspend fun queryFleetSessions(device: ShellDevice): List<ShellFleetSession> {
        if (device.url.contains("/relay/")) return emptyList()
        val hosts = runCatching { hostsStore.hosts.first() }.getOrDefault(emptyList())
        val base = device.url.substringBefore("/relay/").trimEnd('/')
        val host = hosts.firstOrNull {
            it.baseUrl.trimEnd('/') == device.url.trimEnd('/') || it.baseUrl.trimEnd('/') == base
        } ?: return emptyList()
        if (host.isRelay) return emptyList()
        val result = withTimeoutOrNull(6_000) {
            runCatching { harnessClientFactory.clientFor(host).sessionList() }.getOrNull()
        } ?: return emptyList()
        val items = when (result) {
            is RpcResult.Ok -> result.value.items
            is RpcResult.Err -> return emptyList()
        }
        return items.mapNotNull { summary ->
            val pending = pendingQuestionCount(summary)
            if (!summary.running && pending == 0) return@mapNotNull null
            val status = buildList {
                if (pending > 0) add(getString(R.string.shell_fleet_pending, pending))
                if (summary.running) add(getString(R.string.shell_fleet_running))
            }.joinToString(" · ")
            ShellFleetSession(
                device = device,
                sessionId = summary.sessionId,
                title = sessionTitleOf(summary)
                    ?: getString(R.string.shell_inbox_untitled, summary.sessionId.take(8)),
                status = status,
                updatedAt = summary.updatedAt,
                priority = if (pending > 0) 0 else 1,
            )
        }
    }

    private fun pendingQuestionCount(summary: SessionSummary): Int {
        val values = summary.projections?.values ?: return 0
        val questions = values["userQuestions"] as? JsonObject ?: return 0
        return (questions["active"] as? JsonArray)?.size ?: 0
    }

    private fun sessionTitleOf(summary: SessionSummary): String? {
        val value = summary.projections?.values?.get("title") ?: return null
        return when (value) {
            is JsonPrimitive -> value.contentOrNull
            is JsonObject -> value["title"]?.jsonPrimitive?.contentOrNull
            else -> null
        }
    }

    private fun checkDeviceHealth() {
        val devices = shellDevices.load()
        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.shell_device_health_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val progress = AlertDialog.Builder(this)
            .setMessage(R.string.shell_device_health_checking)
            .setCancelable(false)
            .create()
        progress.show()
        lifecycleScope.launch {
            val rows = coroutineScope {
                devices.map { device -> async(Dispatchers.IO) { probeDeviceHealth(device) } }.awaitAll()
            }
            progress.dismiss()
            if (isFinishing || isDestroyed) return@launch
            chrome.showDeviceHealthPage(
                dark = pageDark,
                rows = rows,
                onSelect = { row ->
                    shellDevices.setCurrent(row.device.id)
                    applyServerUrl(row.device.url)
                },
                onRefresh = { checkDeviceHealth() },
                onBack = { showConnectionPage() },
            )
        }
    }

    private suspend fun probeDeviceHealth(device: ShellDevice): ShellDeviceHealth {
        val started = System.nanoTime()
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder().url(device.url).head().build()
                client.newCall(request).execute().use { it.code }
            }
        }
        val elapsed = ((System.nanoTime() - started) / 1_000_000).toInt()
        return result.fold(
            onSuccess = { code ->
                ShellDeviceHealth(device, true, getString(R.string.shell_device_health_online, code, elapsed))
            },
            onFailure = { error ->
                val message = error.message.orEmpty()
                if (message.contains("SSL", ignoreCase = true) ||
                    message.contains("certificate", ignoreCase = true)
                ) {
                    ShellDeviceHealth(device, null, getString(R.string.shell_device_health_tls))
                } else {
                    ShellDeviceHealth(device, false, getString(R.string.shell_device_health_offline))
                }
            },
        )
    }

    private fun testConnection() {
        val url = targetUrl
        if (url.isBlank()) {
            Toast.makeText(this, R.string.shell_settings_no_device, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val started = System.nanoTime()
                    val request = okhttp3.Request.Builder().url(url).head().build()
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    client.newCall(request).execute().use { response ->
                        response.code to ((System.nanoTime() - started) / 1_000_000).toInt()
                    }
                }.getOrNull()
            }
            if (result == null) {
                Toast.makeText(this@WebShellActivity, R.string.shell_settings_test_connection_failed, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this@WebShellActivity,
                    getString(R.string.shell_settings_test_connection_ok, result.first, result.second),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun configureNtfy() {
        chrome.showTextDialog(
            dark = pageDark,
            title = getString(R.string.shell_settings_ntfy_title),
            description = getString(R.string.shell_settings_ntfy_description),
            initial = NtfyConfig.raw(this).orEmpty(),
            hint = "https://ntfy.example.com/topic",
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI,
            errorText = getString(R.string.shell_settings_ntfy_invalid),
            onSubmit = { raw ->
                if (raw.trim().isEmpty()) {
                    NtfyConfig.clear(this)
                    stopService(Intent(this, NtfySubscriberService::class.java))
                    true
                } else {
                    if (NtfyConfig.save(this, raw) == null) return@showTextDialog false
                    startNtfyService()
                    true
                }
            },
        )
    }

    private fun startNtfyService() {
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, NtfySubscriberService::class.java))
        }
    }

    private fun registerUnifiedPush() {
        val distributors = runCatching { UnifiedPush.getDistributors(this) }.getOrDefault(emptyList())
        if (distributors.isEmpty()) {
            Toast.makeText(this, R.string.shell_settings_unifiedpush_missing, Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            UnifiedPush.register(this, "dsh-mobile", distributors.first(), null)
            Toast.makeText(this, R.string.shell_settings_unifiedpush_registered, Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, R.string.shell_settings_unifiedpush_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun copyUnifiedPushEndpoint() {
        val endpoint = UnifiedPushConfig.load(this) ?: return
        runCatching {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("UnifiedPush endpoint", endpoint))
            Toast.makeText(this, R.string.shell_settings_unifiedpush_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleNotificationSetting(transform: (AppSettings) -> AppSettings) {
        lifecycleScope.launch {
            hostsStore.setSetting(transform)
            showNotificationsPage()
        }
    }

    private fun toggleBackground() {
        lifecycleScope.launch {
            val enabled = runCatching { hostsStore.settings.first().keepConnectedInBackground }.getOrDefault(false)
            val next = !enabled
            hostsStore.setSetting { it.copy(keepConnectedInBackground = next) }
            if (next) {
                ContextCompat.startForegroundService(this@WebShellActivity, Intent(this@WebShellActivity, ConnectionService::class.java))
            } else {
                stopService(Intent(this@WebShellActivity, ConnectionService::class.java))
            }
            showBackgroundPage()
        }
    }

    private fun checkForUpdate() {
        val current = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: return
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) { updateChecker.checkNow(current) }
            when (outcome) {
                is UpdateChecker.CheckOutcome.Available -> {
                    val update = outcome.update
                    chrome.showUpdateDialog(
                        dark = pageDark,
                        version = update.version,
                        onInstall = { downloadUpdate(update) },
                        onOpenRelease = { openExternalUri(Uri.parse(update.url)) },
                    )
                }
                is UpdateChecker.CheckOutcome.UpToDate ->
                    Toast.makeText(this@WebShellActivity, R.string.shell_settings_up_to_date, Toast.LENGTH_SHORT).show()
                else ->
                    showUpdateFailureDialog()
            }
        }
    }

    private fun autoCheckForUpdate() {
        val prefs = getSharedPreferences("update_check", MODE_PRIVATE)
        val last = prefs.getLong("last_check", 0L)
        if (System.currentTimeMillis() - last < 12L * 60L * 60L * 1000L) return
        val current = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: return
        prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) { updateChecker.checkNow(current) }
            if (outcome is UpdateChecker.CheckOutcome.Available) {
                val update = outcome.update
                chrome.showUpdateDialog(
                    dark = pageDark,
                    version = update.version,
                    onInstall = { downloadUpdate(update) },
                    onOpenRelease = { openExternalUri(Uri.parse(update.url)) },
                )
            }
        }
    }

    private fun showUpdateFailureDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.shell_update_failed_title)
            .setMessage(R.string.shell_update_failed_message)
            .setPositiveButton(R.string.shell_update_open_release) { _, _ ->
                openExternalUri(Uri.parse("https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/latest"))
            }
            .setNegativeButton(R.string.shell_update_later, null)
            .show()
    }

    private fun downloadUpdate(update: com.labteto.dshmobile.update.AvailableUpdate) {
        val direct = update.apk?.url
        if (direct == null) {
            openExternalUri(Uri.parse(update.url))
            return
        }
        val candidates = listOf(
            "https://gh-proxy.com/$direct",
            "https://ghproxy.net/$direct",
            direct,
        ).distinct()
        val progressCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(10), dp(22), dp(4))
        }
        val accent = Color.parseColor("#3964FE")
        val progressLabel = TextView(this).apply {
            text = getString(R.string.shell_update_downloading)
            textSize = 13f
            setTextColor(if (pageDark) Color.WHITE else Color.parseColor("#20242C"))
        }
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = true
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accent)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
        }
        progressCard.addView(progressLabel)
        progressCard.addView(progressBar)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.shell_update_downloading_title)
            .setView(progressCard)
            .setCancelable(false)
            .create()
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.86f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                for (candidate in candidates) {
                    val downloaded = runCatching {
                        downloadApk(candidate) { percent ->
                            mainHandler.post {
                                if (dialog.isShowing) {
                                    if (percent < 0) {
                                        progressBar.isIndeterminate = true
                                        progressLabel.text = getString(R.string.shell_update_downloading)
                                    } else {
                                        progressBar.isIndeterminate = false
                                        progressBar.progress = percent
                                        progressLabel.text = getString(R.string.shell_update_downloading_percent, percent)
                                    }
                                }
                            }
                        }
                    }.getOrNull()
                    if (downloaded != null) return@withContext downloaded
                }
                null
            }
            if (dialog.isShowing) dialog.dismiss()
            if (file == null) {
                showUpdateFailureDialog()
                return@launch
            }
            installUpdate(file)
        }
    }

    private fun downloadApk(url: String, onProgress: (Int) -> Unit): File {
        val request = okhttp3.Request.Builder().url(url).build()
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val contentType = response.header("Content-Type").orEmpty()
            if (contentType.contains("text/html", ignoreCase = true)) error("HTML response")
            val body = response.body ?: error("empty response")
            val total = body.contentLength()
            val dir = File(cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "update.apk")
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    var lastPercent = -1
                    if (total <= 0) onProgress(-1)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) {
                            val percent = ((copied * 100) / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            if (target.length() < 1_000_000L || !looksLikeApk(target)) {
                target.delete()
                error("not an APK")
            }
            return target
        }
    }

    private fun looksLikeApk(file: File): Boolean = runCatching {
        java.io.FileInputStream(file).use { input ->
            val magic = ByteArray(4)
            input.read(magic) == 4 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()
        }
    }.getOrDefault(false)

    private fun installUpdate(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            Toast.makeText(this, R.string.shell_settings_update_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingTestNotification = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        postTestNotificationInternal()
    }

    private fun postTestNotificationInternal() {
        notifications.ensureChannels()
        notifications.postSession(
            channel = DshNotifications.CHANNEL_ACTION,
            id = 4242,
            title = getString(R.string.shell_settings_test_notification),
            text = getString(R.string.shell_settings_test_notification_text),
            sessionId = null,
            actionLabel = getString(R.string.notif_open),
        )
    }

    @SuppressLint("BatteryLife")
    private fun openBatterySettings() {
        val power = getSystemService(android.os.PowerManager::class.java)
        if (power != null && power.isIgnoringBatteryOptimizations(packageName)) {
            runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                .onFailure { openAppDetailsSettings() }
            return
        }
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"),
                ),
            )
        }.onFailure {
            runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                .onFailure { openAppDetailsSettings() }
        }
    }

    private fun openAutostartSettings() {
        val candidates = listOf(
            android.content.ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
            android.content.ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
            android.content.ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            ),
            android.content.ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            ),
        )
        for (component in candidates) {
            val intent = Intent().setComponent(component)
            if (packageManager.resolveActivity(intent, 0) != null) {
                runCatching { startActivity(intent) }.onFailure { openAppDetailsSettings() }
                return
            }
        }
        openAppDetailsSettings()
    }

    private fun openAppDetailsSettings() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }

    /** Back-button manager: switch, add, rename or delete the deployments this shell knows. */
    private fun showDeviceManager() {
        val devices = shellDevices.load()
        chrome.showDeviceManager(
            dark = pageDark,
            devices = devices,
            currentId = shellDevices.currentId(),
            onSelect = { device ->
                shellDevices.setCurrent(device.id)
                applyServerUrl(device.url)
            },
            onRename = { device -> showRenameDeviceDialog(device) },
            onDelete = { device -> deleteDevice(device) },
            onAdd = { showAddDeviceDialog() },
            onRefresh = { reloadPage() },
            onExit = { finishAffinity() },
        )
    }

    private fun showAddDeviceDialog() {
        chrome.showTextDialog(
            dark = pageDark,
            title = getString(R.string.device_add_title),
            description = getString(R.string.device_add_description),
            initial = "",
            hint = getString(R.string.device_add_hint),
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI,
            errorText = getString(R.string.device_url_invalid),
            onSubmit = { raw ->
                val target = parseServerUrl(raw) ?: return@showTextDialog false
                val device = shellDevices.add(target.url)
                shellDevices.setCurrent(device.id)
                applyServerUrl(target.url)
                true
            },
        )
    }

    /**
     * Pairing without a camera: relay origin + eight-digit code.
     *
     * The claim page accepts `?code=`, so this only has to build the same URL the QR would have
     * carried and treat it as a device. The relay validates the code; a wrong one shows its own
     * claim page rather than a silent failure.
     */
    private fun showPairingCodeDialog() {
        val initial = runCatching {
            val uri = java.net.URI(targetUrl)
            val port = if (uri.port > 0) uri.port else if (uri.scheme == "https") 443 else 80
            if (port == 3443 || targetUrl.contains("/relay")) "${uri.scheme}://${uri.host}:$port" else ""
        }.getOrDefault("")
        chrome.showPairingDialog(
            dark = pageDark,
            initialAddress = initial,
            onSubmit = { address, code ->
                val base = address.trim().trimEnd('/')
                val uri = runCatching { java.net.URI(base) }.getOrNull()
                val codeOk = code.trim().matches(Regex("^\\d{6,12}$"))
                if (uri?.host == null || (uri.scheme != "http" && uri.scheme != "https") || !codeOk) {
                    false
                } else {
                    val url = "$base/relay/pair?code=${code.trim()}"
                    val device = shellDevices.add(url, name = getString(R.string.shell_scan_qr_device))
                    shellDevices.setCurrent(device.id)
                    Toast.makeText(this, R.string.shell_scan_qr_added, Toast.LENGTH_SHORT).show()
                    applyServerUrl(url)
                    true
                }
            },
            onScan = { openQrScanner() },
        )
    }

    private fun openQrScanner() {
        qrScanLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(getString(R.string.shell_scan_qr_prompt))
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }

    private fun handleScannedQr(raw: String) {
        var relayOrigin: String? = null
        val url = when (val parsed = RelayPairing.parsePayload(raw)) {
            is PairingPayloadResult.Valid -> {
                val base = parsed.payload.url.trimEnd('/')
                relayOrigin = base
                if (base.endsWith("/relay/pair")) base else "$base/relay/pair"
            }
            is PairingPayloadResult.TooNew -> {
                Toast.makeText(this, R.string.shell_scan_qr_too_new, Toast.LENGTH_SHORT).show()
                return
            }
            PairingPayloadResult.NotAPairingCode -> {
                val text = raw.trim()
                text.takeIf {
                    it.startsWith("http://", ignoreCase = true) ||
                        it.startsWith("https://", ignoreCase = true)
                }
            }
        }
        if (url == null) {
            Toast.makeText(this, R.string.shell_scan_qr_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        val target = parseServerUrl(url) ?: run {
            Toast.makeText(this, R.string.shell_scan_qr_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        // Show the decoded address before it becomes a device. A scanned code is easy to trust,
        // and a relay pair page is exactly the kind of address that should not be silently
        // committed when the QR came from the wrong tab or the wrong host.
        AlertDialog.Builder(this)
            .setTitle(R.string.shell_scan_qr_confirm_title)
            .setMessage(target.url)
            .setPositiveButton(R.string.shell_scan_qr_confirm) { _, _ ->
                // A relay invitation is a one-time page; the device is the relay origin. Storing
                // the pair URL would reopen the claim form on every launch after pairing.
                val stored = relayOrigin?.let { it.trimEnd('/') + "/" } ?: target.url
                val device = shellDevices.add(stored, name = getString(R.string.shell_scan_qr_device))
                shellDevices.setCurrent(device.id)
                Toast.makeText(this, R.string.shell_scan_qr_added, Toast.LENGTH_SHORT).show()
                applyServerUrl(target.url)
            }
            .setNegativeButton(R.string.common_close, null)
            .show()
    }

    private fun showRenameDeviceDialog(device: ShellDevice) {
        chrome.showTextDialog(
            dark = pageDark,
            title = getString(R.string.device_rename_title),
            description = getString(R.string.device_rename_description),
            initial = device.name,
            hint = getString(R.string.device_name_hint),
            inputType = android.text.InputType.TYPE_CLASS_TEXT,
            errorText = getString(R.string.device_name_invalid),
            onSubmit = { raw ->
                val name = raw.trim().take(40)
                if (name.isEmpty()) {
                    false
                } else {
                    shellDevices.rename(device.id, name)
                    showDeviceManager()
                    true
                }
            },
        )
    }

    private fun deleteDevice(device: ShellDevice) {
        val devices = shellDevices.load()
        if (devices.size <= 1) {
            Toast.makeText(this, R.string.device_at_least_one, Toast.LENGTH_SHORT).show()
            showDeviceManager()
            return
        }
        val wasCurrent = shellDevices.currentId() == device.id
        shellDevices.remove(device.id)
        if (wasCurrent) {
            val next = shellDevices.load().firstOrNull()
            if (next != null) {
                shellDevices.setCurrent(next.id)
                applyServerUrl(next.url)
            }
        } else {
            showDeviceManager()
        }
    }

    private data class ServerTarget(
        val host: String,
        val port: Int,
        val useTls: Boolean,
        val basePath: String,
        val url: String,
    )

    private fun parseServerUrl(raw: String): ServerTarget? {
        val trimmed = raw.trim()
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
        val port = if (uri.port > 0) uri.port else if (scheme == "https") 443 else 80
        if (port !in 1..65535) return null
        val basePath = uri.path.orEmpty().trimEnd('/').takeIf { it.isNotEmpty() } ?: ""
        val authority = when {
            scheme == "https" && port == 443 -> host
            scheme == "http" && port == 80 -> host
            else -> "$host:$port"
        }
        return ServerTarget(
            host = host,
            port = port,
            useTls = scheme == "https",
            basePath = basePath,
            // Preserve an exact non-root path (notably /relay/pair); only the origin gets a
            // trailing slash so the page's own relative URLs keep resolving.
            url = "$scheme://$authority$basePath" + if (basePath.isBlank()) "/" else "",
        )
    }

    /**
     * Switch the shell's address and reload at it.
     *
     * Returns false (and changes nothing) when the text is not an http(s) URL, so the dialog can
     * keep itself open and show its error line. The new endpoint is remembered both as the shell
     * address and as a host record, which is what lets a gateway cookie survive a switch back.
     */
    private fun markAndroidUrl(raw: String): String = runCatching {
        val uri = Uri.parse(raw)
        if (uri.getQueryParameter("dsh_android") == "1") raw
        else uri.buildUpon().appendQueryParameter("dsh_android", "1").build().toString()
    }.getOrDefault(raw)

    /**
     * After a successful relay pairing the relay redirects to its origin. Point the saved device
     * there, so the next launch opens the app instead of the one-time claim form.
     */
    private fun normalizeRelayDeviceUrl(loaded: String?) {
        val host = targetHost ?: return
        if (!host.basePath.startsWith("/relay/")) return
        val uri = runCatching { java.net.URI(loaded ?: return) }.getOrNull() ?: return
        val path = uri.path ?: return
        if (path.startsWith("/relay/")) return
        val authority = uri.authority ?: return
        val origin = "${uri.scheme}://$authority/"
        val current = shellDevices.load().firstOrNull { it.id == shellDevices.currentId() } ?: return
        if (current.url.trimEnd('/') != origin.trimEnd('/')) {
            shellDevices.updateUrl(current.id, origin)
            recordPageEvent("relay device normalized")
        }
        targetHost = host.copy(basePath = "")
        targetUrl = origin
    }

    private fun applyServerUrl(raw: String): Boolean {
        val target = parseServerUrl(raw) ?: return false
        recordPageEvent("switch device " + target.host + ":" + target.port)
        lifecycleScope.launch {
            val remembered = withContext(Dispatchers.IO) {
                runCatching {
                    hostsStore.rememberHost(
                        name = target.host,
                        host = target.host,
                        port = target.port,
                        isLoopback = target.host == "127.0.0.1" || target.host == "localhost" || target.host == "::1",
                        useTls = target.useTls,
                        basePath = target.basePath,
                        isGateway = target.useTls,
                    )
                }.getOrNull()
            }
            withContext(Dispatchers.IO) {
                runCatching { hostsStore.setShellUrl(target.url) }
            }
            if (isFinishing || isDestroyed) return@launch
            targetHost = remembered
            targetUrl = target.url
            if (remembered != null) {
                launch { withContext(Dispatchers.IO) { runCatching { connectionManager.connect(remembered) } } }
            }
            prefetchDeferred = null
            interceptPrefetch = false
            clearHistoryAfterLoad = true
            hideError()
            webView.loadUrl(markAndroidUrl(target.url))
        }
        return true
    }

    /**
     * Ask for the microphone once the app page is up.
     *
     * A WebView grants `getUserMedia` through `WebChromeClient.onPermissionRequest`, but that call
     * races the page's own capture start when the Android runtime permission has never been
     * granted. Asking here, before the user presses the microphone, leaves that callback with a
     * straight grant.
     */
    private fun showSpeechPermissionDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.shell_voice_permission_title)
            .setMessage(message)
            .setPositiveButton(R.string.shell_voice_open_settings) { _, _ -> openSpeechEngineSettings() }
            .setNegativeButton(R.string.shell_update_later, null)
            .show()
    }

    private fun openSpeechEngineSettings() {
        val service = packageManager.queryIntentServices(Intent("android.speech.RecognitionService"), 0).firstOrNull()
        val intent = if (service != null) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${service.serviceInfo.packageName}"))
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        runCatching { startActivity(intent) }
    }

    private fun ensureMicrophonePermission() {
        if (micPermissionAsked) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        val title = webView.title.orEmpty()
        if (title.contains("登录") || title.contains("Sign in", ignoreCase = true)) return
        micPermissionAsked = true
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startNativeSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingNativeSpeech = true
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showSpeechPermissionDialog(getString(R.string.shell_voice_unavailable))
            return
        }
        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    showSpeechPermissionDialog(getString(R.string.shell_voice_permission_message))
                } else {
                    Toast.makeText(this@WebShellActivity, R.string.shell_voice_failed, Toast.LENGTH_SHORT).show()
                }
                recognizer.destroy()
                if (speechRecognizer === recognizer) speechRecognizer = null
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank() && ::webView.isInitialized) {
                    val quoted = org.json.JSONObject.quote(text)
                    webView.evaluateJavascript(
                        "window.__dshInsertSpeech && window.__dshInsertSpeech($quoted)",
                        null,
                    )
                }
                recognizer.destroy()
                if (speechRecognizer === recognizer) speechRecognizer = null
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
    }

    private fun openExternalUri(uri: Uri) {
        val scheme = uri.scheme?.lowercase()
        if (scheme.isNullOrBlank() || scheme == "javascript" || scheme == "data" || scheme == "file") return
        val intent = if (scheme == "intent") {
            runCatching { Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) }.getOrNull()
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        } ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .onFailure { failure ->
                val fallback = if (scheme == "intent") intent.getStringExtra("browser_fallback_url") else null
                if (!fallback.isNullOrBlank() && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                    if (::webView.isInitialized && !isFinishing && !isDestroyed) webView.loadUrl(fallback)
                } else if (failure is ActivityNotFoundException) {
                    Toast.makeText(this, R.string.webshell_open_failed, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun onDownloadRequested(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        if (url.startsWith("blob:") || url.startsWith("data:")) return
        val saved = WebShellDownloads.enqueue(this, url, userAgent, mimeType, contentDisposition)
        val message = if (saved != null) {
            getString(R.string.webshell_download_started, saved)
        } else {
            getString(R.string.webshell_download_failed)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ---- lifecycle ----------------------------------------------------------

    override fun onResume() {
        super.onResume()
        recordPageEvent("resume")
        if (::webView.isInitialized) {
            webView.onResume()
            // Returning from the background is the other moment a stale surface shows up.
            mainHandler.postDelayed({ repairPageLayout() }, 300)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sessionIdFromIntent(intent)?.let {
            pendingSessionId = it
            openPendingSession()
        }
    }

    private fun sessionIdFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val extra = intent.getStringExtra(DshNotifications.EXTRA_SESSION_ID)
        if (!extra.isNullOrBlank()) return extra
        return intent.data?.pathSegments?.lastOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun openPendingSession() {
        val id = pendingSessionId ?: return
        if (!::webView.isInitialized || !pageReady.get()) return
        val quoted = org.json.JSONObject.quote(id)
        webView.evaluateJavascript(
            "(function(){try{if(window.__dshOpenSession){window.__dshOpenSession($quoted);return '1';}}catch(e){}return '0';})()",
        ) { result ->
            if (result?.contains("1") == true) pendingSessionId = null
        }
    }

    override fun onPause() {
        // Keep the WebView (and its WebSocket) running while the app is in the background: the
        // conversation must keep syncing, not freeze until the next resume. Android may throttle
        // timers, but the live socket and its callbacks stay alive for a normal background stint.
        cookieManager.flush()
        super.onPause()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        mainHandler.removeCallbacksAndMessages(null)
        if (isFinishing && ::webView.isInitialized) {
            root.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }

    // ---- small helpers ------------------------------------------------------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun isNightUi(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private data class ResolvedTarget(val host: HostConfig, val url: String, val cookie: String?)

    private data class InsetsDp(val top: Float, val bottom: Float, val ime: Float)

    /**
     * The numbers that decide whether the page can fill the window.
     *
     * Kept in the diagnostics page because a "page only fills a strip" report cannot be told apart
     * from a page layout bug without knowing whether the WebView itself is short.
     */
    private fun layoutSummary(): String {
        val metrics = resources.displayMetrics
        val web = if (::webView.isInitialized) "${webView.width}x${webView.height}" else "n/a"
        val container = if (::root.isInitialized) "${root.width}x${root.height}" else "n/a"
        val decor = runCatching { window.decorView.height }.getOrDefault(0)
        return "web=$web root=$container window=${metrics.widthPixels}x${metrics.heightPixels} " +
            "decor=$decor density=${metrics.density} ime=${insets.ime}"
    }

    /** Append one line to the page lifecycle log shown by diagnostics. */
    private fun recordPageEvent(message: String) {
        val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        synchronized(pageEvents) {
            pageEvents.addLast("$stamp $message")
            while (pageEvents.size > 80) pageEvents.removeFirst()
        }
    }

    private fun pageEventSummary(limit: Int = 24): String = synchronized(pageEvents) {
        if (pageEvents.isEmpty()) return@synchronized "(none yet)"
        pageEvents.toList().takeLast(limit).joinToString("\n")
    }

    private companion object {
        const val TAG = "DshWebShell"
        const val SPLASH_TIMEOUT_MS = 8_000L

        /**
         * How long the intercepted document request may wait for the parallel preload.
         *
         * The preload starts before `loadUrl`, so a fast network usually wins this race; on a slow
         * one the wait has to stay small or it would add latency instead of removing it.
         */
        const val PREFETCH_WAIT_MS = 350L
        val DARK_BACKGROUND = 0xFF151517.toInt()
        val DARK_TEXT = 0xFFF5F5F5.toInt()
        val DARK_SECONDARY = 0xFFA9AEB5.toInt()
        val LIGHT_TEXT = 0xFF0F1115.toInt()
        val LIGHT_SECONDARY = 0xFF61666B.toInt()
        val GATE_COOKIE = Regex("(?:^|;\\s*)(dsh_gate=[^;]+)")
        const val BACK_PROBE =
            "(function(){try{return !!(window.__dshBack && window.__dshBack());}catch(e){return false;}})()"
    }
}
