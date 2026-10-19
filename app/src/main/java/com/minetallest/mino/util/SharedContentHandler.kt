package com.minetallest.mino.util

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.minetallest.mino.domain.DocumentAttachment
import com.minetallest.mino.domain.ImageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.Deflater

/**
 * Handles incoming shared content from Android's share sheet.
 * Supports text, images, and files up to 500MB.
 */
object SharedContentHandler {

    data class SharedContent(
        val text: String? = null,
        val images: List<ImageAttachment> = emptyList(),
        val documents: List<DocumentAttachment> = emptyList(),
        val errorMessage: String? = null,
    ) {
        /** Preview text for the share confirmation dialog */
        fun previewText(): String {
            val parts = mutableListOf<String>()
            text?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            images.forEach { parts.add("🖼️ ${it.name}") }
            documents.forEach { doc ->
                val fileInfo = "📄 ${doc.name} (${formatFileSize(doc.size)})"
                parts.add(fileInfo)
            }
            return parts.joinToString("\n")
        }

        companion object {
            private fun formatFileSize(bytes: Long): String {
                return when {
                    bytes < 1024 -> "$bytes B"
                    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                    else -> "${bytes / (1024 * 1024 * 1024)} GB"
                }
            }
        }
    }

    private const val MAX_FILE_BYTES = 524_288_000L  // 500 MB
    private const val MAX_IMAGES = 4
    private const val MAX_DOCUMENTS = 3

    private val textMimeTypes = setOf(
        "text/plain", "text/markdown", "text/csv", "application/json",
        "application/javascript", "text/typescript", "text/x-python",
        "text/html", "text/css", "text/sql", "text/xml",
    )

    private val documentExtensions = setOf(
        "txt", "md", "csv", "json", "js", "jsx", "ts", "tsx", "py", "html",
        "css", "sql", "xml", "yaml", "yml", "toml", "ini", "cfg", "log",
        "sh", "bash", "gitignore", "env", "properties",
    )

    /**
     * Process an incoming share intent and extract content.
     */
    suspend fun processShareIntent(context: android.content.Context, intent: Intent): SharedContent {
        return withContext(Dispatchers.IO) {
            when (intent.action) {
                Intent.ACTION_SEND -> handleSend(context, intent)
                Intent.ACTION_SEND_MULTIPLE -> handleSendMultiple(context, intent)
                else -> SharedContent(errorMessage = "Unknown share action")
            }
        }
    }

    private fun handleSend(context: android.content.Context, intent: Intent): SharedContent {
        var images: List<ImageAttachment> = emptyList()
        var documents: List<DocumentAttachment> = emptyList()
        var text: String? = null
        var errorMessage: String? = null

        // Extract text first
        text = intent.getStringExtra(Intent.EXTRA_TEXT)

        // Extract stream (file/image)
        val streamUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)

        if (streamUri != null) {
            val uri = streamUri
            val mimeType = intent.type?.lowercase() ?: context.contentResolver.getType(uri)?.lowercase()

            when {
                mimeType?.startsWith("image/") == true -> {
                    // Handle as image
                    val imageResult = compressImage(context, uri)
                    if (imageResult != null) {
                        images = listOf(imageResult)
                    } else {
                        errorMessage = "Could not process image"
                    }
                }
                mimeType?.startsWith("text/") == true || isTextFile(uri) -> {
                    // Handle as text document
                    val docResult = readDocument(context, uri)
                    if (docResult != null) {
                        documents = listOf(docResult)
                    } else {
                        errorMessage = "Could not read document"
                    }
                }
                else -> {
                    // Generic file - read as document
                    val docResult = readDocument(context, uri)
                    if (docResult != null) {
                        documents = listOf(docResult)
                    } else {
                        errorMessage = "Could not read file: ${getFileName(context, uri)}"
                    }
                }
            }
        } else if (text != null) {
            // Text only share
            text = text.trim().takeIf { it.isNotBlank() }
        }

        // If we have text but no attachments, include it
        if (text != null && images.isEmpty() && documents.isEmpty()) {
            return SharedContent(text = text)
        }

