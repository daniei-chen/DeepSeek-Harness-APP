# DeepSeek Harness APP

Open-source Android client for **self-hosted DeepSeek Harness (DSH)**.

A thin, self-hosted-first mobile shell: the chat UI is the real DSH web client, while the native
layer provides connection profiles, file-panel actions, notifications, permissions and the Android
pieces a browser cannot provide.

> **Status:** active development. This is an unofficial community project, not affiliated with or
> endorsed by DeepSeek. “DeepSeek Harness” and the whale logo are trademarks of their respective
> owners.

## What is inside

- **Android app** (`app/`, `core/`) — Kotlin/Compose + WebView shell, minSdk 26.
- **`host-plugin/`** — the internal `@dsh-android/dsh-file-ops-bundle` host plugin. It is a
  first-class part of this project, not an external optional package:
  - workspace file download;
  - workspace folder download as zip;
  - recursive file/folder delete;
  - path confinement through `DSH_FILE_OPS_ROOTS`;
  - the right-side workspace file panel gets a `⋯` menu with download/delete.
- **`notify-plugin/`** — self-hosted ntfy publisher for task/approval events. The app itself has
  no cloud push; this plugin lets the host publish to the operator's own ntfy endpoint.
- **`mobile-web-plugin/`** — the phone UI layer: responsive drawer, compact stats, file-panel
  actions, safe-area/keyboard handling.
- **`docs/`** — self-hosting, connections, notifications, file operations and architecture notes.

## Current

- Fresh installs open **Settings** first; no personal DSH URL is baked into the app.
- Self-hosted DSH connection profiles (add / rename / delete / switch devices).
- Native settings surface reached from the back button: device manager, LAN scan, notification
  setup/test, foreground-service toggle, battery-unrestricted shortcut, vendor autostart shortcut,
  update check, version/source.
- WebView shell with drawer gestures, custom back navigation, in-app document preview.
- Workspace file panel: preview HTML/PDF/Word/PPT, download files/folders, delete files/folders
  through the internal `host-plugin/`.
- Android notification channels and test notification; native DSH event connection for
  approval/question notifications while the app is alive.
- In-app self-hosted ntfy subscription (Settings → Notifications → Self-hosted ntfy push) for
  terminal/approval events published by `notify-plugin/`.
- Multi-device failover: when the current profile fails to load, the next saved profile is tried.
- In-app update check against GitHub Releases; APK download tries the direct asset and
  `gh-proxy.com` / `ghproxy.net` mirrors before falling back to the release page.
- Multiple connection routes: LAN, your own HTTPS address, or a tunnel you run yourself
  (Cloudflare Tunnel / frp / Tailscale). No official relay.
- No account, no telemetry, no ads, no monetization.

## Roadmap

- Full task completion/pause notifications with deep links and per-session grouping.
- **Self-hosted ntfy / UnifiedPush push** for notifications after the app is killed. This is a
  required, documented feature of the project; it is optional for each user to enable. There is
  **no official cloud push service**.
- LAN discovery and a future local gateway for transport failover.


## Build

```bash
./gradlew :app:assembleDebug
```

Release builds accept the optional environment variables documented in `app/build.gradle.kts`
(`DSH_KEYSTORE`, `DSH_KEYSTORE_PASSWORD`, `DSH_KEY_ALIAS`, `DSH_KEY_PASSWORD`). Never commit a
keystore or signing password.

## Host plugin installation

The file-ops bundle is part of this repository:

```text
host-plugin/                 # @dsh-android/dsh-file-ops-bundle
mobile-web-plugin/           # phone UI client bundle
```

See [`host-plugin/README.md`](host-plugin/README.md) for the DSH profile installation and
`DSH_FILE_OPS_ROOTS` configuration. See [`mobile-web-plugin/README.md`](mobile-web-plugin/README.md)
for the client bundle.

## Documentation

- [Self-hosting](docs/SELF_HOSTING.md)
- [Set up HTTP/HTTPS](docs/SETUP_HTTP.md)
- [Release 0.9.16](docs/RELEASE.md)
- [Connections](docs/CONNECTIONS.md)
- [Notifications and self-hosted push](docs/NOTIFICATIONS.md)
- [File operations](docs/FILE_OPERATIONS.md)
- [Architecture](docs/ARCHITECTURE.md)

## Security

Please read [`SECURITY.md`](SECURITY.md). Do not publish gate cookies, signing keys, personal
domains or workspace paths in issues or pull requests.

## License

MIT. See [`LICENSE`](LICENSE) and [`NOTICE.md`](NOTICE.md).
