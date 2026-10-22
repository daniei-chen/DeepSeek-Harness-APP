# Install DeepSeek Harness APP — guide for AI agents

This file is written so an agent (Claude Code, Codex, Cursor, DSH itself, …) can install the
product end-to-end **without reading the source**. Follow it in order and do not skip the
verification steps: every failure mode below has a concrete symptom and a concrete fix.

DeepSeek Harness APP is an Android WebView client for a **self-hosted** DSH. There is no cloud,
no account, no telemetry. It has two halves and both are required:

| Half | What it is | Where it goes |
| --- | --- | --- |
| **Host plugin** (`combined-plugin`) | DSH host plugin + WebView adaptation + file operations + `/dsh-mobile` routes | the machine running DSH |
| **Android app** (`app/`) | WebView shell, connection profiles, notifications, file panel, pairing QR | the phone |

---

## 0. TL;DR (copy-paste)

```bash
git clone https://github.com/daniei-chen/DeepSeek-Harness-APP.git
cd DeepSeek-Harness-APP

# 1) host plugin + relay + responsive base into the desktop profile
#    (--app-plugin defaults to ./combined-plugin, which the installer packs itself)
node setup/dsh-mobile-setup.mjs install --profile desktop \
  --file-root "$HOME" --relay-port 3443 --yes

# 2) verify what is installed
node setup/dsh-mobile-setup.mjs status --json

# 3) restart DSH (required: the plugin only loads at process start)

# 4) install the APK on the phone
curl -L -O https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/download/v1.1.3/DeepSeek-Harness-1.1.3-personal.10.apk
adb install -r DeepSeek-Harness-1.1.3-personal.10.apk
```

Then open the app → `设置 → 连接 → 添加设备` → enter the DSH address (or scan the relay QR).

---

## 1. Exact versions

| Item | Value |
| --- | --- |
| App release | **v1.1.3** |
| APK file | `DeepSeek-Harness-1.1.3-personal.10.apk` |
| APK `versionCode` | **50103** — must be **greater** than the installed one |
| APK SHA-256 | `328494D9D1BE793A3F2A2516B56F3AED1EC3E2BB35EC02A0C5864EC6539A4681` |
| Host plugin | **0.1.14** |
| Plugin tarball | `dsh-android-deepseek-harness-app-plugin-0.1.14.tgz` |
| Plugin SHA-256 | `9FD6B24A883ECEE79CFE739520B619E7A52EC3DFA19187289300BEAFF3F51C57` |
| Responsive base (vendor) | `vendor/dsh-android-dsh-client-ui-responsive-0.3.4-dsh021.2.tgz` |
| DSH tested against | `0.2.1` (desktop profile; `0.2.1-rc.2` also works) |
| Android | 8.0+ (`minSdk 26`) |
| Node | 22+ (the DSH desktop runtime's own Node is fine) |

Direct downloads:

- APK: <https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/download/v1.1.3/DeepSeek-Harness-1.1.3-personal.10.apk>
- Plugin: <https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/download/app-plugin-v0.1.14/dsh-android-deepseek-harness-app-plugin-0.1.14.tgz>

Checksums:

```bash
sha256sum DeepSeek-Harness-1.1.3-personal.10.apk
# 328494D9D1BE793A3F2A2516B56F3AED1EC3E2BB35EC02A0C5864EC6539A4681
sha256sum dsh-android-deepseek-harness-app-plugin-0.1.14.tgz
# 9FD6B24A883ECEE79CFE739520B619E7A52EC3DFA19187289300BEAFF3F51C57
```

**1.1.3 self-check.** The app and the plugin are updated separately, so the shell reads the plugin
version out of the page (`window.__dshMobilePlugin.version`) and compares it with the build it shipped
against. After installing, the top of 设置 should read `主机插件 0.1.14 · 与本应用同步`. If it says
`低于 …` or `主机没有在提供手机插件`, the host half is not active: re-run step 2 and **restart DSH**.
Two features need plugin 0.1.14 or newer — sharing text into the composer, and the notification reply
field. With an older plugin the share falls back to the clipboard and the reply field is absent.

---

## 2. Host side

### 2.1 Prerequisites

- DSH installed and runnable (`dsh --version` works, or the desktop app is installed).
- The `dsh` CLI must be on `PATH`. The desktop app ships it at
  `<DSH install>/resources/runtime/cli/bin/` (`dsh.cmd` on Windows).
- A profile to install into. `desktop` is the DSH Desktop profile; `default` is the usual CLI one.
  List them with: `ls ~/.dsh/profiles`.

### 2.2 Install (recommended: one command)

```bash
node setup/dsh-mobile-setup.mjs install \
  --profile desktop \
  --file-root "$HOME" \
  --relay-port 3443 \
  --yes
```

`--app-plugin` defaults to the `combined-plugin/` directory, which the installer packs with
`npm pack` — so a fresh clone installs the current source with no extra download. To install a
**published** plugin instead (no checkout needed), pass the tarball:

```bash
curl -L -o /tmp/dsh-app-plugin.tgz \
  https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/download/app-plugin-v0.1.14/dsh-android-deepseek-harness-app-plugin-0.1.14.tgz
node setup/dsh-mobile-setup.mjs install --profile desktop \
  --app-plugin /tmp/dsh-app-plugin.tgz --file-root "$HOME" --relay-port 3443 --yes
```

What it does:

1. installs `@dsh-android/deepseek-harness-app-plugin` into the profile;
2. installs the vendored `@dsh-android/dsh-client-ui-responsive` base (required by the plugin's
   `cordis.patch.yml`);
3. installs and configures `dsh-relay` (TLS listener, QR/passcode pairing, device revoke);
4. adds the bundles to `dsh.profile.bundles` in `~/.dsh/profiles/<profile>/package.json`;
5. writes a managed block into `cordis.patch.yml` with the file roots, relay port, audit log and
   `/dsh-mobile` + `/dsh-file-ops` proxy paths;
6. disables the relay's own browser settings card, which is incompatible with DSH 0.2.1.

Useful flags: `--file-root` (repeatable, default `$HOME`), `--public-hostname` (repeatable),
`--no-relay`, `--no-notify`, `--no-base`, `--dry-run`, `--json`, `--backup <dir>`.

OS notes:

- **Windows (PowerShell):** the command works as-is (`$HOME` resolves). If `dsh` is not on `PATH`,
  add `<DSH install>\resources\runtime\cli\bin` to `$env:PATH`, or use the wrapper:
  `powershell -ExecutionPolicy Bypass -File setup/install.ps1 -Profile desktop -FileRoot "$HOME"`.
- **Linux / macOS:** `./setup/install.sh --profile desktop --file-root "$HOME"`.
- The profile must have been started at least once; launch DSH before installing into it.

### 2.3 Install (manual, minimal)

```bash
# from the repository root; pnpm accepts the plugin directory directly
dsh plugin --profile desktop add \
  "file:$PWD/combined-plugin" \
  "file:$PWD/vendor/dsh-android-dsh-client-ui-responsive-0.3.4-dsh021.2.tgz"
```

Then add the bundle name to the profile's `dsh.profile.bundles` (the plugin **does not load**
without this):

