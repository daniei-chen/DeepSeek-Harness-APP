# DSH Mobile ProGuard rules (release build).
# Minification is enabled from 2.4.0; these rules cover the reflective seams.

# kotlinx.serialization — @Serializable classes live outside core.wire.dto too (settings, drafts,
# update metadata), and their generated serializers are looked up through companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.labteto.dshmobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.labteto.dshmobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# WebView shell: the page reaches these through addJavascriptInterface, so R8 must keep both the
# annotated methods and the bridge object names intact.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.labteto.dshmobile.webshell.** { *; }
