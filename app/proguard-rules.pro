# kotlinx.serialization: keep generated serializers for @Serializable classes (content model, routes).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.studydesk.**$$serializer { *; }
-keepclassmembers class app.studydesk.** {
    *** Companion;
}
# commonmark: extensions are found by class.
-keep class org.commonmark.** { *; }
# Ktor/OkHttp optional dependencies.
-dontwarn org.slf4j.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
