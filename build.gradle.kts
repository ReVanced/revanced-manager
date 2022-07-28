buildscript {
    extra.apply {
        // Global variable for some dependencies
        set("compose_version", "1.2.0-beta03")
        set("ktor_version", "2.0.1")
        set("room_version", "2.4.2")
    }
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("com.android.application") version "7.4.0-alpha08" apply false
    id("com.android.library") version "7.4.0-alpha08" apply false
    id("org.jetbrains.kotlin.android") version "1.6.21" apply false
    id("com.google.devtools.ksp") version "1.6.21-+" apply false
}
repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io")
}
