package com.labteto.dshmobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.labteto.dshmobile.webshell.WebShellActivity
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.AppLaunchAction

/**
 * Fork: a small home-screen widget that opens the app or starts a voice prompt.
 *
 * Deliberately a RemoteViews widget rather than Glance: it adds no dependency, and the two actions
 * it offers are the same PendingIntent shapes the Quick Settings tile uses. Richer status (running
 * session counts, last reply) belongs to a later phase where a shared data layer exists for it.
 */
class DshWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    private fun buildViews(context: Context): android.widget.RemoteViews {
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_dsh)
        views.setOnClickPendingIntent(R.id.widget_root, pending(context, action = null, request = 1))
        views.setOnClickPendingIntent(
            R.id.widget_voice,
            pending(context, AppLaunchAction.ACTION_VOICE, request = 2),
        )
        views.setOnClickPendingIntent(R.id.widget_open, pending(context, action = null, request = 3))
        return views
    }

    private fun pending(context: Context, action: String?, request: Int): PendingIntent {
        val intent = Intent(context, WebShellActivity::class.java).apply {
            this.action = action ?: Intent.ACTION_MAIN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
