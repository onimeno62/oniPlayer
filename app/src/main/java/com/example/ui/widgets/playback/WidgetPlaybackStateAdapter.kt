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
import java.util.LinkedHashMap

/** Adapter between the single playback state source and widget state. */
object WidgetPlaybackStateAdapter {
    private const val MAX_ARTWORK_CACHE_ENTRIES = 8
    private const val SNAPSHOT_PREFS = "oni_widget_playback_snapshot"
    private const val KEY_INITIALIZED = "initialized"
    private const val KEY_SONG_ID = "song_id"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_ALBUM = "album"
    private const val KEY_ARTWORK_URI = "artwork_uri"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_POSITION_MS = "position_ms"
    private const val KEY_DURATION_MS = "duration_ms"
    private const val KEY_SHUFFLE = "shuffle"
    private const val KEY_REPEAT = "repeat"
    private const val KEY_ACTIVE_LYRIC = "active_lyric"
    private const val KEY_PREVIOUS_LYRIC = "previous_lyric"
    private const val KEY_NEXT_LYRIC = "next_lyric"
    private const val KEY_HAS_LYRICS = "has_lyrics"

    private val artworkCache = object : LinkedHashMap<String, Bitmap>(MAX_ARTWORK_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > MAX_ARTWORK_CACHE_ENTRIES
    }

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
            positionMs = state.positionMs.coerceAtLeast(0L),
            durationMs = state.durationMs.coerceAtLeast(0L),
            isShuffle = state.shuffleEnabled,
            isRepeat = state.repeatMode == RepeatMode.ONE,
            activeLyric = currentLyric,
            previousLyric = prevLyric,
            nextLyric = nextLyric,
            hasLyrics = !song?.lyrics.isNullOrBlank()
        )
    }

    fun persistPlaybackState(context: Context, state: PlaybackState) {
        val widgetState = fromPlaybackState(state)
        context.applicationContext.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_SONG_ID, widgetState.songId)
            .putString(KEY_TITLE, widgetState.title)
            .putString(KEY_ARTIST, widgetState.artist)
            .putString(KEY_ALBUM, widgetState.album)
            .putString(KEY_ARTWORK_URI, widgetState.albumArtworkUri)
            .putBoolean(KEY_IS_PLAYING, widgetState.isPlaying)
            .putLong(KEY_POSITION_MS, widgetState.positionMs)
            .putLong(KEY_DURATION_MS, widgetState.durationMs)
            .putBoolean(KEY_SHUFFLE, widgetState.isShuffle)
            .putBoolean(KEY_REPEAT, widgetState.isRepeat)
            .putString(KEY_ACTIVE_LYRIC, widgetState.activeLyric)
            .putString(KEY_PREVIOUS_LYRIC, widgetState.previousLyric)
            .putString(KEY_NEXT_LYRIC, widgetState.nextLyric)
            .putBoolean(KEY_HAS_LYRICS, widgetState.hasLyrics)
            .apply()
    }

    fun fromPersistedPlaybackState(context: Context): OniWidgetPlaybackState {
        val prefs = context.applicationContext.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) return OniWidgetPlaybackState()
        return OniWidgetPlaybackState(
            songId = prefs.getString(KEY_SONG_ID, null),
            title = prefs.getString(KEY_TITLE, "No track playing") ?: "No track playing",
            artist = prefs.getString(KEY_ARTIST, "oniPlayer") ?: "oniPlayer",
            album = prefs.getString(KEY_ALBUM, "") ?: "",
            albumArtworkUri = prefs.getString(KEY_ARTWORK_URI, null),
            isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false),
            positionMs = prefs.getLong(KEY_POSITION_MS, 0L),
            durationMs = prefs.getLong(KEY_DURATION_MS, 0L),
            isShuffle = prefs.getBoolean(KEY_SHUFFLE, false),
            isRepeat = prefs.getBoolean(KEY_REPEAT, false),
            activeLyric = prefs.getString(KEY_ACTIVE_LYRIC, null),
            previousLyric = prefs.getString(KEY_PREVIOUS_LYRIC, null),
            nextLyric = prefs.getString(KEY_NEXT_LYRIC, null),
            hasLyrics = prefs.getBoolean(KEY_HAS_LYRICS, false)
        )
    }

    private fun resolveLyrics(rawLyrics: String?, positionMs: Long): Triple<String?, String?, String?> {
        if (rawLyrics.isNullOrBlank()) return Triple(null, null, null)
        if (LyricsHelper.isSynced(rawLyrics)) {
            val lines = LyricsHelper.parseLrc(rawLyrics).filter { it.text.isNotBlank() }
            if (lines.isEmpty()) return Triple(null, null, null)
            val activeIdx = LyricsHelper.getActiveLineIndex(lines, positionMs)
            return Triple(lines.getOrNull(activeIdx)?.text, lines.getOrNull(activeIdx - 1)?.text, lines.getOrNull(activeIdx + 1)?.text)
        }
        val lines = rawLyrics.lines().map(String::trim).filter(String::isNotEmpty)
        return Triple(lines.getOrNull(0), null, lines.getOrNull(1))
    }

    fun loadArtworkBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        synchronized(artworkCache) { artworkCache[uriString]?.let { return it } }
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = when {
                uriString.startsWith("content://") -> context.contentResolver.openInputStream(uri)
                uriString.startsWith("file://") -> uri.path?.let { File(it).inputStream() }
                File(uriString).exists() -> File(uriString).inputStream()
                else -> null
            }
            inputStream?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 2; inPreferredConfig = Bitmap.Config.RGB_565 }
                BitmapFactory.decodeStream(stream, null, options)?.also { bitmap -> synchronized(artworkCache) { artworkCache[uriString] = bitmap } }
            }
        } catch (_: Exception) { null }
    }

    fun createOpenAppIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP }

    fun togglePlayPause(context: Context) {
        val engine = OniAudioEngine.getInstance(context)
        if (engine.isPlaying.value) engine.pause() else engine.resume()
    }

    fun skipNext(context: Context) = OniAudioEngine.getInstance(context).skipNext()
    fun skipPrevious(context: Context) = OniAudioEngine.getInstance(context).skipPrevious()
    fun toggleShuffle(context: Context) = OniAudioEngine.getInstance(context).toggleShuffle()
    fun toggleRepeat(context: Context) = OniAudioEngine.getInstance(context).toggleRepeat()
}
