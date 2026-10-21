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

- APK **0.11.1**, `versionCode` **32101**, `DeepSeek-Harness-0.11.1-personal.111.apk`
- Host plugin **0.1.10**, `dsh-android-deepseek-harness-app-plugin-0.1.10.tgz`
- DSH tested against `0.2.1`

## Hard rules

1. **`versionCode` must strictly increase.** `0.11.1` computes to `32101` (`1101 + 31000`). Build
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
7. **The visible product is `WebShellActivity`**, not the native Compose `AppRoot`. UI changes that
   must reach users go through the WebView shell and `combined-plugin/lib/client.js`.

## Build and test

```bash
# plugin + installer tests
node --test combined-plugin/test/*.test.mjs setup/lib.test.mjs

# app unit tests
./gradlew :app:testDebugUnitTest

# release APK (versionCode computed from the name)
DSH_VERSION_NAME=0.11.1 ./gradlew :app:assembleRelease
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
