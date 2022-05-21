val composeVersion = rootProject.extra.get("compose_version") as String
val ktorVersion = rootProject.extra.get("ktor_version") as String

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "1.6.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    google()
    listOf("revanced-patcher", "revanced-patches").forEach { repo ->
        maven {
            url = uri("https://maven.pkg.github.com/revanced/$repo")
            credentials {
                username =
                    (project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")) as String
                password =
                    (project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")) as String
            }
        }
    }
}

val appId = "app.revanced.manager"

android {
    namespace = appId
    compileSdk = 32

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        applicationId = appId
        minSdk = 23
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        optIn("kotlin.RequiresOptIn")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.1"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.5.0-rc01")

    // ReVanced
    implementation("app.revanced:revanced-patcher:1.0.0-dev.10")
    implementation("app.revanced:revanced-patches:1.0.0-dev.7")

    // Compose Destinations
    implementation("io.github.raamcosta.compose-destinations:core:1.5.5-beta")
    ksp("io.github.raamcosta.compose-destinations:ksp:1.5.5-beta")

    // Accompanist
    implementation("com.google.accompanist:accompanist-drawablepainter:0.24.8-beta")

    // libsu
    implementation("com.github.topjohnwu.libsu:core:4.0.3")
    implementation("com.github.topjohnwu.libsu:io:4.0.3")


    // HTTP client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Signing
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    // Material 3 + 2
    implementation("androidx.compose.material3:material3-window-size-class:1.0.0-alpha11")
    implementation("androidx.compose.material3:material3:1.0.0-alpha11")
    implementation("androidx.compose.material:material:1.1.1")

    // Tests
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions.optIn(library: String) {
    freeCompilerArgs = freeCompilerArgs + "-opt-in=$library"
}