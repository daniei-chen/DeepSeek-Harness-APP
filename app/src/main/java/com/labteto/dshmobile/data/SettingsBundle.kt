package com.labteto.dshmobile.data

import com.labteto.dshmobile.connection.AppSettings
import com.labteto.dshmobile.connection.ConnectMode
import com.labteto.dshmobile.connection.HostConfig
import com.labteto.dshmobile.core.wire.WireJson
import com.labteto.dshmobile.webshell.ShellDevice
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * One portable snapshot of everything this app remembers about a deployment.
 *
 * This is the whole of the settings export format, and it is a pure value: nothing here touches a
 * file, `SharedPreferences`, `DataStore` or the UI. [SettingsBundle.encode] turns a snapshot into
 * the JSON a user keeps, [SettingsBundle.decode] turns that JSON back into a snapshot and reports
 * exactly what it had to discard. The caller (settings UI) owns the file picker and applying the
 * result.
 *
 * **No credentials.** The two things that could actually authenticate this device — the harness
 * sign-in cookie and a paired relay's bearer token — live in `HarnessSessionStore` and
 * `RelayCredentialStore`, never in [HostConfig], so they cannot leak through this record even by
 * accident. [HostConfig] carries only addressing plus the relay's public-key pin and its own id for
 * this device, which are identifiers rather than secrets.
 *
 * [schema] is the format version, not the app's version ([appVersion] is informational). This build
 * writes and accepts `schema == 1` only; anything else is refused by [SettingsBundle.decode]
 * instead of being half-applied.
 */
@Serializable
internal data class SettingsBundle(
    val schema: Int = SCHEMA,
    /** Epoch millis the snapshot was taken, for the file name and for human reference. */
    val exportedAt: Long,
    /** `BuildConfig.VERSION_NAME` of the exporting build; informational, never validated. */
    val appVersion: String,
    val devices: List<ShellDevice> = emptyList(),
    val hosts: List<HostConfig> = emptyList(),
    // AppSettings is a plain data class in the connection layer (it is persisted field by field
    // into DataStore and therefore carries no generated serializer), so its exported shape is
    // spelled out by AppSettingsSerializer below.
    @Serializable(with = AppSettingsSerializer::class)
    val settings: AppSettings = AppSettings(),
    /** Self-hosted ntfy topic, when the user configured one. Empty means "not configured". */
    val ntfy: String? = null,
    /** UnifiedPush endpoint, when a distributor handed one out. */
    val unifiedPush: String? = null,
) {
    companion object {
        /** The only schema version this build writes, and the only one it reads. */
        const val SCHEMA: Int = 1

        /**
         * Encode [bundle] as indented JSON.
         *
         * Pretty-printed on purpose: an export is a file a person opens, reads and sometimes edits
         * by hand, so it is not the compact wire form even though it is built by [WireJson]. The
         * indentation pass runs on the already-built tree, which keeps every encode decision
         * (defaults written, explicit nulls omitted) exactly [WireJson]'s.
         */
        fun encode(bundle: SettingsBundle): String = PrettyJson.encodeToString(
            JsonElement.serializer(),
            WireJson.encodeToJsonElement(serializer(), bundle),
        )

        /**
         * Decode and validate an exported bundle.
         *
         * Never throws: unparseable text, a non-object root, an unknown [SCHEMA] and a record that
         * does not match the format all come back as [BundleResult.Invalid] with a reason. Invalid
         * *entries* are a different matter — one device with a non-HTTP URL must not cost the user
         * the other nine, so those are skipped and counted in [BundleResult.Ok.dropped] instead.
         *
         * The returned bundle is the sanitized one: it holds only the entries that passed, with
         * surrounding whitespace trimmed from the fields that are validated.
         */
        fun decode(json: String): BundleResult {
            val root = runCatching { WireJson.parseToJsonElement(json) }.getOrNull() as? JsonObject
                ?: return BundleResult.Invalid(BundleError.NotJson, "not a JSON object")
            val declared = (root["schema"] as? JsonPrimitive)?.contentOrNull?.trim()
            if (declared != null && declared.toIntOrNull() != SCHEMA) {
                return BundleResult.Invalid(BundleError.SchemaMismatch, "schema=$declared")
            }
            val decoded = runCatching { WireJson.decodeFromString(serializer(), json) }
            val bundle = decoded.getOrNull()
                ?: return BundleResult.Invalid(
                    BundleError.NotJson,
                    decoded.exceptionOrNull()?.message ?: "unreadable bundle",
                )

            val devices = bundle.devices.mapNotNull { device ->
                val url = device.url.trim()
                if (device.id.isBlank() || !hasHttpScheme(url)) null else device.copy(url = url)
            }
            val hosts = bundle.hosts.mapNotNull { host ->
                val address = host.host.trim()
                // A port outside the TCP range is not a harness this app can ever reach; an empty
                // id or address is a record the host list and the credential store are keyed on.
                if (host.id.isBlank() || address.isEmpty() || host.port !in MIN_PORT..MAX_PORT) {
                    null
                } else {
                    host.copy(host = address)
                }
            }
            return BundleResult.Ok(
                bundle = bundle.copy(devices = devices, hosts = hosts),
                droppedDevices = bundle.devices.size - devices.size,
                droppedHosts = bundle.hosts.size - hosts.size,
            )
        }
    }
}

