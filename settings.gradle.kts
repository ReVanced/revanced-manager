pluginManagement {
    // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
    val properties = File(".gradle/gradle.properties").inputStream().use { java.util.Properties().apply { load(it) } }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven {
            url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
    val properties = File(".gradle/gradle.properties").inputStream().use { java.util.Properties().apply { load(it) } }
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven {
            url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
rootProject.name = "ReVanced Manager"
include(":app")
