plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    `maven-publish`
}

android {
    namespace = "app.revanced.manager.plugin.downloader"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("release") {
            groupId = "app.revanced"
            artifactId = "manager-downloader-plugin"
            version = "1.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}