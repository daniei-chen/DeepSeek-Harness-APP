# File operations

The workspace file panel is read-only in the stock DSH `workspaceFiles` API. This project adds the
missing file verbs through the combined APP plugin's `/dsh-file-ops` route
(`@dsh-android/deepseek-harness-app-plugin`). The legacy `host-plugin/` bundle covers the original
download/delete pair only.

## Install

Install the combined plugin into the DSH profile and reload the host. Set:

```text
DSH_FILE_OPS_ROOTS=<path-delimiter-separated workspace roots>
```

If unset, the plugin falls back to the DSH process working directory.

## UI

With the combined plugin installed, the **Workspace files** header gets **Upload** and **New
folder**, and every row gets a `⋯` menu:

- **Download** — files stream with `Content-Disposition`; folders are zipped.
- **Rename** — one level, same parent, name is a single path segment.
- **Delete** — recursive delete, no confirmation; the row is removed immediately and the tree refreshes.
- **Upload** — multi-file picker from the phone, streamed as `application/octet-stream`; existing names are refused (`wx`).
- **New folder** — one child under the current directory.

## Audit log

Set `auditLog` in the plugin config (and `DSH_MOBILE_AUDIT_LOG` when running the host manually) to
an append-only JSONL path outside the workspace roots, for example
`<profile>/.dsh-mobile/audit.jsonl`. Each successful file action records time, action, path, result,
remote address and a short user-agent. **Settings → 手机访问 → 最近文件操作** shows the newest
entries. An audit-write failure never fails the file action.

## Security boundaries

- The route has no second cookie gate; keep it behind the DSH front gate, relay, or your own
  authenticated reverse proxy.
- Paths are resolved and confined to `DSH_FILE_OPS_ROOTS`.
- Workspace roots cannot be deleted or renamed.
- Names are single path segments; `..` and path separators are rejected.
- Symbolic-link downloads are rejected.
- Uploads refuse to overwrite an existing file.
- Never expose `/dsh-file-ops` without the gate/reverse-proxy auth.
