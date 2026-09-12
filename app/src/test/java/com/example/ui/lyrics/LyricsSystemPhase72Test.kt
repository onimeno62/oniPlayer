package com.example.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSystemPhase72Test {

    @Test
    fun testParseLrcAndActiveLineIndex() {
        val lrc = """
            [00:00.00] Intro instrumental
            [00:05.00] First lyric line
            [00:10.50] Second lyric line
            [00:20.00] Third lyric line
        """.trimIndent()

        val lines = LyricsHelper.parseLrc(lrc)
        assertEquals(4, lines.size)
        assertTrue(LyricsHelper.isSynced(lrc))

        // Position 2s -> line 0
        assertEquals(0, LyricsHelper.getActiveLineIndex(lines, 2000L))
        // Position 5s -> line 1
        assertEquals(1, LyricsHelper.getActiveLineIndex(lines, 5000L))
        // Position 15s -> line 2 (holds until line 3)
        assertEquals(2, LyricsHelper.getActiveLineIndex(lines, 15000L))
        // Position 25s -> line 3
        assertEquals(3, LyricsHelper.getActiveLineIndex(lines, 25000L))
    }

    @Test
    fun testLanguageDetectionDeterministic() {
        // Japanese
        val japaneseLyrics = "[00:05.00] 私の心の中にいつも君がいる"
        assertEquals(LyricsLanguage.JAPANESE, LyricsHelper.detectLanguage(japaneseLyrics))

        // Korean
        val koreanLyrics = "[00:10.00] 사랑해 지금 너와 함께라면"
        assertEquals(LyricsLanguage.KOREAN, LyricsHelper.detectLanguage(koreanLyrics))

        // Romaji
        val romajiLyrics = "[00:10.00] watashi no kokoro no sekai kimi to kono basho de"
        assertEquals(LyricsLanguage.ROMAJI, LyricsHelper.detectLanguage(romajiLyrics))

        // Spanish
        val spanishLyrics = "[00:12.00] Porque te amo con todo el corazon en esta vida"
        assertEquals(LyricsLanguage.SPANISH, LyricsHelper.detectLanguage(spanishLyrics))

        // Persian
        val persianLyrics = "[00:15.00] من و تو در این شب برای عشق"
        assertEquals(LyricsLanguage.PERSIAN, LyricsHelper.detectLanguage(persianLyrics))

        // English
        val englishLyrics = "[00:01.00] Hello darkness my old friend I came to talk"
        assertEquals(LyricsLanguage.ENGLISH, LyricsHelper.detectLanguage(englishLyrics))
    }

    @Test
    fun testStripLrcTags() {
        val lrc = "[01:23.45] This is a line\n[01:28.00] Another line"
        val plain = LyricsHelper.stripLrcTags(lrc)
        assertFalse(plain.contains("[01:23.45]"))
        assertTrue(plain.contains("This is a line"))
        assertTrue(plain.contains("Another line"))
    }

    @Test
    fun testShiftLrcTimestamps() {
        val lrc = "[00:10.00] Line one\n[00:20.00] Line two"
        val shifted = LyricsHelper.shiftLrcTimestamps(lrc, 500L) // +0.5s
        assertTrue(shifted.contains("[00:10.50]"))
        assertTrue(shifted.contains("[00:20.50]"))
    }
}
