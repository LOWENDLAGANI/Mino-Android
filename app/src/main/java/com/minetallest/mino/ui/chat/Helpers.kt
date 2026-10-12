package com.minetallest.mino.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.minetallest.mino.domain.ChatMessage
import com.minetallest.mino.domain.Role
import com.minetallest.mino.util.TtsManager
import kotlinx.coroutines.delay

/** One TTS engine for the whole app — only one message speaks at a time (spec §10.8). */
object TtsHolder {
    @Volatile
    private var manager: TtsManager? = null

    fun get(context: Context): TtsManager =
        manager ?: synchronized(this) {
            manager ?: TtsManager(context.applicationContext).also { manager = it }
        }
}

@Composable
fun rememberThreadTts(): TtsManager = TtsHolder.get(LocalContext.current)

/** Decodes a base64 data URL into a bitmap (images stay local, spec §10.9). */
@Composable
fun rememberBitmapFromDataUrl(url: String): Bitmap? {
    return remember(url) {
        try {
            val bytes = Base64.decode(url.substringAfter("base64,"), Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * Smooth streaming reveal (port of `useSmoothText`): steady progress while streaming;
 * finished answers are never held back.
 */
@Composable
fun rememberSmoothText(target: String, active: Boolean): String {
    var length by remember { mutableIntStateOf(0) }
    LaunchedEffect(active, target) {
        if (!active) {
            length = target.length
            return@LaunchedEffect
        }
        while (length < target.length) {
            delay(20)
            val step = ((target.length - length) / 5).coerceIn(2, 40)
            length = (length + step).coerceAtMost(target.length)
        }
    }
    if (!active) return target
    return target.take(length.coerceAtMost(target.length))
}

/** Renders the conversation to a PNG (spec §6.6 "Share image"). */
fun renderConversation(context: Context, messages: List<ChatMessage>): Bitmap {
    val width = 900
    val padding = 36f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(205, 223, 212)
        textSize = 28f
    }
    val speakerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(110, 194, 148)
        textSize = 26f
        isFakeBoldText = true
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(238, 245, 240)
        textSize = 36f
        isFakeBoldText = true
    }
    val lineCount = { paint: Paint, text: String ->
        var lines = 0
        val available = (width - padding * 2).toInt()
        text.split('\n').forEach { paragraph ->
            if (paragraph.isBlank()) {
                lines += 1
            } else {
                var rest = paragraph
                while (rest.isNotEmpty()) {
                    val measured = paint.breakText(rest, true, available.toFloat(), null)
                    if (measured <= 0) break
                    rest = rest.substring(measured)
                    lines += 1
                }
            }
        }
        lines
    }

    val usable = messages.filter { it.role != Role.SYSTEM && it.displayedContent().isNotBlank() }
    var height = padding + 60f + 24f
    val blocks = usable.map { message ->
        val speaker = if (message.role == Role.USER) "You" else "Mino"
        val body = message.displayedContent()
        val lines = lineCount(textPaint, body)
        val blockHeight = 44f + lines * 40f + 26f
        Triple(speaker, body, blockHeight)
    }
    blocks.forEach { height += it.third }
    height += padding
    val safeHeight = height.coerceAtMost(4200f).toInt()

    val bitmap = Bitmap.createBitmap(width, safeHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(11, 19, 16))
    var y = padding + 44f
    canvas.drawText("Mino conversation", padding, y, titlePaint)
    y += 60f
    canvas.drawLine(padding, y, width - padding, y, Paint().apply {
        color = android.graphics.Color.rgb(40, 58, 48)
        strokeWidth = 2f
    })
    y += 40f
    for ((speaker, body, _) in blocks) {
        if (y > safeHeight - 120f) break
        canvas.drawText(speaker, padding, y, speakerPaint)
        y += 40f
        val available = (width - padding * 2).toInt()
        body.split('\n').forEach { paragraph ->
            var rest = paragraph
            if (rest.isBlank()) {
                y += 30f
                return@forEach
            }
            while (rest.isNotEmpty()) {
                if (y > safeHeight - 80f) break
                val measured = textPaint.breakText(rest, true, available.toFloat(), null)
                if (measured <= 0) break
                canvas.drawText(rest.substring(0, measured), padding, y, textPaint)
                rest = rest.substring(measured)
                y += 40f
            }
        }
        y += 26f
    }
    return bitmap
}
