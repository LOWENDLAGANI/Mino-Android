package com.minetallest.mino.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.minetallest.mino.domain.Markdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Read aloud: one message at a time, code blocks announced, links stripped. */
class TtsManager(context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false

    private val _available = MutableStateFlow(false)
    /** Observable engine readiness so the UI can reveal the read-aloud action. */
    val available: StateFlow<Boolean> = _available.asStateFlow()

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
            _available.value = ready
            if (ready) {
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        clearSpeaking()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        clearSpeaking()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        clearSpeaking()
                    }
                })
            }
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
        speakingMessageId = messageId
        onSpeakingChanged?.invoke(messageId)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mino-$messageId")
    }

    fun stop() {
        engine?.stop()
        clearSpeaking()
    }

    private fun clearSpeaking() {
        if (speakingMessageId != null) {
            speakingMessageId = null
            onSpeakingChanged?.invoke(null)
        }
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        _available.value = false
    }
}
