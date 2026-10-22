# DeepSeek Harness APP

Open-source Android client for **self-hosted DeepSeek Harness (DSH)**.

A thin, self-hosted-first mobile shell: the chat UI is the real DSH web client, while the native
layer adds connection management, notifications, an inbox and cockpit, file operations and the
Android pieces a browser cannot provide.

> **Status:** active development. This is an unofficial community project, not affiliated with or
> endorsed by DeepSeek. “DeepSeek Harness” and the whale logo are trademarks of their respective
> owners.

> **For AI agents / 给 AI 安装：** follow **[docs/INSTALL_FOR_AGENTS.md](docs/INSTALL_FOR_AGENTS.md)**.
> It has the exact versions, SHA-256 values, copy-paste commands, verification steps and a
> troubleshooting table for **both** halves — the host plugin and the Android APK. `AGENTS.md`
> holds the repository's hard rules (versionCode, restarts, bundle registration).

## Quick install

Host (the machine running DSH):

```bash
git clone https://github.com/daniei-chen/DeepSeek-Harness-APP.git
cd DeepSeek-Harness-APP
node setup/dsh-mobile-setup.mjs install --profile desktop \
  --file-root "$HOME" --relay-port 3443 --yes
# --app-plugin defaults to ./combined-plugin and is packed automatically
# restart DSH afterwards — the plugin loads only at process start
```

Phone:

```bash
adb install -r DeepSeek-Harness-0.9.36-personal.96.apk
```

Then `设置 → 连接 → 添加设备` (or scan the relay QR at `https://<host>:3443/relay/pair`).

## Latest

- **APK 1.0.3** — Settings → 远控 shows the relay pairing QR inside the app itself, with code,
  expiry and paired-device entry; layout recovery for pages left at a stale height (auto on
  resume / keyboard close, plus 设置 → 诊断 → 修复界面排版); QR scan, mDNS + subnet LAN discovery,
  device health, fleet overview, cross-host inbox, session cockpit and actionable notifications.
- **Host plugin 0.1.11** — `/dsh-mobile/relay/pair` serves the live pairing invitation so the phone
  can draw the QR itself; file upload / rename / new folder, file-operation audit log, relay extra
  proxy paths for `/dsh-file-ops` and `/dsh-mobile`.
- **One-command host setup** — `dsh-relay` (TLS, QR/passcode pairing, device revoke), the combined
  APP plugin, optional ntfy publisher and the vendored responsive base.
- **Update check 0.9.36** — reads the release list and accepts only app-version tags, so a host
  plugin release can no longer become GitHub's "latest" and hide an app update.

Install facts (used by `docs/INSTALL_FOR_AGENTS.md`):

| | |
| --- | --- |
| APK `versionCode` | **50003** (must be greater than the installed one) |
| APK SHA-256 | `73CBF8732383632087A24AA00B5A38FD1ACEC641407E6A48828B41F04A5D92F6` |
| Plugin SHA-256 | `3A46CA179080A9AB09B87A47114786CBE7ECC9EDAE4AA74A6E173E19141693F5` |

Downloads:

- [APK v1.0.3](https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/tag/v1.0.3)
- [Host plugin 0.1.11](https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/tag/app-plugin-v0.1.11)

## What is inside

- **`app/`, `core/`** — Kotlin Android app, minSdk 26. The visible surface is the DSH web client in
  a WebView; the native layer owns connection profiles, notifications, files and Android chrome.
- **`combined-plugin/`** — the single host plugin:
  - `/dsh-file-ops`: download, zip, delete, rename, mkdir, upload, append-only audit log;
  - `/dsh-mobile/info` and `/dsh-mobile/audit`: the Settings → 手机访问 panel;
  - BrowserSkill `browser_*` tools;
  - Android WebView adaptation (drawer, safe areas, IME, image upload, back stack).
- **`setup/`** — one-command installer, `status`, `doctor` and reversible `uninstall`.
- **`notify-plugin/`** — self-hosted ntfy publisher for task/approval events.
- **`vendor/`** — the responsive base plugin repacked for DSH `0.2.1-rc.2`.
- **`docs/`** — self-hosting, connections, notifications, file operations and architecture notes.

