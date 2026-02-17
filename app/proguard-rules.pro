# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep MediaPlayer and audio related classes
-keep class android.media.** { *; }
-keep class androidx.media.** { *; }

# Keep JAudioTagger classes
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data classes and models
-keep class com.blackstack.beatbox.AudioFile { *; }
-keep class com.blackstack.beatbox.Playlist { *; }
-keep class com.blackstack.beatbox.AppTheme { *; }
-keep class com.blackstack.beatbox.AppLanguage { *; }
-keep class com.blackstack.beatbox.EQPreset { *; }

# Keep service classes
-keep class com.blackstack.beatbox.MediaPlaybackService { *; }
-keep class com.blackstack.beatbox.MediaPlaybackService$MusicBinder { *; }

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