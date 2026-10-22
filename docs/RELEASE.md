# Release 1.0.2

## What this release is

A self-hosted Android client for DeepSeek Harness. No project cloud, no account, no telemetry,
no monetization.

## 1.0 quality gates

- **New user:** install -> first connection in under ten minutes, by QR or by the pairing-code path (设置 -> 连接 -> 配对码连接).
- **Upgrade:** every release has a strictly higher versionCode (1.0.2 = 50002); settings and devices survive.
- **Failure is explainable:** connection doctor, notification self-check and the offline session snapshot each name the cause and the next action.
- **Updates:** read from the repository's latest.json first, so release ordering and GitHub rate limits cannot hide a version.
- **Offline:** the last synced session list stays readable without a network.
- **Zero cloud:** no project account, telemetry, relay or push service; relay/ntfy/UnifiedPush are the user's own.

## Install

- APK: `dist/DeepSeek-Harness-1.0.2-personal.3.apk`
- SHA-256: `638DBF61EFEFFE671943AEFD48CD32F388C0762E74ABD6EA12CE1BAA41C150E2`
- Source: https://github.com/daniei-chen/DeepSeek-Harness-APP

## First run

A fresh install opens **Settings** first. It contains no personal DSH URL.

1. Settings → Devices → Add device
2. Enter your own `http://` or `https://` DSH address
3. Complete the harness gate/login page
4. Later launches reopen the saved device

See `docs/SETUP_HTTP.md` for Caddy/nginx/Cloudflare Tunnel/frp/Tailscale setups.

## Included