        SharedContent(
            text = text,
            images = images,
            documents = documents,
            errorMessage = errorMessage,
        )
    }

    private fun handleSendMultiple(context: android.content.Context, intent: Intent): SharedContent {
        var images: List<ImageAttachment> = emptyList()
        var documents: List<DocumentAttachment> = emptyList()
        var text: String? = null
        var errorMessage: String? = null

        // Extract text if present
        text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }

        // Extract multiple URIs
        val uris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        )?.filterNotNull() ?: emptyList()

        if (uris.isEmpty() && text == null) {
            return SharedContent(errorMessage = "No content to share")
        }

        for (uri in uris) {
            if (images.size + documents.size >= MAX_IMAGES + MAX_DOCUMENTS) {
                errorMessage = "Too many files (max ${MAX_IMAGES + MAX_DOCUMENTS})"
                break
            }

            val mimeType = intent.type?.lowercase() ?: context.contentResolver.getType(uri)?.lowercase()

            when {
                mimeType?.startsWith("image/") == true -> {
                    if (images.size < MAX_IMAGES) {
                        val imageResult = compressImage(context, uri)
                        if (imageResult != null) {
                            images = images + imageResult
                        }
                    }
                }
                mimeType?.startsWith("text/") == true || isTextFile(uri) -> {
                    if (documents.size < MAX_DOCUMENTS) {
                        val docResult = readDocument(context, uri)
                        if (docResult != null) {
                            documents = documents + docResult
                        }
                    }
                }
                else -> {
                    if (documents.size < MAX_DOCUMENTS) {
                        val docResult = readDocument(context, uri)
                        if (docResult != null) {
                            documents = documents + docResult
                        }
                    }
                }
            }
        }

        SharedContent(
            text = text,
            images = images,
            documents = documents,
            errorMessage = errorMessage,
        )
    }

    private fun compressImage(context: android.content.Context, uri: Uri): ImageAttachment? {
        return try {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val scale = Images.MAX_SIDE.toFloat() / maxOf(info.size.width, info.size.height).toFloat()
                    if (scale < 1f) {
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            } else {
                decodeScaled(context, uri) ?: return null
            }

            val output = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, Images.JPEG_QUALITY, output)
            if (!bitmap.isRecycled) bitmap.recycle()
            val bytes = output.toByteArray()
            val name = getFileName(context, uri) ?: "image.jpg"

            ImageAttachment(
                url = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP),
                name = name,
                size = bytes.size.toLong(),
            )
        } catch (e: Throwable) {
            null
        }
    }

    private fun decodeScaled(context: android.content.Context, uri: Uri): android.graphics.Bitmap? {
        val resolver = context.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (maxOf(w, h) / 2 >= Images.MAX_SIDE) {
            sample *= 2
            w /= 2
            h /= 2
        }

        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) } ?: return null
        val scale = Images.MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height).toFloat()

        return if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt(),
                (decoded.height * scale).toInt(),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }
    }

    private fun readDocument(context: android.content.Context, uri: Uri): DocumentAttachment? {
        return try {
            val name = getFileName(context, uri) ?: "file.txt"
            val extension = name.substringAfterLast('.', "").lowercase()
            val mime = context.contentResolver.getType(uri)?.lowercase()

            val allowed = (mime != null && mime in textMimeTypes) || extension in documentExtensions
            if (!allowed) {
                // Try to read as binary and convert to text
                null
            } else {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

                if (bytes.size > MAX_FILE_BYTES) {
                    null
                } else {
                    // For very large files, read first portion
                    val text = if (bytes.size > 1_000_000) {
                        // Read first ~1MB for preview
                        bytes.take(1_000_000).toString(Charsets.UTF_8) + "\n\n[File truncated - full file stored on device]"
                    } else {
                        bytes.toString(Charsets.UTF_8)
                    }

                    DocumentAttachment(
                        name = name,
                        size = bytes.size.toLong(),
                        text = text,
                        truncated = bytes.size > 1_000_000,
                    )
                }
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun isTextFile(uri: Uri): Boolean {
        val name = uri.lastPathSegment ?: return false
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in documentExtensions
    }

    private fun getFileName(context: android.content.Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (e: Throwable) {
            uri.lastPathSegment?.substringAfter('/')
        }
    }
}
