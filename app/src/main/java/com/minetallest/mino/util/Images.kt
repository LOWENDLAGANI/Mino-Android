package com.minetallest.mino.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import com.minetallest.mino.domain.DocumentAttachment
import com.minetallest.mino.domain.ImageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Spec §10.9 — images compressed client-side: max side 1024, JPEG q0.8, stored as data URLs.
 * Documents: text/code only, ≤100 KB, ≤3, truncated flag when clipped.
 */
object Images {

    const val MAX_SIDE = 1024
    const val JPEG_QUALITY = 80
    const val MAX_IMAGES = 4
    const val MAX_DOCUMENTS = 3
    const val MAX_DOCUMENT_BYTES = 524_288_000  // 500 MB

    private val docMimeTypes = setOf(
        "text/plain", "text/markdown", "text/csv", "application/json",
        "application/javascript", "text/typescript", "text/x-python",
        "text/html", "text/css", "text/sql",
    )
    private val docExtensions = setOf("txt", "md", "csv", "json", "js", "jsx", "ts", "tsx", "py", "html", "css", "sql")

    /** Compress image on IO thread to avoid blocking the main thread. */
    suspend fun compressToDataUrl(context: Context, uri: Uri): ImageAttachment? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap: Bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val scale = MAX_SIDE.toFloat() / maxOf(info.size.width, info.size.height).toFloat()
                        if (scale < 1f) {
                            decoder.setTargetSize(
                                (info.size.width * scale).toInt().coerceAtLeast(1),
                                (info.size.height * scale).toInt().coerceAtLeast(1),
                            )
                        }
                    }.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    decodeScaled(context, uri) ?: return@withContext null
                }
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                if (!bitmap.isRecycled) bitmap.recycle()
                val bytes = output.toByteArray()
                val name = queryDisplayName(context, uri) ?: "image.jpg"
                ImageAttachment(
                    url = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP),
                    name = name,
                    size = bytes.size.toLong(),
                )
            } catch (e: Throwable) {
                null
            }
        }
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (maxOf(w, h) / 2 >= MAX_SIDE) {
            sample *= 2
            w /= 2
            h /= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val scale = MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
                .also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    } catch (e: Throwable) {
        null
    }

    sealed class DocResult {
        data class Ok(val documents: List<DocumentAttachment>) : DocResult()
        data class Error(val message: String) : DocResult()
    }

    /** Reads text/code documents on IO thread to avoid blocking the main thread. */
    suspend fun readDocuments(context: Context, uris: List<Uri>, alreadyHave: Int): DocResult {
        return withContext(Dispatchers.IO) {
        val out = ArrayList<DocumentAttachment>()
        for (uri in uris) {
            if (out.size + alreadyHave >= MAX_DOCUMENTS) {
                return DocResult.Error("Max $MAX_DOCUMENTS files per message")
            }
            val name = queryDisplayName(context, uri) ?: "file.txt"
            val extension = name.substringAfterLast('.', "").lowercase()
            val mime = context.contentResolver.getType(uri)?.lowercase()
            val allowed = (mime != null && mime in docMimeTypes) || extension in docExtensions
            if (!allowed) {
                return DocResult.Error("Text and code files are supported (up to 500 MB each)")
            }
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
            } catch (e: Throwable) {
                return DocResult.Error("Could not read $name")
            }
            if (bytes.size > MAX_DOCUMENT_BYTES) {
                return DocResult.Error("$name is larger than 500 MB")
            }
            val text = bytes.toString(Charsets.UTF_8)
            out.add(
                DocumentAttachment(
                    name = name,
                    size = bytes.size.toLong(),
                    text = text,
                    truncated = false,
                ),
            )
        }
        return DocResult.Ok(out)
        }
    }
}
