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
     */
    fun getActiveLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        
        // Find the last line whose timestamp is <= positionMs
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= positionMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
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

