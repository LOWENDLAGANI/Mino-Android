package com.minetallest.mino.util

import android.content.Context
import android.speech.tts.TextToSpeech
import com.minetallest.mino.domain.Markdown
import java.util.Locale

/** Spec §10.8 — read aloud: one message at a time, code blocks announced, links stripped. */
class TtsManager(context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false

    /** id of the message currently speaking, or null. */
    @Volatile
    var speakingMessageId: String? = null
        private set

    var supported: Boolean = false
        private set

    var onSpeakingChanged: ((String?) -> Unit)? = null

    init {
        engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            supported = ready
            onSpeakingChanged?.invoke(speakingMessageId)
        }
    }

    fun speakOrStop(messageId: String, markdown: String) {
        val tts = engine ?: return
        if (!ready) return
        if (speakingMessageId == messageId) {
            stop()
            return
        }
        val text = Markdown.toSpeech(markdown)
        if (text.isBlank()) return
        tts.language = Locale.getDefault()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mino-$messageId")
        speakingMessageId = messageId
        onSpeakingChanged?.invoke(messageId)
    }

    fun stop() {
        engine?.stop()
        if (speakingMessageId != null) {
            speakingMessageId = null
            onSpeakingChanged?.invoke(null)
        }
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
