buildscript {
    extra.apply {
        // Global variable for some dependencies
        set("compose_version", "1.1.1")
        set("ktor_version", "2.0.1")
        set("room_version", "2.4.2")
        set("koin_version", "3.2.0")
    }
//    dependencies {
//        classpath("com.google.dagger:hilt-android-gradle-plugin:2.38.1")
//    }
    repositories {
        google()
    }
}

plugins {
    id("com.android.application") version "7.4.0-alpha03" apply false
    id("com.android.library") version "7.4.0-alpha03" apply false
    id("org.jetbrains.kotlin.android") version "1.6.10" apply false
    id("com.google.devtools.ksp") version "1.6.10-+" apply false
}
repositories {
    google()
}
