package com.labteto.dshmobile.ui.media

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Fork: on-device OCR for the "photo to text" flow.
 *
 * The Chinese recognizer is used for the personal build because its test corpus is Chinese; the
 * bundled model also carries Latin script, so mixed notes work. Everything runs locally — the photo
 * never leaves the phone for recognition.
 */
suspend fun recognizeImageText(context: Context, uri: Uri): String =
    suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (error: Exception) {
            runCatching { recognizer.close() }
            continuation.resumeWithException(error)
            return@suspendCancellableCoroutine
        }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                runCatching { recognizer.close() }
                continuation.resume(result.text)
            }
            .addOnFailureListener { error ->
                runCatching { recognizer.close() }
                continuation.resumeWithException(error)
            }
        continuation.invokeOnCancellation { runCatching { recognizer.close() } }
    }
