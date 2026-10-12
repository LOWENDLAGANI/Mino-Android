package com.minetallest.mino.net

import com.minetallest.mino.auth.IdentityProvider
import com.minetallest.mino.util.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Shared JSON config + HTTP client (spec §9: Bearer token on every call when identified). */
class ApiClient(
    private val identity: IdentityProvider,
    baseUrlProvider: suspend () -> String,
) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val baseUrlProvider = baseUrlProvider

    suspend fun baseUrl(): String = baseUrlProvider().trimEnd('/')

    suspend fun isConfigured(): Boolean = baseUrl().isNotBlank()

    /** Builds a request with the identity header; null token = unidentified (spec §4.2). */
    suspend fun requestBuilder(path: String): Request.Builder {
        val base = baseUrl()
        val builder = Request.Builder().url(if (path.startsWith("http")) path else base + path)
        identity.idTokenOrNull()?.let { builder.header("Authorization", "Bearer $it") }
        val secHeaders = SecurityGuard.buildAuthHeaders(path)
        secHeaders.forEach { (k, v) -> builder.header(k, v) }
        return builder
    }

    suspend fun postJson(path: String, body: JsonObject): Request {
        return requestBuilder(path)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    suspend fun putJson(path: String, body: JsonObject): Request {
        return requestBuilder(path)
            .put(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    /** Executes a request off the main thread and returns (code, body) — admin routes (spec §9.6). */
    suspend fun execute(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { it.code to (it.body?.string() ?: "") }
        } catch (e: Exception) {
            0 to ""
        }
    }

    /** GET returning parsed JSON, or null when the server is unreachable/misconfigured. */
    suspend fun getJsonObject(path: String): JsonObject? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val response = http.newCall(requestBuilder(path).get().build()).execute()
            response.use {
                val text = it.body?.string() ?: return@use null
                if (!it.isSuccessful) return@use null
                json.parseToJsonElement(text) as? JsonObject
            }
        } catch (e: Exception) {
            null
        }
    }
}
