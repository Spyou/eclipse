# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Room TypeConverters - keep enum valueOf
-keep class com.spyou.youtubedownload.data.local.database.Converters { *; }

# Keep all data model classes (enums, serializable, etc.)
-keep class com.spyou.youtubedownload.data.model.** { *; }
-keep class com.spyou.youtubedownload.data.local.database.DownloadEntity { *; }

# youtubedl-android + common + Python/QuickJS bridge
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Keep ALL classes loaded via reflection by the native Python/QuickJS runtime
-keep class ** extends java.lang.reflect.InvocationHandler { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Apache Commons Compress (used by youtubedl for zip extraction, loaded via reflection)
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# Don't obfuscate anything — only shrink unused code
-dontobfuscate

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.spyou.youtubedownload.**$$serializer { *; }
-keepclassmembers class com.spyou.youtubedownload.** {
    *** Companion;
}
-keepclasseswithmembers class com.spyou.youtubedownload.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all enums (needed for valueOf/name)
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Coil
-keep class coil.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
