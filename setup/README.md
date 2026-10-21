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

The installer prints a backup path and the relay URLs. Restart DSH, then open the pair page **on
the computer**:

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
