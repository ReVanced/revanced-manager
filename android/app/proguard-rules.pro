# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Required for the patcher to function correctly
-keep class app.revanced.patcher.** {
  *;
}
-keep class com.android.** {
  *;
}
-keep class kotlin.** {
  *;
}

-keepnames class com.google.common.collect.**
-keepnames class org.xmlpull.** {
  *;
}

-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn com.google.j2objc.annotations.*