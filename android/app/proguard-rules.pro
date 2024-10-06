-dontobfuscate

-keep class app.revanced.** { *; }
-keep class com.android.tools.smali.** { *; }
-keep class kotlin.** { *; }
-keep class com.google.auto.value.** { *; }
-keep class com.android.apksig.internal.** { *; }
-keepnames class com.google.common.collect.**
-keepnames class org.xmlpull.** { *; }

-dontwarn com.google.auto.value.**
-dontwarn com.google.j2objc.annotations.*
-dontwarn java.awt.**
-dontwarn javax.**
