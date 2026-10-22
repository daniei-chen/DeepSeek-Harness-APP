# Self-hosting

This project connects to a DeepSeek Harness (DSH) host that **you** run. It does not provide a
hosted DSH, relay, account system or push service.

## 1. Run DSH

Install DeepSeek Harness on a machine you control and open its web UI locally. Confirm that the
web client works in a browser before involving the phone.

## 2. Put it on HTTPS

Recommended options:

- your own domain + nginx/Caddy reverse proxy;
- Cloudflare Tunnel;
- frp or another self-hosted tunnel;
- Tailscale/WireGuard, if you only need private access.

The Android app expects an `https://` URL. Keep the DSH gate cookie/login enabled; do not expose
the host directly to the public internet without authentication.

## 3. Build/install the Android app

```bash
./gradlew :app:assembleDebug
```

Or download a signed APK from the repository releases.

## 4. Add the host plugin

There is exactly one host plugin: `combined-plugin/`. It carries the phone adaptation, the
workspace file verbs and the `/dsh-mobile` routes, and the installer packs and installs it:

```bash
node setup/dsh-mobile-setup.mjs install --profile desktop --file-root "$HOME" --yes
```

Set `DSH_FILE_OPS_ROOTS` to the folders the file panel may touch. Then **restart DSH** — the plugin
only loads at process start.

The two older packages that used to live here (`host-plugin/`, `mobile-web-plugin/`) were removed in
1.1.1: they had been folded into `combined-plugin/`, and the phone adaptation they carried still ran
three periodic timers that made the web UI repaint on its own. Do not install them.

## 5. Pair the phone

In the app: **Settings → Connection → Add device → Enter URL** (or scan a QR code if your build
provides pairing). Enter your DSH URL and complete the gate login once.

No cloud service is contacted by this repository.
