# Notifications

## Default: local notifications while the app is alive

The app can keep a foreground service and a WebSocket to the DSH host. When a session changes to a
state that needs attention, the native layer posts an Android notification.

Events:

- turn completed;
- turn failed;
- waiting for approval;
- waiting for a user answer/question;
- background job completed/failed;
- connection lost (optional).

Notifications are deduplicated and grouped by session. Tapping a notification deep-links to the
corresponding session.

Approval notifications carry two inline actions, **Approve** and **Reject**. They resolve the
pending Remote Event waterfall directly, so the user does not have to open the app for the common
case. The action works while the app process still owns the live connection; if Android killed the
process, the tap opens the app instead of claiming a success it cannot prove.

Completion, goal and question notifications additionally carry a **reply field** (`RemoteInput`).
Text typed there is sent with `mode = "queue"` to the session the notification names, because a reply
written in the shade always follows the turn it answers — "send this next" is the only honest intent.
Approvals deliberately get no reply field: they are waiting on a decision between two buttons, and a
text box would invite an answer the harness cannot accept. Same boundary as approve/reject otherwise:
with the process gone there is no live API client to send through, so `ReplyReceiver` reports failure
rather than a reply that never left the phone.

The reply `PendingIntent` is explicit and **mutable** on Android 12+; the system writes the typed text
into the broadcast as extras, and an immutable one silently drops it.

Android requires:

- `POST_NOTIFICATIONS` runtime permission (Android 13+);
- a notification channel for each category;
- a foreground-service notification if the app keeps the socket alive in the background.

## Required feature: optional self-hosted ntfy / UnifiedPush

**This is a required feature of the project and must remain documented.** It is optional for the
user to enable, but the implementation and documentation are part of the product.

Without a cloud service, Android cannot wake a killed app. For “notify me even after the app is
swiped away”, the user must run their own push path:

- **ntfy**: the host plugin publishes events to a user-owned ntfy server/topic; the user's ntfy
  Android app (or a UnifiedPush distributor) receives them.
- **UnifiedPush**: the host publishes to the user's distributor endpoint; the app registers as a
  UnifiedPush distributor client.

The repository includes `notify-plugin/` for the host-side ntfy publisher. Configure it with
`DSH_NTFY_URL`, `DSH_NTFY_TOPIC` and optionally `DSH_NTFY_TOKEN`; see that package's README.

The Android app can subscribe to the same topic directly: **Settings → Notifications → Self-hosted
ntfy push**, enter `https://your-ntfy/topic`. It runs an opt-in foreground service and posts the
received events as local notifications. Leaving the field empty disables the subscription.

For a real UnifiedPush distributor (ntfy can act as one), install the distributor, tap
**Settings → Notifications → UnifiedPush**, then copy the **UnifiedPush endpoint** row. Set it on
the host as `DSH_UNIFIEDPUSH_ENDPOINT`; `notify-plugin/` posts the same events there.

The app must never ship a public ntfy topic, relay URL or push credential. Setup is documented as a
manual, self-hosted step.

## Limits

- App foreground/background with a live process: notifications work.
- App killed by the user or system: only a user-configured push path can wake it.
- No official FCM/cloud fallback is provided by this repository.
