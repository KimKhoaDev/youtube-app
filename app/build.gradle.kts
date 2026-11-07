plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.youtube.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.youtube.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += setOf("META-INF/*.version", "META-INF/DEPENDENCIES", "META-INF/**/LICENSE.txt")
    }
    lint { checkReleaseBuilds = false }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.ripple)
    implementation(libs.kyant.shapes)

    // Kyant0's liquid glass library, from Maven Central (components live in com.youtube.app.liquidglass)
    implementation(libs.kyant.backdrop)

    // Online content (no API key needed)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.8")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Google sign-in (works with Google Play services or microG)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
