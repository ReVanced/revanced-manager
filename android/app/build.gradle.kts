plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "app.revanced.manager.flutter"
    compileSdk = 34
    ndkVersion = "27.0.12077973"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "app.revanced.manager.flutter"
        minSdk = 26
        targetSdk = 34
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        configureEach {
            isShrinkResources = false
            isMinifyEnabled = false

            signingConfig = signingConfigs["debug"]

            ndk.abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }

        release {
            isShrinkResources = true
            isMinifyEnabled = true

            val keystoreFile = file("keystore.jks")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = keystoreFile
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEYSTORE_ENTRY_ALIAS")
                    keyPassword = System.getenv("KEYSTORE_ENTRY_PASSWORD")
                }
            } else {
                resValue("string", "app_name", "ReVanced Manager (Debug)")
                applicationIdSuffix = ".debug"

                signingConfig = signingConfigs["debug"]
            }

            resValue("string", "app_name", "ReVanced Manager")
        }

        debug {
            resValue("string", "app_name", "ReVanced Manager (Debug)")
            applicationIdSuffix = ".debug"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            excludes.add("/prebuilt/**")
        }

        resources {
            excludes.add("/prebuilt/**")
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs) // https://pub.dev/packages/flutter_local_notifications#gradle-setup
    implementation(libs.revanced.patcher)
    implementation(libs.revanced.library)
}

