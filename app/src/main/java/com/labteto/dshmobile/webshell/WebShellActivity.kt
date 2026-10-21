package com.labteto.dshmobile.webshell

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
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
import android.provider.Settings
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.labteto.dshmobile.R
import com.labteto.dshmobile.connection.HarnessSessionStore
import com.labteto.dshmobile.connection.HostConfig
import com.labteto.dshmobile.connection.HostsStore
import com.labteto.dshmobile.connection.ConnectionManager
import com.labteto.dshmobile.connection.ConnectionService
import com.labteto.dshmobile.connection.DiscoveryEngine
import com.labteto.dshmobile.connection.AppSettings
import com.labteto.dshmobile.notify.DshNotifications
import com.labteto.dshmobile.notify.NtfyConfig
import com.labteto.dshmobile.notify.NtfySubscriberService
import com.labteto.dshmobile.update.UpdateChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    @Inject lateinit var updateChecker: UpdateChecker

    private val cookieManager: CookieManager get() = CookieManager.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pageReady = AtomicBoolean(false)
    private val backAvailable = AtomicBoolean(false)
    private val failoverTried = mutableSetOf<String>()
    private var failoverInProgress = false
    private var pageHadError = false
    private var pendingSessionId: String? = null
    private val prefetchClient by lazy { newPrefetchClient() }
    private val chrome by lazy { WebShellChrome(this) }
    private val shellDevices by lazy { ShellDevices(this) }

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
    private var pageDark = false
    private var clearHistoryAfterLoad = false
    private var micPermissionAsked = false

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
        startLoading()

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
        setContentView(root)
        applyPageTheme(dark = isNightUi())
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
            setOffscreenPreRaster(true)
        }
        webView.webViewClient = shellClient
        webView.webChromeClient = shellChromeClient
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
            insets = InsetsDp(status / density, navigation / density, ime / density)
            applyInsetsToPage()
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
                markPageReady()
                showSettings()
                return@launch
            }
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
            webView.loadUrl(resolved.url)
        }
    }

    private suspend fun resolveTarget(): ResolvedTarget? {
        val savedUrl = runCatching { hostsStore.shellUrl.first() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return null
        val host = runCatching { rememberHostForUrl(savedUrl) }.getOrNull() ?: return null
        val cookie = runCatching { harnessSessions.cookie(host.id) }.getOrNull()
        return ResolvedTarget(host, host.baseUrl.trimEnd('/') + "/", cookie)
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
            if (!pageHadError) {
                failoverTried.clear()
                failoverInProgress = false
            }
            pageHadError = false
            markPageReady()
            hideError()
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
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) {
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
    }

    private fun applyInsetsToPage() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(WebShellScripts.insets(insets.top, insets.bottom, insets.ime), null)
    }

    private fun applyPageTheme(dark: Boolean) {
        pageDark = dark
        if (::root.isInitialized) root.setBackgroundColor(if (dark) DARK_BACKGROUND else Color.WHITE)
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

    // ---- navigation and recovery -------------------------------------------

    private fun reloadPage() {
        failoverTried.clear()
        failoverInProgress = false
        pageHadError = false
        hideError()
        interceptPrefetch = false
        webView.loadUrl(targetUrl)
    }

    private fun clearCacheAndReload() {
        webView.clearCache(true)
        Toast.makeText(this, R.string.webshell_cache_cleared, Toast.LENGTH_SHORT).show()
        reloadPage()
    }

    private fun recreateWebView() {
        if (isFinishing || isDestroyed) return
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
        webView.loadUrl(targetUrl)
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
        val appSettings = runCatching { runBlocking { hostsStore.settings.first() } }.getOrDefault(AppSettings())
        chrome.showSettings(
            dark = pageDark,
            currentDeviceName = currentName,
            versionName = versionName,
            notificationsEnabled = notifications.canPost() &&
                NotificationManagerCompat.from(this).areNotificationsEnabled(),
            turnNotifications = appSettings.notifyTurnComplete,
            actionNotifications = appSettings.notifyNeedsAction,
            goalNotifications = appSettings.notifyGoal,
            backgroundEnabled = runCatching {
                runBlocking { hostsStore.settings.first().keepConnectedInBackground }
            }.getOrDefault(false),
            ntfyEnabled = NtfyConfig.load(this) != null,
            onManageDevices = { showDeviceManager() },
            onScanLan = { scanLan() },
            onNotificationSettings = { openNotificationSettings() },
            onTestNotification = { sendTestNotification() },
            onToggleTurnNotifications = {
                toggleNotificationSetting { it.copy(notifyTurnComplete = !it.notifyTurnComplete) }
            },
            onToggleActionNotifications = {
                toggleNotificationSetting { it.copy(notifyNeedsAction = !it.notifyNeedsAction) }
            },
            onToggleGoalNotifications = {
                toggleNotificationSetting { it.copy(notifyGoal = !it.notifyGoal) }
            },
            onConfigureNtfy = { configureNtfy() },
            onBackgroundToggle = { toggleBackground() },
            onBatterySettings = { openBatterySettings() },
            onAutostart = { openAutostartSettings() },
            onCheckUpdate = { checkForUpdate() },
            onSource = { openExternalUri(Uri.parse("https://github.com/daniei-chen/DeepSeek-Harness-APP")) },
            onRefresh = { reloadPage() },
            onExit = { finishAffinity() },
        )
    }

    private fun scanLan() {
        Toast.makeText(this, R.string.shell_settings_scanning, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val ports = runCatching { hostsStore.settings.first().knownPorts }.getOrDefault(listOf(3080))
            val found = withContext(Dispatchers.IO) {
                runCatching { discoveryEngine.scan(ports) }.getOrDefault(emptyList())
            }
            if (isFinishing || isDestroyed) return@launch
            if (found.isEmpty()) {
                Toast.makeText(this@WebShellActivity, R.string.shell_settings_scan_empty, Toast.LENGTH_SHORT).show()
                return@launch
            }
            found.forEach { host ->
                shellDevices.add(
                    url = host.baseUrl.trimEnd('/') + "/",
                    name = "局域网 ${host.host}",
                )
            }
            Toast.makeText(
                this@WebShellActivity,
                getString(R.string.shell_settings_scan_found),
                Toast.LENGTH_SHORT,
            ).show()
            showDeviceManager()
        }
    }

    private fun openNotificationSettings() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }.onFailure { openAppDetailsSettings() }
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

    private fun toggleNotificationSetting(transform: (AppSettings) -> AppSettings) {
        lifecycleScope.launch {
            hostsStore.setSetting(transform)
            showSettings()
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
            showSettings()
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
                    Toast.makeText(this@WebShellActivity, R.string.shell_settings_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadUpdate(update: com.labteto.dshmobile.update.AvailableUpdate) {
        val direct = update.apk?.url
        if (direct == null) {
            openExternalUri(Uri.parse(update.url))
            return
        }
        val candidates = listOf(
            direct,
            "https://gh-proxy.com/$direct",
            "https://ghproxy.net/$direct",
        ).distinct()
        Toast.makeText(this, R.string.shell_settings_downloading, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                for (candidate in candidates) {
                    val downloaded = runCatching { downloadApk(candidate) }.getOrNull()
                    if (downloaded != null) return@withContext downloaded
                }
                null
            }
            if (file == null) {
                Toast.makeText(this@WebShellActivity, R.string.shell_settings_update_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            installUpdate(file)
        }
    }

    private fun downloadApk(url: String): File {
        val request = okhttp3.Request.Builder().url(url).build()
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("empty response")
            val dir = File(cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "update.apk")
            body.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return target
        }
    }

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
            url = "$scheme://$authority$basePath".trimEnd('/') + "/",
        )
    }

    /**
     * Switch the shell's address and reload at it.
     *
     * Returns false (and changes nothing) when the text is not an http(s) URL, so the dialog can
     * keep itself open and show its error line. The new endpoint is remembered both as the shell
     * address and as a host record, which is what lets a gateway cookie survive a switch back.
     */
    private fun applyServerUrl(raw: String): Boolean {
        val target = parseServerUrl(raw) ?: return false
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
            webView.loadUrl(target.url)
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
        if (::webView.isInitialized) webView.onResume()
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

    private companion object {
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
