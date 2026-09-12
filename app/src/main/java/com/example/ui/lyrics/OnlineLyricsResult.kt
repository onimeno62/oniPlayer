package com.example.ui.lyrics

/**
 * First-class supported and classified lyric languages.
 */
enum class LyricsLanguage(val code: String, val displayName: String, val flagEmoji: String) {
    UNKNOWN("unknown", "Unknown", "🌐"),
    ORIGINAL("original", "Original", "🎵"),
    ENGLISH("en", "English", "🇺🇸"),
    JAPANESE("ja", "Japanese", "🇯🇵"),
    ROMAJI("ja-Latn", "Romaji (Phonetic)", "🔤"),
    KOREAN("ko", "Korean", "🇰🇷"),
    CHINESE("zh", "Chinese", "🇨🇳"),
    SPANISH("es", "Spanish", "🇪🇸"),
    FRENCH("fr", "French", "🇫🇷"),
    GERMAN("de", "German", "🇩🇪"),
    PERSIAN("fa", "Persian", "🇮🇷"),
    ARABIC("ar", "Arabic", "🇸🇦"),
    RUSSIAN("ru", "Russian", "🇷🇺");

    val labelWithFlag: String
        get() = "$flagEmoji $displayName"

    companion object {
        fun fromCode(code: String?): LyricsLanguage {
            if (code.isNullOrBlank()) return UNKNOWN
            val lower = code.trim().lowercase()
            return entries.firstOrNull { 
                it.code.equals(lower, ignoreCase = true) || 
                it.name.equals(lower, ignoreCase = true) 
            } ?: UNKNOWN
        }

        fun fromDisplayName(name: String?): LyricsLanguage {
            if (name.isNullOrBlank()) return UNKNOWN
            val lower = name.trim().lowercase()
            return entries.firstOrNull { 
                it.displayName.equals(name.trim(), ignoreCase = true) ||
                lower.startsWith(it.name.lowercase())
            } ?: UNKNOWN
        }
    }
}

/**
 * Real online lyrics sources/databases.
 */
enum class LyricsSource(val id: String, val displayName: String) {
    LRCLIB("lrclib", "LRCLIB Database"),
    LYRIST("lyrist", "Lyrist API"),
    LYRICS_OVH("ovh", "Lyrics.ovh"),
    LOCAL("local", "Local File"),
    MANUAL("manual", "Manual Entry");

    companion object {
        fun fromSourceId(id: String?): LyricsSource {
            if (id.isNullOrBlank()) return LRCLIB
            val lower = id.lowercase()
            return when {
                lower.contains("lrclib") -> LRCLIB
                lower.contains("lyrist") -> LYRIST
                lower.contains("ovh") -> LYRICS_OVH
                lower.contains("local") -> LOCAL
                lower.contains("manual") || lower.contains("custom") -> MANUAL
                else -> LRCLIB
            }
        }
    }
}

/**
 * Domain model representing a verified real online lyrics search result.
 */
data class OnlineLyricsResult(
    val title: String,
    val artist: String = "",
    val album: String = "",
    val score: Double,
    val lyrics: String,
    val language: LyricsLanguage,
    val source: LyricsSource,
    val isSynchronized: Boolean
) {
    val cleanSnippet: String by lazy {
        lyrics.lines()
            .map { LyricsHelper.stripLrcTags(it).trim() }
            .firstOrNull { it.isNotBlank() } ?: ""
    }
}
