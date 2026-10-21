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
                root.style.setProperty('--dsh-android-system-top', '${top}px');
                root.style.setProperty('--dsh-android-system-bottom', '${bottom}px');
                root.style.setProperty('--dsh-android-ime-bottom', '${ime}px');
              } catch (e) {}
            })();
        """.trimIndent()
    }
}
