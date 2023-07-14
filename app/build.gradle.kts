plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.8.21"
    id("com.mikepenz.aboutlibraries.plugin") version "10.8.0"
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
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.paging:paging-common-ktx:3.1.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")

    // Accompanist
    val accompanistVersion = "0.30.1"
    //implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    //implementation("com.google.accompanist:accompanist-placeholder-material:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-drawablepainter:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-webview:$accompanistVersion")
    //implementation("com.google.accompanist:accompanist-flowlayout:$accompanistVersion")
    //implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")

    // HTML Scraper
    implementation("it.skrape:skrapeit:1.1.5") {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    // Coil (async image loading, network image)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("me.zhanghai.android.appiconloader:appiconloader-coil:1.5.0")

    // KotlinX
    val serializationVersion = "1.5.1"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")

    // Room
    val roomVersion = "2.5.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ReVanced
    implementation("app.revanced:revanced-patcher:11.0.4")

    // Signing
    implementation("com.android.tools.build:apksig:8.0.2")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    // Koin
    val koinVersion = "3.4.2"
    implementation("io.insert-koin:koin-android:$koinVersion")
    implementation("io.insert-koin:koin-androidx-compose:3.4.5")
    implementation("io.insert-koin:koin-androidx-workmanager:$koinVersion")

    // Compose Navigation
    implementation("dev.olshevski.navigation:reimagined:1.4.0")

    // Licenses
    implementation("com.mikepenz:aboutlibraries-compose:10.8.0")

    // Ktor
    val ktorVersion = "2.3.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Markdown to HTML
    implementation("org.jetbrains:markdown:0.4.1")
}
