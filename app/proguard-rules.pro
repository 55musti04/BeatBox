# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Compose Foundation
# ---------------------------------------------------------------------------
# FlowRow is called directly from LyricsScreen. R8 may drop the static
# FlowLayoutKt.FlowRow entry point in minified release builds, causing a
# NoSuchMethodError at runtime. Keep it explicitly.
-keep class androidx.compose.foundation.layout.FlowLayoutKt { *; }
-keep class androidx.compose.foundation.layout.FlowRowOverflow { *; }

# ---------------------------------------------------------------------------
# Retrofit / Gson
# ---------------------------------------------------------------------------
# Retrofit builds its call adapters by reading the annotations on the
# interface methods, and Gson resolves JSON properties by field name through
# reflection. R8 renames both members unless they are kept, which makes every
# network response deserialize to nulls in release builds.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit service interfaces: keep the members, allow the name to change.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson models: every JSON property is annotated with @SerializedName, so the
# field may be renamed as long as the annotation survives.
-keepclassmembers,allowobfuscation class com.blackstack.beatbox.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# The response models themselves are referenced directly, but keeping them
# makes the mapping explicit and survives future refactors of the package.
-keep class com.blackstack.beatbox.TTMLResponse { *; }
-keep class com.blackstack.beatbox.LRCLIBResponse { *; }
-keep class com.blackstack.beatbox.LyricsOvhResponse { *; }
-keep class com.blackstack.beatbox.Musixmatch* { *; }
-keep class com.blackstack.beatbox.Netease* { *; }
-keep class com.blackstack.beatbox.Genius* { *; }

# ---------------------------------------------------------------------------
# Domain models
# ---------------------------------------------------------------------------
-keep class com.blackstack.beatbox.AudioFile { *; }
-keep class com.blackstack.beatbox.Playlist { *; }
-keep class com.blackstack.beatbox.AppTheme { *; }
-keep class com.blackstack.beatbox.AppLanguage { *; }
-keep class com.blackstack.beatbox.EQPreset { *; }

# ---------------------------------------------------------------------------
# Service
# ---------------------------------------------------------------------------
# The service is referenced from the manifest (kept automatically), but the
# Binder inner class is only reached through reflection from the Activity.
-keep class com.blackstack.beatbox.MediaPlaybackService { *; }
-keep class com.blackstack.beatbox.MediaPlaybackService$MusicBinder { *; }

# ---------------------------------------------------------------------------
# Third party
# ---------------------------------------------------------------------------
# JAudioTagger reads tag fields through reflection.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# ---------------------------------------------------------------------------
# General
# ---------------------------------------------------------------------------
# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
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
