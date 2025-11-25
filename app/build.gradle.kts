plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms.google.services)
}

android {
    namespace = "com.example.careerguidance"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.careerguidance"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.11"
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
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-analytics")
    // Firebase Authentication
    implementation(libs.firebase.auth)

    // Google Sign-In (Identity API)
    implementation(libs.google.auth)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    // Firebase
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.compose.foundation:foundation:1.6.7")
    implementation("androidx.activity:activity-compose:1.9.0")
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.0")
    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // Optional
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
    }
