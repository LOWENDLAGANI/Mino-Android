package com.minetallest.mino.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.minetallest.mino.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Anonymous Firebase identity (spec §1.4, §4.2, §14): first-time visitors get a real UID
 * used for plan/dew/entitlement. Bound via programmatic FirebaseOptions so the same
 * `NEXT_PUBLIC_FIREBASE_*` values from the backend deployment configure it — no
 * google-services.json needed. When unconfigured, the app is purely local (spec #3).
 */
class FirebaseIdentity(private val context: Context) : IdentityProvider {

    @Volatile
    private var resolved: Boolean = false

    private val options: FirebaseOptions?
        get() {
            if (BuildConfig.FIREBASE_API_KEY.isBlank()) return null
            return FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                .build()
        }

    private val auth: FirebaseAuth? by lazy {
        try {
            val app = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                val opts = options ?: return@lazy null
                FirebaseApp.initializeApp(context, opts)
            } ?: return@lazy null
            FirebaseAuth.getInstance(app)
        } catch (e: Throwable) {
            null
        }
    }

    override suspend fun idTokenOrNull(): String? {
        val a = auth ?: return null
        return withContext(Dispatchers.IO) {
            try {
                a.currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Throwable) {
                null
            }
        }
    }

    override suspend fun uid(): String? = auth?.currentUser?.uid

    override suspend fun ensureSignedIn(): Boolean {
        if (resolved) return auth?.currentUser != null
        val a = auth ?: return false
        if (a.currentUser != null) {
            resolved = true
            return true
        }
        return withContext(Dispatchers.IO) {
            try {
                val result = a.signInAnonymously().await()
                result.user != null
            } catch (e: Throwable) {
                false
            }.also { resolved = true }
        }
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
        addOnFailureListener { _ -> if (continuation.isActive) continuation.resumeWith(Result.failure(Exception("task failed"))) }
    }
