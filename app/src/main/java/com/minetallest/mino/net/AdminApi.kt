package com.minetallest.mino.net

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.tasks.await

/**
 * Admin console transport (spec §9.6, §12).
 *
 * Every write goes to the HTTP API with the caller's own token, so the security rules
 * make the final decision — this client never predicts whether it is the admin.
 */
class AdminApi(
    private val api: ApiClient,
    private val database: FirebaseDatabase?,
) {

    data class ModelTarget(val id: String, val name: String)

    /** `GET /api/admin/config` → `{ ok, isAdmin, config }`. */
    suspend fun readConfig(): Pair<Boolean, AppConfig?>? {
        val obj = api.getJsonObject("/api/admin/config") ?: return null
        val ok = obj["ok"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val isAdmin = obj["isAdmin"]?.jsonPrimitive?.content?.toBoolean() ?: false
        if (!ok || !isAdmin) return isAdmin to null
        val config = obj["config"]?.jsonObject ?: return isAdmin to null
        return isAdmin to AppConfig.parseStatic(config)
    }

    /** `PUT /api/admin/config` — admin only, written with the same token (spec §9.6). */
    suspend fun writeConfig(config: JsonObject): Boolean {
        val body = buildJsonObject { config.forEach { (k, v) -> put(k, v) } }
        val (code, _) = api.execute(api.putJson("/api/admin/config", body))
        return code in 200..299
    }

    /** `GET /api/admin/models` → opaque ids + Mino names only. */
    suspend fun models(): List<ModelTarget> {
        val obj = api.getJsonObject("/api/admin/models") ?: return emptyList()
        return try {
            (obj["models"] as? JsonArray)?.mapNotNull { item ->
                val o = item.jsonObject
                val id = o["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                ModelTarget(id = id, name = o["name"]?.jsonPrimitive?.content ?: id)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** `POST /api/admin/models { id }` — a real single-model probe (no fallback chain). */
    suspend fun probeModel(id: String): String {
        val body = buildJsonObject { put("id", id) }
        val (code, text) = api.execute(api.postJson("/api/admin/models", body))
        if (code == 0) return "The server could not be reached."
        return try {
            val obj = api.json.parseToJsonElement(text).jsonObject
            obj["result"]?.jsonPrimitive?.content ?: "HTTP $code"
        } catch (e: Exception) {
            text.ifBlank { "HTTP $code" }
        }
    }

    /** `POST /api/push { action: "broadcast", title, body, url }` (spec §9.5). */
    suspend fun broadcast(title: String, body: String, url: String): String {
        val payload = buildJsonObject {
            put("action", "broadcast")
            put("title", title)
            put("body", body)
            put("url", url.ifBlank { "/" })
        }
        val (code, text) = api.execute(api.postJson("/api/push", payload))
        if (code == 0) return "The server could not be reached."
        return try {
            val obj = api.json.parseToJsonElement(text).jsonObject
            val sent = obj["sent"]?.jsonPrimitive?.content
            val failed = obj["failed"]?.jsonPrimitive?.content
            if (sent != null) "Broadcast sent to $sent subscriber(s), $failed failed."
            else obj["error"]?.jsonPrimitive?.content ?: "HTTP $code"
        } catch (e: Exception) {
            "HTTP $code"
        }
    }

    // ------------------------------------------------------ RTDB admin reads/writes

    private fun db(): FirebaseDatabase? = database

    private suspend fun <T> read(path: String, block: (Any?) -> T): T? {
        val db = db() ?: return null
        return try {
            val snap = db.getReference(path).get().await()
            block(snap.value)
        } catch (e: Throwable) {
            null
        }
    }

    data class Person(
        val uid: String,
        val name: String,
        val firstSeen: Long,
        val lastSeen: Long,
        val plan: String?,
        val planExpires: Long,
        val banned: Boolean,
    )

    /** `admin/registry` merged with `subscriptions` + `admin/bans` (spec §12 §2, §4). */
    suspend fun people(): List<Person> = withContext(Dispatchers.IO) {
        val registry = read("admin/registry") { it as? Map<*, *> } ?: emptyMap<Any?, Any?>()
        val subs = read("subscriptions") { it as? Map<*, *> } ?: emptyMap<Any?, Any?>()
        val bans = read("admin/bans") { it as? Map<*, *> } ?: emptyMap<Any?, Any?>()
        registry.mapNotNull { (key, value) ->
            val uid = key as? String ?: return@mapNotNull null
            val map = value as? Map<*, *> ?: return@mapNotNull null
            val sub = subs[uid] as? Map<*, *>
            val ban = bans[uid] as? Map<*, *>
            Person(
                uid = uid,
                name = (map["name"] as? String) ?: uid.take(8),
                firstSeen = (map["firstSeen"] as? Number)?.toLong() ?: 0L,
                lastSeen = (map["lastSeen"] as? Number)?.toLong() ?: 0L,
                plan = sub?.get("plan") as? String,
                planExpires = (sub?.get("expiresAt") as? Number)?.toLong() ?: 0L,
                banned = ban != null,
            )
        }.sortedByDescending { it.lastSeen }
    }

    data class Ban(val uid: String, val reason: String, val by: String, val until: Long)

    suspend fun bans(): List<Ban> = withContext(Dispatchers.IO) {
        (read("admin/bans") { it as? Map<*, *> } ?: emptyMap<Any?, Any?>()).mapNotNull { (key, value) ->
            val uid = key as? String ?: return@mapNotNull null
            val map = value as? Map<*, *> ?: return@mapNotNull null
            Ban(
                uid = uid,
                reason = (map["reason"] as? String) ?: "",
                by = (map["by"] as? String) ?: "",
                until = (map["until"] as? Number)?.toLong() ?: 0L,
            )
        }.sortedByDescending { it.until }
    }

    /** Grant or revoke a plan for one uid — the rules validate the write, not us. */
    suspend fun setPlan(uid: String, plan: String?, expiresAt: Long, reference: String): Boolean {
        val db = db() ?: return false
        return try {
            val value: Any? = if (plan == null) {
                null
            } else {
                buildMap<String, Any?> {
                    put("plan", plan)
                    put("grantedAt", System.currentTimeMillis())
                    put("expiresAt", expiresAt)
                    if (reference.isNotBlank()) put("announcementId", reference)
                }
            }
            db.getReference("subscriptions/$uid").setValue(value).await()
            true
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun setBan(uid: String, reason: String, until: Long): Boolean {
        val db = db() ?: return false
        return try {
            val who = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
            if (until <= 0) {
                db.getReference("admin/bans/$uid").setValue(null).await()
            } else {
                db.getReference("admin/bans/$uid").setValue(
                    mapOf("reason" to reason, "by" to who, "until" to until),
                ).await()
            }
            true
        } catch (e: Throwable) {
            false
        }
    }

    /** Pause / continue a subscription (`paused` flag on the owner's node). */
    suspend fun setPaused(uid: String, paused: Boolean): Boolean {
        val db = db() ?: return false
        return try {
            db.getReference("subscriptions/$uid/paused").setValue(paused).await()
            true
        } catch (e: Throwable) {
            false
        }
    }

    /** Wipes: remove `users/{uid}`, `admin/registry/{uid}`, `subscriptions/{uid}` (§12 §9). */
    suspend fun wipe(uid: String): Boolean {
        val db = db() ?: return false
        return try {
            listOf("users/$uid", "admin/registry/$uid", "subscriptions/$uid").forEach { path ->
                db.getReference(path).setValue(null).await()
            }
            true
        } catch (e: Throwable) {
            false
        }
    }

    data class Code(val code: String, val plan: String?, val dew: Double?, val days: Int, val active: Boolean)

    suspend fun codes(): List<Code> = withContext(Dispatchers.IO) {
        (read("codes") { it as? Map<*, *> } ?: emptyMap<Any?, Any?>()).mapNotNull { (key, value) ->
            val code = key as? String ?: return@mapNotNull null
            val map = value as? Map<*, *> ?: return@mapNotNull null
            Code(
                code = code,
                plan = map["plan"] as? String,
                dew = (map["dew"] as? Number)?.toDouble(),
                days = (map["days"] as? Number)?.toInt() ?: 0,
                active = map["active"] as? Boolean ?: false,
            )
        }.sortedBy { it.code }
    }

    /** Create a redeem word (default shape: Dew top-up, spec §11.5 / §12 §5). */
    suspend fun createCode(code: String, dew: Double?, plan: String?, days: Int): Boolean {
        val db = db() ?: return false
        val word = code.trim().uppercase()
        if (word.length < 4) return false
        return try {
            val payload = buildMap<String, Any?> {
                put("code", word)
                put("active", true)
                put("createdAt", System.currentTimeMillis())
                if (dew != null) put("dew", dew)
                if (plan != null) put("plan", plan)
                if (days > 0) put("days", days)
            }
            db.getReference("codes/$word").setValue(payload).await()
            true
        } catch (e: Throwable) {
            false
        }
    }

    /** Terminate — switch the code off (never delete history). */
    suspend fun deactivateCode(code: String): Boolean {
        val db = db() ?: return false
        return try {
            db.getReference("codes/$code/active").setValue(false).await()
            true
        } catch (e: Throwable) {
            false
        }
    }
}