/** What [SettingsBundle.decode] found. */
internal sealed interface BundleResult {
    /**
     * A usable snapshot. [bundle] holds only the valid devices and hosts; [droppedDevices] and
     * [droppedHosts] say how many were skipped, so the UI can tell the user instead of silently
     * importing less than the file contained.
     */
    data class Ok(
        val bundle: SettingsBundle,
        val droppedDevices: Int = 0,
        val droppedHosts: Int = 0,
    ) : BundleResult {
        /** Total skipped entries, for a one-line message. */
        val dropped: Int get() = droppedDevices + droppedHosts
    }

    /** Nothing to import. [reason] is machine-readable; [detail] is for logs, not for the user. */
    data class Invalid(val reason: BundleError, val detail: String? = null) : BundleResult
}

/** Why [SettingsBundle.decode] refused a file. */
internal enum class BundleError {
    /** The text is not a JSON object, or does not carry the fields the format requires. */
    NotJson,

    /** The file declares a schema version this build does not know (newer or older app). */
    SchemaMismatch,
}

/** A bundle stamped with this moment. The export UI only has to supply what it knows. */
internal fun settingsBundleNow(
    appVersion: String,
    devices: List<ShellDevice>,
    hosts: List<HostConfig>,
    settings: AppSettings,
    ntfy: String? = null,
    unifiedPush: String? = null,
    now: Long = System.currentTimeMillis(),
): SettingsBundle = SettingsBundle(
    exportedAt = now,
    appVersion = appVersion,
    devices = devices,
    hosts = hosts,
    settings = settings,
    ntfy = ntfy?.trim()?.takeIf { it.isNotEmpty() },
    unifiedPush = unifiedPush?.trim()?.takeIf { it.isNotEmpty() },
)

private const val MIN_PORT = 1
private const val MAX_PORT = 65_535

/** [WireJson] plus indentation, used only to format an already-built tree. */
private val PrettyJson: Json = Json { prettyPrint = true }

private fun hasHttpScheme(url: String): Boolean =
    url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

/**
 * The exported shape of [AppSettings], field for field.
 *
 * [AppSettings] is a plain data class: the connection layer persists it field by field into
 * DataStore and it carries no generated serializer, so the export names its fields here. That
 * duplication is deliberate and one-directional — this is the *stable exported format*, which
 * should not start changing shape whenever the in-app settings record grows a field. Every field
 * defaults to the value [AppSettings] itself defaults to, so a file written by an older build (or
 * hand-edited with a key removed) still imports, and a key this build does not know is ignored by
 * [WireJson]'s `ignoreUnknownKeys`.
 */
