# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# Required for serialization to work properly
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

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
-dontwarn org.slf4j.**
-dontwarn it.skrape.fetcher.*
-dontwarn com.google.j2objc.annotations.*

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault