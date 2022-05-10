plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
    // Version variables
    val composeVersion = "1.1.1"
    val accompanistVersion = "0.22.1-rc"
    // ReVanced
    implementation("app.revanced:revanced-patcher:1.0.0-dev.10")
    implementation("app.revanced:revanced-patches:1.0.0-dev.7")
    // libsu
    implementation("com.github.topjohnwu.libsu:core:4.0.3")
    implementation("com.github.topjohnwu.libsu:io:4.0.3")
    // Signing
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    // AndroidX
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.activity:activity-compose:1.4.0")
    // Compose
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material3:material3:1.0.0-alpha10")
    implementation("androidx.navigation:navigation-compose:2.5.0-beta01")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-util:$composeVersion")
    // Google
    implementation("com.google.accompanist:accompanist-drawablepainter:$accompanistVersion")
    implementation("com.google.android.material:material:1.6.0")
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions.optIn(library: String) {
    freeCompilerArgs = freeCompilerArgs + "-opt-in=$library"
}