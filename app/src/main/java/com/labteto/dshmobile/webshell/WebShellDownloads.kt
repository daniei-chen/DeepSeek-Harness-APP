package com.labteto.dshmobile.webshell

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import androidx.annotation.RequiresApi
import com.labteto.dshmobile.R
import java.io.File
import java.net.URLDecoder

/**
 * Files the web client hands to the shell.
 *
 * The harness UI downloads through `blob:` URLs (and occasionally through a plain link), neither of
 * which a bare WebView can save by itself. Blobs arrive here as base64 from the page-side shim and
 * land in the public Downloads collection; plain links are delegated to Android's download manager
 * with the gateway cookie attached, because every `/api` download sits behind the access gate.
 */
internal object WebShellDownloads {

    private const val FOLDER = "DeepSeek Harness"
    private const val MAX_NAME_LENGTH = 120
    private val UNSAFE_NAME = Regex("[\\u0000-\\u001f<>:\"|?*]")
    private val DISPOSITION_NAME = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)

    /** Save bytes produced by the page (`FileReader.readAsDataURL`) into Downloads. */
    suspend fun saveBase64(context: Context, rawName: String, rawMime: String, base64: String): String? {
        val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        val mime = rawMime.takeIf { it.length < 100 && it.contains('/') } ?: "application/octet-stream"
        val fileName = safeFileName(rawName, extensionFor(mime))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(context, fileName, mime, bytes)
        } else {
            // API 26-28 would need WRITE_EXTERNAL_STORAGE for the public Downloads collection. The
            // fork never asks for that dangerous permission, so old devices get the app-scoped
            // Downloads folder, which the system file picker can still reach.
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val directory = File(base, FOLDER).apply { mkdirs() }
            val file = File(directory, fileName)
            runCatching {
                file.writeBytes(bytes)
                file.absolutePath
            }.getOrNull()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(context: Context, fileName: String, mime: String, bytes: ByteArray): String? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        val written = runCatching {
            resolver.openOutputStream(uri).use { stream -> stream?.write(bytes) }
        }.isSuccess
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/$fileName"
    }

    /** Enqueue a plain http(s) download through the system download manager. */
    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        mimeType: String?,
        contentDisposition: String?,
    ): String? {
        val fileName = safeFileName(
            dispositionFileName(contentDisposition) ?: urlFileName(url) ?: "download",
            extensionFor(mimeType.orEmpty()),
        )
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription(context.getString(R.string.webshell_downloading))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            mimeType?.takeIf { it.contains('/') }?.let { setMimeType(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$FOLDER/$fileName")
            } else {
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "$FOLDER/$fileName")
            }
            if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
            CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
        }
        return runCatching {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            fileName
        }.getOrNull()
    }

    private fun dispositionFileName(value: String?): String? = value
        ?.let { DISPOSITION_NAME.find(it)?.groupValues?.getOrNull(1) }
        ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }

    private fun urlFileName(url: String): String? = runCatching {
        Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun extensionFor(mime: String): String = when (mime.lowercase().substringBefore(';').trim()) {
        "text/markdown" -> "md"
        "text/plain" -> "txt"
        "text/csv" -> "csv"
        "application/json" -> "json"
        "application/pdf" -> "pdf"
        "application/zip" -> "zip"
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "bin"
    }

    private fun safeFileName(raw: String, fallbackExtension: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base.replace(UNSAFE_NAME, "_").trim().take(MAX_NAME_LENGTH)
        // `..` would escape the directory when the API 26-28 branch writes into an app folder.
        val candidate = cleaned.takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "dsh-download-${System.currentTimeMillis()}"
        return if (candidate.contains('.')) candidate else "$candidate.$fallbackExtension"
    }
}
