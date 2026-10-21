# One-command host setup

`dsh-mobile-setup.mjs` installs the host-side pieces of the app into a DSH profile and writes a
reversible profile patch:

- `dsh-relay` — TLS, QR/passcode pairing, device list and per-device revoke;
- `@dsh-android/deepseek-harness-app-plugin` — file operations, BrowserSkill and the browser
  mobile UI;
- `@dsh-android/dsh-mobile-notify-bundle` — optional ntfy/UnifiedPush publisher;
- `@dsh-android/dsh-client-ui-responsive` — the responsive base row, vendored so a fresh profile
  does not need a personal `file:local/...` path.

## Requirements

- DSH `0.2.1-rc.2` and its `dsh` command on `PATH`;
- Node.js 22 or newer;
- an existing profile (`desktop` by default) that has been started at least once.

## Quick start

From the repository root:

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File setup/install.ps1 `
  -Profile desktop `
  -FileRoot "D:\AI\DeepSeek"
```

```bash
# Linux / macOS
./setup/install.sh --profile desktop --file-root "$HOME/workspaces"
```

The installer prints a backup path and the relay URLs. Restart DSH, then open **Settings →
手机访问** on the computer. It shows the real bind address (DSH Desktop is loopback-only by
default), the LAN candidates, relay health and copyable pair/LAN URLs. This is also where you can
confirm why the phone's LAN scan cannot see a Desktop host.

Open the pair page **on the computer**:

```text
https://127.0.0.1:3443/relay/pair
```

Scan the QR with the app, or type the eight-digit code. Device management is at:

```text
https://127.0.0.1:3443/relay/devices
```

## Status and diagnostics

```powershell
node setup/dsh-mobile-setup.mjs status  --profile desktop
node setup/dsh-mobile-setup.mjs doctor  --profile desktop
```

`doctor` checks the profile, package dependencies, bundle entries, managed patch, file roots and
the relay listener. It exits non-zero if a required check fails.

## Pair a device without the QR

```powershell
node setup/dsh-mobile-setup.mjs pair --profile desktop
```

Prints the relay's LAN address, a live eight-digit code, its expiry and the pair/devices URLs. Add
`--fresh` to replace an outstanding code, or `--json` to script it. On the phone use
**Settings → Connection → Pair with a code** and enter the same two values — this is the fallback
when the camera or the QR page is not available, and it is also how an agent can verify pairing
end-to-end.

## Roll back

The installer writes `package.json`, `cordis.patch.yml`, `pnpm-lock.yaml`, the workspace file and
`compatibility.json` (when present) to:

```text
<profile>/.dsh-mobile-setup/backups/<timestamp>/
```

To restore the latest backup:

```powershell
node setup/dsh-mobile-setup.mjs uninstall --profile desktop --yes
```

This restores the profile files and removes a `cordis.patch.yml` that the installer created. It
does not delete the profile directory. Run `dsh plugin --profile desktop install` afterwards if
you also want pnpm to prune the old packages from `node_modules`.

For development rollback, the repository itself is the source of truth: every step is committed on
its own branch, so `git revert` / `git reset` is enough.

## Safety notes

- The relay binds `0.0.0.0:3443` by default because phones must reach it over the LAN. Do not
  forward that port to the internet without a real certificate and `--public-hostname`.
- Self-signed TLS is intended for the app's SPKI pinning. A browser will show a warning until you
  provide a trusted certificate (`tls: files` in the relay config).
- File operations are confined to the `--file-root` values, but the route itself has no second
  cookie gate. Keep it behind the relay or your own authenticated reverse proxy.
- The installer does not install a Windows service, modify firewall rules, or write outside the
  DSH profile (apart from the temporary package cache).
- `dsh-relay`'s optional browser settings card injects a client-side `settingsScope` service that
  DSH 0.2.1 does not expose. The installer keeps the relay host plugin and removes only the
  `dsh.client` metadata from the installed relay package, so the relay still listens and the
  plugin page can finish syncing. Use **Settings → 手机访问** for relay status; the relay's own
  card is not required.
- A hand-made minimal profile can still fail the whole web boot with `Failed to load plugins` /
  `dsh-relay: pending (waiting for service: settingsScope)`; use `--no-relay` there instead.
