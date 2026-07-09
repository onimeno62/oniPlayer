package com.example.playback

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import coil.Coil
import coil.request.ImageRequest
import coil.target.Target
import com.example.MainActivity
import com.example.R
import com.example.data.entity.SongEntity
import com.example.ui.lyrics.LyricsHelper

object OniWidgetUpdater {
    private const val TAG = "OniWidgetUpdater"

    fun updateAllWidgets(context: Context, song: SongEntity?, isPlaying: Boolean, position: Long) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Mini Widget Update
        val miniComponentName = ComponentName(context, OniMiniWidgetProvider::class.java)
        val miniIds = appWidgetManager.getAppWidgetIds(miniComponentName)
        if (miniIds.isNotEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.widget_mini)
            setupMiniViews(context, views, song, isPlaying)
            appWidgetManager.updateAppWidget(miniIds, views)
        }

        // 2. Standard Widget Update
        val standardComponentName = ComponentName(context, OniStandardWidgetProvider::class.java)
        val standardIds = appWidgetManager.getAppWidgetIds(standardComponentName)
        if (standardIds.isNotEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.widget_standard)
            setupStandardViews(context, views, song, isPlaying)
            
            // Async load art if present, otherwise update immediately
            val artUri = song?.albumArtUri
            if (artUri != null && artUri.isNotEmpty()) {
                loadArtAsync(context, artUri) { bitmap ->
                    val viewsWithArt = RemoteViews(context.packageName, R.layout.widget_standard)
                    setupStandardViews(context, viewsWithArt, song, isPlaying)
                    if (bitmap != null) {
                        viewsWithArt.setImageViewBitmap(R.id.widget_standard_art, bitmap)
                    } else {
                        viewsWithArt.setImageViewResource(R.id.widget_standard_art, R.drawable.ic_play_arrow)
                    }
                    appWidgetManager.updateAppWidget(standardIds, viewsWithArt)
                }
            } else {
                views.setImageViewResource(R.id.widget_standard_art, R.drawable.ic_play_arrow)
                appWidgetManager.updateAppWidget(standardIds, views)
            }
        }

        // 3. Lyrics Widget Update
        val lyricsComponentName = ComponentName(context, OniLyricsWidgetProvider::class.java)
        val lyricsIds = appWidgetManager.getAppWidgetIds(lyricsComponentName)
        if (lyricsIds.isNotEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.widget_lyrics)
            setupLyricsViews(context, views, song, isPlaying, position)

            // Async load art if present, otherwise update immediately
            val artUri = song?.albumArtUri
            if (artUri != null && artUri.isNotEmpty()) {
                loadArtAsync(context, artUri) { bitmap ->
                    val viewsWithArt = RemoteViews(context.packageName, R.layout.widget_lyrics)
                    setupLyricsViews(context, viewsWithArt, song, isPlaying, position)
                    if (bitmap != null) {
                        viewsWithArt.setImageViewBitmap(R.id.widget_lyrics_art, bitmap)
                    } else {
                        viewsWithArt.setImageViewResource(R.id.widget_lyrics_art, R.drawable.ic_play_arrow)
                    }
                    appWidgetManager.updateAppWidget(lyricsIds, viewsWithArt)
                }
            } else {
                views.setImageViewResource(R.id.widget_lyrics_art, R.drawable.ic_play_arrow)
                appWidgetManager.updateAppWidget(lyricsIds, views)
            }
        }
    }

    private fun loadArtAsync(context: Context, uri: String, onLoaded: (Bitmap?) -> Unit) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .target(object : Target {
                override fun onSuccess(result: Drawable) {
                    val bitmap = (result as? BitmapDrawable)?.bitmap
                    onLoaded(bitmap)
                }
                override fun onError(error: Drawable?) {
                    onLoaded(null)
                }
            })
            .build()
        Coil.imageLoader(context).enqueue(request)
    }

    private fun setupMiniViews(context: Context, views: RemoteViews, song: SongEntity?, isPlaying: Boolean) {
        views.setTextViewText(R.id.widget_mini_title, song?.customTitle ?: song?.title ?: "OniPlayer")
        views.setTextViewText(R.id.widget_mini_artist, song?.customArtist ?: song?.artist ?: "Offline Audio")
        
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        views.setImageViewResource(R.id.widget_mini_btn_play_pause, playPauseIcon)

        // Click intents
        views.setOnClickPendingIntent(R.id.widget_mini_btn_play_pause, getServicePendingIntent(context, MusicPlaybackService.ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_mini_icon, getAppPendingIntent(context))
    }

    private fun setupStandardViews(context: Context, views: RemoteViews, song: SongEntity?, isPlaying: Boolean) {
        views.setTextViewText(R.id.widget_standard_title, song?.customTitle ?: song?.title ?: "OniPlayer")
        views.setTextViewText(R.id.widget_standard_artist, song?.customArtist ?: song?.artist ?: "Select a track")
        
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        views.setImageViewResource(R.id.widget_standard_btn_play_pause, playPauseIcon)

        // Click intents
        views.setOnClickPendingIntent(R.id.widget_standard_btn_prev, getServicePendingIntent(context, MusicPlaybackService.ACTION_PREVIOUS))
        views.setOnClickPendingIntent(R.id.widget_standard_btn_play_pause, getServicePendingIntent(context, MusicPlaybackService.ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_standard_btn_next, getServicePendingIntent(context, MusicPlaybackService.ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.widget_standard_art, getAppPendingIntent(context))
    }

    private fun setupLyricsViews(context: Context, views: RemoteViews, song: SongEntity?, isPlaying: Boolean, position: Long) {
        views.setTextViewText(R.id.widget_lyrics_title, song?.customTitle ?: song?.title ?: "OniPlayer")
        views.setTextViewText(R.id.widget_lyrics_artist, song?.customArtist ?: song?.artist ?: "Live Synced Lyrics")
        
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        views.setImageViewResource(R.id.widget_lyrics_btn_play_pause, playPauseIcon)

        // Click intents
        views.setOnClickPendingIntent(R.id.widget_lyrics_btn_prev, getServicePendingIntent(context, MusicPlaybackService.ACTION_PREVIOUS))
        views.setOnClickPendingIntent(R.id.widget_lyrics_btn_play_pause, getServicePendingIntent(context, MusicPlaybackService.ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_lyrics_btn_next, getServicePendingIntent(context, MusicPlaybackService.ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.widget_lyrics_art, getAppPendingIntent(context))

        // Process current lyrics
        if (song != null && !song.lyrics.isNullOrBlank()) {
            val lines = LyricsHelper.parseLrc(song.lyrics)
            if (lines.isNotEmpty()) {
                val activeIndex = LyricsHelper.getActiveLineIndex(lines, position)
                val prevLine = if (activeIndex > 0) lines[activeIndex - 1].text else ""
                val currentLine = if (activeIndex >= 0 && activeIndex < lines.size) lines[activeIndex].text else "Instrumental Section"
                val nextLine = if (activeIndex >= 0 && activeIndex < lines.size - 1) lines[activeIndex + 1].text else ""

                views.setTextViewText(R.id.widget_lyrics_line_prev, prevLine)
                views.setTextViewText(R.id.widget_lyrics_line_current, currentLine)
                views.setTextViewText(R.id.widget_lyrics_line_next, nextLine)
            } else {
                // Not synchronized lyric fallback (chunked/split by newlines)
                val rawLines = song.lyrics?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                if (rawLines.isNotEmpty()) {
                    views.setTextViewText(R.id.widget_lyrics_line_prev, "")
                    views.setTextViewText(R.id.widget_lyrics_line_current, "Plain Lyrics (Open App for full text)")
                    views.setTextViewText(R.id.widget_lyrics_line_next, "")
                } else {
                    views.setTextViewText(R.id.widget_lyrics_line_prev, "")
                    views.setTextViewText(R.id.widget_lyrics_line_current, "No lyrics added yet")
                    views.setTextViewText(R.id.widget_lyrics_line_next, "")
                }
            }
        } else {
            views.setTextViewText(R.id.widget_lyrics_line_prev, "")
            views.setTextViewText(R.id.widget_lyrics_line_current, "Play a track to view synced lyrics")
            views.setTextViewText(R.id.widget_lyrics_line_next, "")
        }
    }

    private fun getServicePendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.getService(context, action.hashCode(), intent, flags)
    }

    private fun getAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.getActivity(context, 1010, intent, flags)
    }
}
