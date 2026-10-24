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

Current release (2026-10-24):

- APK **1.1.18**, `versionCode` **50118**, `DeepSeek-Harness-1.1.18-personal.25.apk`
- Host plugin **0.1.39**, `dsh-android-deepseek-harness-app-plugin-0.1.39.tgz`
- DSH tested against `0.2.1`

0.1.39 makes the phone ignore the desktop's page zoom, and stops the keyboard stretching the page.
`dsh-claude-style`'s 页面缩放 preference is an inline `body { zoom: 1.25 }`, and this sheet is laid out
in fixed pixels: at 125 % the 400 px viewport became a 320 px layout, the safe-area paddings (device
pixels from the shell) were reserved 25 % too large, and the fullscreen right panel — upstream sizes it
`100vw` — rendered 500 px wide on a 400 px screen, hung 100 px off the left edge and clipped every
card's left margin. The phone form now pins the body to `zoom: 1` and the theme's page box to
`100vh`/`100vw`, gated on the shell's own UA marker (`data-dsh-android-shell`), so a phone and a tablet
in the app render the same sheet whatever the desktop is set to. The keyboard: the shell runs
`adjustResize`, so focusing the composer shrinks the WebView (890 → 590 px at 400×890); the theme
writes that innerHeight into its frame as a *length*, and with a zoom left on the body 590 px of frame
rendered 738 px on a 590 px viewport — the document grew 148 px on focus, the page scrolled under the
status bar and stayed scrolled after typing (the user's 页面拉长 / 变回去 / 顶部有空). With the zoom
pinned and the frame tied to `calc(100vh / var(--dsh-phone-zoom, 1))`, `scrollHeight == innerHeight`
while the keyboard is open (738 → 590). Measured at 400×890 in the app: header 40…80 px on the home
page and 40…116 on a conversation, composer card 772…846, right panel 0…400, no page scroll anywhere.
Every earlier layout experiment (pinned header rows 0.1.31/0.1.34, safe-area division and panel clamp
0.1.33) is gone — the theme's own geometry is back.

1.1.17 fixes the theme's vertical rhythm on the phone. `dsh-claude-style` reserves a 65 px title row
for the 对话/轨迹 strip the phone hides, so a 34 px row of icons floated in the middle of a 76 px band
(37 px of empty header above the conversation), and its composer card sat 8 px off the bottom edge, on
the gesture pill. The phone form pins the rows (`grid-template-rows: auto 0px`, `height: auto`, 2 px
padding) and gives the composer stack a 12 px lift. Measured at 393×852 with the phone's insets
(40 px status bar, 24 px navigation): header 76 → 39 px, composer card bottom 820 → 808 of 852.
1.1.16 adapts the phone to **dsh-claude-style**, the Claude Code desktop theme (0.18.5). The theme lays
the conversation header out for a desktop window: at 393 px the background-job chip ran from x=254 to
x=432, straight across the "…" menu (298) and the panel toggle (343), the subagent chip took the rest,
and the session title was squeezed to 11 px. The phone form now hides the 对话/轨迹 view switch (the
user asked for it), hides the subagent count chip (the team button stays), collapses the job chip to its
spinner, and gives the title the row's free width with an ellipsis. The theme also draws its own model
control and hides the core trigger; the plugin's `::after` model-name rewrite (1.1.3) then stamped a
second name on the same spot — "DeepSeek-V4.1-Flash" printed through "DeepSeek-V4.1-Flash". That
rewrite is cancelled on the theme's button. And the theme's sidebar never closed the phone drawer, so
picking a session left the drawer over the conversation it had just opened: a capture-phase click
watcher closes the drawer 90 ms after a `session-row` / `new-session` / `panel-row` tap.
1.1.14 packs the composer's stats dock and names slow sub-resources. The dock lays its children out
as equal grid columns in some builds, so `flex-start` alone left the three readings a thumb apart; it
is forced to flex with `width: auto` children now. Page health also gains `slowRes=`, the three
slowest images/scripts/stylesheets above 300 ms — `onPageFinished` waits for those, so a launch whose
DOM was ready at 0.6 s but finished at 5.9 s is one slow resource, not a slow page.
1.1.13 fixes how diagnostics samples. The page-health block used to be taken once, 900 ms after a
document loaded, so a report opened after real use described the page as it was at start-up — the one
moment it is least useful. It now samples on open (one `evaluateJavascript`) and then renders. The
update download progress moved off the system `AlertDialog` onto the shell's own card, because a dark
Material panel next to rounded light cards read as a different app.
1.1.12 names the slow calls. The fetch hook around the page now records each call's duration, and
`__dshShellNetwork` reports the three slowest above 150 ms (`| slow=api/…/messages 1240ms!`, `!` for
a failure). Session switching is either a slow fetch or slow rendering, and until this line existed
there was no way to tell which — a count of calls says nothing about a single slow one.
1.1.11 measures start-up instead of guessing at it. Diagnostics gains `first=` (native navigation →
first committed frame), `ready=` (→ `onPageFinished`) and the page's own `ttfb` / `fcp` / `dcl`
from `PerformanceNavigationTiming`. Read that line before touching the launch path: it separates a
slow request from slow script, which is the difference between prefetching more and rendering less.
A "skeleton first, then replace it" design was considered and rejected — in a WebView SPA it is two
renders, it tears down the page, and replacing the tree interrupts React's own mount (the source of
the "it refreshes by itself" reports). Warm start already prefetches the main document with OkHttp.
1.1.10 makes transfers honest. Uploads go through `XMLHttpRequest` so the page can show a percentage
and a failure carries the HTTP status (fetch reports neither); the file panel's 下载 hands the real
http(s) URL to Android's download manager instead of building a `blob:` URL, which a WebView silently
drops — on the phone that button did nothing at all. Page health gains `scrollMin=N@k`, sampled with
`requestAnimationFrame` only while a scroll is in flight (no timer idles between scrolls). The error
page lists the last three sessions from the on-disk snapshot instead of hiding them behind a button,
and the dead settings page plus the two bridge methods nobody calls are gone.
1.1.9 makes the paperclip a one-tap control: the client's own two-row menu (上传附件 / 上传图片) is
clicked through by text as soon as it mounts, and 上传附件 wins because a file picker can still reach
photos while an image picker cannot reach documents. The old hook keyed on the trigger's class and on
`button`/`menuitem` rows; both broke when the menu was re-rendered, so it now watches only the mount.
1.1.8 makes the shell recover from the one failure the page will not fix by itself: when the client
paints 「连接异常，刷新重试」and leaves it there, the plugin taps it after 2.6 s — never with a draft in
the composer, never in the background, at most 3 times per 2 minutes, counted in sessionStorage so a
reload cannot reset it; the attempt count shows up as `autoRetry=` in the page-health line.
1.1.7 collapses the device entry points. Settings → 连接 opens the device manager itself (list,
switch, rename, delete, add) and the old connection page moved to 「更多连接设置」 at the bottom of
that dialog; the header device pill added in 1.1.6 is gone, so devices live in exactly one place.
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
DSH_VERSION_NAME=1.1.18 ./gradlew :app:assembleRelease
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
