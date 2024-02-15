# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# These packages are referenced by ReVanced Patches
-keep class app.revanced.patcher.** { *; }
-keep class com.android.tools.smali.** { *; }
-keep class kotlin.** { *; }
-keepnames class com.google.common.collect.**

# This package uses reflection internally, so do not remove and rename
-keep class com.android.apksig.internal.** { *; }

# Fix crash
-keepnames class org.xmlpull.** { *; }

# Fix build errors
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn com.google.j2objc.annotations.*