-dontobfuscate

-keep class app.revanced.patcher.** { *; }
-keep class kotlin.** { *; }
-keep class com.android.tools.smali.** { *; }
-keep class app.revanced.manager.patcher.runtime.process.* { *; }
-keep class app.revanced.manager.plugin.** { *; }
-keepnames class com.android.apksig.internal.** { *; }
-keepnames class org.xmlpull.** { *; }

-dontwarn com.google.auto.value.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn org.slf4j.**
-dontwarn it.skrape.fetcher.*
-dontwarn com.google.j2objc.annotations.*