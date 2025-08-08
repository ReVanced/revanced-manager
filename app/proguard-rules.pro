-dontobfuscate

-keep class app.revanced.manager.patcher.runtime.process.* { *; }
-keep class app.revanced.manager.plugin.** { *; }
-keep class app.revanced.patcher.** { *; }
-keep class com.android.tools.smali.** { *; }
-keep class kotlin.** { *; }
-keepnames class com.android.apksig.internal.** { *; }
-keepnames class org.xmlpull.** { *; }

-dontwarn com.google.j2objc.annotations.*
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn org.slf4j.**