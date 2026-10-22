# AGENTS.md

Instructions for AI agents working in this repository.

## What this repo is

**DeepSeek Harness APP** — a self-hosted Android client for DeepSeek Harness (DSH):
a Kotlin/WebView app (`app/`, `core/`) plus a DSH host plugin (`combined-plugin/`), an installer
(`setup/`), a self-hosted ntfy publisher (`notify-plugin/`) and a vendored responsive base
(`vendor/`). No cloud, no accounts, no telemetry — do not add any.

## If your task is "install this"

Read **[docs/INSTALL_FOR_AGENTS.md](docs/INSTALL_FOR_AGENTS.md)** and follow it top to bottom.
It contains the exact versions, hashes, commands, verification steps and troubleshooting table for
both halves (host plugin + APK). Do not improvise an install from the source tree.

Current release (2026-10-22):

- APK **1.1.3**, `versionCode` **50103**, `DeepSeek-Harness-1.1.3-personal.10.apk`
- Host plugin **0.1.17**, `dsh-android-deepseek-harness-app-plugin-0.1.17.tgz`
- DSH tested against `0.2.1`

1.1.3 is about the connection and the composer's last two readings: a failed generation is no longer reported until it has lasted, a run of failures stops retrying instead of looping forever, HTTP 429 pauses reconnecting for a minute instead of hammering the limiter, the model trigger shows the full name plus its reasoning effort, and the model label is one line with ellipsis only as a fallback. 1.1.2 fixed the settings sheet; 1.1.1 was the stability pass: the host plugin had been pinned to 0.1.7 (whose client still ran a
two-second stats poll), `mobile-web-plugin/` and `host-plugin/` are deleted, and the shell no longer
dispatches a synthetic `resize` or repaints its theme without a diff. If "it refreshes by itself"
is reported again, read the page-health block in Settings → Diagnostics first: it counts
`composerRemount`, the shell's own layout writes, and self-reloads.

## What 1.1.0 changed

The native Compose tree is **gone**. `app/src/main/java/com/labteto/dshmobile/ui/` held ~25 k lines
that no entry point reached — the manifest declares exactly one activity, `WebShellActivity`, which
is an `AppCompatActivity` around a `WebView`. R8 had already stripped it from the dex, but the ML Kit
Chinese OCR it (alone) pulled in shipped as 9.4 MB of native library: 76 % of the APK. Deleting the
tree took the download from 12.3 MB to ~3 MB and removed the Compose compiler, the Compose BOM,
navigation-compose, hilt-navigation-compose and ML Kit from the build. If you need the old UI it
lives in git history at tag `v1.0.5`.

Everything that used to live in that package and was still reachable moved: `ConnectFailure` to
`connection/`, `AppLaunchAction` to `webshell/`. Do not recreate a `ui/` package.

## Hard rules

1. **`versionCode` must strictly increase.** `1.0.5` computes to `50005` (`10005 + 40000`). Build
   with `DSH_VERSION_NAME` only; never pass a lower `DSH_VERSION_CODE`, or Android rejects the
   install with `INSTALL_FAILED_VERSION_DOWNGRADE` (this happened in 0.9.35).
2. **The host plugin only loads at DSH start.** After changing `node_modules` or `package.json`,
   restart DSH before verifying anything.
3. **Adding the package is not enough.** `@dsh-android/deepseek-harness-app-plugin` must also be in
   `dsh.profile.bundles`; `dsh plugin add` does not edit that list.
4. **Host-plugin releases are pre-releases** (`--prerelease`). Otherwise GitHub's "latest release"
   points at a plugin tag and every installed app reports "up to date".
5. **No personal endpoints.** The app's first run opens Settings and asks for the user's own DSH
   address. Never commit a real host, token, cookie or relay fingerprint.
6. **File operations are deliberately destructive and unconfirmed.** The user rejected a read-only
   mode and a second delete confirmation; do not reintroduce either.
7. **The visible product is `WebShellActivity`**, and since 1.1.0 it is the *only* UI. UI changes
   that must reach users go through the WebView shell and `combined-plugin/lib/client.js`. The app
   and the plugin are two halves installed separately: after touching `combined-plugin/`, both the
   `.tgz` and the APK have to be rebuilt and the new host plugin version reflected in
   `BuildConfig.PLUGIN_VERSION` (the shell compares them and says which side is behind).

## Build and test

```bash
# plugin + installer tests
node --test combined-plugin/test/*.test.mjs setup/lib.test.mjs

# app unit tests
./gradlew :app:testDebugUnitTest

# release APK (versionCode computed from the name)
DSH_VERSION_NAME=1.1.3 ./gradlew :app:assembleRelease
```

Signing uses `DSH_KEYSTORE`, `DSH_KEYSTORE_PASSWORD`, `DSH_KEY_ALIAS`, `DSH_KEY_PASSWORD`; without
them the release is unsigned. The public APK is signed with the project's personal key
(`CN=DSH Kaogong Fork`), so upgrades require the same key.

## Layout

| Path | Contents |
| --- | --- |
| `app/src/main/java/com/labteto/dshmobile/webshell/` | the real UI: `WebShellActivity`, chrome pages, injected scripts |
| `app/src/main/java/com/labteto/dshmobile/connection/` | hosts, discovery, relay pairing, connection manager |
| `core/` | wire protocol, relay pairing, testable parsing |
| `combined-plugin/lib/client.js` | Android WebView adaptation (mobile CSS, drawer, file panel) |
| `combined-plugin/lib/mobile-access.js` | host routes `/dsh-mobile/info`, `/dsh-mobile/audit`, `/dsh-mobile/relay/pair` |
| `setup/` | one-command installer, `status`, `doctor`, reversible `uninstall` |
| `docs/` | setup, connections, file operations, notifications, release notes, agent install guide |
