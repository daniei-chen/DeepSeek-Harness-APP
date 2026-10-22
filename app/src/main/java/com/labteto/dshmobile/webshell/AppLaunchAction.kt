package com.labteto.dshmobile.webshell

/**
 * Launch actions the surfaces outside the app itself can ask for.
 *
 * The home-screen widget and the Quick Settings tile both start [WebShellActivity] with one of
 * these on the intent; the activity reads it in [WebShellActivity.onNewIntent] and in its first
 * create. Only the actions a WebView shell can actually honour live here — the Compose tree that
 * used to hold the rest is gone as of 1.1.0.
 */
object AppLaunchAction {
    /** Open the native speech recognizer once the page is ready. */
    const val VOICE: String = "voice"

    /** The intent action the shortcuts/tile/widget put on the [android.content.Intent]. */
    const val ACTION_VOICE: String = "com.labteto.dshmobile.action.VOICE"
}
