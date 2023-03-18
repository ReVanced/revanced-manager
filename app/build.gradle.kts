plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.7.20"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    google()
    maven {
        url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
        credentials {
            username = (project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")) as String
            password = (project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")) as String
        }
    }
}

android {
    namespace = "app.revanced.manager.compose"
    compileSdk = 33
    buildToolsVersion = "33.0.0"

    defaultConfig {
        applicationId = "app.revanced.manager.compose"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures.compose = true

    composeOptions.kotlinCompilerExtensionVersion = "1.4.0"
}

dependencies {

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.activity:activity-compose:1.6.1")

    // Compose
    val composeVersion = "1.4.0-alpha05"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")

    // Accompanist
    val accompanistVersion = "0.29.1-alpha"
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    //implementation("com.google.accompanist:accompanist-placeholder-material:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-drawablepainter:$accompanistVersion")
    //implementation("com.google.accompanist:accompanist-flowlayout:$accompanistVersion")
    //implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")

    // KotlinX
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // Material 3
    implementation("androidx.compose.material3:material3:1.1.0-alpha08")


    // ReVanced
    implementation("app.revanced:revanced-patcher:6.4.3")

    // Koin
    implementation("io.insert-koin:koin-android:3.3.2")
    implementation("io.insert-koin:koin-androidx-compose:3.4.1")

    // Compose Navigation
    implementation("dev.olshevski.navigation:reimagined:1.3.1")

    // Ktor
    val ktorVersion = "2.1.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}