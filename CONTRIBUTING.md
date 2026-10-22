# Contributing

This is an unofficial, self-hosted-first Android client for DeepSeek Harness.

## Pull requests

- Keep the WebView shell thin; native code is acceptable for permissions, notifications,
  connection management and file hand-off.
- Do not add cloud services, telemetry, accounts or monetization.
- Do not commit secrets, personal domains, gate cookies, signing keys or workspace paths.
- For UI changes, include a phone-width screenshot and note the WebView/Android version tested.
- Run `./gradlew :app:assembleDebug` before opening a PR.

## Reporting issues

Include:

- app version and Android/WebView version;
- connection route (LAN / own HTTPS / tunnel);
- the host plugin version the app reports in Settings (`主机插件 …`), and whether DSH was restarted after installing it;
- a screenshot or log with secrets removed.

Security issues: see [`SECURITY.md`](SECURITY.md).
