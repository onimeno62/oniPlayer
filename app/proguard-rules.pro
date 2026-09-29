# oniPlayer ProGuard/R8 rules
# Performance optimization: strip debug/verbose/info logs from release builds
# Matches Poweramp/WAVORA best practice for battery and perf.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep MediaSession service for background playback
-keep class com.example.playback.MusicPlaybackService { *; }

# Keep Room entities
-keep class com.example.data.entity.** { *; }

# Keep Moshi adapters
-keep class com.example.data.api.**JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Media3 ships its own consumer/R8 rules; avoid keeping the entire library.
-dontwarn androidx.media3.**
