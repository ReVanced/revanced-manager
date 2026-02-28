import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import io.github.z4kn4fein.semver.toVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.random.Random

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.devtools)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.about.libraries.android)
    signing
}

val outputApkFileName = "${rootProject.name}-$version.apk"

dependencies {
    // AndroidX Core
    implementation(libs.androidx.ktx)
    implementation(libs.runtime.ktx)
    implementation(libs.runtime.compose)
    implementation(libs.splash.screen)
    implementation(libs.activity.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.preferences.datastore)
    implementation(libs.appcompat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.livedata)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)

    // Accompanist
    implementation(libs.accompanist.drawablepainter)

    // Placeholder
    implementation(libs.placeholder.material3)

    // Coil (async image loading, network image)
    implementation(libs.coil.compose)
    implementation(libs.coil.appiconloader)

    // KotlinX
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collection.immutable)
    implementation(libs.kotlinx.datetime)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)

    // ReVanced
    implementation(libs.revanced.patcher)
    implementation(libs.revanced.library)

    // Downloader plugins
    implementation(project(":api"))

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
    implementation(libs.koin.compose.navigation)
    implementation(libs.koin.workmanager)

    // Licenses
    implementation(libs.about.libraries.core)
    implementation(libs.about.libraries.m3)

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

    // Ackpine
    implementation(libs.ackpine.core)
    implementation(libs.ackpine.ktx)
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Semantic versioning string parser
        classpath(libs.semver.parser)
    }
}

android {
    namespace = "app.revanced.manager"
    compileSdk = 36
    buildToolsVersion = "35.0.1"

    defaultConfig {
        applicationId = "app.revanced.manager.flutter"
        minSdk = 26
        targetSdk = 35

        val versionStr = if (version == "unspecified") "1.0.0" else version.toString()
        versionName = versionStr
        versionCode = with(versionStr.toVersion()) {
            major * 100_000_000 +
                    minor * 100_000 +
                    patch * 100 +
                    (preRelease?.substringAfterLast('.')?.toInt() ?: 99)
        }
        vectorDrawables.useSupportLibrary = true

        val resDir = file("src/main/res")
        val locales = resDir.listFiles()
            .orEmpty()
            //noinspection WrongGradleMethod
            .filter { it.isDirectory && it.name.matches(Regex("values-[a-z]{2}(-r[A-Z]{2})?")) }
            //noinspection WrongGradleMethod
            .map { it.name.removePrefix("values-").replace("-r", "-") }
            .sorted()
            //noinspection WrongGradleMethod
            .joinToString(prefix = "{", separator = ",", postfix = "}") { "\"$it\"" }

        buildConfigField("String[]", "SUPPORTED_LOCALES", locales)
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "ReVanced Manager (Debug)")

            buildConfigField("long", "BUILD_ID", "${Random.nextLong()}L")
        }

        release {
            if (!project.hasProperty("noProguard")) {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }

            val keystoreFile = file("keystore.jks")

            if (project.hasProperty("signAsDebug") || !keystoreFile.exists()) {
                applicationIdSuffix = ".debug_signed"
                resValue("string", "app_name", "ReVanced Manager (Debug signed)")
                signingConfig = signingConfigs.getByName("debug")

                isPseudoLocalesEnabled = true
            } else {
                signingConfig = signingConfigs.create("release") {
                    storeFile = keystoreFile
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEYSTORE_ENTRY_ALIAS")
                    keyPassword = System.getenv("KEYSTORE_ENTRY_PASSWORD")
                }
            }

            buildConfigField("long", "BUILD_ID", "0L")
        }
    }

    applicationVariants.all {
        outputs.all {
            this as com.android.build.gradle.internal.api.ApkVariantOutputImpl

            outputFileName = outputApkFileName
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

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            // Useless files
            excludes += "/XPP3_*_VERSION"
            excludes += "/font-awesome-license.txt"
            excludes += "/smali.properties"
            excludes += "/baksmali.properties"
            excludes += "/properties/apktool.properties"
            excludes += "/org/antlr/**"
            excludes += "/org/mockito/**"
            excludes += "/org/bouncycastle/pqc/**.properties"
            excludes += "/org/bouncycastle/x509/**.properties"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/**/*.txt"
            excludes += "/META-INF/**/*.properties"
            excludes += "/META-INF/DEPENDENCIES"
        }
        jniLibs {
            // 32-bit x86 is dead
            excludes += "/lib/x86/*.so"

            // Equivalent of AndroidManifest's extractNativeLibs=true to ensure libs are compressed
            useLegacyPackaging = true
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.apply {
            // Debug metadata
            add("/META-INF/*.version")
            add("/META-INF/*.kotlin_module")
            add("/kotlin-tooling-metadata.json")

            // Kotlin debugging (https://github.com/Kotlin/kotlinx.coroutines/issues/2274)
            add("/DebugProbesKt.bin")
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters",
        )
    }
}

configurations {
    all {
        // ReVanced Library has a dependency which conflicts with whatever this is. We don't use protobuf, so it should be fine.
        exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
    }
}

aboutLibraries {
    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = DuplicateMode.MERGE
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = DuplicateRule.EXACT
    }
}

tasks {
    // Needed by gradle-semantic-release-plugin.
    // Tracking: https://github.com/KengoTODA/gradle-semantic-release-plugin/issues/435.
    val publish by registering {
        group = "publishing"
        description = "Build the release APK"

        dependsOn("assembleRelease")

        val apk = project.layout.buildDirectory.file("outputs/apk/release/${outputApkFileName}")
        val ascFile = apk.map { it.asFile.resolveSibling("${it.asFile.name}.asc") }

        inputs.file(apk).withPropertyName("inputApk")
        outputs.file(ascFile).withPropertyName("outputAsc")

        doLast {
            signing {
                useGpgCmd()
                sign(apk.get().asFile)
            }
        }
    }
}
