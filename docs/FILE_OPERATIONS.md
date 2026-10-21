# File operations

The workspace file panel is read-only in the stock DSH `workspaceFiles` API. This project adds the
missing download/delete experience through the **internal** `host-plugin/` bundle
(`@dsh-android/dsh-file-ops-bundle`).

## Install

Install `host-plugin/` into the DSH profile and reload the host. Set:

```text
DSH_FILE_OPS_ROOTS=<path-delimiter-separated workspace roots>
```

If unset, the plugin falls back to the DSH process working directory.

## UI

With `mobile-web-plugin/` installed, every row in **Workspace files** gets a `⋯` menu:

- **Download** — files stream with `Content-Disposition`; folders are zipped.
- **Delete** — recursive delete after confirmation; the file tree refreshes.

## Security boundaries

- Requests require the DSH gate cookie.
- Paths are resolved and confined to `DSH_FILE_OPS_ROOTS`.
- Workspace roots cannot be deleted.
- Symbolic-link downloads are rejected.
- Never expose `/dsh-file-ops` without the gate/reverse-proxy auth.