```jsonc
// ~/.dsh/profiles/desktop/package.json
"dsh": { "profile": { "bundles": [
  // …existing…
  "@dsh-android/deepseek-harness-app-plugin"
] } }
```

`dsh plugin add` only edits dependencies; it does not edit `bundles`. Re-run
`node setup/dsh-mobile-setup.mjs install …` if you want the managed block as well.

> **Windows note:** `dsh plugin` forwards arguments through several shells, and a path containing
> spaces breaks. Copy the tarball to a space-free directory (e.g. `C:\Temp\`) before adding it.

### 2.4 Restart DSH (required)

The plugin is imported when the DSH process starts. Replacing files under `node_modules` is not
enough — **quit and reopen the DSH desktop app** (or restart the `dsh` host process).

### 2.5 Verify the host

```bash
node setup/dsh-mobile-setup.mjs status --json
```

Expect:

```json
{ "installed": { "app": true, "relay": true, "base": true }, "relayPortOpen": true }
```

Check the route exists in the installed copy:

```bash
grep -c '"/dsh-mobile/relay/pair"' \
  ~/.dsh/profiles/desktop/node_modules/@dsh-android/deepseek-harness-app-plugin/lib/mobile-access.js
# 1
```

Check the relay is listening (self-signed certificate is expected):

```bash
curl -k https://127.0.0.1:3443/relay/health
# {"service":"dsh-relay","ok":true}
```

Ask the relay for a live pairing invitation (the same path the phone's **远控** page uses):

```bash
node setup/dsh-mobile-setup.mjs pair --json
# {"ok":true,"payload":{"url":"https://192.168.2.13:3443","code":"12345678",...}}
```

---

## 3. Phone side

### 3.1 Install the APK

```bash
curl -L -O https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/download/v1.1.3/DeepSeek-Harness-1.1.3-personal.10.apk
```

With a device connected over ADB:

```bash
adb install -r DeepSeek-Harness-1.1.3-personal.10.apk
adb shell dumpsys package com.labteto.dshmobile.kaogong | grep -E "versionCode|versionName"
# versionCode=50103 versionName=1.1.3
```

Without ADB: open the APK download link on the phone and install it. Allow "install unknown apps"
for the browser/file manager you use.

The APK is signed with the project's personal key (`CN=DSH Kaogong Fork`). Upgrades must be signed
with the same key; if a build was ever installed from a different key, uninstall once.

### 3.2 First connection

The app has **no built-in URL** — this is deliberate.

**Direct LAN** (DSH bound to `0.0.0.0`, or behind Caddy/nginx):

1. `设置 → 连接 → 管理设备 → 添加设备`
2. Enter `http://<host>:<port>/` or `https://<host>/`
3. Complete the DSH gate/login page.

**Relay (recommended when DSH binds loopback only — the desktop default):**

1. On the computer, open `https://<host-lan-ip>:3443/relay/pair` and set a relay password if asked.
   The same values are printed by `node setup/dsh-mobile-setup.mjs pair`.
2. In the app: `设置 → 连接 → 扫描二维码`, scan that page's QR.
3. Confirm the decoded address; the app opens the relay pair page, enter/confirm the code.
4. The phone is now paired; the DSH UI opens through the relay.

**No camera?** `设置 → 连接 → 配对码连接`, then type the relay address
(`https://<host-lan-ip>:3443`) and the eight-digit code. It is the same claim, without the QR.

### 3.3 The in-app pairing QR (`设置 → 远控`)

Available **after** the phone is connected once, and only with host plugin **0.1.14+**. It shows a
fresh relay invitation drawn on the phone (code, expiry, copy link/code, paired devices) so a
second device can be paired without opening the desktop page.

If it says "主机上的 APP 插件还是旧版", the host is still running an older plugin: restart DSH (2.4).

---

## 4. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `INSTALL_FAILED_VERSION_DOWNGRADE` / `安装失败(-25) 无法降级安装` | the APK's `versionCode` is lower than the installed app | use the official asset (1.1.1 = **50103**); never pass a lower `DSH_VERSION_CODE` when building |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | different signing key | uninstall the old app once, then install |
| App opens Settings, cannot connect | DSH binds `127.0.0.1` (desktop default) | use the relay (3.2) or bind DSH to the LAN address |
| `ERR_CONNECTION_REFUSED` on `https://127.0.0.1:3443/...` from the phone | `127.0.0.1` is the phone itself | use `https://<host-lan-ip>:3443/...` |
| `DSH RELAY Not found` on `/relay/pair/` | trailing slash | use exactly `/relay/pair` (the app preserves it) |
| Relay answers `403` / "Host refused" | its DNS-rebinding fence only trusts loopback, its own LAN addresses and `publicHostnames` | re-run `install` with `--public-hostname <name>`, or reach it by the LAN IP |
| Relay asks for a password on first open | no password is set yet | open `https://127.0.0.1:3443/relay/password` on the host and set one |
| `Failed to load plugins` / `dsh-relay: pending (waiting for service: settingsScope)` | relay's browser card is incompatible with DSH 0.2.1 | the setup installer disables it (`disableRelayClientCard`); re-run `install` once, then restart |
| `远控` says the host plugin is old | DSH still runs the previous plugin | restart DSH; confirm `status --json` shows `app: true` |
| Update check says "up to date" but a release exists | older client read GitHub's `/releases/latest` (ordered by creation time; plugin releases are now pre-releases) | update to **1.1.3+**, which reads the release list and keeps only app-version tags |
| Page renders in a strip / drawer offset | `dsh-bloom-theme` + a stale WebView viewport | 0.9.35+ repairs automatically; `设置 → 诊断 → 修复界面排版`; or disable Bloom on the phone |
| File panel is empty after opening | DSH's first `listDirectory` does not paint | the client refreshes once after 1.2 s; if still empty, reopen the panel |
| Notifications never arrive after the phone sleeps | Android killed the app | enable `设置 → 后台`, allow autostart/battery exemption, and run self-hosted ntfy/UnifiedPush |

---

## 5. Uninstall / rollback

```bash
node setup/dsh-mobile-setup.mjs uninstall --profile desktop --yes
```

The installer keeps timestamped backups under `~/.dsh/profiles/<profile>/.dsh-mobile/backups/`.
Uninstalling the host plugin does not affect the phone app; remove the device in the app instead.

---

## 6. Release checklist (maintainers)

1. Bump `combined-plugin/package.json` if the plugin changed; `npm pack --pack-destination dist`.
2. Build the APK **without** `DSH_VERSION_CODE` so the computed, monotonically increasing code is
   used (`1.1.3 → 10103 + 40000 = 50103`):
   ```bash
   DSH_VERSION_NAME=1.1.3 ./gradlew :app:assembleRelease
   ```
   (Windows: `$env:DSH_VERSION_NAME="1.1.3"; .\gradlew.bat :app:assembleRelease`)
3. Verify: `aapt dump badging app-release.apk | head -1` — `versionCode` must be **greater** than the
   previous release.
4. Publish plugin releases as **pre-releases** (`gh release create … --prerelease`) so they never
   become GitHub's "latest" for clients that still read `/releases/latest`.
5. Update this file's version table, `README.md` and `docs/RELEASE.md` (including the SHA-256).
