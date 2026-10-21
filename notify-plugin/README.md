# @dsh-android/dsh-mobile-notify-bundle

Self-hosted ntfy publisher for DSH terminal and action events. This is the project's **required,
optional-to-enable** push path for notifications after Android kills the app.

## Configure the host

```bash
export DSH_NTFY_URL="https://ntfy.example.com"
export DSH_NTFY_TOPIC="dsh-alerts"
export DSH_NTFY_TOKEN="optional-token"
```

For UnifiedPush, install a distributor in the app, register under
**Settings → Notifications → UnifiedPush**, copy the endpoint and set:

```bash
export DSH_UNIFIEDPUSH_ENDPOINT="https://distributor.example/endpoint/..."
```

Then install this bundle into the DSH profile and reload the host. DSH posts:

- `turn/end`, `turn/error`, `turn/failed`
- approval and question requests
- goal changes
- agent errors

to the configured ntfy topic.

## Phone side

- Install the ntfy Android app (or a UnifiedPush distributor that bridges ntfy) and subscribe to the
  same topic; or
- keep the app's foreground service alive for local notifications.

No public topic, relay or credential is shipped by this repository.
