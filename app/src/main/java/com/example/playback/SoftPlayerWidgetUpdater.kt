package com.example.playback

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.updateAll
import com.example.data.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SoftPlayerWidgetUpdater {
    private val updaterScope = CoroutineScope(Dispatchers.IO)

    fun update(
        context: Context,
        song: SongEntity?,
        isPlaying: Boolean,
        progress: Long,
        duration: Long,
        isFavorite: Boolean,
        artwork: Bitmap?
    ) {
        updaterScope.launch {
            try {
                if (song == null) {
                    WidgetStateStore.save(
                        context = context,
                        songId = null,
                        title = "No Song",
                        artist = "Ready to play",
                        playing = false,
                        progress = 0L,
                        duration = 0L,
                        favorite = false,
                        lyrics = null
                    )
                    WidgetStateStore.saveArtwork(context, null, null)
                } else {
                    WidgetStateStore.save(
                        context = context,
                        songId = song.id,
                        title = song.displayTitle ?: song.title,
                        artist = song.displayArtist ?: song.artist,
                        playing = isPlaying,
                        progress = progress,
                        duration = duration,
                        favorite = isFavorite,
                        lyrics = song.lyrics
                    )
                    WidgetStateStore.saveArtwork(context, song.id, artwork)
                }

                // Push update to all widget instances on the home screen
                SoftPlayerWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateClear(context: Context) {
        update(
            context = context,
            song = null,
            isPlaying = false,
            progress = 0L,
            duration = 0L,
            isFavorite = false,
            artwork = null
        )
    }
}
