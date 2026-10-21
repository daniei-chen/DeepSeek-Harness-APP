# Release 0.9.16

## What this release is

A self-hosted Android client for DeepSeek Harness. No project cloud, no account, no telemetry,
no monetization.

## Install

- APK: https://deepseek.kaogong.art/dsh-kaogong-a7f3c9.apk
- SHA-256: `C180BF4669D853AED7F39524201825A3B8A858C8FD37991FE3D0E3F341320552`
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
- WebView shell with drawer gestures, custom back handling, in-app document preview
- workspace file panel: download files/folders, zip folders, recursive delete (`host-plugin/`)
- Android notification channels and permission flow
- notification toggles for task completion, approval/question and goal events
- notification session deep links (`uiWorkspace.openSession`)
- optional self-hosted ntfy subscriber (`Settings → Notifications`)
- optional host-side ntfy publisher (`notify-plugin/`)
- in-app GitHub update check and APK download through direct/GitHub mirror fallbacks
- battery-optimization and vendor autostart shortcuts
- first-run settings, no built-in personal endpoint

## Known boundaries

- The app does not operate a relay, push service or hosted cloud. A user who wants
  notifications after Android kills the app must run ntfy/UnifiedPush themselves.
- A loopback gateway that would unify LAN/HTTPS/VPN transports is not part of this release;
  the app uses connection profiles, direct URLs and URL-level failover.
- Real-device QA is required for notification delivery, OEM background policies, LAN discovery
  and the Android installer flow. The release has passed compile/lint/unit-test and public APK
  checks, not a physical-device lab run.
- The APK is signed with the project’s current test key. Replace it before public distribution.
