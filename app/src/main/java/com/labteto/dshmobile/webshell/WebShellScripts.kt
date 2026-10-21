package com.labteto.dshmobile.webshell

import java.util.Locale

/**
 * JavaScript the shell injects into the harness web client once its document exists.
 *
 * The injected surface is deliberately small: the page is the product, so everything here either
 * repairs a plain-WebView gap (new-tab links, `blob:` downloads, the page's own dark-theme fact) or
 * feeds it facts only the Android shell knows (system-bar and IME insets). No routing, no DOM
 * rewriting and no storage access happens from the shell.
 */
internal object WebShellScripts {

    /** Names of the two [android.webkit.WebView.addJavascriptInterface] objects. */
    const val SHELL_BRIDGE = "DshShellBridge"
    const val BACK_BRIDGE = "dshBackBridge"

    /**
     * Idempotent DOM patches.
     *
     * - A single-window WebView drops `window.open` / `target=_blank`, so both are turned into a
     *   same-window navigation for this host or a hand-off to the system browser for anything else.
     * - `<a download>` with a `blob:` URL never reaches `DownloadListener`; it is fetched in page
     *   context and saved through the shell bridge instead.
     * - The harness theme is published as `<body data-ds-dark-theme>`; mirroring it lets the system
     *   bars follow the page instead of the device.
     */
    val SHIMS: String = """
        (function () {
          if (window.__dshShellShims) { return; }
          window.__dshShellShims = true;

          function note(message) {
            try { window.$SHELL_BRIDGE.toast(String(message)); } catch (e) {}
          }
          function external(url) {
            try { window.$SHELL_BRIDGE.openExternal(String(url)); } catch (e) {}
          }
          function saveBlob(url, name) {
            try {
              fetch(url).then(function (response) { return response.blob(); }).then(function (blob) {
                var reader = new FileReader();
                reader.onload = function () {
                  try {
                    var text = String(reader.result);
                    var comma = text.indexOf(',');
                    window.$SHELL_BRIDGE.saveFile(
                      name || 'download',
                      blob.type || 'application/octet-stream',
                      comma >= 0 ? text.substring(comma + 1) : ''
                    );
                  } catch (e) { note('save failed'); }
                };
                reader.onerror = function () { note('save failed'); };
                reader.readAsDataURL(blob);
              }).catch(function () { note('download failed'); });
            } catch (e) { note('download failed'); }
          }
          window.__dshShellSaveBlob = saveBlob;

          try {
            var originalOpen = window.open;
            window.open = function (url) {
              try {
                if (url) {
                  var target = new URL(String(url), location.href);
                  if (/^https?:${'$'}/.test(target.protocol)) {
                    if (target.host === location.host || /(^|\.)deepseek\.com${'$'}/.test(target.host)) {
                      location.href = target.href;
                    } else {
                      external(target.href);
                    }
                    return null;
                  }
                }
              } catch (e) {}
              try { return originalOpen ? originalOpen.apply(window, arguments) : null; } catch (e) { return null; }
            };
          } catch (e) {}

          try {
            document.addEventListener('click', function (event) {
              var link = event.target && event.target.closest ? event.target.closest('a[target="_blank"]') : null;
              if (!link || !link.href || link.href.indexOf('blob:') === 0) { return; }
              try {
                var target = new URL(link.href, location.href);
                if (!/^https?:${'$'}/.test(target.protocol)) { return; }
                event.preventDefault();
                event.stopPropagation();
                if (target.host === location.host || /(^|\.)deepseek\.com${'$'}/.test(target.host)) {
                  location.href = target.href;
                } else {
                  external(target.href);
                }
              } catch (e) {}
            }, true);
          } catch (e) {}

          try {
            var originalClick = HTMLAnchorElement.prototype.click;
            HTMLAnchorElement.prototype.click = function () {
              try {
                if (this.href && this.href.indexOf('blob:') === 0) { saveBlob(this.href, this.download); return; }
              } catch (e) {}
              return originalClick.apply(this, arguments);
            };
          } catch (e) {}

          try {
            document.addEventListener('click', function (event) {
              var link = event.target && event.target.closest ? event.target.closest('a[download]') : null;
              if (!link || !link.href || link.href.indexOf('blob:') !== 0) { return; }
              event.preventDefault();
              event.stopPropagation();
              saveBlob(link.href, link.download);
            }, true);
          } catch (e) {}

          try {
            var originalRevoke = URL.revokeObjectURL;
            URL.revokeObjectURL = function (url) {
              try { setTimeout(function () { try { originalRevoke.call(URL, url); } catch (e) {} }, 60000); }
              catch (e) {}
            };
          } catch (e) {}

          function reportTheme() {
            try {
              var dark = (document.body && document.body.hasAttribute('data-ds-dark-theme')) ||
                         (document.documentElement && document.documentElement.hasAttribute('data-ds-dark-theme'));
              window.$SHELL_BRIDGE.setDark(!!dark);
            } catch (e) {}
          }
          try {
            new MutationObserver(reportTheme).observe(document.documentElement, {
              attributes: true, subtree: true, attributeFilter: ['data-ds-dark-theme']
            });
          } catch (e) {}
          reportTheme();

          // Drawer gestures: a right swipe opens the off-screen sidebar (the same action as
          // tapping the narrow-screen drawer button), a left swipe closes it again. The toggle
          // button is the plugin's own published control, so this is a pointer gesture aimed at
          // the same handler rather than a second drawer implementation.
          try {
            var gestureX = 0;
            var gestureY = 0;
            var gestureAt = 0;
            var gestureId = -1;
            function drawerOpen() {
              var frame = document.querySelector('[data-dsh-frame]');
              return frame !== null && !frame.hasAttribute('data-sidebar-collapsed');
            }
            function drawerToggle() {
              var button = document.querySelector('button[data-dsh-sidebar-toggle]');
              if (!button) { return false; }
              button.click();
              return true;
            }
            document.addEventListener('touchstart', function (event) {
              gestureId = -1;
              if (!document.documentElement.hasAttribute('data-dsh-mobile-form')) { return; }
              if (event.touches.length !== 1) { return; }
              var target = event.target;
              if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)) { return; }
              var touch = event.touches[0];
              gestureX = touch.clientX;
              gestureY = touch.clientY;
              gestureAt = Date.now();
              gestureId = touch.identifier;
            }, { passive: true });
            document.addEventListener('touchcancel', function () { gestureId = -1; }, { passive: true });
            document.addEventListener('touchend', function (event) {
              if (gestureId === -1) { return; }
              if (!document.documentElement.hasAttribute('data-dsh-mobile-form')) { gestureId = -1; return; }
              if (event.changedTouches.length !== 1) { gestureId = -1; return; }
              var touch = event.changedTouches[0];
              if (touch.identifier !== gestureId) { gestureId = -1; return; }
              gestureId = -1;
              if (document.documentElement.hasAttribute('data-dsh-modal-open')) { return; }
              if (Date.now() - gestureAt > 800) { return; }
              var dx = touch.clientX - gestureX;
              var dy = touch.clientY - gestureY;
              if (Math.abs(dx) < 70 || Math.abs(dy) > Math.abs(dx) * 0.6) { return; }
              var open = drawerOpen();
              if (dx > 0 && !open) { drawerToggle(); }
              else if (dx < 0 && open) { drawerToggle(); }
            }, { passive: true });
          } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Re-assert the viewport against the real WebView size.
     *
     * The harness client is a full-height flex app: its root is sized from the viewport, and that
     * viewport only changes when the WebView is resized. After a keyboard or a surface detach the
     * root can be left at a stale, shorter height — the page then paints in a band at the top and
     * the shell's background shows underneath. `min-height` never fights a page that is already
     * full height; the explicit root height is the last resort for one that cached a pixel size.
     */
    fun repairViewport(): String = """
        (function () {
          try {
            var viewport = Math.round(window.visualViewport ? window.visualViewport.height : window.innerHeight);
            var root = document.documentElement;
            var body = document.body;
            if (!root || !body || !viewport) { return; }
            // Everything below is conditional on a real difference. Dispatching `resize` when
            // nothing changed makes the harness remount the composer and re-read the model list,
            // which is what "it refreshes by itself" looks like from the outside.
            var changed = false;
            var desired = viewport + 'px';
            if (root.style.minHeight !== desired) { root.style.minHeight = desired; changed = true; }
            if (body.style.minHeight !== desired) { body.style.minHeight = desired; changed = true; }
            if (root.style.getPropertyValue('--dsh-android-viewport-height') !== desired) {
              root.style.setProperty('--dsh-android-viewport-height', desired);
            }
            var app = document.getElementById('root');
            if (app) {
              var height = app.getBoundingClientRect().height;
              if (height > 0 && height < viewport * 0.6) {
                app.style.height = desired;
                changed = true;
              }
            }
            if (!changed) { return; }
            void body.offsetHeight;
            window.dispatchEvent(new Event('resize'));
            if (window.visualViewport) {
              try { window.visualViewport.dispatchEvent(new Event('resize')); } catch (e) {}
            }
          } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Mark the page when a third-party glass theme makes `position: fixed` overlays local.
     *
     * `backdrop-filter`, `filter`, `contain`, `perspective` and `will-change: transform` each turn
     * an element into the containing block of its fixed descendants. A theme that puts any of
     * them on the app shell therefore moves the sidebar drawer and dialogs relative to itself —
     * the page then paints in a band and the drawer sits at an offset. The stylesheet that ships
     * with this plugin drops only the frosted layer under `html[data-dsh-glass="off"]`, so the
     * theme keeps its palette and the geometry goes back to the viewport.
     */
    fun glassCompat(mode: String): String = """
        (function () {
          try {
            var root = document.documentElement;
            if (!root) { return; }
            var mode = '${mode}';
            // `injectPageScripts` runs on start, commit and finish. Re-arming four timers on each
            // of those passes makes the page look like it is reloading; one pass per document is
            // enough, and a mode change re-arms it explicitly.
            if (window.__dshGlassReady === true && window.__dshGlassMode === mode) { return; }
            window.__dshGlassReady = true;
            window.__dshGlassMode = mode;
            // Ship the rule with the shell, not only with the host plugin: the layout must be
            // repairable even when the host still runs an older plugin build.
            if (!document.getElementById('dsh-shell-glass')) {
              var style = document.createElement('style');
              style.id = 'dsh-shell-glass';
              style.textContent =
                'html[data-dsh-glass="off"] *,html[data-dsh-glass="off"] *::before,html[data-dsh-glass="off"] *::after{' +
                'backdrop-filter:none !important;-webkit-backdrop-filter:none !important;}' +
                'html[data-dsh-glass="off"] body::before,html[data-dsh-glass="off"] body::after{' +
                'filter:none !important;animation-duration:0s !important;}';
              (document.head || root).appendChild(style);
            }
            var decide = function () {
              var off = mode === 'force';
              if (mode === 'auto') {
                var seeds = [document.getElementById('root')];
                var sidebar = document.querySelector('[class*="_sidebarCol"]');
                if (sidebar) { seeds.push(sidebar); }
                var bad = /backdrop|blur|contain|perspective|transform/i;
                for (var i = 0; i < seeds.length && !off; i++) {
                  var node = seeds[i];
                  while (node && node !== document.body && node !== root) {
                    var style = window.getComputedStyle(node);
                    var backdrop = style.backdropFilter || style.webkitBackdropFilter || 'none';
                    var filter = style.filter || 'none';
                    var contain = style.contain || 'none';
                    var perspective = style.perspective || 'none';
                    var willChange = style.willChange || 'auto';
                    if ((backdrop !== 'none') ||
                        (filter !== 'none' && bad.test(filter)) ||
                        (contain !== 'none') ||
                        (perspective !== 'none') ||
                        (willChange !== 'auto' && /transform/i.test(willChange))) {
                      off = true;
                      break;
                    }
                    node = node.parentElement;
                  }
                }
                // Theme glass often lives on a full-screen pseudo-element (Bloom's aurora layer
                // is body::before with filter: blur(76px)), which the ancestor walk cannot see.
                // A 76px full-viewport blur is also a GPU-memory hog on a phone, so it is worth
                // catching even when the geometry looks fine.
                if (!off) {
                  var pseudoBlur = function (node, which) {
                    try {
                      var s = window.getComputedStyle(node, which);
                      var f = s.filter || 'none';
                      var b = s.backdropFilter || s.webkitBackdropFilter || 'none';
                      return (f !== 'none' && bad.test(f)) || (b !== 'none');
                    } catch (e) { return false; }
                  };
                  var bodyEl = document.body;
                  var appEl = document.getElementById('root');
                  off = pseudoBlur(bodyEl, '::before') || pseudoBlur(bodyEl, '::after') ||
                        pseudoBlur(root, '::before') || (appEl !== null && pseudoBlur(appEl, '::before'));
                  if (!off) {
                    var bodyStyle = window.getComputedStyle(bodyEl);
                    var bodyFilter = bodyStyle.filter || 'none';
                    off = bodyFilter !== 'none' && bad.test(bodyFilter);
                  }
                }
              }
              var next = off ? 'off' : 'on';
              // Attribute writes are mutations too: the harness observes the document, so only
              // the actual transition is published.
              if (root.getAttribute('data-dsh-glass') !== next) {
                root.setAttribute('data-dsh-glass', next);
              }
            };
            decide();
            setTimeout(decide, 1200);
          } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Push the current system-bar / IME insets into the page as CSS variables.
     *
     * `@dsh-android/dsh-client-ui-responsive` consumes exactly these three names: its mobile top bar
     * reads `--dsh-android-system-top`, the composer seat reads `--dsh-android-system-bottom` and
     * `--dsh-android-ime-bottom`, and its keyboard-boundary observer uses the IME value to decide
     * whether the composer row has to be pinned above the keyboard.
     */
    fun insets(topDp: Float, bottomDp: Float, imeDp: Float): String {
        val top = String.format(Locale.US, "%.2f", topDp)
        val bottom = String.format(Locale.US, "%.2f", bottomDp)
        val ime = String.format(Locale.US, "%.2f", imeDp)
        return """
            (function () {
              try {
                var root = document.documentElement;
                if (!root) { return; }
                // Only publish a value that actually changed. Every style write mutates the
                // document's style attribute, and the harness observes it: re-writing identical
                // insets on each resize pass is churn the page pays for with a re-render.
                var set = function (name, value) {
                  if (root.style.getPropertyValue(name) !== value) {
                    root.style.setProperty(name, value);
                  }
                };
                set('--dsh-android-system-top', '${top}px');
                set('--dsh-android-system-bottom', '${bottom}px');
                set('--dsh-android-ime-bottom', '${ime}px');
              } catch (e) {}
            })();
        """.trimIndent()
    }
}
