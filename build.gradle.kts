buildscript {
    extra.apply {
        set("compose_version", "1.1.1")
    }
}

plugins {
    id("com.android.application") version "7.3.0-alpha09" apply false
    id("com.android.library") version "7.3.0-alpha09" apply false
    id("org.jetbrains.kotlin.android") version "1.6.10" apply false
}