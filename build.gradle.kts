buildscript {
    extra.apply {
        // Global variable for some dependencies
        set("compose_version", "1.1.1")
    }
}

plugins {
    id("com.android.application") version "7.3.0-alpha07" apply false
    id("com.android.library") version "7.3.0-alpha07" apply false
    id("org.jetbrains.kotlin.android") version "1.6.10" apply false
    id("com.google.devtools.ksp") version "1.6.10-+" apply false
}