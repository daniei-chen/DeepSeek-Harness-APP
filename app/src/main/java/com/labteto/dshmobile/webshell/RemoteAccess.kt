package com.labteto.dshmobile.webshell

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.labteto.dshmobile.core.wire.RelayPairingPayload
import com.labteto.dshmobile.core.wire.WireJson

/**
 * The one place the shell turns a relay invitation into something a camera can read.
 *
 * The payload is written with the same serializer the scanner reads, so a QR drawn here and a QR
 * drawn by the relay's own page are byte-for-byte the same JSON — a device paired from either one
 * ends up with the same record.
 */
internal object RemoteAccess {

    /** Serialize an invitation exactly as `dsh-relay` does before it renders its own QR. */
    fun encode(payload: RelayPairingPayload): String =
        WireJson.encodeToString(RelayPairingPayload.serializer(), payload)

    /**
     * Draw [text] as a QR code.
     *
     * Error correction stays at M — the level the relay itself uses — so a code photographed off a
     * phone screen at an angle still decodes. A one-module quiet zone is enough because the view
     * that shows it adds its own white margin.
     */
    fun qrBitmap(text: String, sizePx: Int): Bitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                pixels[row + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()

    /** `4:32` — the time left before a code stops being claimable. */
    fun remaining(expiresAt: Long, now: Long = System.currentTimeMillis()): String {
        val seconds = ((expiresAt - now) / 1_000).coerceAtLeast(0)
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
