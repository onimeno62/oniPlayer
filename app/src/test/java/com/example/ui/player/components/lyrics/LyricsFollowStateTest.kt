package com.example.ui.player.components.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsFollowStateTest {

    @Test
    fun activeLineIsActive() {
        assertEquals(LyricEmphasis.Active, lyricEmphasisFor(index = 5, activeIndex = 5))
    }

    @Test
    fun previousAndNextTwoLinesAreNear() {
        assertEquals(LyricEmphasis.Near, lyricEmphasisFor(4, 5))
        assertEquals(LyricEmphasis.Near, lyricEmphasisFor(6, 5))
        assertEquals(LyricEmphasis.Near, lyricEmphasisFor(7, 5))
    }

    @Test
    fun farLinesAreDistant() {
        assertEquals(LyricEmphasis.Distant, lyricEmphasisFor(3, 5))
        assertEquals(LyricEmphasis.Distant, lyricEmphasisFor(8, 5))
        assertEquals(LyricEmphasis.Distant, lyricEmphasisFor(0, 5))
    }

    @Test
    fun beforeFirstTimestampFirstLinesAreUpcoming() {
        assertEquals(LyricEmphasis.Near, lyricEmphasisFor(0, -1))
        assertEquals(LyricEmphasis.Near, lyricEmphasisFor(1, -1))
        assertEquals(LyricEmphasis.Distant, lyricEmphasisFor(2, -1))
    }

    @Test
    fun singleLineLyrics() {
        assertEquals(LyricEmphasis.Active, lyricEmphasisFor(0, 0))
    }

    @Test
    fun offsetFormatting() {
        assertEquals("+0.30s", formatOffset(300L))
        assertEquals("-0.50s", formatOffset(-500L))
        assertEquals("0.00s", formatOffset(0L))
        assertEquals("+1.20s", formatOffset(1200L))
    }

    @Test
    fun lyricTimeFormatting() {
        assertEquals("0:00", formatLyricTime(0L))
        assertEquals("1:05", formatLyricTime(65_000L))
        assertEquals("0:00", formatLyricTime(-10L))
    }
}
