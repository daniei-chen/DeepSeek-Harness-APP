# @dsh-android/dsh-file-ops-bundle

Host-side file operations for the phone's workspace file panel. This package is **part of the
DeepSeek Harness APP project** and is required for the file-panel download/delete experience.

## What it adds

The shipped `workspaceFiles` API is read-only (`list/stat/read/readBytes/changes`). This bundle
registers exactly the missing verbs:

| Action | Endpoint | Notes |
|---|---|---|
| Download a file | `GET /dsh-file-ops?action=download&path=...` | `Content-Disposition` attachment |
| Download a folder | same | zipped with Windows `tar -a` |
| Delete file/folder | `POST /dsh-file-ops?action=delete` | recursive; JSON `{ "path": "..." }` |

Every path must be inside one of `DSH_FILE_OPS_ROOTS` (path-delimiter separated). If the variable is
empty it falls back to the DSH process working directory. Workspace roots themselves cannot be
deleted; symbolic-link downloads are rejected.

The companion `mobile-web-plugin` injects the `⋯` download/delete menu into the right-side
workspace file panel.

## Install into a DSH profile

The package is a DSH bundle (`dsh.bundle.patch` inserts the `file-ops` row).

From the profile directory:

```bash
pnpm add file:/absolute/path/to/host-plugin
```

or install the packed tarball through the DSH plugin manager. The bundle row is:

```yaml
- insert:
    - id: file-ops
      name: "@dsh-android/dsh-file-ops-bundle"
```

Then restart/reload the DSH host so the `webServer` route is registered.

Set the roots before starting DSH, for example on Windows:

```powershell
$env:DSH_FILE_OPS_ROOTS = "D:\Workspaces"
```

On Linux/macOS:

```bash
export DSH_FILE_OPS_ROOTS="$HOME/workspaces"
```

## Security

- The route is behind the same DSH gate cookie as the rest of the harness.
- Only paths under configured roots are accepted.
- Do not expose `/dsh-file-ops` without the gate/reverse-proxy authentication.
- The web client's file menu is the only intended caller.
