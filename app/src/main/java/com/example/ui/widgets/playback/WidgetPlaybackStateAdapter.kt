package com.example.ui.widgets.playback

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.MainActivity
import com.example.playback.OniAudioEngine
import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.widgets.core.OniWidgetPlaybackState
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Adapter that safely maps oniPlayer core playback state to [OniWidgetPlaybackState].
 * Decouples widgets completely from ViewModels.
 */
object WidgetPlaybackStateAdapter {
    private val artworkCache = ConcurrentHashMap<String, Bitmap>()

    fun fromPlaybackState(state: PlaybackState): OniWidgetPlaybackState {
        val song = state.currentSong
        val (currentLyric, prevLyric, nextLyric) = resolveLyrics(song?.lyrics, state.positionMs)

        return OniWidgetPlaybackState(
            songId = song?.id,
            title = song?.displayTitle ?: "No track playing",
            artist = song?.displayArtist ?: "oniPlayer",
            album = song?.displayAlbum ?: "",
            albumArtworkUri = song?.albumArtUri,
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isShuffle = state.shuffleEnabled,
            isRepeat = state.repeatMode == RepeatMode.ONE,
            activeLyric = currentLyric,
            previousLyric = prevLyric,
            nextLyric = nextLyric,
            hasLyrics = !song?.lyrics.isNullOrBlank()
        )
    }

    private fun resolveLyrics(rawLyrics: String?, positionMs: Long): Triple<String?, String?, String?> {
        if (rawLyrics.isNullOrBlank()) return Triple(null, null, null)

        if (LyricsHelper.isSynced(rawLyrics)) {
            val lines = LyricsHelper.parseLrc(rawLyrics).filter { it.text.isNotBlank() }
            if (lines.isEmpty()) return Triple(null, null, null)

            val activeIdx = LyricsHelper.getActiveLineIndex(lines, positionMs)
            val current = if (activeIdx in lines.indices) lines[activeIdx].text else lines.firstOrNull()?.text
            val prev = if (activeIdx > 0 && activeIdx - 1 in lines.indices) lines[activeIdx - 1].text else null
            val next = if (activeIdx + 1 in lines.indices) lines[activeIdx + 1].text else null
            return Triple(current, prev, next)
        } else {
            val plainLines = rawLyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val current = plainLines.getOrNull(0)
            val next = plainLines.getOrNull(1)
            return Triple(current, null, next)
        }
    }

    /**
     * Loads artwork bitmap with bounds memory safety for remote views / Glance.
     */
    fun loadArtworkBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        artworkCache[uriString]?.let { return it }

        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = if (uriString.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)
            } else if (uriString.startsWith("file://") || File(uriString).exists()) {
                File(uri.path ?: uriString).inputStream()
            } else {
                null
            }

            inputStream?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // downsample for widget efficiency
                }
                val bitmap = BitmapFactory.decodeStream(stream, null, options)
                if (bitmap != null) {
                    artworkCache[uriString] = bitmap
                }
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    fun createOpenAppIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    // Playback control actions delegated cleanly to OniAudioEngine
    fun togglePlayPause(context: Context) {
        val engine = OniAudioEngine.getInstance(context)
        if (engine.isPlaying.value) {
            engine.pause()
        } else {
            engine.resume()
        }
    }

    fun skipNext(context: Context) {
        OniAudioEngine.getInstance(context).skipNext()
    }

    fun skipPrevious(context: Context) {
        OniAudioEngine.getInstance(context).skipPrevious()
    }

    fun toggleShuffle(context: Context) {
        OniAudioEngine.getInstance(context).toggleShuffle()
    }

    fun toggleRepeat(context: Context) {
        OniAudioEngine.getInstance(context).toggleRepeat()
    }
}
