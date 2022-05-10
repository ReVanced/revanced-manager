val composeVersion = rootProject.extra.get("compose_version") as String

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    google()
    maven {
        url = uri("https://maven.pkg.github.com/revanced/revanced-patcher") // note the "r"!
        credentials {
            username =
                (project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")) as String
            password =
                (project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")) as String
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/revanced/revanced-patches") // note the "r"!
        credentials {
            username =
                (project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")) as String
            password =
                (project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")) as String
        }
    }
}

android {
    namespace = "app.revanced.manager"
    compileSdk = 32

    defaultConfig {
        applicationId = "app.revanced.manager"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
    implementation("com.google.accompanist:accompanist-drawablepainter:+")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material3:material3-window-size-class:1.0.0-alpha10")
    implementation("androidx.navigation:navigation-compose:2.4.0-alpha06")
    implementation("com.github.topjohnwu.libsu:core:4.0.3")
    implementation("com.github.topjohnwu.libsu:io:4.0.3")
    implementation("com.beust:klaxon:5.5")
    implementation("org.bouncycastle:bcpkix-jdk15on:+")
    implementation("app.revanced:revanced-patcher:+")
    implementation("app.revanced:revanced-patches:+")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material3:material3:1.0.0-alpha10")
    implementation("androidx.compose.material:material:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}