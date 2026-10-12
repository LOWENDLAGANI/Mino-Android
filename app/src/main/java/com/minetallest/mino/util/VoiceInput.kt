package com.minetallest.mino.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.minetallest.mino.domain.Refusals

/**
 * Spec §6.7 voice input — single utterance, non-interim, appends `" " + transcript`.
 * Unavailable → [Refusals.VOICE_UNSUPPORTED]; failure → [Refusals.VOICE_FAILED].
 */
class VoiceInput(private val context: Context) {

    var supported: Boolean = SpeechRecognizer.isRecognitionAvailable(context)
        private set

    private var recognizer: SpeechRecognizer? = null

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onBeginning: () -> Unit = {},
        onEnd: () -> Unit = {},
    ) {
        if (!supported) {
            onError(Refusals.VOICE_UNSUPPORTED)
            return
        }
        try {
            stop()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { this.recognizer = it }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = onBeginning()
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() = onEnd()
                override fun onError(error: Int) {
                    val message =
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_NO_MATCH) {
                            Refusals.VOICE_FAILED
                        } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            Refusals.VOICE_UNSUPPORTED
                        } else {
                            Refusals.VOICE_FAILED
                        }
                    onError(message)
                }

                override fun onResults(results: Bundle?) {
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (transcript.isNotEmpty()) onResult(transcript) else onError(Refusals.VOICE_FAILED)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(intent)
        } catch (e: Throwable) {
            onError(Refusals.VOICE_FAILED)
        }
    }

    fun stop() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (e: Throwable) {
        }
        recognizer = null
    }
}
