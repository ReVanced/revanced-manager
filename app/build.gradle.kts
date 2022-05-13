plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.4.0-alpha06")

    // ReVanced
    implementation("app.revanced:revanced-patcher:+")
    implementation("app.revanced:revanced-patches:+")

    // Compose Destinations
    implementation("io.github.raamcosta.compose-destinations:core:+")
    ksp("io.github.raamcosta.compose-destinations:ksp:+")

    // Accompanist
    implementation("com.google.accompanist:accompanist-drawablepainter:+")

    // libsu
    implementation("com.github.topjohnwu.libsu:core:4.0.3")
    implementation("com.github.topjohnwu.libsu:io:4.0.3")

    // ???
    implementation("com.beust:klaxon:5.5")

    // Signing
    implementation("org.bouncycastle:bcpkix-jdk15on:+")

    // Material 3 + 2
    implementation("androidx.compose.material3:material3-window-size-class:1.0.0-alpha10")
    implementation("androidx.compose.material3:material3:1.0.0-alpha10")
    implementation("androidx.compose.material:material:1.1.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions.optIn(library: String) {
    freeCompilerArgs = freeCompilerArgs + "-opt-in=$library"
}