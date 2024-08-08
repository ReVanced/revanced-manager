import kotlin.random.Random

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools)
    alias(libs.plugins.about.libraries)
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.9.23"
}

android {
    namespace = "app.revanced.manager"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "app.revanced.manager"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "ReVanced Manager (dev)")

            buildConfigField("long", "BUILD_ID", "${Random.nextLong()}L")
        }

        release {
            if (!project.hasProperty("noProguard")) {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }

            if (project.hasProperty("signAsDebug")) {
                applicationIdSuffix = ".debug"
                resValue("string", "app_name", "ReVanced Manager Debug")
                signingConfig = signingConfigs.getByName("debug")
            }

            buildConfigField("long", "BUILD_ID", "0L")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources.excludes.addAll(listOf(
            "/prebuilt/**",
            "META-INF/DEPENDENCIES",
            "META-INF/**.version",
            "DebugProbesKt.bin",
            "kotlin-tooling-metadata.json",
            "org/bouncycastle/pqc/**.properties",
            "org/bouncycastle/x509/**.properties",
        ))
        jniLibs {
            useLegacyPackaging = true
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures.compose = true
    buildFeatures.aidl = true
    buildFeatures.buildConfig=true

    android {
        androidResources {
            generateLocaleConfig = true
        }
    }

    composeOptions.kotlinCompilerExtensionVersion = "1.5.10"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

kotlin {
    jvmToolchain(17)
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
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.livedata)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)

    // Accompanist
    implementation(libs.accompanist.drawablepainter)

    // Placeholder
    implementation(libs.placeholder.material3)

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
    implementation(libs.revanced.patcher)
    implementation(libs.revanced.library)

    // Native processes
    implementation(libs.kotlin.process)

    // HiddenAPI
    compileOnly(libs.hidden.api.stub)

    // LibSU
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.nio)

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

    // Markdown
    implementation(libs.markdown.renderer)

    // Fading Edges
    implementation(libs.fading.edges)

    // Scrollbars
    implementation(libs.scrollbars)

    // EnumUtil
    implementation(libs.enumutil)
    ksp(libs.enumutil.ksp)
    
    // Reorderable lists
    implementation(libs.reorderable)

    // Compose Icons
    implementation(libs.compose.icons.fontawesome)
}
