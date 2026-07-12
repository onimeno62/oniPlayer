package com.example.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object WidgetStateStore {
    private const val PREFS_NAME = "soft_player_widget_prefs"

    var songId: String? = null
        private set
    var title: String? = null
        private set
    var artist: String? = null
        private set
    var isPlaying: Boolean = false
        private set
    var progress: Long = 0L
        private set
    var duration: Long = 0L
        private set
    var isFavorite: Boolean = false
        private set
    var lyrics: String? = null
        private set
    var artworkBitmap: Bitmap? = null
        private set
    var artworkSongId: String? = null
        private set

    private var isLoaded = false

    fun save(
        context: Context,
        songId: String?,
        title: String?,
        artist: String?,
        playing: Boolean,
        progress: Long,
        duration: Long,
        favorite: Boolean,
        lyrics: String?
    ) {
        this.songId = songId
        this.title = title
        this.artist = artist
        this.isPlaying = playing
        this.progress = progress
        this.duration = duration
        this.isFavorite = favorite
        this.lyrics = lyrics
        this.isLoaded = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("song_id", songId)
            .putString("title", title)
            .putString("artist", artist)
            .putBoolean("playing", playing)
            .putLong("progress", progress)
            .putLong("duration", duration)
            .putBoolean("favorite", favorite)
            .putString("lyrics", lyrics)
            .apply()
    }

    fun saveArtwork(context: Context, songId: String?, bitmap: Bitmap?) {
        val file = File(context.cacheDir, "widget_artwork.png")
        if (bitmap == null) {
            artworkBitmap = null
            artworkSongId = null
            if (file.exists()) {
                file.delete()
            }
            return
        }

        // Avoid re-processing and re-saving artwork for the same song if we already have it in memory or file
        if (this.artworkSongId == songId && artworkBitmap != null && file.exists()) {
            return
        }

        this.artworkSongId = songId

        // Downscale bitmap to max 200px to keep disk footprint tiny, compression super fast and prevent Binder TransactionTooLargeException
        val maxDim = 200
        val scaledBitmap = try {
            if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val width = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val height = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }

        // Process bitmap to have high-fidelity rounded corners (approx 14% of dimension)
        val roundedBitmap = try {
            val radius = (Math.min(scaledBitmap.width, scaledBitmap.height) * 0.14f)
            getRoundedCornerBitmap(scaledBitmap, radius)
        } catch (e: Exception) {
            scaledBitmap
        }

        artworkBitmap = roundedBitmap
        try {
            FileOutputStream(file).use { out ->
                roundedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = 0xff424242.toInt()
        }
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = android.graphics.RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, radius, radius, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    fun load(context: Context) {
        // If we already have the state loaded/saved in-memory (same process), 
        // we can skip loading from disk to avoid heavy I/O blocking during play
        if (isLoaded) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        songId = prefs.getString("song_id", null)
        artworkSongId = songId
        title = prefs.getString("title", "No Title")
        artist = prefs.getString("artist", "Unknown Artist")
        isPlaying = prefs.getBoolean("playing", false)
        progress = prefs.getLong("progress", 0L)
        duration = prefs.getLong("duration", 0L)
        isFavorite = prefs.getBoolean("favorite", false)
        lyrics = prefs.getString("lyrics", null)

        val file = File(context.cacheDir, "widget_artwork.png")
        artworkBitmap = if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        isLoaded = true
    }
}
