package com.minetallest.mino.net

import com.minetallest.mino.domain.ChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject

/**
 * `POST /api/chat` over SSE (spec §9.1). Parses `data: ` lines identically to the web
 * build; cancel = `Call.cancel()` (the Android form of `AbortController`).
 */
class SseChatClient(private val api: ApiClient) {

    class Handle(private val call: Call) {
        @Volatile
        var finished: Boolean = false
            internal set

        @Volatile
        var canceled: Boolean = false
            internal set

        fun cancel() {
            canceled = true
            if (!finished) call.cancel()
        }
    }

    class StreamResult(
        val events: Flow<ChatEvent>,
        val handle: Handle,
    )

    suspend fun start(request: JsonObject): StreamResult {
        val httpRequest = api.postJson("/api/chat", request)
        val call = api.http.newCall(httpRequest)
        val handle = Handle(call)
        val events = flow {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        emit(ChatEvent.Error(parseFailure(response)))
                        return@use
                    }
                    emitEvents(response, ::emit)
                }
            } catch (e: Exception) {
                if (!handle.finished) {
                    emit(ChatEvent.Error(NETWORK_ERROR))
                }
            } finally {
                handle.finished = true
            }
        }.flowOn(Dispatchers.IO)
        return StreamResult(events, handle)
    }

    private suspend fun emitEvents(response: Response, emit: suspend (ChatEvent) -> Unit) {
        val body = response.body ?: return
        val source = body.source()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            val trimmed = line.removeSuffix("\r")
            if (!trimmed.startsWith("data:")) continue
            val payload = trimmed.substring(5).removePrefix(" ")
            val event = com.minetallest.mino.domain.SseEvents.parsePayload(payload) ?: continue
            emit(event)
            if (event == ChatEvent.Done) break
        }
    }

    private fun parseFailure(response: Response): String {
        val text = try {
            response.body?.string().orEmpty()
        } catch (e: Exception) {
            ""
        }
        val serverMessage = try {
            JSONObject(text).optString("error").ifBlank { null }
        } catch (e: Exception) {
            null
        }
        return serverMessage ?: when (response.code) {
            400 -> "Mino could not understand that request. Try again."
            402 -> com.minetallest.mino.domain.Refusals.PAUSED_PLAN
            403 -> "Mino refused that request."
            404 -> "Mino's chat service was not found on this server."
            429 -> "Too many messages at once. Please wait a moment and try again."
            503 -> "Mino's chat service is not set up on this server yet."
            else -> "Mino could not reach the server (HTTP ${response.code})."
        }
    }

    companion object {
        const val NETWORK_ERROR =
            "Mino could not reach the server. Check the server address in Settings → Server."
    }
}
