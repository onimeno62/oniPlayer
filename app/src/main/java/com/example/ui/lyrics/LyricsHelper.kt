package com.example.ui.lyrics

import android.util.Log
import java.io.File
import java.util.regex.Pattern

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

object LyricsHelper {
    private const val TAG = "LyricsHelper"

    // Matches [mm:ss.xx] or [mm:ss:xx] or [mm:ss.xxx] or [mm:ss]
    private val LRC_REGEX = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?\\]")

    /**
     * Parses a lyrics string into a list of LrcLine.
     * If no timestamps are found, returns an empty list (indicating plain lyrics).
     */
    fun parseLrc(lyricsText: String?): List<LrcLine> {
        if (lyricsText.isNullOrBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()
        val rawLines = lyricsText.split("\n", "\r")

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Check if line starts with an LRC tag like [ar:Artist], [ti:Title]
            if (trimmed.startsWith("[ar:") || trimmed.startsWith("[ti:") || 
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") || 
                trimmed.startsWith("[length:")) {
                continue
            }

            val matcher = LRC_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val milliStr = matcher.group(3)
                
                var milli = 0L
                if (milliStr != null) {
                    val m = milliStr.toLongOrNull() ?: 0L
                    milli = if (milliStr.length == 2) {
                        m * 10 // centiseconds to milliseconds
                    } else if (milliStr.length == 1) {
                        m * 100
                    } else {
                        m // already milliseconds or other format
                    }
                }

                val timestampMs = (min * 60 * 1000) + (sec * 1000) + milli
                timestamps.add(timestampMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val lyricsContent = trimmed.substring(lastMatchEnd).trim()
                for (time in timestamps) {
                    lines.add(LrcLine(time, lyricsContent))
                }
            }
        }

        // Sort lines by timestamp
        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Returns whether the given lyrics text is synchronized (contains timestamp tags).
     */
    fun isSynced(lyricsText: String?): Boolean {
        if (lyricsText.isNullOrBlank()) return false
        val matcher = LRC_REGEX.matcher(lyricsText)
        return matcher.find()
    }

    /**
     * Strips all LRC metadata and timestamp tags, returning beautifully formatted clean plain text.
     */
    fun stripLrcTags(lyricsText: String?): String {
        if (lyricsText.isNullOrBlank()) return ""
        val lines = lyricsText.split("\n", "\r")
        val cleanLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                cleanLines.add("")
                continue
            }
            if (trimmed.startsWith("[ar:") || trimmed.startsWith("[ti:") || 
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") || 
                trimmed.startsWith("[length:")) {
                continue
            }
            // Strip timestamp matches: [00:00.00] or [00:00]
            val cleaned = LRC_REGEX.matcher(trimmed).replaceAll("").trim()
            cleanLines.add(cleaned)
        }
        return cleanLines.joinToString("\n").trim()
    }

    /**
     * Finds the index of the active lyrics line for a given playback position in milliseconds.
     * During gaps between lines (such as instrumental breaks or empty LRC tags),
     * keeps the highlight on the current non-blank line until the next line begins.
     */
    fun getActiveLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        
        // Find the last line with non-blank text whose timestamp is <= positionMs
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= positionMs) {
                if (lines[i].text.isNotBlank()) {
                    activeIndex = i
                }
            } else {
                break
            }
        }
        return activeIndex
    }

    /**
     * Deterministically classifies the natural language of lyrics based on Unicode character
     * scripts and key vocabulary markers. Offline, fast, and does not require AI.
     */
    fun detectLanguage(lyricsText: String, titleOrMetadata: String = ""): LyricsLanguage {
        val lowerMeta = titleOrMetadata.lowercase()
        if (lowerMeta.contains("romaji") || lowerMeta.contains("romanized")) return LyricsLanguage.ROMAJI
        if (lowerMeta.contains("english ver") || lowerMeta.contains("english trans")) return LyricsLanguage.ENGLISH
        if (lowerMeta.contains("spanish ver") || lowerMeta.contains("español")) return LyricsLanguage.SPANISH

        val clean = stripLrcTags(lyricsText).take(1000)
        if (clean.isBlank()) return LyricsLanguage.UNKNOWN

        val hasKana = clean.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }
        val hasHangul = clean.any { it in '\uAC00'..'\uD7AF' || it in '\u1100'..'\u11FF' }
        val hasKanji = clean.any { it in '\u4E00'..'\u9FFF' }
        val hasCyrillic = clean.any { it in '\u0400'..'\u04FF' }
        val hasPersianArabic = clean.any { it in '\u0600'..'\u06FF' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }

        if (hasKana) return LyricsLanguage.JAPANESE
        if (hasHangul) return LyricsLanguage.KOREAN
        if (hasKanji) return LyricsLanguage.CHINESE
        if (hasCyrillic) return LyricsLanguage.RUSSIAN

        if (hasPersianArabic) {
            // Persian specific characters: گ، چ، پ، ژ (گ=\u06AF, چ=\u0686, پ=\u067E, ژ=\u0698)
            val hasPersianSpecific = clean.any { it in listOf('\u06AF', '\u0686', '\u067E', '\u0698') }
            val persianWords = listOf("من", "تو", "ما", "دل", "عشق", "برای", "این", "که", "به", "شد", "بود", "یک")
            val words = clean.split(Regex("\\s+")).take(50)
            if (hasPersianSpecific || words.any { persianWords.contains(it) }) {
                return LyricsLanguage.PERSIAN
            }
            return LyricsLanguage.ARABIC
        }

        val lower = clean.lowercase()
        val words = lower.split(Regex("[\\s,;.!?'\"()]+")).filter { it.isNotBlank() }.take(80)

        val romajiMarkers = setOf("watashi", "anata", "bokura", "kokoro", "sekai", "kimi", "kono", "sono", "ano", "ashita", "sarang", "jigeum", "neowa", "tsubasa", "kaze", "hikari", "yoru")
        if (words.count { romajiMarkers.contains(it) } >= 2) {
            return LyricsLanguage.ROMAJI
        }

        val spanishMarkers = setOf("que", "de", "la", "el", "en", "te", "amor", "corazon", "corazón", "por", "para", "como", "vida", "suenos", "sueños", "siempre")
        if (words.count { spanishMarkers.contains(it) } >= 3) {
            return LyricsLanguage.SPANISH
        }

        val frenchMarkers = setOf("je", "tu", "est", "le", "la", "dans", "pour", "avec", "nous", "vous", "mon", "une", "pas", "etre", "être", "amour")
        if (words.count { frenchMarkers.contains(it) } >= 3) {
            return LyricsLanguage.FRENCH
        }

        val germanMarkers = setOf("ich", "du", "ist", "und", "der", "die", "das", "nicht", "wir", "ein", "eine", "zu", "mit", "herz")
        if (words.count { germanMarkers.contains(it) } >= 3) {
            return LyricsLanguage.GERMAN
        }

        return LyricsLanguage.ENGLISH
    }

    /**
     * Formats milliseconds into LRC timestamp: [mm:ss.xx]
     */
    fun formatLrcTime(ms: Long): String {
        val totalSecs = ms / 1000
        val min = totalSecs / 60
        val sec = totalSecs % 60
        val centisec = (ms % 1000) / 10
        return String.format("[%02d:%02d.%02d]", min, sec, centisec)
    }

    /**
     * Rebuilds an LRC string from a list of LrcLines.
     */
    fun buildLrcString(lines: List<LrcLine>): String {
        val sb = StringBuilder()
        for (line in lines) {
            sb.append(formatLrcTime(line.timestampMs))
            sb.append(" ")
            sb.append(line.text)
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    /**
     * Shifts all timestamps in an LRC string by offsetMs (can be positive or negative).
     * Timestamps are clamped to >= 0ms.
     */
    fun shiftLrcTimestamps(lyricsText: String?, offsetMs: Long): String {
        if (lyricsText.isNullOrBlank() || offsetMs == 0L) return lyricsText ?: ""
        val parsed = parseLrc(lyricsText)
        if (parsed.isEmpty()) return lyricsText

        val shifted = parsed.map { line ->
            line.copy(timestampMs = maxOf(0L, line.timestampMs + offsetMs))
        }
        return buildLrcString(shifted)
    }

    /**
     * Searches for a local .lrc file next to the song's audio file path.
     * E.g., if song is /music/track.mp3, checks for /music/track.lrc
     */
    fun findLocalLrcFile(songFilePath: String): String? {
        try {
            val audioFile = File(songFilePath)
            if (!audioFile.exists()) return null

            val parent = audioFile.parentFile ?: return null
            val baseName = audioFile.nameWithoutExtension
            
            // Check for .lrc file
            val lrcFile = File(parent, "$baseName.lrc")
            if (lrcFile.exists() && lrcFile.isFile) {
                Log.d(TAG, "Found local LRC file at: ${lrcFile.absolutePath}")
                return lrcFile.readText()
            }

            // Also check for case-insensitive extensions or .txt lyrics
            val txtFile = File(parent, "$baseName.txt")
            if (txtFile.exists() && txtFile.isFile) {
                Log.d(TAG, "Found local TXT lyrics file at: ${txtFile.absolutePath}")
                return txtFile.readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking for local LRC file: ${e.message}", e)
        }
        return null
    }
}
