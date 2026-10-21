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

Notifications are deduplicated and grouped by session. Tapping a notification should deep-link to
the corresponding session.

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

The app must never ship a public ntfy topic, relay URL or push credential. Setup is documented as a
manual, self-hosted step.

## Limits

- App foreground/background with a live process: notifications work.
- App killed by the user or system: only a user-configured push path can wake it.
- No official FCM/cloud fallback is provided by this repository.
