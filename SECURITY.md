# Security policy

## Never commit

- DSH gate cookies or session tokens;
- Android keystores (`*.jks`, `*.keystore`) or signing passwords;
- personal domains, IP addresses, SSH keys or workspace paths;
- APKs or build outputs containing signing material.

## Reporting

Please open a private security advisory on GitHub instead of a public issue for vulnerabilities.

## Scope

The app is a WebView shell for a self-hosted DSH instance. The security of the DSH host, its
reverse proxy, gate cookie and workspace permissions remains the operator's responsibility.

## What the app itself guarantees

- **No cloud backup.** `allowBackup="false"` since 1.1.0. Relay device tokens, host lists and
  session snapshots otherwise ride Android's automatic backup, which contradicts the no-cloud
  promise this distribution is built on. Settings move between phones through the explicit export
  (Settings → 导出设置) instead.
- **Exports carry no credentials.** The exported JSON holds devices, harnesses and switches; the
  relay token and the sign-in cookie live in `RelayCredentialStore`/`HarnessSessionStore` and are
  never part of `HostConfig`, so they cannot leak through an export.
- **The web-link handler is opt-in.** The `http`/`https` intent filter ships on a disabled
  activity-alias; the user turns it on in Settings. When on, only links whose origin matches a saved
  device open in the app — everything else is handed to the real browser.
- **File operations stay destructive on purpose.** There is no read-only mode and no second delete
  confirmation; the root directory is bounded by the host plugin's configuration and every operation
  is audited there.
