# R8 & ProGuard Obfuscation & Security Rules for Mino

# Keep Kotlinx Serialization
-keepattributes *Annotation*, ElementValuePairs, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerializedName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room DB Entities and Daos
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface * { *; }

# Keep Domain Data Models & Database Entities
-keep class com.minetallest.mino.domain.** { *; }
-keep class com.minetallest.mino.data.db.** { *; }

# Firebase & OkHttp
-dontwarn com.google.firebase.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Enable Optimization & Obfuscation
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
