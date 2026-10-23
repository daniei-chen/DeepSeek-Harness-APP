package com.labteto.dshmobile.webshell

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.labteto.dshmobile.BuildConfig
import com.labteto.dshmobile.R
import com.labteto.dshmobile.connection.DoctorReport
import com.labteto.dshmobile.connection.DoctorStatus
import com.labteto.dshmobile.connection.SessionSnapshot
import kotlin.math.roundToInt

/** One saved shell device and the result of the last health probe. */
internal data class ShellDeviceHealth(
    val device: ShellDevice,
    val online: Boolean?,
    val detail: String,
)

/** One host in the multi-host overview. Session counts are null when the query failed. */
internal data class ShellFleetHost(
    val device: ShellDevice,
    val online: Boolean?,
    val detail: String,
    val sessionCount: Int? = null,
    val runningCount: Int? = null,
)

/** One cross-host session that is running or has an active question/plan review. */
internal data class ShellFleetSession(
    val device: ShellDevice,
    val sessionId: String,
    val title: String,
    val status: String,
    val updatedAt: Long,
    /** 0 = waiting question/plan review, 1 = running only. */
    val priority: Int = 1,
)

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
        deviceCount: Int,
        versionName: String,
        pluginVersion: String?,
        simpleMode: Boolean,
        inboxCount: Int,
        notificationsEnabled: Boolean,
        backgroundEnabled: Boolean,
        ntfyEnabled: Boolean,
        linkHandlerEnabled: Boolean,
        onConnection: () -> Unit,
        onRemote: () -> Unit,
        onInbox: () -> Unit,
        onMobileSetup: () -> Unit,
        onDiagnostics: () -> Unit,
        onNotifications: () -> Unit,
        onBackground: () -> Unit,
        onToggleLinkHandler: () -> Unit,
        onExport: () -> Unit,
        onImport: () -> Unit,
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
        card.addView(aboutCard(colors, versionName, pluginVersion, onSource))
        card.addView(
            actionRow(
                colors = colors,
                left = R.drawable.ic_shell_refresh to activity.getString(R.string.webshell_action_refresh),
                leftAction = {
                    dialog.dismiss()
                    onRefresh()
                },
                right = R.drawable.ic_shell_check to activity.getString(R.string.shell_settings_check_update),
                rightAction = {
                    dialog.dismiss()
                    onCheckUpdate()
                },
            ),
        )
        card.addView(sectionLabel(colors, activity.getString(R.string.shell_group_connection)))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_device,
                title = activity.getString(R.string.shell_settings_section_connection),
                subtitle = currentDeviceName?.let {
                    activity.getString(R.string.shell_settings_current_device, it) +
                        if (deviceCount > 1) " · " + activity.getString(R.string.shell_settings_devices_count, deviceCount) else ""
                } ?: activity.getString(R.string.shell_settings_connection_hint),
            ) {
                // Devices are the only thing this row is for, so it opens the manager itself:
                // 设置 → 连接 → 设备 was one tap too many for the one setting people change.
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
        if (!simpleMode) {
            card.addView(sectionLabel(colors, activity.getString(R.string.shell_group_phone)))
        if (!simpleMode) {
            card.addView(
                settingsRow(
                    colors = colors,
                    iconRes = R.drawable.ic_shell_link,
                    title = activity.getString(R.string.shell_remote),
                    subtitle = activity.getString(R.string.shell_remote_hint),
                ) {
                    dialog.dismiss()
                    onRemote()
                },
            )
        }
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
        }
        card.addView(sectionLabel(colors, activity.getString(R.string.shell_group_alerts)))
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
        if (!simpleMode) {
            card.addView(sectionLabel(colors, activity.getString(R.string.shell_group_data)))
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_link,
                title = activity.getString(R.string.shell_link_handler),
                subtitle = activity.getString(R.string.shell_link_handler_hint) + " · " +
                    activity.getString(
                        if (linkHandlerEnabled) {
                            R.string.shell_settings_background_on
                        } else {
                            R.string.shell_settings_background_off
                        },
                    ),
            ) {
                dialog.dismiss()
                onToggleLinkHandler()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_edit,
                title = activity.getString(R.string.shell_settings_export),
                subtitle = activity.getString(R.string.shell_settings_export_hint),
            ) {
                dialog.dismiss()
                onExport()
            },
        )
        card.addView(
            settingsRow(
                colors = colors,
                iconRes = R.drawable.ic_shell_check,
                title = activity.getString(R.string.shell_settings_import),
                subtitle = activity.getString(R.string.shell_settings_import_hint),
            ) {
                dialog.dismiss()
                onImport()
            },
        )
        }
        card.addView(sectionLabel(colors, activity.getString(R.string.shell_group_support)))
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
        card.addView(divider(colors))
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
        simpleMode: Boolean,
        onScanQr: () -> Unit,
        onPairCode: () -> Unit,
        onScanLan: () -> Unit,
        onNetworkInfo: () -> Unit,
        onFleet: () -> Unit,
        onFleetInbox: () -> Unit,
        onDeviceHealth: () -> Unit,
        onDoctor: () -> Unit,
        onTestConnection: () -> Unit,
        onToggleSimple: () -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_settings_more_connection)) { dialog.dismiss(); onBack() })
        if (!simpleMode) {
            card.addView(
                settingsRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_settings_scan_qr), activity.getString(R.string.shell_settings_scan_qr_hint)) {
                    dialog.dismiss()
                    onScanQr()
                },
            )
            card.addView(
                settingsRow(colors, R.drawable.ic_shell_edit, activity.getString(R.string.shell_pair_row), activity.getString(R.string.shell_pair_row_hint)) {
                    dialog.dismiss()
                    onPairCode()
                },
            )
            card.addView(
                settingsRow(colors, R.drawable.ic_shell_settings, activity.getString(R.string.shell_settings_scan_lan), activity.getString(R.string.shell_settings_scan_lan_hint)) {
                    dialog.dismiss()
                    onScanLan()
                },
            )
        }
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_settings_network_info), activity.getString(R.string.shell_settings_network_info_hint)) {
                dialog.dismiss()
                onNetworkInfo()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_fleet), activity.getString(R.string.shell_fleet_hint)) {
                dialog.dismiss()
                onFleet()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_notifications, activity.getString(R.string.shell_fleet_inbox), activity.getString(R.string.shell_fleet_inbox_hint)) {
                dialog.dismiss()
                onFleetInbox()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_notifications, activity.getString(R.string.shell_device_health), activity.getString(R.string.shell_device_health_hint)) {
                dialog.dismiss()
                onDeviceHealth()
            },
        )
        card.addView(
            settingsRow(
                colors,
                R.drawable.ic_shell_settings,
                activity.getString(
                    if (simpleMode) R.string.shell_advanced_show else R.string.shell_advanced_hide,
                ),
                activity.getString(R.string.shell_simple_mode_hint),
            ) {
                dialog.dismiss()
                onToggleSimple()
            },
        )
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_doctor), activity.getString(R.string.shell_doctor_hint)) {
                dialog.dismiss()
                onDoctor()
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

    fun showDiagnosticsPage(
        dark: Boolean,
        message: String,
        glassCompatLabel: String,
        onCopy: () -> Unit,
        onClearCache: () -> Unit,
        onGlassCompat: () -> Unit,
        onRepair: () -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_diagnostics_title)) { dialog.dismiss(); onBack() })
        card.addView(
            TextView(activity).apply {
                text = message
                textSize = 12f
                setTextColor(colors.secondary)
                setPadding(dp(14), dp(8), dp(14), dp(14))
                typeface = android.graphics.Typeface.MONOSPACE
            },
        )
        card.addView(divider(colors))
        card.addView(menuRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_diagnostics_copy)) {
            dialog.dismiss()
            onCopy()
        })
        card.addView(settingsRow(
            colors,
            R.drawable.ic_shell_settings,
            activity.getString(R.string.shell_glass_compat),
            glassCompatLabel,
        ) {
            dialog.dismiss()
            onGlassCompat()
        })
        card.addView(menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_diagnostics_clear_cache)) {
            dialog.dismiss()
            onClearCache()
        })
        card.addView(menuRow(colors, R.drawable.ic_shell_settings, activity.getString(R.string.shell_diagnostics_repair)) {
            dialog.dismiss()
            onRepair()
        })
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    /**
     * The connection doctor: one row per probe, in the order the connection is built.
     *
     * A failing row carries the fix in the same line, because "TCP 失败" without a next step is
     * the same dead end as no diagnostics at all.
     */
    fun showDoctorPage(
        dark: Boolean,
        title: String,
        target: String,
        report: DoctorReport,
        onRerun: () -> Unit,
        onCopy: () -> Unit,
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
        card.addView(pageHeader(colors, title) { dialog.dismiss(); onBack() })

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), dp(6))
        }

        column.addView(
            TextView(activity).apply {
                text = target
                textSize = 11.5f
                setTextColor(colors.secondary)
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(dp(8), dp(2), dp(8), dp(6))
            },
        )
        val headlineColor = when {
            report.failures > 0 -> colors.danger
            report.warnings > 0 -> colors.warn
            else -> colors.ok
        }
        column.addView(
            TextView(activity).apply {
                text = report.summary
                textSize = 15f
                setTextColor(headlineColor)
                setPadding(dp(8), 0, dp(8), dp(8))
            },
        )

        report.steps.forEach { step ->
            val tone = when (step.status) {
                DoctorStatus.OK -> colors.ok
                DoctorStatus.WARN -> colors.warn
                DoctorStatus.FAIL -> colors.danger
                DoctorStatus.SKIP -> colors.secondary
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(7), dp(8), dp(7))
            }
            val title = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            title.addView(
                View(activity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(tone)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(9) }
                },
            )
            title.addView(
                TextView(activity).apply {
                    text = step.label
                    textSize = 13.5f
                    setTextColor(colors.primary)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            row.addView(title)
            row.addView(
                TextView(activity).apply {
                    text = step.detail
                    textSize = 12f
                    setTextColor(if (step.status == DoctorStatus.FAIL) colors.danger else colors.secondary)
                    setPadding(dp(17), dp(2), 0, 0)
                },
            )
            if (!step.action.isNullOrBlank()) {
                row.addView(
                    TextView(activity).apply {
                        text = "→ ${step.action}"
                        textSize = 11.5f
                        setTextColor(colors.secondary)
                        setPadding(dp(17), dp(3), 0, 0)
                    },
                )
            }
            column.addView(row)
        }

        val cap = (activity.resources.displayMetrics.heightPixels - dp(190)).coerceAtLeast(dp(300))
        val scroll = ScrollView(activity).apply {
            isFillViewport = false
            addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cap)
        }
        card.addView(scroll, 1)

        card.addView(divider(colors))
        card.addView(menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_doctor_rerun)) {
            dialog.dismiss()
            onRerun()
        })
        card.addView(menuRow(colors, R.drawable.ic_shell_check, activity.getString(R.string.shell_doctor_copy)) {
            onCopy()
        })
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    /**
     * The offline snapshot: what was running the last time the host answered.
     *
     * Rows are not clickable on purpose — opening one needs a live session, and a button that
     * silently fails is worse than a read-only list that says so.
     */
    fun showOfflinePage(
        dark: Boolean,
        hostLabel: String,
        rows: List<SessionSnapshot>,
        onRetry: () -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_offline_title)) { dialog.dismiss(); onBack() })
        card.addView(
            TextView(activity).apply {
                text = "$hostLabel\n${activity.getString(R.string.shell_offline_hint)}"
                textSize = 12f
                setTextColor(colors.secondary)
                setPadding(dp(14), 0, dp(14), dp(8))
            },
        )
        if (rows.isEmpty()) {
            card.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_offline_empty)
                    textSize = 13f
                    setTextColor(colors.secondary)
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                },
            )
        } else {
            val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            rows.forEach { row ->
                val column = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(9), dp(14), dp(9))
                }
                column.addView(
                    TextView(activity).apply {
                        text = row.title
                        textSize = 14f
                        setTextColor(colors.primary)
                        maxLines = 1
                    },
                )
                val status = when {
                    row.pending != null -> activity.getString(R.string.shell_offline_pending)
                    row.running -> activity.getString(R.string.shell_offline_running)
                    else -> activity.getString(R.string.shell_inbox_status_idle)
                }
                column.addView(
                    TextView(activity).apply {
                        text = "$status · " + activity.getString(
                            R.string.shell_offline_updated,
                            format.format(java.util.Date(row.updatedAt)),
                        )
                        textSize = 11.5f
                        setTextColor(colors.secondary)
                        setPadding(0, dp(2), 0, 0)
                    },
                )
                list.addView(column)
            }
            val height = dp((rows.size * 56 + 16).coerceIn(120, 420))
            card.addView(
                ScrollView(activity).apply {
                    isFillViewport = false
                    addView(list, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                },
            )
        }
        card.addView(divider(colors))
        card.addView(menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_offline_retry)) {
            dialog.dismiss()
            onRetry()
        })
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    /**
     * The remote-access page: the relay's live pairing QR, the code, and where it points.
     *
     * The QR is drawn on the phone from the payload the host read out of the relay, so the page
     * works with the relay's self-signed certificate and without a browser tab on the desktop.
     */
    fun showRemoteAccessPage(
        dark: Boolean,
        qr: Bitmap?,
        code: String?,
        expiry: String?,
        addresses: List<Pair<String, String>>,
        note: String,
        message: String?,
        onRefresh: () -> Unit,
        onCopyCode: () -> Unit,
        onCopyLink: () -> Unit,
        onDevices: () -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_remote_title)) { dialog.dismiss(); onBack() })

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(2), dp(8), dp(6))
        }
        // The QR on this page is an invitation *for another device*. Saying so here is the only
        // thing that stops "why is the pairing code on my phone?" — it belongs to the host.
        column.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_remote_scope_note)
                textSize = 11.5f
                setTextColor(colors.secondary)
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(4), dp(10), dp(8))
            },
        )

        if (qr != null) {
            val frame = FrameLayout(activity).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(Color.WHITE)
                }
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(dp(244), dp(244)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(2)
                }
            }
            frame.addView(
                ImageView(activity).apply {
                    setImageBitmap(qr)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            column.addView(frame)
        }

        if (!code.isNullOrBlank()) {
            column.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_remote_code, code.chunked(4).joinToString(" "))
                    textSize = 19f
                    setTextColor(colors.primary)
                    typeface = android.graphics.Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    setPadding(0, dp(12), 0, 0)
                },
            )
        }
        if (!expiry.isNullOrBlank()) {
            column.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_remote_expires, expiry)
                    textSize = 12f
                    setTextColor(colors.secondary)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(2), 0, 0)
                },
            )
        }
        column.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_remote_qr_hint)
                textSize = 11.5f
                setTextColor(colors.secondary)
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
            },
        )
        if (!message.isNullOrBlank()) {
            column.addView(
                TextView(activity).apply {
                    text = message
                    textSize = 12f
                    setTextColor(colors.danger)
                    gravity = Gravity.CENTER
                    setPadding(dp(10), dp(2), dp(10), dp(8))
                },
            )
        }

        column.addView(divider(colors))
        column.addView(menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_remote_refresh)) {
            dialog.dismiss()
            onRefresh()
        })
        if (!code.isNullOrBlank()) {
            column.addView(menuRow(colors, R.drawable.ic_shell_edit, activity.getString(R.string.shell_remote_copy_code)) {
                onCopyCode()
            })
        }
        column.addView(menuRow(colors, R.drawable.ic_shell_link, activity.getString(R.string.shell_remote_copy_link)) {
            onCopyLink()
        })
        column.addView(menuRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_remote_devices)) {
            dialog.dismiss()
            onDevices()
        })

        if (addresses.isNotEmpty()) {
            column.addView(divider(colors))
            column.addView(sectionLabel(colors, activity.getString(R.string.shell_remote_addresses)))
            addresses.forEach { (label, value) -> column.addView(infoRow(colors, label, value)) }
        }
        column.addView(
            TextView(activity).apply {
                text = note
                textSize = 11f
                setTextColor(colors.secondary)
                setPadding(dp(16), dp(6), dp(16), dp(8))
            },
        )

        // Leave room for the card's own padding, its header and the status/navigation bars; the
        // window is WRAP_CONTENT, so an over-tall card would simply be clipped at the bottom.
        val cap = (activity.resources.displayMetrics.heightPixels - dp(160)).coerceAtLeast(dp(320))
        val scroll = ScrollView(activity).apply {
            isFillViewport = false
            addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cap)
        }
        card.addView(scroll, 1)

        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showFleetInboxPage(
        dark: Boolean,
        rows: List<ShellFleetSession>,
        onOpen: (ShellFleetSession) -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_fleet_inbox_title)) { dialog.dismiss(); onBack() })
        if (rows.isEmpty()) {
            card.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_fleet_inbox_empty)
                    textSize = 13f
                    setTextColor(colors.secondary)
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                },
            )
        } else {
            rows.forEach { row ->
                card.addView(
                    settingsRow(
                        colors = colors,
                        iconRes = R.drawable.ic_shell_notifications,
                        title = row.device.name + " · " + row.title,
                        subtitle = row.status,
                    ) {
                        dialog.dismiss()
                        onOpen(row)
                    },
                )
            }
        }
        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_fleet_inbox_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showFleetPage(
        dark: Boolean,
        rows: List<ShellFleetHost>,
        onSelect: (ShellFleetHost) -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_fleet_title)) { dialog.dismiss(); onBack() })
        if (rows.isEmpty()) {
            card.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_fleet_empty)
                    textSize = 13f
                    setTextColor(colors.secondary)
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                },
            )
        } else {
            rows.forEach { row ->
                card.addView(
                    settingsRow(
                        colors = colors,
                        iconRes = R.drawable.ic_shell_device,
                        title = row.device.name,
                        subtitle = row.detail + " · " + row.device.url,
                    ) {
                        dialog.dismiss()
                        onSelect(row)
                    },
                )
            }
        }
        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_fleet_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showDeviceHealthPage(
        dark: Boolean,
        rows: List<ShellDeviceHealth>,
        onSelect: (ShellDeviceHealth) -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_device_health_title)) { dialog.dismiss(); onBack() })
        if (rows.isEmpty()) {
            card.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.shell_device_health_empty)
                    textSize = 13f
                    setTextColor(colors.secondary)
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                },
            )
        } else {
            rows.forEach { row ->
                card.addView(
                    settingsRow(
                        colors = colors,
                        iconRes = R.drawable.ic_shell_device,
                        title = row.device.name,
                        subtitle = row.detail + " · " + row.device.url,
                    ) {
                        dialog.dismiss()
                        onSelect(row)
                    },
                )
            }
        }
        card.addView(divider(colors))
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_device_health_refresh)) {
                dialog.dismiss()
                onRefresh()
            },
        )
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        show(dialog, root, gravity = Gravity.CENTER)
    }

    fun showInboxPage(
        dark: Boolean,
        rows: List<ShellInboxRow>,
        connectionLabel: String,
        query: String,
        onSearch: () -> Unit,
        onClearSearch: () -> Unit,
        onOpen: (ShellInboxRow) -> Unit,
        onDetails: (ShellInboxRow) -> Unit,
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

        if (query.isBlank()) {
            card.addView(menuRow(colors, R.drawable.ic_shell_search, activity.getString(R.string.shell_inbox_search)) {
                dialog.dismiss()
                onSearch()
            })
        } else {
            card.addView(
                settingsRow(
                    colors,
                    R.drawable.ic_shell_search,
                    activity.getString(R.string.shell_inbox_search_active, query),
                    activity.getString(R.string.shell_inbox_search_hint),
                ) {
                    dialog.dismiss()
                    onSearch()
                },
            )
            card.addView(menuRow(colors, R.drawable.ic_shell_exit, activity.getString(R.string.shell_inbox_search_clear)) {
                dialog.dismiss()
                onClearSearch()
            })
        }
        card.addView(divider(colors))

        if (rows.isEmpty()) {
            card.addView(
                TextView(activity).apply {
                    text = if (query.isBlank()) {
                        activity.getString(R.string.shell_inbox_empty)
                    } else {
                        activity.getString(R.string.shell_inbox_no_match, query)
                    }
                    textSize = 13f
                    setTextColor(colors.secondary)
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                },
            )
        } else {
            val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            rows.forEach { row ->
                list.addView(
                    inboxRow(
                        colors = colors,
                        row = row,
                        onOpen = {
                            dialog.dismiss()
                            onOpen(row)
                        },
                        onDetails = {
                            dialog.dismiss()
                            onDetails(row)
                        },
                    ),
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

    fun showSessionCockpit(
        dark: Boolean,
        title: String,
        status: String,
        details: List<Pair<String, String>>,
        running: Boolean,
        onOpen: () -> Unit,
        onStop: () -> Unit,
        onFork: () -> Unit,
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
        card.addView(pageHeader(colors, activity.getString(R.string.shell_cockpit_title)) { dialog.dismiss(); onBack() })
        card.addView(
            TextView(activity).apply {
                text = title
                textSize = 17f
                setTextColor(colors.primary)
                maxLines = 2
                setPadding(dp(14), dp(6), dp(14), dp(2))
            },
        )
        card.addView(
            TextView(activity).apply {
                text = status
                textSize = 12f
                setTextColor(colors.secondary)
                setPadding(dp(14), 0, dp(14), dp(10))
            },
        )
        details.forEach { (label, value) -> card.addView(infoRow(colors, label, value)) }
        card.addView(divider(colors))
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_device, activity.getString(R.string.shell_cockpit_open)) {
                dialog.dismiss()
                onOpen()
            },
        )
        if (running) {
            card.addView(
                menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_cockpit_stop), destructive = true) {
                    dialog.dismiss()
                    onStop()
                },
            )
        }
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_edit, activity.getString(R.string.shell_cockpit_fork)) {
                dialog.dismiss()
                onFork()
            },
        )
        card.addView(
            menuRow(colors, R.drawable.ic_shell_refresh, activity.getString(R.string.shell_cockpit_refresh)) {
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
        onCheck: () -> Unit,
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
        card.addView(
            settingsRow(colors, R.drawable.ic_shell_search, activity.getString(R.string.shell_notify_check), activity.getString(R.string.shell_notify_check_hint)) {
                dialog.dismiss()
                onCheck()
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

    private fun aboutCard(
        colors: Colors,
        versionName: String,
        pluginVersion: String?,
        onSource: () -> Unit,
    ): LinearLayout {
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
                text = pluginVersionLabel(pluginVersion)
                textSize = 11f
                setPadding(0, dp(2), 0, 0)
                setTextColor(
                    when {
                        pluginVersion == null -> colors.warn
                        isOlder(pluginVersion, BuildConfig.PLUGIN_VERSION) -> colors.warn
                        else -> colors.secondary
                    },
                )
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

    /**
     * Which half of the install is behind.
     *
     * The app and the host plugin are two artifacts updated separately, and running mismatched
     * halves is the most common way this product looks broken. Naming the side that is stale is the
     * difference between a five-second fix and an hour of guessing.
     */
    private fun pluginVersionLabel(pluginVersion: String?): String = when {
        pluginVersion == null -> activity.getString(R.string.shell_plugin_version_missing)
        isOlder(pluginVersion, BuildConfig.PLUGIN_VERSION) -> activity.getString(
            R.string.shell_plugin_version_old,
            pluginVersion,
            BuildConfig.PLUGIN_VERSION,
        )
        else -> activity.getString(R.string.shell_plugin_version_ok, pluginVersion)
    }

    /** Numeric semver comparison; a missing or non-numeric segment counts as 0. */
    private fun isOlder(candidate: String, expected: String): Boolean {
        val left = candidate.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
        val right = expected.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a < b
        }
        return false
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
        onMore: () -> Unit,
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
            menuRow(colors, R.drawable.ic_shell_settings, activity.getString(R.string.shell_settings_more_connection)) {
                dialog.dismiss()
                onMore()
            },
        )
        card.addView(divider(colors))
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

    /**
     * Pairing without a camera: the relay address plus the eight-digit code.
     *
     * The address is the relay's own origin (`https://<lan-ip>:3443`), not the DSH port — the
     * claim endpoint belongs to the relay. Both fields are validated by the caller so this dialog
     * stays a dumb form.
     */
    fun showPairingDialog(
        dark: Boolean,
        initialAddress: String,
        onSubmit: (String, String) -> Boolean,
        onScan: () -> Unit,
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
                text = activity.getString(R.string.shell_pair_title)
                textSize = 17f
                setTextColor(colors.primary)
            },
        )
        card.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_pair_description)
                textSize = 13f
                setPadding(0, dp(6), 0, dp(14))
                setTextColor(colors.secondary)
            },
        )
        val address = EditText(activity).apply {
            setText(initialAddress)
            setSelection(text.length)
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_NEXT
            maxLines = 1
            setSingleLine(true)
            setTextColor(colors.primary)
            setHintTextColor(colors.secondary)
            hint = activity.getString(R.string.shell_pair_address_hint)
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = fieldBackground(colors)
        }
        val code = EditText(activity).apply {
            textSize = 18f
            letterSpacing = 0.18f
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            maxLines = 1
            setSingleLine(true)
            setTextColor(colors.primary)
            setHintTextColor(colors.secondary)
            hint = activity.getString(R.string.shell_pair_code_hint)
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = fieldBackground(colors)
        }
        val error = TextView(activity).apply {
            text = activity.getString(R.string.shell_pair_invalid)
            textSize = 12f
            setTextColor(colors.danger)
            setPadding(0, dp(8), 0, 0)
            visibility = View.GONE
        }
        card.addView(address, matchWidth())
        card.addView(code, matchWidth(9))
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
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isClickable = true
                setOnClickListener { dialog.dismiss() }
            },
        )
        buttons.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_pair_scan)
                textSize = 14f
                setTextColor(colors.primary)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isClickable = true
                setOnClickListener {
                    dialog.dismiss()
                    onScan()
                }
            },
        )
        buttons.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_pair_connect)
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(dp(18), dp(10), dp(18), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(ACCENT)
                }
                isClickable = true
                setOnClickListener {
                    val accepted = runCatching { onSubmit(address.text.toString(), code.text.toString()) }
                        .getOrDefault(false)
                    if (accepted) {
                        dialog.dismiss()
                    } else {
                        error.visibility = View.VISIBLE
                    }
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
        dialog.setOnShowListener {
            address.requestFocus()
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            )
        }
        show(dialog, root, gravity = Gravity.CENTER)
    }

    // ---- shared building blocks --------------------------------------------

    /**
     * Present a page, scrollable and clear of the system bars.
     *
     * The window used to be sized to WRAP_CONTENT, which is fine for a confirmation and wrong for
     * every real page: the settings sheet is a single column of thirteen rows, so anything past the
     * screen edge was simply clipped — the last row could not be reached at all. The cap plus an
     * inner ScrollView means a page that fits still looks centred, and one that does not scrolls
     * instead of losing its tail.
     */
    private fun show(dialog: Dialog, content: View, gravity: Int) {
        val metrics = activity.resources.displayMetrics
        val bottomInset = runCatching {
            val insets = ViewCompat.getRootWindowInsets(activity.window?.decorView ?: return@runCatching 0)
            insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        }.getOrDefault(0)
        val maxHeight = (metrics.heightPixels * 0.88f).toInt() - bottomInset
        val scrollable = if (content is ScrollView) {
            content
        } else {
            ScrollView(activity).apply {
                isFillViewport = true
                clipToPadding = false
                setPadding(0, 0, 0, bottomInset)
                addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
        dialog.setContentView(scrollable)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight)
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

    private fun infoRow(colors: Colors, label: String, value: String): View = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(42)
        setPadding(dp(14), dp(5), dp(14), dp(5))
        addView(
            TextView(activity).apply {
                text = label
                textSize = 12f
                setTextColor(colors.secondary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f)
            },
        )
        addView(
            TextView(activity).apply {
                text = value
                textSize = 12.5f
                setTextColor(colors.primary)
                gravity = Gravity.END
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f)
            },
        )
    }

    private fun inboxRow(
        colors: Colors,
        row: ShellInboxRow,
        onOpen: () -> Unit,
        onDetails: () -> Unit,
    ): View {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(62)
            setPadding(dp(14), dp(6), dp(8), dp(6))
            background = rowRipple(colors)
            isClickable = true
            isFocusable = true
            setOnClickListener { onOpen() }
        }
        container.addView(
            ImageView(activity).apply {
                setImageResource(
                    when {
                        row.action -> R.drawable.ic_shell_notifications
                        row.running -> R.drawable.ic_shell_refresh
                        else -> R.drawable.ic_shell_check
                    },
                )
                setColorFilter(if (row.action) ACCENT else colors.secondary)
                layoutParams = LinearLayout.LayoutParams(dp(21), dp(21))
            },
        )
        val copy = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, dp(8), 0)
        }
        copy.addView(
            TextView(activity).apply {
                text = row.title
                textSize = 15f
                setTextColor(colors.primary)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            },
        )
        copy.addView(
            TextView(activity).apply {
                text = row.subtitle
                textSize = 11.5f
                setTextColor(if (row.action) ACCENT else colors.secondary)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(2), 0, 0)
            },
        )
        container.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.shell_inbox_details)
                textSize = 12f
                setTextColor(ACCENT)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.isPressed = false
                    onDetails()
                }
            },
        )
        return container
    }

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

    /**
     * Two prominent actions side by side, under the masthead.
     *
     * These used to be ordinary list rows — refresh at the very bottom of the page, check-update at
     * the top — which made both feel like footnotes and left refresh unreachable once the sheet
     * overflowed. They are pills now: same weight, comfortable 46 dp targets, both in the first
     * screenful.
     */
    private fun actionRow(
        colors: Colors,
        left: Pair<Int, String>,
        leftAction: () -> Unit,
        right: Pair<Int, String>,
        rightAction: () -> Unit,
    ): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12), dp(10), dp(12), dp(6))
        addView(
            actionPill(colors, left.first, left.second, leftAction),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(5) },
        )
        addView(
            actionPill(colors, right.first, right.second, rightAction),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(5) },
        )
    }

    private fun actionPill(colors: Colors, iconRes: Int, label: String, action: () -> Unit): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = dp(46)
            background = fieldBackground(colors)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            addView(
                ImageView(activity).apply {
                    setImageResource(iconRes)
                    setColorFilter(if (colors.card == Color.WHITE) Color.parseColor("#3964FE") else Color.parseColor("#7A9BFF"))
                    layoutParams = LinearLayout.LayoutParams(dp(17), dp(17))
                },
            )
            addView(
                TextView(activity).apply {
                    text = label
                    textSize = 13.5f
                    maxLines = 1
                    setTextColor(colors.primary)
                    setPadding(dp(7), 0, 0, 0)
                },
            )
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
        val ok: Int = if (dark) Color.parseColor("#3DD68C") else Color.parseColor("#1A7F37")
        val warn: Int = if (dark) Color.parseColor("#F5A524") else Color.parseColor("#B54708")
    }

    private companion object {
        val ACCENT: Int = Color.parseColor("#3964FE")
    }
}
