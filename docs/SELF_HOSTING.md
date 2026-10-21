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

Install `host-plugin/` as described in its README. This is required for workspace file
download/delete. Set `DSH_FILE_OPS_ROOTS` to the folders the file panel may touch.

Install `mobile-web-plugin/` for the phone UI and the file-panel `⋯` menu.

## 5. Pair the phone

In the app: **Settings → Connection → Add device → Enter URL** (or scan a QR code if your build
provides pairing). Enter your DSH URL and complete the gate login once.

No cloud service is contacted by this repository.
