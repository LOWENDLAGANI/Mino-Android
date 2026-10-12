package com.minetallest.mino.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.minetallest.mino.domain.ChatMessage
import com.minetallest.mino.domain.Role
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

/** Clipboard + Android share sheet helpers (spec §6.6, §10.10). */
object Share {

    fun copyToClipboard(context: Context, label: String, text: String): Boolean = try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    } catch (e: Throwable) {
        false
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun shareImage(context: Context, uri: android.net.Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    /** `You: …` / `Mino: …` blocks joined by blank lines (spec §6.6 "Copy chat"). */
    fun chatToPlainText(messages: List<ChatMessage>): String =
        messages.filter { it.role != Role.SYSTEM && it.displayedContent().isNotBlank() }
            .joinToString("\n\n") { message ->
                val speaker = if (message.role == Role.USER) "You" else "Mino"
                "$speaker: ${message.displayedContent()}"
            }

    /**
     * Share-as-link (spec §10.10): conversation compressed into a URL fragment the server
     * never sees. Android emits the app's own deflate/base64url envelope (documented
     * deviation — the web `/share` page uses its own JS decompressor).
     */
    fun shareLink(origin: String, title: String, messages: List<ChatMessage>, maxMessages: Int = 40): String? {
        if (origin.isBlank()) return null
        val usable = messages.filter { it.role != Role.SYSTEM && it.displayedContent().isNotBlank() }
        val taken = usable.takeLast(maxMessages)
        val json = buildString {
            append("{\"t\":")
            append(jsonString(title))
            append(",\"m\":[")
            taken.forEachIndexed { index, message ->
                if (index > 0) append(',')
                append("{\"r\":").append(jsonString(message.role.wire))
                    .append(",\"x\":").append(jsonString(message.displayedContent()))
                    .append('}')
            }
            append("]}")
        }
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(json.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val buffer = ByteArray(64 * 1024)
        val out = ByteArrayOutputStream()
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            out.write(buffer, 0, count)
        }
        deflater.end()
        val encoded = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
        return origin.trimEnd('/') + "/share#m=" + encoded
    }

    fun shareSummary(taken: Int, total: Int): String =
        "Link copied · latest $taken of $total messages"

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }
}
