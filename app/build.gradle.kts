import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val minoApiBase: String = (project.findProperty("mino.api.base") as String?) ?: ""

android {
    namespace = "com.minetallest.mino"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minetallest.mino"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$minoApiBase\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${(project.findProperty("mino.firebase.apiKey") as String?) ?: ""}\"")
        buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"${(project.findProperty("mino.firebase.authDomain") as String?) ?: ""}\"")
        buildConfigField("String", "FIREBASE_DATABASE_URL", "\"${(project.findProperty("mino.firebase.databaseUrl") as String?) ?: ""}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${(project.findProperty("mino.firebase.projectId") as String?) ?: ""}\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"${(project.findProperty("mino.firebase.storageBucket") as String?) ?: ""}\"")
        buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"${(project.findProperty("mino.firebase.messagingSenderId") as String?) ?: ""}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${(project.findProperty("mino.firebase.appId") as String?) ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.squareup.okio:okio:3.9.1")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
