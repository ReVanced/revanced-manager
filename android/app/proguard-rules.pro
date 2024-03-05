# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep class app.revanced.** { *; }
-keep class com.android.tools.smali.** { *; }
-keep class kotlin.** { *; }
-keep class com.google.auto.value.** { *; }
-keep class com.android.apksig.internal.** { *; }
-keepnames class com.google.common.collect.**

-dontwarn com.google.auto.value.**
-dontwarn com.google.j2objc.annotations.*
-dontwarn java.awt.**
-dontwarn javax.**
