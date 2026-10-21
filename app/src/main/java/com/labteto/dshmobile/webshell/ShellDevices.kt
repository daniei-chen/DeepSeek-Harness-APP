package com.labteto.dshmobile.webshell

import android.content.Context
import com.labteto.dshmobile.core.wire.WireJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

/** One address the WebView shell can switch to, with the name the person gave it. */
@Serializable
internal data class ShellDevice(
    val id: String,
    val name: String,
    val url: String,
)

/**
 * The device list behind the back-button manager.
 *
 * This is deliberately separate from `HostsStore`: hosts describe how the *native* client reaches a
 * harness (gateway / LAN / relay), while this list is only "which URL should the WebView load".
 * Keeping it in plain SharedPreferences means the manager works even before any native RPC
 * connection has ever been made.
 */
internal class ShellDevices(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val serializer = ListSerializer(ShellDevice.serializer())

    fun load(): List<ShellDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching { WireJson.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    fun currentId(): String? = prefs.getString(KEY_CURRENT, null)

    fun setCurrent(id: String) {
        prefs.edit().putString(KEY_CURRENT, id).apply()
    }

    /** Ensure there is at least the current address as a named device, and return the full list. */
    fun ensure(url: String): List<ShellDevice> {
        val existing = load()
        if (existing.isNotEmpty()) {
            if (currentId() == null || existing.none { it.id == currentId() }) {
                setCurrent(existing.first().id)
            }
            return existing
        }
        val device = ShellDevice(UUID.randomUUID().toString(), defaultName(0), url)
        save(listOf(device))
        setCurrent(device.id)
        return listOf(device)
    }

    fun add(url: String): ShellDevice = add(url, null)

    fun add(url: String, name: String?): ShellDevice {
        val existing = load()
        val device = ShellDevice(
            UUID.randomUUID().toString(),
            name?.trim()?.takeIf { it.isNotEmpty() } ?: defaultName(existing.size),
            url,
        )
        save(existing + device)
        setCurrent(device.id)
        return device
    }

    fun rename(id: String, name: String): ShellDevice? {
        val existing = load()
        val renamed = existing.map { if (it.id == id) it.copy(name = name) else it }
        save(renamed)
        return renamed.firstOrNull { it.id == id }
    }

    fun remove(id: String): List<ShellDevice> {
        val remaining = load().filterNot { it.id == id }
        save(remaining)
        if (currentId() == id) {
            remaining.firstOrNull()?.let { setCurrent(it.id) } ?: prefs.edit().remove(KEY_CURRENT).apply()
        }
        return remaining
    }

    private fun save(devices: List<ShellDevice>) {
        prefs.edit().putString(KEY_DEVICES, WireJson.encodeToString(serializer, devices)).apply()
    }

    private val defaultNames = listOf("设备一", "设备二", "设备三", "设备四", "设备五", "设备六", "设备七", "设备八", "设备九", "设备十")

    private fun defaultName(index: Int): String = defaultNames.getOrElse(index) { "设备${index + 1}" }

    private companion object {
        const val PREFS = "shell_devices"
        const val KEY_DEVICES = "devices_json"
        const val KEY_CURRENT = "current_id"
    }
}
