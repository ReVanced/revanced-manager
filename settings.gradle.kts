pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
        maven {
            // A repository must be specified for some reason. "registry" is a dummy.
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: extra["gpr.user"] as String?
                password = System.getenv("GITHUB_TOKEN") ?: extra["gpr.key"] as String?
            }
        }
    }
}
rootProject.name = "ReVanced Manager"
include(":app")
include(":downloader-plugin")
include(":example-downloader-plugin")
