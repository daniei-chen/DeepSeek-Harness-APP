package com.labteto.dshmobile.webshell

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.labteto.dshmobile.R
import kotlin.math.roundToInt

/** One row in the native inbox. */
internal data class ShellInboxRow(
    val sessionId: String,
    val title: String,
    val subtitle: String,
    val action: Boolean,
    val running: Boolean,
)

/**
 * The shell's own chrome: the device manager and its small text-input dialogs.
 *
 * Drawn by hand instead of `AlertDialog`/`BottomSheet` so it follows the harness web client's visual
 * language: a centred rounded card over a dim scrim, 15sp rows, quiet secondary text and one blue
 * accent. It also follows the page's dark/light fact.
 */
internal class WebShellChrome(private val activity: AppCompatActivity) {

    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).roundToInt()

    // ---- settings -----------------------------------------------------------

    fun showSettings(
        dark: Boolean,
        currentDeviceName: String?,
        versionName: String,
        notificationsEnabled: Boolean,
        turnNotifications: Boolean,
        actionNotifications: Boolean,
        goalNotifications: Boolean,
        backgroundEnabled: Boolean,
        ntfyEnabled: Boolean,
        onManageDevices: () -> Unit,
        onScanLan: () -> Unit,
        onTestConnection: () -> Unit,
        onNotificationSettings: () -> Unit,
        onTestNotification: () -> Unit,
        onToggleTurnNotifications: () -> Unit,
        onToggleActionNotifications: () -> Unit,
        onToggleGoalNotifications: () -> Unit,
        onConfigureNtfy: () -> Unit,
        onBackgroundToggle: () -> Unit,
        onBatterySettings: () -> Unit,
        onAutostart: () -> Unit,
        onCheckUpdate: () -> Unit,
        onSource: () -> Unit,
        onRefresh: () -> Unit,
        onExit: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(8), dp(2))
        }
        val titleColumn = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleColumn.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_title)
                textSize = 18f
                setTextColor(colors.primary)
            },
        )
        titleColumn.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_subtitle)
                textSize = 12f
                setPadding(0, dp(3), 0, 0)
                setTextColor(colors.secondary)
            },
        )
        header.addView(titleColumn)
        header.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.device_close)
                textSize = 13f
                setTextColor(colors.secondary)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                isClickable = true
                setOnClickListener { dialog.dismiss() }
            },
        )
        card.addView(header, matchWidth())

        card.addView(sectionLabel(colors, activity.getString(R.string.shell_settings_section_connection)))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_device,
                title = activity.getString(R.string.shell_settings_manage_devices),
                subtitle = currentDeviceName?.let { activity.getString(R.string.shell_settings_current_device, it) }
                    ?: activity.getString(R.string.shell_settings_manage_devices_hint),
            ) {
                dialog.dismiss()
                onManageDevices()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_settings,
                title = activity.getString(R.string.shell_settings_scan_lan),
                subtitle = activity.getString(R.string.shell_settings_scan_lan_hint),
            ) {
                dialog.dismiss()
                onScanLan()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_refresh,
                title = activity.getString(R.string.shell_settings_test_connection),
                subtitle = activity.getString(R.string.shell_settings_test_connection_hint),
            ) {
                dialog.dismiss()
                onTestConnection()
            },
        )

        card.addView(sectionLabel(colors, activity.getString(R.string.shell_settings_section_notifications)))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_notifications,
                title = activity.getString(R.string.shell_settings_notification_settings),
                subtitle = if (notificationsEnabled) {
                    activity.getString(R.string.shell_settings_notification_enabled)
                } else {
                    activity.getString(R.string.shell_settings_notification_disabled)
                },
            ) {
                dialog.dismiss()
                onNotificationSettings()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_check,
                title = activity.getString(R.string.shell_settings_test_notification),
                subtitle = activity.getString(R.string.shell_settings_test_notification_hint),
            ) {
                dialog.dismiss()
                onTestNotification()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_check,
                title = activity.getString(R.string.shell_settings_notify_turn),
                subtitle = enabledLabel(turnNotifications),
            ) {
                dialog.dismiss()
                onToggleTurnNotifications()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_notifications,
                title = activity.getString(R.string.shell_settings_notify_action),
                subtitle = enabledLabel(actionNotifications),
            ) {
                dialog.dismiss()
                onToggleActionNotifications()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_check,
                title = activity.getString(R.string.shell_settings_notify_goal),
                subtitle = enabledLabel(goalNotifications),
            ) {
                dialog.dismiss()
                onToggleGoalNotifications()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_notifications,
                title = activity.getString(R.string.shell_settings_ntfy),
                subtitle = if (ntfyEnabled) {
                    activity.getString(R.string.shell_settings_notification_enabled)
                } else {
                    activity.getString(R.string.shell_settings_ntfy_hint)
                },
            ) {
                dialog.dismiss()
                onConfigureNtfy()
            },
        )

        card.addView(sectionLabel(colors, activity.getString(R.string.shell_settings_section_background)))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_settings,
                title = activity.getString(R.string.shell_settings_keep_background),
                subtitle = if (backgroundEnabled) {
                    activity.getString(R.string.shell_settings_background_on)
                } else {
                    activity.getString(R.string.shell_settings_background_off)
                },
            ) {
                dialog.dismiss()
                onBackgroundToggle()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_battery,
                title = activity.getString(R.string.shell_settings_battery),
                subtitle = activity.getString(R.string.shell_settings_battery_hint),
            ) {
                dialog.dismiss()
                onBatterySettings()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_settings,
                title = activity.getString(R.string.shell_settings_autostart),
                subtitle = activity.getString(R.string.shell_settings_autostart_hint),
            ) {
                dialog.dismiss()
                onAutostart()
            },
        )

        card.addView(sectionLabel(colors, activity.getString(R.string.shell_settings_section_about)))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_refresh,
                title = activity.getString(R.string.shell_settings_check_update),
                subtitle = activity.getString(R.string.shell_settings_update_hint),
            ) {
                dialog.dismiss()
                onCheckUpdate()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_device,
                title = activity.getString(R.string.shell_settings_version, versionName),
                subtitle = activity.getString(R.string.shell_settings_source_hint),
            ) {
                dialog.dismiss()
                onSource()
            },
        )

        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.webshell_action_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        card.addView(
            menuRow(colors, R.drawable.ic_shell_exit, activity.getString(R.string.device_exit), destructive = true) {
                dialog.dismiss()
                onExit()
            },
        )

        root.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        show(dialog, root, gravity = Gravity.CENTER)
    }

    // ---- settings home + second-level pages --------------------------------

    fun showSettingsHome(
        dark: Boolean,
        currentDeviceName: String?,
        versionName: String,
        inboxCount: Int,
        notificationsEnabled: Boolean,
        backgroundEnabled: Boolean,
        ntfyEnabled: Boolean,
        onConnection: () -> Unit,
        onInbox: () -> Unit,
        onMobileSetup: () -> Unit,
        onDiagnostics: () -> Unit,
        onNotifications: () -> Unit,
        onBackground: () -> Unit,
        onCheckUpdate: () -> Unit,
        onSource: () -> Unit,
        onRefresh: () -> Unit,
        onExit: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        card.addView(pageHeader(colors, activity.getString(R.string.shell_settings_title)) { dialog.dismiss() })
        card.addView(aboutCard(colors, versionName, onSource))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_refresh,
                title = activity.getString(R.string.shell_settings_check_update),
                subtitle = activity.getString(R.string.shell_settings_update_hint),
            ) {
                dialog.dismiss()
                onCheckUpdate()
            },
        )
        card.addView(divider(colors))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_device,
                title = activity.getString(R.string.shell_settings_section_connection),
                subtitle = currentDeviceName?.let { activity.getString(R.string.shell_settings_current_device, it) }
                    ?: activity.getString(R.string.shell_settings_connection_hint),
            ) {
                dialog.dismiss()
                onConnection()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_notifications,
                title = activity.getString(R.string.shell_inbox_title),
                subtitle = if (inboxCount > 0) {
                    activity.getString(R.string.shell_inbox_count, inboxCount)
                } else {
                    activity.getString(R.string.shell_inbox_hint)
                },
            ) {
                dialog.dismiss()
                onInbox()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_device,
                title = activity.getString(R.string.shell_mobile_access),
                subtitle = activity.getString(R.string.shell_mobile_access_hint),
            ) {
                dialog.dismiss()
                onMobileSetup()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_settings,
                title = activity.getString(R.string.shell_diagnostics),
                subtitle = activity.getString(R.string.shell_diagnostics_hint),
            ) {
                dialog.dismiss()
                onDiagnostics()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_notifications,
                title = activity.getString(R.string.shell_settings_section_notifications),
                subtitle = if (notificationsEnabled) {
                    activity.getString(R.string.shell_settings_notification_enabled)
                } else {
                    activity.getString(R.string.shell_settings_notification_disabled)
                } + if (ntfyEnabled) " · ntfy" else "",
            ) {
                dialog.dismiss()
                onNotifications()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_settings,
                title = activity.getString(R.string.shell_settings_section_background),
                subtitle = if (backgroundEnabled) {
                    activity.getString(R.string.shell_settings_background_on)
                } else {
                    activity.getString(R.string.shell_settings_background_off)
                },
            ) {
                dialog.dismiss()
                onBackground()
            },
        )
        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.webshell_action_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        card.addView(
            menuRow(colors, R.drawable.ic_shell_exit, activity.getString(R.string.device_exit), destructive = true) {
                dialog.dismiss()
                onExit()
            },
        )
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showConnectionPage(
        dark: Boolean,
        onManageDevices: () -> Unit,
        onScanLan: () -> Unit,
        onNetworkInfo: () -> Unit,
        onTestConnection: () -> Unit,
        onBack: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        card.addView(pageHeader(colors, activity.getString(R.string.shell_settings_section_connection)) { dialog.dismiss(); onBack() })
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_settings_manage_devices), activity.getString(R.string.shell_settings_manage_devices_hint)) {
                dialog.dismiss()
                onManageDevices()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_settings, activity.getString(R.string.shell_settings_scan_lan), activity.getString(R.string.shell_settings_scan_lan_hint)) {
                dialog.dismiss()
                onScanLan()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_settings_network_info), activity.getString(R.string.shell_settings_network_info_hint)) {
                dialog.dismiss()
                onNetworkInfo()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_settings_test_connection), activity.getString(R.string.shell_settings_test_connection_hint)) {
                dialog.dismiss()
                onTestConnection()
            },
        )
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showInboxPage(
        dark: Boolean,
        rows: List<ShellInboxRow>,
        connectionLabel: String,
        onOpen: (ShellInboxRow) -> Unit,
        onRefresh: () -> Unit,
        onBack: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        card.addView(pageHeader(colors, activity.getString(R.string.shell_inbox_title)) { dialog.dismiss(); onBack() })
        card.addView(
            TextView(activity).apply {
                text = connectionLabel
                textSize = 12f
                setTextColor(colors.secondary)
                setPadding(dp(14), 0, dp(14), dp(8))
            },
        )

        if (rows.isEmpty()) {
            card.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_inbox_empty)
                    textSize = 13f
                    setTextColor(colors.secondary)
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                },
            )
        } else {
            val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            rows.forEach { row ->
                list.addView(
                    settingsRow(
                        colors = colors,
                        iconRes = when {
                            row.action -> R.drawable.ic_shell_notifications
                            row.running -> R.drawable.ic_shell_refresh
                            else -> R.drawable.ic_shell_check
                        },
                        title = row.title,
                        subtitle = row.subtitle,
                    ) {
                        dialog.dismiss()
                        onOpen(row)
                    },
                )
            }
            val height = dp((rows.size * 64 + 24).coerceIn(120, 360))
            card.addView(
                ScrollView(activity).apply {
                    isFillViewport = false
                    addView(list, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                },
            )
        }

        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_inbox_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showNotificationsPage(
        dark: Boolean,
        notificationsEnabled: Boolean,
        turnNotifications: Boolean,
        actionNotifications: Boolean,
        goalNotifications: Boolean,
        ntfyEnabled: Boolean,
        unifiedPushEndpoint: String?,
        onNotificationSettings: () -> Unit,
        onTestNotification: () -> Unit,
        onToggleTurn: () -> Unit,
        onToggleAction: () -> Unit,
        onToggleGoal: () -> Unit,
        onConfigureNtfy: () -> Unit,
        onRegisterUnifiedPush: () -> Unit,
        onCopyUnifiedPushEndpoint: () -> Unit,
        onBack: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        card.addView(pageHeader(colors, activity.getString(R.string.shell_settings_section_notifications)) { dialog.dismiss(); onBack() })
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_notifications, activity.getString(R.string.shell_settings_notification_settings), if (notificationsEnabled) activity.getString(R.string.shell_settings_notification_enabled) else activity.getString(R.string.shell_settings_notification_disabled)) {
                dialog.dismiss()
                onNotificationSettings()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_settings_test_notification), activity.getString(R.string.shell_settings_test_notification_hint)) {
                dialog.dismiss()
                onTestNotification()
            },
        )
        card.addView(settingsRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_settings_notify_turn), enabledLabel(turnNotifications)) { dialog.dismiss(); onToggleTurn() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_notifications, activity.getString(R.string.shell_settings_notify_action), enabledLabel(actionNotifications)) { dialog.dismiss(); onToggleAction() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_settings_notify_goal), enabledLabel(goalNotifications)) { dialog.dismiss(); onToggleGoal() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_notifications, activity.getString(R.string.shell_settings_ntfy), if (ntfyEnabled) enabledLabel(true) else activity.getString(R.string.shell_settings_ntfy_hint)) { dialog.dismiss(); onConfigureNtfy() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_notifications, activity.getString(R.string.shell_settings_unifiedpush), activity.getString(R.string.shell_settings_unifiedpush_hint)) { dialog.dismiss(); onRegisterUnifiedPush() })
        if (!unifiedPushEndpoint.isNullOrBlank()) {
            card.addView(settingsRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_settings_unifiedpush_endpoint), unifiedPushEndpoint) { dialog.dismiss(); onCopyUnifiedPushEndpoint() })
        }
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showBackgroundPage(
        dark: Boolean,
        backgroundEnabled: Boolean,
        onToggleBackground: () -> Unit,
        onBatterySettings: () -> Unit,
        onAutostart: () -> Unit,
        onBack: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        card.addView(pageHeader(colors, activity.getString(R.string.shell_settings_section_background)) { dialog.dismiss(); onBack() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_settings, activity.getString(R.string.shell_settings_keep_background), if (backgroundEnabled) activity.getString(R.string.shell_settings_background_on) else activity.getString(R.string.shell_settings_background_off)) { dialog.dismiss(); onToggleBackground() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_battery, activity.getString(R.string.shell_settings_battery), activity.getString(R.string.shell_settings_battery_hint)) { dialog.dismiss(); onBatterySettings() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_settings, activity.getString(R.string.shell_settings_autostart), activity.getString(R.string.shell_settings_autostart_hint)) { dialog.dismiss(); onAutostart() })
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showAboutPage(
        dark: Boolean,
        versionName: String,
        onCheckUpdate: () -> Unit,
        onSource: () -> Unit,
        onBack: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        card.addView(pageHeader(colors, activity.getString(R.string.shell_settings_section_about)) { dialog.dismiss(); onBack() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_settings_check_update), activity.getString(R.string.shell_settings_update_hint)) { dialog.dismiss(); onCheckUpdate() })
        card.addView(settingsRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_settings_version, versionName), activity.getString(R.string.shell_settings_source_hint)) { dialog.dismiss(); onSource() })
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    private fun aboutCard(colors: Colors, versionName: String, onSource: () -> Unit): LinearLayout {
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            ImageView(activity).apply {
                setImageResource(R.mipmap.ic_launcher)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    rightMargin = dp(8)
                }
                contentDescription = activity.getString(R.string.shell_settings_about_app_name)
            },
        )
        header.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_about_app_name)
                textSize = 15f
                setTextColor(colors.primary)
            },
        )
        card.addView(header)
        card.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_about_intro)
                textSize = 12f
                setPadding(0, dp(6), 0, dp(2))
                setTextColor(colors.secondary)
            },
        )
        card.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_version, versionName)
                textSize = 11f
                setTextColor(colors.secondary)
            },
        )
        card.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_about_github)
                textSize = 11f
                setPadding(0, dp(4), 0, 0)
                setTextColor(ACCENT)
                isClickable = true
                setOnClickListener { onSource() }
            },
        )
        return card
    }

    private fun pageHeader(colors: Colors, title: String, onClose: () -> Unit): LinearLayout {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(8), dp(4))
            minimumHeight = dp(48)
        }
        row.addView(
            TextView(activity).apply {
                text = title
                textSize = 18f
                setTextColor(colors.primary)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        row.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_page_back)
                textSize = 13f
                setTextColor(colors.secondary)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                isClickable = true
                setOnClickListener { onClose() }
            },
        )
        return row
    }

    // ---- update dialog ------------------------------------------------------

    fun showUpdateDialog(
        dark: Boolean,
        version: String,
        onInstall: () -> Unit,
        onOpenRelease: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(18), dp(18), dp(18), dp(18)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }
        card.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_update_title)
                textSize = 17f
                setTextColor(colors.primary)
            },
        )
        card.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_update_message, version)
                textSize = 13f
                setPadding(0, dp(6), 0, dp(14))
                setTextColor(colors.secondary)
            },
        )
        val buttons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttons.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_release_page)
                textSize = 13f
                setTextColor(colors.secondary)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isClickable = true
                setOnClickListener {
                    dialog.dismiss()
                    onOpenRelease()
                }
            },
        )
        buttons.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_settings_install)
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(dp(16), dp(10), dp(16), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(ACCENT)
                }
                isClickable = true
                setOnClickListener {
                    dialog.dismiss()
                    onInstall()
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(4)
            },
        )
        card.addView(buttons, matchWidth())
        root.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        show(dialog, root, gravity = Gravity.CENTER)
    }

    // ---- device manager -----------------------------------------------------

    fun showDeviceManager(
        dark: Boolean,
        devices: List<ShellDevice>,
        currentId: String?,
        onSelect: (ShellDevice) -> Unit,
        onRename: (ShellDevice) -> Unit,
        onDelete: (ShellDevice) -> Unit,
        onAdd: () -> Unit,
        onRefresh: () -> Unit,
        onExit: () -> Unit,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(8), dp(2))
        }
        val titleColumn = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleColumn.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.device_manager_title)
                textSize = 18f
                setTextColor(colors.primary)
            },
        )
        titleColumn.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.device_manager_subtitle)
                textSize = 12f
                setPadding(0, dp(3), 0, 0)
                setTextColor(colors.secondary)
            },
        )
        header.addView(titleColumn)
        header.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.device_close)
                textSize = 13f
                setTextColor(colors.secondary)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                isClickable = true
                setOnClickListener { dialog.dismiss() }
            },
        )
        card.addView(header, matchWidth())

        val listColumn = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        devices.forEach { device ->
            listColumn.addView(
                deviceRow(
                    colors = colors,
                    device = device,
                    selected = device.id == currentId,
                    onlyOne = devices.size <= 1,
                    onSelect = {
                        dialog.dismiss()
                        onSelect(device)
                    },
                    onRename = {
                        dialog.dismiss()
                        onRename(device)
                    },
                    onDelete = {
                        dialog.dismiss()
                        onDelete(device)
                    },
                ),
            )
        }
        listColumn.addView(
            addDeviceRow(colors) {
                dialog.dismiss()
                onAdd()
            },
        )
        val listHeight = dp((devices.size * 66 + 48).coerceIn(96, 300))
        val scroll = ScrollView(activity).apply {
            isFillViewport = false
            addView(
                listColumn,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight)
        }
        card.addView(scroll, matchWidth())

        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.webshell_action_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        card.addView(
            menuRow(colors, R.drawable.ic_shell_exit, activity.getString(R.string.device_exit), destructive = true) {
                dialog.dismiss()
                onExit()
            },
        )

        root.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        show(dialog, root, gravity = Gravity.CENTER)
    }

    private fun deviceRow(
        colors: Colors,
        device: ShellDevice,
        selected: Boolean,
        onlyOne: Boolean,
        onSelect: () -> Unit,
        onRename: () -> Unit,
        onDelete: () -> Unit,
    ): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(62)
            setPadding(dp(12), dp(6), dp(8), dp(6))
            background = rowRipple(colors)
            isClickable = true
            isFocusable = true
            setOnClickListener { onSelect() }
        }
        row.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_shell_device)
                setColorFilter(if (selected) ACCENT else colors.secondary)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            },
        )
        val copy = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        copy.addView(
            TextView(activity).apply {
                text = device.name
                textSize = 15f
                setTextColor(if (selected) ACCENT else colors.primary)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            },
        )
        copy.addView(
            TextView(activity).apply {
                text = device.url
                textSize = 11f
                setTextColor(colors.secondary)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                setPadding(0, dp(2), 0, 0)
            },
        )
        row.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (selected) {
            row.addView(
                ImageView(activity).apply {
                    setImageResource(R.drawable.ic_shell_check)
                    setColorFilter(ACCENT)
                    contentDescription = activity.getString(R.string.device_current)
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
                },
            )
        }
        row.addView(
            iconButton(colors, R.drawable.ic_shell_edit, R.string.device_rename) { onRename() },
        )
        row.addView(
            iconButton(
                colors,
                R.drawable.ic_shell_delete,
                R.string.device_delete,
                danger = true,
                enabled = !onlyOne,
            ) { onDelete() },
        )
        return row
    }

    private fun addDeviceRow(colors: Colors, action: () -> Unit): View = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        setPadding(dp(14), 0, dp(14), 0)
        background = rowRipple(colors)
        isClickable = true
        setOnClickListener { action() }
        addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_shell_add)
                setColorFilter(ACCENT)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            },
        )
        addView(
            TextView(activity).apply {
                text = activity.getString(R.string.device_add)
                textSize = 15f
                setTextColor(ACCENT)
                setPadding(dp(12), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
    }

    private fun iconButton(
        colors: Colors,
        iconRes: Int,
        descriptionRes: Int,
        danger: Boolean = false,
        enabled: Boolean = true,
        action: () -> Unit,
    ): ImageView = ImageView(activity).apply {
        setImageResource(iconRes)
        setColorFilter(if (danger) colors.danger else colors.secondary)
        contentDescription = activity.getString(descriptionRes)
        alpha = if (enabled) 1f else 0.35f
        isEnabled = enabled
        isClickable = true
        setPadding(dp(7), dp(7), dp(7), dp(7))
        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
        setOnClickListener { if (enabled) action() }
    }

    // ---- generic text dialog ------------------------------------------------

    fun showTextDialog(
        dark: Boolean,
        title: String,
        description: String,
        initial: String,
        hint: String,
        inputType: Int,
        errorText: String,
        onSubmit: (String) -> Boolean,
    ) {
        val colors = Colors(dark)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply { setPadding(dp(18), dp(18), dp(18), dp(18)) }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(colors, radiusDp = 20)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }
        card.addView(
            TextView(activity).apply {
                text = title
                textSize = 17f
                setTextColor(colors.primary)
            },
        )
        card.addView(
            TextView(activity).apply {
                text = description
                textSize = 13f
                setPadding(0, dp(6), 0, dp(14))
                setTextColor(colors.secondary)
            },
        )
        val field = EditText(activity).apply {
            setText(initial)
            setSelection(text.length)
            textSize = 14f
            this.inputType = inputType
            imeOptions = EditorInfo.IME_ACTION_DONE
            maxLines = 1
            setSingleLine(true)
            setTextColor(colors.primary)
            setHintTextColor(colors.secondary)
            this.hint = hint
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = fieldBackground(colors)
        }
        val error = TextView(activity).apply {
            text = errorText
            textSize = 12f
            setTextColor(colors.danger)
            setPadding(0, dp(8), 0, 0)
            visibility = View.GONE
        }
        card.addView(field, matchWidth())
        card.addView(error, matchWidth())

        val buttons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(16), 0, 0)
        }
        buttons.addView(
            TextView(activity).apply {
                text = activity.getString(android.R.string.cancel)
                textSize = 14f
                setTextColor(colors.secondary)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                isClickable = true
                setOnClickListener { dialog.dismiss() }
            },
        )
        buttons.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.device_save)
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(dp(18), dp(10), dp(18), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(ACCENT)
                }
                isClickable = true
                setOnClickListener {
                    val accepted = runCatching { onSubmit(field.text.toString()) }.getOrDefault(false)
                    if (accepted) {
                        dialog.dismiss()
                    } else {
                        error.visibility = View.VISIBLE
                        field.requestFocus()
                    }
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(6)
            },
        )
        card.addView(buttons, matchWidth())
        root.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        dialog.setOnShowListener {
            field.requestFocus()
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            )
        }
        show(dialog, root, gravity = Gravity.CENTER)
    }

    // ---- shared building blocks --------------------------------------------

    private fun show(dialog: Dialog, content: View, gravity: Int) {
        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(gravity)
            setDimAmount(0.35f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        dialog.show()
    }

    private fun cardBackground(colors: Colors, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(colors.card)
    }

    private fun fieldBackground(colors: Colors): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        setColor(colors.field)
        setStroke(dp(1), colors.divider)
    }

    private fun rowRipple(colors: Colors): RippleDrawable {
        val mask = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Color.WHITE)
        }
        return RippleDrawable(ColorStateList.valueOf(colors.ripple), null, mask)
    }

    private fun divider(colors: Colors): View = View(activity).apply {
        setBackgroundColor(colors.divider)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            leftMargin = dp(14)
            rightMargin = dp(14)
        }
    }

    private fun enabledLabel(enabled: Boolean): String = activity.getString(
        if (enabled) R.string.shell_settings_enabled else R.string.shell_settings_disabled,
    )

    private fun settingsRow(
        colors: Colors,
        iconRes: Int,
        title: String,
        subtitle: String? = null,
        action: () -> Unit,
    ): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(58)
            setPadding(dp(14), dp(7), dp(12), dp(7))
            background = rowRipple(colors)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        row.addView(
            ImageView(activity).apply {
                setImageResource(iconRes)
                setColorFilter(colors.secondary)
                layoutParams = LinearLayout.LayoutParams(dp(21), dp(21))
            },
        )
        val copy = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, dp(8), 0)
        }
        copy.addView(
            TextView(activity).apply {
                text = title
                textSize = 15f
                setTextColor(colors.primary)
                maxLines = 1
            },
        )
        if (!subtitle.isNullOrBlank()) {
            copy.addView(
                TextView(activity).apply {
                    text = subtitle
                    textSize = 11.5f
                    setTextColor(colors.secondary)
                    maxLines = 1
                    setPadding(0, dp(2), 0, 0)
                },
            )
        }
        row.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(
            TextView(activity).apply {
                text = "›"
                textSize = 20f
                setTextColor(colors.secondary)
            },
        )
        return row
    }

    private fun sectionLabel(colors: Colors, label: String): TextView = TextView(activity).apply {
        text = label
        textSize = 11f
        letterSpacing = 0.04f
        setTextColor(colors.secondary)
        setPadding(dp(16), dp(11), dp(16), dp(4))
    }

    private fun menuRow(
        colors: Colors,
        iconRes: Int,
        label: String,
        destructive: Boolean = false,
        action: () -> Unit,
    ): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            minimumHeight = dp(52)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            background = rowRipple(colors)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        row.addView(
            ImageView(activity).apply {
                setImageResource(iconRes)
                setColorFilter(if (destructive) colors.danger else colors.primary)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            },
        )
        row.addView(
            TextView(activity).apply {
                text = label
                textSize = 15f
                setTextColor(if (destructive) colors.danger else colors.primary)
                setPadding(dp(14), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        return row
    }

    private fun matchWidth(topMarginDp: Int = 0, bottomMarginDp: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(topMarginDp)
            bottomMargin = dp(bottomMarginDp)
        }

    /** The two palettes the shell chrome needs, mirroring the web client's light/dark surfaces. */
    private class Colors(dark: Boolean) {
        val card: Int = if (dark) Color.parseColor("#1C1C1E") else Color.WHITE
        val field: Int = if (dark) Color.parseColor("#2A2A2C") else Color.parseColor("#F5F6F7")
        val primary: Int = if (dark) Color.parseColor("#F5F5F5") else Color.parseColor("#0F1115")
        val secondary: Int = if (dark) Color.parseColor("#A9AEB5") else Color.parseColor("#61666B")
        val divider: Int = if (dark) Color.parseColor("#33FFFFFF") else Color.parseColor("#14000000")
        val ripple: Int = if (dark) Color.parseColor("#33FFFFFF") else Color.parseColor("#14000000")
        val danger: Int = if (dark) Color.parseColor("#F2555A") else Color.parseColor("#E5484D")
    }

    private companion object {
        val ACCENT: Int = Color.parseColor("#3964FE")
    }
}