- self-hosted connection profiles, rename/delete/switch, LAN scan, failover
- LAN scan now reports progress, scans configured ports plus `3080`, and discovers `dsh-relay` on `3443/3444`
- LAN scan also browses `_dsh._tcp` mDNS in parallel; multicast-filtered networks still fall back to the subnet sweep.
- **Relay pairing now sticks:** a scanned relay invitation is stored as the relay origin, not `/relay/pair`; an existing device still pointing at the claim page rewrites itself as soon as the relay redirects to the app. No more reopening the pairing form on every launch.
- **Honest LAN hint:** the scan row now says it only finds hosts bound to a LAN address, because DSH Desktop listens on 127.0.0.1 by default.
- **Page event timeline:** diagnostics now lists the last 24 page events — started, finished, main-frame error, renderer crash, webview recreated, device switch, resume, requested reload — so "it refreshed by itself" names its author instead of being a guess.
- **Pseudo-element glass detection:** the compatibility check now also reads `:before/:after` and the body, which catches Bloom's full-screen 76px blur layer (a GPU-memory hog on a phone) that the ancestor walk could not see.
- **Offline snapshot:** the last synced session list is kept per host; when the page cannot load, "View recent sessions offline" shows what was running, read-only, with a retry button.
- **Folder load fix:** expanding a folder in the file panel now triggers one automatic refresh when the expansion produced no child rows — the manual refresh that used to be required is no longer needed (host plugin 0.1.10).
- **Notification self-check:** permission, system switch, each channel's importance, battery-optimisation exemption and self-hosted push in one report, each with its fix.
- **Quiet layout repair:** the automatic repair pass only writes when a value really changed and no longer dispatches a synthetic `resize`; repeated identical inset writes are skipped too. That churn was making the harness remount the composer and re-read its model list on its own. The speed pill now keeps its last reading for 6 s across stream gaps.
- **Inbox search:** filter the native inbox by title or status, with a clear action and a proper empty state.
- **File preview:** text and images open in a phone-sized sheet from the file panel (256 KB text, 6 MB images); other types keep the download path. Requires host plugin 0.1.10.
- **Connection doctor:** `设置 → 连接 → 连接体检` walks the same path the WebView walks — address shape, loopback trap, DNS, TCP, TLS certificate, HTTP/gate, host plugin, relay and the live page connection — and prints the fix next to every failing step. Reports can be copied as text.
- **Glass-theme compatibility:** the shell detects `backdrop-filter`/`filter`/`contain`/`perspective` on the app shell (the Bloom regression) and drops only the frosted layer, so the theme keeps its palette and fixed drawers stop drifting. Three modes in `设置 → 诊断`: auto-detect, always off, leave the page alone.
- **Self-hosted update manifest:** the app reads `latest.json` from the repository first and only falls back to the GitHub API, so release ordering and API rate limits cannot hide an update.
- **Update check:** reads the release list and keeps only app-shaped tags (`v0.9.36`), so a host-plugin release published after an app release can no longer become GitHub's "latest" and make the app report "up to date" while an update exists. Plugin releases are also published as pre-releases so older clients keep working.
- **Layout recovery:** back to `adjustResize`, plus a real 1px view-resize nudge (the only thing that makes the renderer re-read the viewport), a `min-height` repair pass, and an automatic repair on resume, after the keyboard closes and after page load. `Settings → Diagnostics → Repair page layout` forces a full repaint.
- **Settings → 远控 (Remote access):** the relay pairing QR is drawn inside the app from the live invitation, with the 8-digit code, remaining validity, copy pairing link/code, relay addresses and a shortcut to the paired-device page. Requires host plugin **0.1.10**; on an older host the page says which side to update.
- Bloom theme note: `dsh-bloom-theme` can leave the WebView painting only part of the page. 0.9.35 repairs it automatically; if it recurs, either disable Bloom for the phone or tap `修复界面排版`.
- Native diagnostics and update-progress dialogs now use the same card/typography language as the shell.
- QR scan now asks for confirmation and shows the decoded URL before adding a device; non-root paths such as `/relay/pair` are kept exact (no trailing-slash 404).
- Settings → Connection → **Scan QR code**: accepts a plain `http(s)` DSH URL or a `dsh-relay-pair` payload and opens the relay pairing page; adding a device immediately switches the WebView.
- Settings → Connection → This phone's network shows Wi-Fi IPv4 addresses, subnet and scan ports
- empty scans explain the real cause (loopback-only DSH, firewall, guest Wi-Fi isolation) instead of a blank toast
- **Fleet overview**: one parallel `sessionList` per saved host, showing session and running counts; read-only until a host is selected.
- **Cross-host inbox**: sessions running or waiting on a question/plan review across all saved hosts, sorted with waiting work first.
- **Device health**: probe every saved device in parallel; show HTTP status/latency, offline, or a certificate that still needs first-use trust; tap a row to switch.
- Settings → 手机访问 (host plugin 0.1.5) shows the real bind address, LAN candidates and relay health
- relay URLs can be added from the scan; the first self-signed certificate is trusted per host:port only after an explicit prompt
- The relay floating bottom-right UI link is disabled (`uiLink: false`); relay status lives in Settings → 手机访问 instead.
- WebView shell with drawer gestures, custom back handling, in-app document preview
- workspace file panel: download files/folders, zip folders, recursive delete, upload, rename and new folder (`host plugin 0.1.5`)
- Android notification channels and permission flow
- approval notifications carry inline **Approve** / **Reject** actions; the answer resolves the pending Remote Event directly while the app process owns the live connection
- approvals are keyed per request, so two waiting tool calls in one session cannot replace each other
- approval notification body shows tool name, reason and session title; question notifications carry an **Open** action
- notification toggles for task completion, approval/question and goal events
- notification session deep links (`uiWorkspace.openSession`)
- optional self-hosted ntfy subscriber (`Settings → Notifications`)
- optional UnifiedPush registration with a user-installed distributor and copyable endpoint
- live connection notification with device name, connecting state and disconnect action
- native **Inbox**: current-host sessions sorted by waiting approval/question, running, then recent; tap opens the session in the WebView
- Inbox entry on the settings home shows the live count of waiting requests without opening a second connection
- **Session cockpit** from an inbox row's Details button: model, input/output/cache tokens, decode speed, mean TTFT, context pressure, turns/steps
- Cockpit actions: open in WebView, stop the current turn (with confirmation), fork the session, refresh details
- relay 0.2.1's optional browser settings card is disabled on DSH 0.2.1 because its client asks for a `settingsScope` service this DSH core does not expose; the relay host and `/relay/health` are unaffected
- compact settings home with second-level Connection/Notifications/Background pages
- first-run welcome screen with the official app icon
- official app icon in the About card
- silent startup update check, manual failure dialog and release-page fallback
- optional host-side ntfy publisher (`notify-plugin/`)
- in-app GitHub update check and APK download through direct/GitHub mirror fallbacks
- battery-optimization and vendor autostart shortcuts
- first-run settings, no built-in personal endpoint

## Known boundaries

- The app does not operate a relay, push service or hosted cloud. A user who wants
  notifications after Android kills the app must run ntfy/UnifiedPush themselves.
- A loopback gateway that would unify LAN/HTTPS/VPN transports is not part of this release;
  the app uses connection profiles, direct URLs and URL-level failover.
- Inline notification actions require the app process to still hold the live connection. If
  Android killed the process, the notification opens the app and the approval is answered there.
- The native inbox currently covers the active host. Multi-host aggregation is the next slice.
- Cockpit metrics come from the session's projections. A cold session can show `—` until its
  follow snapshot arrives; use Refresh details.
- Real-device QA is required for notification delivery, OEM background policies, LAN discovery
  and the Android installer flow. The release has passed compile/lint/unit-test and public APK
  checks, not a physical-device lab run.
- The APK is signed with the project’s current test key. Replace it before public distribution.
