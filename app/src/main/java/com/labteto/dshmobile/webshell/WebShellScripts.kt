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
     * The page's own start-up numbers, as one plain string.
     *
     * `ttfb` is the request's wait before the first byte; `fcp` is the first contentful paint; `dcl`
     * is the document finishing its own script. The shell records its side separately, so the two
     * together say which stage a slow start was spent in instead of inviting a guess. Anything the
     * browser does not report comes back as `?`, never as a fabricated zero.
     */
    val PAGE_TIMING: String = """
        (function () {
          try {
            var nav = performance.getEntriesByType('navigation')[0];
            if (!nav) { return ''; }
            var fcp = 0;
            var paints = performance.getEntriesByType('paint') || [];
            for (var i = 0; i < paints.length; i++) {
              if (paints[i].name === 'first-contentful-paint') { fcp = paints[i].startTime; }
            }
            return 'ttfb=' + Math.round(nav.responseStart) +
              ' fcp=' + (fcp ? Math.round(fcp) : '?') +
              ' dcl=' + Math.round(nav.domContentLoadedEventEnd);
          } catch (e) {
            return '';
          }
        })()
    """.trimIndent()

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
     * Re-assert the viewport against the real layout height — and that is all it does.
     *
     * This function used to dispatch a synthetic `resize` (on the window and on `visualViewport`)
     * whenever anything changed. That event is what makes the harness remount its composer and
     * re-request its model list, i.e. it *was* the "it refreshed itself" report. Nothing here
     * touches it any more: when the WebView is really resized the platform already delivers a real
     * resize, and when it is not resized there is nothing to announce.
     *
     * What is left is one value check: the layout height. It is written only when it differs from
     * the height this document was last told about, and the short-root fallback needs the shortfall
     * to be observed twice, so a page still painting its first frame is left alone.
     */
    fun viewportSync(): String = """
        (function () {
          try {
            var root = document.documentElement;
            var body = document.body;
            if (!root || !body) { return; }
            // Layout viewport, not visualViewport: the latter shrinks with the keyboard and is what
            // made this write land on every IME animation frame.
            var height = Math.round(window.innerHeight || 0);
            if (!height) { return; }
            var stats = window.__dshShellStats ||
              (window.__dshShellStats = { syncs: 0, layoutWrites: 0, rootFixes: 0, shortRoot: 0 });
            stats.syncs++;
            var desired = height + 'px';
            if (window.__dshShellLayout !== desired) {
              window.__dshShellLayout = desired;
              if (root.style.minHeight !== desired) { root.style.minHeight = desired; stats.layoutWrites++; }
              if (body.style.minHeight !== desired) { body.style.minHeight = desired; stats.layoutWrites++; }
              if (root.style.getPropertyValue('--dsh-android-viewport-height') !== desired) {
                root.style.setProperty('--dsh-android-viewport-height', desired);
              }
            }
            if (window.__dshShellRootFixed !== true) {
              var app = document.getElementById('root');
              if (app) {
                var boxHeight = app.getBoundingClientRect().height;
                if (boxHeight > 0 && boxHeight < height * 0.6) {
                  stats.shortRoot++;
                  if (stats.shortRoot >= 2) {
                    app.style.height = desired;
                    window.__dshShellRootFixed = true;
                    stats.rootFixes++;
                  }
                } else {
                  stats.shortRoot = 0;
                }
              }
            }
          } catch (e) {}
        })();
    """.trimIndent()

    /**
     * What the page thinks has been happening to it.
     *
     * Installed once per document. Two facts are counted that the native side cannot see:
     * the composer being remounted (a React identity change, i.e. the harness rebuilt it) and the
     * API calls the page made. Together with the shell's own resize counter they answer "who
     * refreshed this" with numbers instead of a guess.
     *
     * Strings only, no JSON: the value comes back through `evaluateJavascript`, and a plain string
     * needs no unescaping on either side.
     */
    fun health(): String = """
        (function () {
          if (window.__dshShellHealth === true) { return; }
          window.__dshShellHealth = true;
          var startedAt = Date.now();
          var calls = [];
          var stats = window.__dshShellStats ||
            (window.__dshShellStats = { syncs: 0, resizes: 0, rootFixes: 0, shortRoot: 0 });
          stats.remounts = 0;
          stats.longTasks = 0;
          stats.longMs = 0;
          stats.scrollMin = 0;
          stats.scrollSamples = 0;
          // Scroll sampling: a long session keeps every message in the DOM, so the honest question
          // is not "how many nodes are there" but "what did the last scroll actually render at".
          // Frames are counted only while a scroll is in flight, and the sample closes 250 ms after
          // it ends — there is no timer idling between scrolls. The `scroll` event is heard in the
          // capture phase so a scrolling inner container counts too, not just the document.
          (function () {
            var frames = 0;
            var sampleStart = 0;
            var stopTimer = 0;
            var running = false;
            var tick = function () {
              if (!running) { return; }
              frames++;
              window.requestAnimationFrame(tick);
            };
            var finish = function () {
              running = false;
              var ms = performance.now() - sampleStart;
              if (ms >= 120 && frames >= 2) {
                var fps = Math.round((frames * 1000) / ms);
                // A tab restored from the background reports a huge first frame; ignore anything
                // outside a plausible rendering rate instead of poisoning the minimum with it.
                if (fps > 0 && fps < 240) {
                  var live = window.__dshShellStats;
                  live.scrollSamples++;
                  live.scrollMin = live.scrollMin === 0 ? fps : Math.min(live.scrollMin, fps);
                }
              }
              frames = 0;
            };
            window.addEventListener('scroll', function () {
              if (!running) {
                running = true;
                sampleStart = performance.now();
                frames = 0;
                window.requestAnimationFrame(tick);
              }
              window.clearTimeout(stopTimer);
              stopTimer = window.setTimeout(finish, 250);
            }, { passive: true, capture: true });
          })();
          // Long tasks are the browser's own measure of main-thread stalls over 50 ms: the honest
          // answer to "is the reply late because the phone is busy". A reply that feels chunky shows
          // up here; one that arrives late but paints instantly does not — that one is the network.
          try {
            if (typeof PerformanceObserver === 'function') {
              new PerformanceObserver(function (list) {
                var entries = list.getEntries();
                for (var i = 0; i < entries.length; i++) {
                  window.__dshShellStats.longTasks++;
                  window.__dshShellStats.longMs += Math.round(entries[i].duration);
                }
              }).observe({ entryTypes: ['longtask'] });
            }
          } catch (e) {}
          try {
            var originalFetch = window.fetch;
            if (typeof originalFetch === 'function') {
              window.fetch = function (input) {
                var entry = null;
                try {
                  var url = typeof input === 'string' ? input : (input && input.url) || '';
                  var path = String(url).split('?')[0].slice(-72);
                  entry = { at: Date.now(), path: path, ms: -1 };
                  calls.push(entry);
                  while (calls.length > 80) { calls.shift(); }
                } catch (e) {}
                var promise = originalFetch.apply(window, arguments);
                // Time the call without changing it: the entry is filled in when the promise
                // settles, so "the history took 1.2 s" is a measurement instead of a feeling.
                try {
                  if (entry !== null && promise && typeof promise.then === 'function') {
                    promise.then(
                      function () { entry.ms = Date.now() - entry.at; },
                      function () { entry.ms = Date.now() - entry.at; entry.failed = true; }
                    );
                  }
                } catch (e) {}
                return promise;
              };
            }
          } catch (e) {}
          try {
            new MutationObserver(function (records) {
              for (var i = 0; i < records.length; i++) {
                var added = records[i].addedNodes;
                for (var j = 0; j < added.length; j++) {
                  var node = added[j];
                  if (node && node.nodeType === 1 && node.matches && node.matches('[data-composer-card]')) {
                    window.__dshShellStats.remounts++;
                  }
                }
              }
            }).observe(document.body, { childList: true, subtree: true });
          } catch (e) {}
          window.__dshShellReport = function () {
            var s = window.__dshShellStats;
            return 'up=' + Math.round((Date.now() - startedAt) / 1000) + 's' +
              ' sync=' + s.syncs + ' layoutWrite=' + (s.layoutWrites || 0) +
              ' rootFix=' + s.rootFixes + ' composerRemount=' + s.remounts +
              ' autoRetry=' + (window.__dshAutoReconnect && window.__dshAutoReconnect.attempts || 0) +
              ' scrollMin=' + (s.scrollMin || 0) + '@' + (s.scrollSamples || 0) +
              ' longTask=' + (s.longTasks || 0) + '/' + (s.longMs || 0) + 'ms';
          };
          window.__dshShellNetwork = function (seconds) {
            var since = Date.now() - (seconds || 120) * 1000;
            var counts = {};
            var order = [];
            var total = 0;
            var slow = [];
            for (var i = 0; i < calls.length; i++) {
              if (calls[i].at < since) { continue; }
              total++;
              var key = calls[i].path;
              if (counts[key] === undefined) { counts[key] = 0; order.push(key); }
              counts[key]++;
              if (calls[i].ms >= 0) { slow.push(calls[i]); }
            }
            var parts = [];
            for (var k = 0; k < order.length && k < 5; k++) {
              parts.push(order[k] + ' x' + counts[order[k]]);
            }
            // The three slowest calls, not the most frequent: opening a long conversation is one
            // request that takes a second, and a count of it says nothing about why it felt slow.
            slow.sort(function (a, b) { return b.ms - a.ms; });
            var worst = [];
            for (var w = 0; w < slow.length && w < 3; w++) {
              if (slow[w].ms < 150) { break; }
              worst.push(slow[w].path + ' ' + slow[w].ms + 'ms' + (slow[w].failed ? '!' : ''));
            }
            return 'api=' + total + (parts.length ? ' ' + parts.join(' , ') : '') +
              (worst.length ? ' | slow=' + worst.join(' , ') : '');
          };
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
