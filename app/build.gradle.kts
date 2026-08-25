plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Official Nothing Glyph SDK AAR from https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit
    namespace = "com.example.glyphequalizer"
    compileSdk = 35 // bump to whatever matches Nothing OS 4.x's Android 16 SDK once confirmed

    defaultConfig {
        applicationId = "com.example.glyphequalizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

}
