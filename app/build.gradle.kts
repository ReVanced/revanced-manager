plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.7.10"
}

android {
    namespace = "app.revanced.manager"
    compileSdk = 32

    defaultConfig {
        applicationId = "app.revanced.manager"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    buildFeatures.compose = true
    composeOptions.kotlinCompilerExtensionVersion = "1.2.0"
}

dependencies {
    // AndroidX core
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.core:core-splashscreen:1.0.0")

    // AndroidX activity
    implementation("androidx.activity:activity-compose:1.6.0-alpha05")

    // Koin
    val koinVersion = "3.2.0"
    implementation("io.insert-koin:koin-android:$koinVersion")
    implementation("io.insert-koin:koin-androidx-compose:$koinVersion")

    // Compose
    val composeVersion = "1.3.0-alpha01"
    implementation("androidx.compose.ui:ui:${composeVersion}")
    debugImplementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.0.0-alpha15")
    implementation("androidx.compose.material:material-icons-extended:${composeVersion}")

    // Accompanist
    val accompanistVersion = "0.26.0-alpha"
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-placeholder-material:$accompanistVersion")

    // Coil (async image loading)
    implementation("io.coil-kt:coil-compose:2.1.0")

    // KotlinX
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // Taxi (navigation)
    implementation("com.github.X1nto:Taxi:1.0.0")

    // Ktor
    val ktorVersion = "2.0.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}