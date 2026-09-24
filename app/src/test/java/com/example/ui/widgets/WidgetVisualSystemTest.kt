package com.example.ui.widgets

import com.example.ui.theme.DefaultSkin
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.defaultpack.shared.OniWidgetCopy
import com.example.ui.widgets.defaultpack.shared.OniWidgetProgressMath
import com.example.ui.widgets.skin.OniWidgetVisualSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetVisualSystemTest {

    @Test
    fun tinyProgressStaysVisible() {
        assertEquals(4f, OniWidgetProgressMath.fillWidth(0.001f, 200f, 4f, true), 0.001f)
    }

    @Test
    fun progressWithoutDurationHasNoFill() {
        assertEquals(0f, OniWidgetProgressMath.fillWidth(0.5f, 200f, 4f, false), 0f)
    }

    @Test
    fun progressClampsToAvailableWidth() {
        assertEquals(200f, OniWidgetProgressMath.fillWidth(1.5f, 200f, 4f, true), 0.001f)
        assertEquals(100f, OniWidgetProgressMath.fillWidth(0.5f, 200f, 4f, true), 0.001f)
    }

    @Test
    fun progressNeverExceedsTinyRails() {
        assertEquals(2f, OniWidgetProgressMath.fillWidth(0.01f, 2f, 4f, true), 0.001f)
    }

    @Test
    fun timeFormattingIsStable() {
        assertEquals("0:00", OniWidgetCopy.time(0))
        assertEquals("0:42", OniWidgetCopy.time(42_000))
        assertEquals("3:51", OniWidgetCopy.time(231_000))
        assertEquals("1:02:03", OniWidgetCopy.time(3_723_000))
        assertEquals("0:00", OniWidgetCopy.time(-5_000))
    }

    @Test
    fun blankStateCopy() {
        val state = OniWidgetPlaybackState()
        assertEquals("Nothing playing", OniWidgetCopy.title(state))
        assertEquals("Tap to open oniPlayer", OniWidgetCopy.artist(state))
        val frame = OniWidgetCopy.lyricFrame(state)
        assertFalse(frame.isLyric)
        assertEquals("Nothing playing", frame.current)
    }

    @Test
    fun pausedStateIsStatedInText() {
        val state = OniWidgetPlaybackState(songId = "1", title = "Song", artist = "Artist", isPlaying = false)
        assertTrue(OniWidgetCopy.statusMeta(state).startsWith("Paused"))
    }

    @Test
    fun lyricFrames() {
        val base = OniWidgetPlaybackState(songId = "1", title = "Song", artist = "Artist")

        val none = OniWidgetCopy.lyricFrame(base.copy(hasLyrics = false))
        assertFalse(none.isLyric)
        assertEquals("No lyrics for this song", none.current)

        val gap = OniWidgetCopy.lyricFrame(base.copy(hasLyrics = true, activeLyric = null, nextLyric = "Next"))
        assertFalse(gap.isLyric)
        assertEquals("Next", gap.next)

        val live = OniWidgetCopy.lyricFrame(
            base.copy(hasLyrics = true, previousLyric = "", activeLyric = "Now", nextLyric = "Later")
        )
        assertTrue(live.isLyric)
        assertNull(live.previous)
        assertEquals("Now", live.current)
        assertEquals("Later", live.next)
    }

    @Test
    fun widgetTokensComeFromSkin() {
        val dark = DefaultSkin.createSkin(isDark = true)
        val light = DefaultSkin.createSkin(isDark = false)
        assertTrue(dark.widgets.atmosphereStrength > light.widgets.atmosphereStrength)
        assertTrue(OniWidgetVisualSystem.track(dark).alpha > 0f)
        assertTrue(OniWidgetVisualSystem.track(light).alpha > 0f)
        assertEquals(dark.colors.primary, OniWidgetVisualSystem.primary(dark))
    }
}
