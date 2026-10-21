package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.R
import com.labteto.dshmobile.data.PluginOperationState

/** Immutable confirmation: approval scripts and registry can never come from the editable form. */
internal data class PluginInstallSubject(val target: String, val registry: String?, val approvedBuilds: List<String>? = null,
    val enabled: Boolean = false, val saveExact: Boolean? = null)

internal fun com.labteto.dshmobile.core.wire.dto.PluginBundle.catalogInstall(enabled: Boolean): PluginInstallSubject? =
    installTarget?.let { PluginInstallSubject(it.spec, null, enabled = enabled,
        saveExact = true.takeUnless { _ -> it.spec.startsWith("link:") }) }
internal fun PluginOperationState.approvalSubject(): PluginInstallSubject? =
    subject?.takeIf { it.isNotBlank() }?.let { target ->
        result?.pendingBuilds?.takeIf { it.isNotEmpty() }?.let { PluginInstallSubject(target, registry, it.toList(), enabled, saveExact) }
    }
internal fun PluginOperationState.displaySubject(): String? = subject ?: result?.let { it.bundle ?: it.target }?.takeIf { it.isNotBlank() }

internal fun pluginReason(code: String?): Int = when (code) {
    "management-required" -> R.string.ux_a_reason_required
    "unaddressable" -> R.string.ux_a_reason_unaddressable
    "unknown-plugin", "not-found" -> R.string.ux_a_reason_missing
    "invalid-spec" -> R.string.ux_a_reason_spec
    "ambiguous-install" -> R.string.ux_a_reason_ambiguous
    "not-bundle", "not-a-bundle" -> R.string.ux_a_reason_bundle
    "already-installed" -> R.string.ux_a_reason_installed
    "network" -> R.string.ux_a_reason_network
    "not-a-package" -> R.string.ux_a_reason_package
    "not-removable" -> R.string.ux_a_reason_removable
    "stop-profile" -> R.string.ux_a_reason_stop
    "bundle-in-use" -> R.string.ux_a_reason_in_use
    "stale-approval" -> R.string.ux_a_reason_approval
    "incompatible-version" -> R.string.ux_a_reason_version
    else -> R.string.ux_a_action_error
}
internal fun pluginPhase(phase: String?): Int = when (phase) {
    "starting", "queued", "resolving" -> R.string.ux_a_phase_starting
    "running", "installing" -> R.string.ux_a_phase_installing
    "applying" -> R.string.ux_a_phase_applying
    "cancelling", "cancel-requested", "requested" -> R.string.ux_a_phase_cancelling
    "cancelled" -> R.string.harness_cancelled
    "done", "completed" -> R.string.harness_applied
    "failed" -> R.string.harness_failed
    else -> R.string.common_loading
}
