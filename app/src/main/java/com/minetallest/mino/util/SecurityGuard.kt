package com.minetallest.mino.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import com.minetallest.mino.BuildConfig
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Security & Anti-Tamper Guard:
 * Provides short-lived HMAC request signing, nonce generation, and runtime
 * integrity checks to protect backend endpoints from automated cURL/script replay.
 */
object SecurityGuard {

    private const val HMAC_ALGORITHM = "HmacSHA256"

    /**
     * Generates request authentication metadata headers.
     * Signs (timestamp + nonce + path) using a salt derived from BuildConfig.
     */
    fun buildAuthHeaders(path: String): Map<String, String> {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val secretKey = (BuildConfig.APPLICATION_ID + "MinoSecureKey").toByteArray(Charsets.UTF_8)

        val payload = "$timestamp|$nonce|$path"
        val signature = hmacSha256(payload, secretKey)

        return mapOf(
            "X-Mino-Timestamp" to timestamp,
            "X-Mino-Nonce" to nonce,
            "X-Mino-Signature" to signature,
        )
    }

    private fun hmacSha256(data: String, key: ByteArray): String {
        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
            val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Checks if the app is currently running under an active debugger in release builds.
     */
    fun isDebuggingDetected(): Boolean {
        return !BuildConfig.DEBUG && Debug.isDebuggerConnected()
    }

    /**
     * Calculates the SHA-256 fingerprint of the application package.
     */
    @Suppress("DEPRECATION")
    fun getAppSignatureHash(context: Context): String {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val sigBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            } ?: return ""

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sigBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Throwable) {
            ""
        }
    }
}
