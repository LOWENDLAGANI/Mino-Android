package com.minetallest.mino

import android.app.Application
import android.content.Context
import com.minetallest.mino.auth.FirebaseIdentity
import com.minetallest.mino.auth.IdentityProvider
import com.minetallest.mino.auth.NoIdentityProvider
import com.minetallest.mino.data.ChatRepository
import com.minetallest.mino.data.db.MinoDatabase
import com.minetallest.mino.data.prefs.Prefs
import com.minetallest.mino.net.ApiClient
import com.minetallest.mino.net.AdminApi
import com.minetallest.mino.net.ConfigSource
import com.minetallest.mino.net.MinoApi
import com.minetallest.mino.net.RedeemSource
import com.minetallest.mino.net.SseChatClient

class MinoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Poor-man's DI — keeps every dependency reachable from the ViewModel. */
class AppContainer(private val context: Context) {
    val appContext: Context = context.applicationContext
    val prefs = Prefs(context)
    val database = MinoDatabase.get(context)
    val repository = ChatRepository(database)

    /** Firebase unconfigured → purely local app; account/plan UI hidden (spec #3). */
    val identity: IdentityProvider =
        if (BuildConfig.FIREBASE_API_KEY.isNotBlank()) FirebaseIdentity(context) else NoIdentityProvider

    /** RTDB handle, or null when Firebase is unconfigured (purely local build). */
    val firebaseDatabase: com.google.firebase.database.FirebaseDatabase? =
        if (BuildConfig.FIREBASE_API_KEY.isNotBlank()) {
            try {
                com.google.firebase.database.FirebaseDatabase.getInstance(
                    com.google.firebase.FirebaseApp.getInstance(),
                )
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }

    val apiClient = ApiClient(identity) { prefs.currentApiBase() }
    val api = MinoApi(apiClient)
    val sse = SseChatClient(apiClient)
    val config = ConfigSource(context)
    val adminApi = AdminApi(apiClient, firebaseDatabase)
    val redeem = RedeemSource(firebaseDatabase)

    val firebaseConfigured: Boolean = BuildConfig.FIREBASE_API_KEY.isNotBlank()
}

val Context.minoContainer: AppContainer
    get() = (applicationContext as MinoApp).container
