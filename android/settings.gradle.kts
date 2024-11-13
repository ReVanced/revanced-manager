pluginManagement {
    val properties = java.util.Properties().apply {
        load(file("local.properties").inputStream())
    }

    val flutterSdkPath = properties.getProperty("flutter.sdk")
    assert(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}

include(":app")
