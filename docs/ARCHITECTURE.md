# Architecture

```text
Android app  (one activity, one WebView — the native Compose tree was removed in 1.1.0)
├─ WebView shell (chat = real DSH web UI)
│  └─ injected client bundle (drawer, stats, file menu, insets, share insertion)
├─ connection profiles + native event connection (notifications, approvals, replies)
├─ foreground notification service (ntfy / UnifiedPush bridges)
├─ connection doctor + page-event timeline (diagnostics)
└─ settings transfer (JSON in/out, no credentials)

DSH host
├─ dsh-file-ops bundle (required for file panel actions)
├─ dsh-mobile routes (/dsh-mobile/info, /dsh-mobile/audit, /dsh-mobile/relay/pair)
└─ optional notification publisher (ntfy/UnifiedPush)
```

The app and the host plugin are updated separately, so the shell compares the plugin version the
page advertises (`window.__dshMobilePlugin.version`) with `BuildConfig.PLUGIN_VERSION` and reports
which half is stale.

## Principles

- **Self-hosted first:** no account, no official relay, no telemetry.
- **The web UI stays the source of truth** for chat and sessions.
- **Native only where it matters:** permissions, notifications, file hand-off, connection profiles.
- **No secrets in the repo:** gate cookies, signing keys and personal domains stay local.

## Future local gateway

A loopback HTTP/WebSocket gateway inside the app can make the WebView origin stable while the
native layer switches between LAN, HTTPS, tunnel and VPN transports. This is the clean path to
multi-connection failover and offline/error pages, but it is a larger project than the first
open-source release.
