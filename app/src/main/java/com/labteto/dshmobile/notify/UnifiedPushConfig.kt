package com.labteto.dshmobile.notify

import android.content.Context

/** The UnifiedPush endpoint assigned by the user's distributor; never a project endpoint. */
object UnifiedPushConfig {
    private const val PREFS = "unifiedpush"
    private const val KEY_ENDPOINT = "endpoint"

    fun load(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENDPOINT, null)
            ?.takeIf { it.isNotBlank() }

    fun save(context: Context, endpoint: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENDPOINT, endpoint)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENDPOINT).apply()
    }
}
