# Architecture

```text
Android app
├─ Compose settings/onboarding (future product surface)
├─ WebView shell (chat = real DSH web UI)
│  └─ injected client bundle (drawer, stats, file menu, insets)
├─ connection profiles
├─ foreground notification service (future)
└─ host bridge (future: events, pairing, push)

DSH host
├─ dsh-file-ops bundle (required for file panel actions)
└─ optional notification publisher (ntfy/UnifiedPush)
```

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
