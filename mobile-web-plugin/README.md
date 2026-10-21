# Mobile web plugin

Phone-only client layer for the DSH web UI:

- drawer swipe gestures;
- compact composer stats (speed + tokens, no turn/step counts);
- model trigger sizing and visibility;
- safe-area/keyboard insets;
- workspace file panel `⋯` menu (download/delete);
- file preview and responsive fixes.

This is a DSH client bundle. It requires the upstream responsive plugin package
`@dsh-android/dsh-client-ui-responsive` as a prerequisite; see the package's `package.json` and
`cordis.patch.yml`.

Build/install it as a profile bundle with the DSH plugin manager (the same way the upstream
responsive bundle is installed), then reload the web page.

The file-panel actions call the `host-plugin` route `/dsh-file-ops`; install that bundle too.