## Current

### Connection

- Self-hosted DSH profiles: add, rename, delete and switch devices.
- **Scan QR code** adds a plain `http(s)` DSH URL or a relay pairing payload in one step.
- LAN scan reports progress and combines `_dsh._tcp` mDNS with the subnet sweep; discovers
  `dsh-relay` on `3443/3444`.
- Relay pairing with first-use certificate trust scoped to the exact `host:port`.
- Relay status is in **Settings → 手机访问**; the relay floating bottom-right link is disabled.
- Device health: HTTP status/latency, offline, or a certificate that still needs trust; tap to switch.
- Fleet overview: one parallel `sessionList` per saved host, showing session and running counts.
- Cross-host inbox: running sessions and active questions/plan reviews across saved hosts, waiting
  work first; tap to switch host and open the session.

### Mobile cockpit

- Native inbox for the current host: approvals, questions, running and recent sessions.
- Session cockpit: model, input/output/cache tokens, decode speed, mean TTFT, context pressure and
  turns/steps; open, stop, fork and refresh.
- File panel: upload, rename, new folder, download, zip and recursive delete; operation audit log.
- Android notification channels, per-session deep links and inline **Approve** / **Reject** actions
  on approval notifications while the app process holds the live connection.
- Optional self-hosted ntfy and UnifiedPush paths for notifications after Android kills the app.

### Product boundaries

- No project cloud, account, telemetry, ads or monetization.
- No project-operated relay or push service.
- Approval waterfalls are process-local and are not replayed after a disconnect; cross-host live
  approval aggregation is therefore not part of the current release.
- Multi-host real-time approvals, per-device scopes and native PTY/live-browser views are planned
  only when a real use case justifies the extra complexity.

## Build

```bash
./gradlew :app:assembleDebug
```

Release builds accept the optional environment variables documented in `app/build.gradle.kts`
(`DSH_KEYSTORE`, `DSH_KEYSTORE_PASSWORD`, `DSH_KEY_ALIAS`, `DSH_KEY_PASSWORD`). Never commit a
keystore or signing password.

## One-command host setup

```powershell
# Windows, from the repository root
powershell -ExecutionPolicy Bypass -File setup/install.ps1 `
  -Profile desktop -FileRoot "D:\AI\DeepSeek"
```

```bash
# Linux / macOS
./setup/install.sh --profile desktop --file-root "$HOME/workspaces"
```

Then restart DSH and open `https://127.0.0.1:3443/relay/pair` on the computer, or use
**Settings → 手机访问** in DSH. See [`setup/README.md`](setup/README.md) for status, doctor and
rollback.

## Roadmap

**Next, if the use case exists**

- Multi-host live approval aggregation (requires per-host event connections or a fleet fan-in).
- Per-device scopes and audit attribution (requires relay-injected device identity).
- File-manager polish: upload progress, unzip, text/image preview, share-sheet import.

**Later, deliberately**

- Native PTY terminal.
- Live browser screencast/control.
- Plugin marketplace.
- Automation recipes and scheduling.

## Documentation

- [Install for AI agents](docs/INSTALL_FOR_AGENTS.md) — versions, hashes, commands, verification
- [Agent rules](AGENTS.md) — hard constraints for anyone changing this repo
- [One-command host setup](setup/README.md)
- [Self-hosting](docs/SELF_HOSTING.md)
- [Set up HTTP/HTTPS](docs/SETUP_HTTP.md)
- [Connections](docs/CONNECTIONS.md)
- [Notifications and self-hosted push](docs/NOTIFICATIONS.md)
- [File operations](docs/FILE_OPERATIONS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Release notes](docs/RELEASE.md)

## Security

Please read [`SECURITY.md`](SECURITY.md). Do not publish gate cookies, signing keys, personal
domains or workspace paths in issues or pull requests.

## License

MIT. See [`LICENSE`](LICENSE) and [`NOTICE.md`](NOTICE.md).
