plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.binary.compatibility.validator)
    `maven-publish`
    signing
}

group = "app.revanced"

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.appcompat)
}

android {
    namespace = "app.revanced.manager.plugin.downloader"
    compileSdk = 35

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

    buildFeatures {
        aidl = true
    }
}

apiValidation {
    nonPublicMarkers += "app.revanced.manager.plugin.downloader.PluginHostApi"
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-manager")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: extra["gpr.user"] as String?
                password = System.getenv("GITHUB_TOKEN") ?: extra["gpr.key"] as String?
            }
        }
    }

    publications {
        create<MavenPublication>("Api") {
            afterEvaluate {
                from(components["release"])
            }

            groupId = "app.revanced"
            artifactId = "revanced-manager-api"
            version = project.version.toString()

            pom {
                name = "ReVanced Manager API"
                description = "API for ReVanced Manager."
                url = "https://revanced.app"

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "ReVanced"
                        name = "ReVanced"
                        email = "contact@revanced.app"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/revanced/revanced-manager.git"
                    developerConnection = "scm:git:git@github.com:revanced/revanced-manager.git"
                    url = "https://github.com/revanced/revanced-manager"
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["Api"])
}
