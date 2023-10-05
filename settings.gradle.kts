pluginManagement {
    repositories {
        // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
        val (gprUser, gprKey) = if (File(".gradle/gradle.properties").exists()) {
            File(".gradle/gradle.properties").inputStream().use {
                java.util.Properties().apply { load(it) }.let {
                    it.getProperty("gpr.user") to it.getProperty("gpr.key")
                }
            }
        } else {
            null to null
        }

        fun RepositoryHandler.githubPackages(name: String) = maven {
            url = uri(name)
            credentials {
                username = gprUser ?: providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = gprKey ?: providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }

        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        githubPackages("https://maven.pkg.github.com/revanced/revanced-patcher")
        githubPackages("https://maven.pkg.github.com/revanced/revanced-library")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
        val (gprUser, gprKey) = if (File(".gradle/gradle.properties").exists()) {
            File(".gradle/gradle.properties").inputStream().use {
                java.util.Properties().apply { load(it) }.let {
                    it.getProperty("gpr.user") to it.getProperty("gpr.key")
                }
            }
        } else {
            null to null
        }

        fun RepositoryHandler.githubPackages(name: String) = maven {
            url = uri(name)
            credentials {
                username = gprUser ?: providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = gprKey ?: providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }

        google()
        mavenCentral()
        maven("https://jitpack.io")
        githubPackages("https://maven.pkg.github.com/revanced/revanced-patcher")
        githubPackages("https://maven.pkg.github.com/revanced/revanced-library")
    }
}
rootProject.name = "ReVanced Manager"
include(":app")
