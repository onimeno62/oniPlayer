package com.example.ui.widgets

import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import com.example.ui.theme.DefaultSkin
import com.example.ui.widgets.core.OniWidgetRegistry
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.defaultpack.DefaultWidgetPack
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetPluginSystemTest {

    @Before
    fun setUp() {
        OniWidgetRegistry.clear()
        OniWidgetRegistry.registerPack(DefaultWidgetPack)
    }

    @Test
    fun testDefaultWidgetsRegistered() {
        val plugins = OniWidgetRegistry.getAllPlugins()
        assertEquals(3, plugins.size)

        val nowPlaying = OniWidgetRegistry.getPlugin("oni.nowplaying")
        val compactPlayer = OniWidgetRegistry.getPlugin("oni.compactplayer")
        val lyrics = OniWidgetRegistry.getPlugin("oni.lyrics")

        assertNotNull(nowPlaying)
        assertNotNull(compactPlayer)
        assertNotNull(lyrics)

        assertEquals("Now Playing", nowPlaying?.name)
        assertEquals("Compact Player", compactPlayer?.name)
        assertEquals("Lyrics & Visualizer", lyrics?.name)
    }

    @Test
    fun testSupportedSizes() {
        val nowPlaying = OniWidgetRegistry.getPlugin("oni.nowplaying")
        assertNotNull(nowPlaying)
        assertTrue(nowPlaying!!.supportedSizes.contains(WidgetSize.SIZE_4X1))
        assertTrue(nowPlaying.supportedSizes.contains(WidgetSize.SIZE_4X2))
        assertTrue(nowPlaying.supportedSizes.contains(WidgetSize.SIZE_4X4))
    }

    @Test
    fun testWidgetSizeResolution() {
        assertEquals(WidgetSize.SIZE_4X1, WidgetSize.fromDimensions(250, 60))
        assertEquals(WidgetSize.SIZE_4X2, WidgetSize.fromDimensions(250, 140))
        assertEquals(WidgetSize.SIZE_4X4, WidgetSize.fromDimensions(250, 300))
    }

    @Test
    fun testWidgetPlaybackStateAdapter() {
        val song = com.example.data.entity.SongEntity(
            id = "test_song_1",
            title = "Midnight Drive",
            artist = "Oni",
            album = "After Dark",
            duration = 200000L,
            filePath = "/music/test.mp3",
            lyrics = "[00:10.00] Line 1\n[00:20.00] Line 2\n[00:30.00] Line 3"
        )

        val playbackState = PlaybackState(
            currentSong = song,
            isPlaying = true,
            positionMs = 25000L,
            durationMs = 200000L,
            bufferedPositionMs = 50000L,
            beatEnergy = 0.5f,
            isPreparing = false,
            autoNextCountdownSeconds = null,
            shuffleEnabled = true,
            shuffleMode = com.example.playback.ShuffleMode.RANDOM,
            repeatMode = RepeatMode.ONE,
            queue = listOf(song)
        )

        val widgetState = WidgetPlaybackStateAdapter.fromPlaybackState(playbackState)

        assertEquals("test_song_1", widgetState.songId)
        assertEquals("Midnight Drive", widgetState.title)
        assertEquals("Oni", widgetState.artist)
        assertEquals("After Dark", widgetState.album)
        assertTrue(widgetState.isPlaying)
        assertTrue(widgetState.isShuffle)
        assertTrue(widgetState.isRepeat)
        assertTrue(widgetState.hasLyrics)
        assertEquals("Line 2", widgetState.activeLyric)
        assertEquals("Line 1", widgetState.previousLyric)
        assertEquals("Line 3", widgetState.nextLyric)
        assertEquals(0.125f, widgetState.progress, 0.001f)
    }

    @Test
    fun testSkinTokensAvailableForWidgets() {
        val darkSkin = DefaultSkin.createSkin(isDark = true)
        val lightSkin = DefaultSkin.createSkin(isDark = false)

        assertTrue(darkSkin.colors.isDark)
        assertFalse(lightSkin.colors.isDark)

        assertNotNull(darkSkin.colors.primary)
        assertNotNull(darkSkin.colors.surface)
        assertNotNull(darkSkin.colors.textPrimary)
        assertNotNull(lightSkin.colors.textPrimary)
    }
}
