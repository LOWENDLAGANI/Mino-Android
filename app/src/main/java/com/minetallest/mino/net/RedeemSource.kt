package com.minetallest.mino.net

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Dew/plan redeem codes (spec §11.5): the claim node carries ONLY `{code, claimedAt}`;
 * the credit write can only ever write the code's own pinned `dew` value — the rules
 * make the final decision, this client just asks.
 */
class RedeemSource(private val database: FirebaseDatabase?) {

    sealed class Result {
        data class Ok(val dew: Double?, val plan: String?) : Result()
        data class Error(val reason: String) : Result()
    }

    suspend fun redeem(code: String): Result {
        val db = database ?: return Result.Error("Accounts are not configured on this device.")
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.Error("Sign in with an account to redeem codes.")
        val word = code.trim().uppercase()
        if (word.length < 4) return Result.Error("Codes are at least 4 characters.")
        return try {
            val snapshot = db.getReference("codes/$word").get().await()
            val map = snapshot.value as? Map<*, *> ?: return Result.Error("That code does not exist.")
            val active = map["active"] as? Boolean ?: false
            if (!active) return Result.Error("That code is no longer active.")
            val dew = (map["dew"] as? Number)?.toDouble()
            val plan = map["plan"] as? String
            // Claim node: only {code, claimedAt} — never plan/days (spec §17, §11.5).
            db.getReference("redeems/${user.uid}/$word")
                .setValue(mapOf("code" to word, "claimedAt" to System.currentTimeMillis()))
                .await()
            if (dew != null) {
                db.getReference("dew/${user.uid}/credits/$word").setValue(dew).await()
            }
            Result.Ok(dew = dew, plan = plan)
        } catch (e: Throwable) {
            Result.Error("The server refused that code. It may already be claimed.")
        }
    }
}
