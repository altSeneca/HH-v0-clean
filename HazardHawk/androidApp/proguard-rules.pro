# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ONNX Runtime ProGuard rules
-keep class ai.onnxruntime.** { *; }
-keep class com.microsoft.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-keepclassmembers class com.microsoft.onnxruntime.** { *; }

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# ONNX Runtime JNI
-keep class ai.onnxruntime.OrtEnvironment { *; }
-keep class ai.onnxruntime.OrtSession { *; }
-keep class ai.onnxruntime.OnnxTensor { *; }
-keep class ai.onnxruntime.providers.** { *; }

# Keep serialization classes for HazardHawk models
-keepclassmembers class com.hazardhawk.models.** {
    <fields>;
    <init>(...);
}

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep classes with @Serializable annotation
-keep @kotlinx.serialization.Serializable class * {
    <fields>;
    <init>(...);
}

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Camera
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Firebase AI and Vertex AI
-keep class com.google.firebase.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.ai.client.generativeai.**

# ML Kit and TensorFlow Lite
-keep class com.google.mlkit.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn org.tensorflow.lite.**

# Koin Dependency Injection
-keep class io.insert-koin.** { *; }
-keep class org.koin.** { *; }
-dontwarn io.insert-koin.**
-dontwarn org.koin.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Ktor HTTP Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ExifInterface
-keep class androidx.exifinterface.** { *; }
-dontwarn androidx.exifinterface.**

# Coil 3.0 image loading library optimizations
-keep class coil.** { *; }
-keep class coil3.** { *; }
-keep class io.coil-kt.coil3.** { *; }
-dontwarn coil.**
-dontwarn coil3.**
-dontwarn io.coil-kt.coil3.**

# Keep OkHttp for Coil network operations
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# iText PDF generation
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ZXing QR Code generation
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.barcodescanner.**


# Security crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
