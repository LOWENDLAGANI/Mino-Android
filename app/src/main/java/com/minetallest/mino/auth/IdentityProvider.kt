package com.minetallest.mino.auth

/**
 * Firebase identity abstraction (spec §4.2). The web build uses anonymous Firebase auth;
 * this app attaches the same `Authorization: Bearer <Firebase ID token>` header.
 * With Firebase unconfigured the app is purely local and every call is unidentified —
 * which the server already supports (spec §16).
 */
interface IdentityProvider {
    /** Firebase ID token, or null when unidentified. */
    suspend fun idTokenOrNull(): String?

    /** Real UID when identified. */
    suspend fun uid(): String?

    /** Fire anonymous sign-in; safe to call repeatedly. */
    suspend fun ensureSignedIn(): Boolean
}

object NoIdentityProvider : IdentityProvider {
    override suspend fun idTokenOrNull(): String? = null
    override suspend fun uid(): String? = null
    override suspend fun ensureSignedIn(): Boolean = false
}