@Serializable
internal data class AppSettingsFields(
    val autoConnectLast: Boolean = true,
    val autoConnectLan: Boolean = false,
    val autoConnectLoopback: Boolean = true,
    val autoStartLoopbackHarness: Boolean = false,
    val autoConnectRelay: Boolean = false,
    val keepConnectedInBackground: Boolean = false,
    val notifyTurnComplete: Boolean = true,
    val notifyGoal: Boolean = true,
    val notifyNeedsAction: Boolean = true,
    val connectMode: String = ConnectMode.GATEWAY,
    val themePreference: String = "system",
    val showCodeWorkView: Boolean = true,
    val chatRenderer: String = "native",
    val localeOverride: String? = null,
    val knownPorts: List<Int> = listOf(3080),
    val updateCheckEnabled: Boolean = false,
    val dismissedUpdate: String? = null,
    val glassCompat: String = "auto",
    val simpleMode: Boolean = true,
)

/**
 * Bridge between the exported [AppSettingsFields] and the in-app [AppSettings].
 *
 * Kotlin serialization needs the serializers of every property of a `@Serializable` class, and
 * [AppSettings] has none, so [SettingsBundle.settings] is annotated with this. Both directions are
 * written out by hand and the compiler checks them: a field added to [AppSettings] without a
 * default shows up here as a missing argument.
 */
internal object AppSettingsSerializer : KSerializer<AppSettings> {
    override val descriptor: SerialDescriptor = AppSettingsFields.serializer().descriptor

    override fun serialize(encoder: Encoder, value: AppSettings) {
        encoder.encodeSerializableValue(AppSettingsFields.serializer(), value.toExportFields())
    }

    override fun deserialize(decoder: Decoder): AppSettings =
        decoder.decodeSerializableValue(AppSettingsFields.serializer()).toAppSettings()
}

private fun AppSettings.toExportFields(): AppSettingsFields = AppSettingsFields(
    autoConnectLast = autoConnectLast,
    autoConnectLan = autoConnectLan,
    autoConnectLoopback = autoConnectLoopback,
    autoStartLoopbackHarness = autoStartLoopbackHarness,
    autoConnectRelay = autoConnectRelay,
    keepConnectedInBackground = keepConnectedInBackground,
    notifyTurnComplete = notifyTurnComplete,
    notifyGoal = notifyGoal,
    notifyNeedsAction = notifyNeedsAction,
    connectMode = connectMode,
    themePreference = themePreference,
    showCodeWorkView = showCodeWorkView,
    chatRenderer = chatRenderer,
    localeOverride = localeOverride,
    knownPorts = knownPorts,
    updateCheckEnabled = updateCheckEnabled,
    dismissedUpdate = dismissedUpdate,
    glassCompat = glassCompat,
    simpleMode = simpleMode,
)

private fun AppSettingsFields.toAppSettings(): AppSettings = AppSettings(
    autoConnectLast = autoConnectLast,
    autoConnectLan = autoConnectLan,
    autoConnectLoopback = autoConnectLoopback,
    autoStartLoopbackHarness = autoStartLoopbackHarness,
    autoConnectRelay = autoConnectRelay,
    keepConnectedInBackground = keepConnectedInBackground,
    notifyTurnComplete = notifyTurnComplete,
    notifyGoal = notifyGoal,
    notifyNeedsAction = notifyNeedsAction,
    connectMode = connectMode,
    themePreference = themePreference,
    showCodeWorkView = showCodeWorkView,
    chatRenderer = chatRenderer,
    localeOverride = localeOverride,
    knownPorts = knownPorts,
    updateCheckEnabled = updateCheckEnabled,
    dismissedUpdate = dismissedUpdate,
    glassCompat = glassCompat,
    simpleMode = simpleMode,
)
