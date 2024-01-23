# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# Required for the patcher to function correctly
-keep class app.revanced.patcher.** {
  *;
}
-keep class brut.** {
  *;
}
-keep class org.xmlpull.** {
  *;
}
-keep class kotlin.** {
  *;
}
-keep class org.jf.** {
  *;
}
-keep class com.android.** {
  *;
}
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn com.google.j2objc.annotations.*