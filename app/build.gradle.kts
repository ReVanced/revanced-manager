plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools)
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.8.21"
}

android {
    namespace = "app.revanced.manager"
    compileSdk = 33
    buildToolsVersion = "33.0.2"

    defaultConfig {
        applicationId = "app.revanced.manager"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/prebuilt/**"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures.compose = true

    composeOptions.kotlinCompilerExtensionVersion = "1.4.7"
}

kotlin {
    jvmToolchain(11)
}

dependencies {

    // AndroidX Core
    implementation(libs.androidx.ktx)
    implementation(libs.runtime.ktx)
    implementation(libs.runtime.compose)
    implementation(libs.splash.screen)
    implementation(libs.compose.activity)
    implementation(libs.paging.common.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.preferences.datastore)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.livedata)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)

    // Accompanist
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.accompanist.webview)

    // HTML Scraper
    implementation(libs.skrapeit.dsl)
    implementation(libs.skrapeit.parser)

    // Coil (async image loading, network image)
    implementation(libs.coil.compose)
    implementation(libs.coil.appiconloader)

    // KotlinX
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collection.immutable)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)

    // ReVanced
    implementation(libs.patcher)

    // Signing
    implementation(libs.apksign)
    implementation(libs.bcpkix.jdk15on)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.workmanager)

    // Compose Navigation
    implementation(libs.reimagined.navigation)

    // Licenses
    implementation(libs.about.libraries)

    // Ktor
    implementation(libs.ktor.core)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization)

    // Markdown to HTML
    implementation(libs.markdown)
}
