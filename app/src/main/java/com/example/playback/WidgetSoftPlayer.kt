package com.example.playback

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.ui.lyrics.LyricsHelper

class WidgetSoftPlayer : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SoftPlayerWidget()
}

class SoftPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(context: Context) {
        // Load the latest state from standard shared preferences
        WidgetStateStore.load(context)

        val title = WidgetStateStore.title ?: "No Title"
        val artist = WidgetStateStore.artist ?: "Unknown Artist"
        val isPlaying = WidgetStateStore.isPlaying
        val progress = WidgetStateStore.progress
        val duration = WidgetStateStore.duration
        val isFavorite = WidgetStateStore.isFavorite
        val lyricsText = WidgetStateStore.lyrics
        val artwork = WidgetStateStore.artworkBitmap

        // Layout container with beautiful frosted gradient background
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.bg_soft_player_widget))
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Square Artwork Cover (100dp x 100dp) with heavily rounded corners
                Box(
                    modifier = GlanceModifier
                        .size(100.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    if (artwork != null) {
                        Image(
                            provider = ImageProvider(artwork),
                            contentDescription = "Cover Art",
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_library_music),
                            contentDescription = "Placeholder Art",
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Right Side: Title, Artist, Progress Bar, Lyrics, Playback Controls
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Header Row: Song Info and Like button
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier.defaultWeight()
                        ) {
                            Text(
                                text = title,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFB0A0C0)),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }

                        // Like/Favorite Button
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(
                                    actionRunCallback<PlayerActionCallback>(
                                        actionParametersOf(ActionKeys.actionKey to MusicPlaybackService.ACTION_TOGGLE_FAVORITE)
                                    )
                                )
                        ) {
                            val favIcon = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                            Image(
                                provider = ImageProvider(favIcon),
                                contentDescription = "Favorite Button",
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Progress bar row with times
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(progress),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFB0A0C0)),
                                fontSize = 10.sp
                            )
                        )

                        // Visual custom progress bar using fixed 110dp container
                        Box(
                            modifier = GlanceModifier
                                .width(110.dp)
                                .height(3.dp)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Progress track background (translucent white)
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(ColorProvider(Color(0x26FFFFFF)))
                            ) {}

                            // Progress indicator (vibrant pink)
                            val fraction = if (duration > 0) {
                                (progress.toFloat() / duration.toFloat()).coerceIn(0.0f, 1.0f)
                            } else {
                                0.0f
                            }
                            if (fraction > 0f) {
                                val filledWidth = (110.0f * fraction).coerceIn(0.0f, 110.0f).dp
                                Box(
                                    modifier = GlanceModifier
                                        .width(filledWidth)
                                        .height(3.dp)
                                        .background(ColorProvider(Color(0xFFFF4081)))
                                ) {}
                            }
                        }

                        Text(
                            text = formatTime(duration),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFB0A0C0)),
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Lyrics block: Active line and next line
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val activeLine = getActiveLyricsLine(lyricsText, progress)
                        val nextLine = getNextLyricsLine(lyricsText, progress)

                        Text(
                            text = activeLine,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = nextLine,
                            style = TextStyle(
                                color = ColorProvider(Color(0x99B0A0C0)),
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Controls Row: Prev, Play/Pause, Next
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        Box(
                            modifier = GlanceModifier
                                .size(24.dp)
                                .clickable(
                                    actionRunCallback<PlayerActionCallback>(
                                        actionParametersOf(ActionKeys.actionKey to MusicPlaybackService.ACTION_PREVIOUS)
                                    )
                                )
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_skip_previous),
                                contentDescription = "Previous Button",
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(28.dp))

                        // Circular glowing Play/Pause Button
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .background(ImageProvider(R.drawable.bg_widget_glass_btn))
                                .clickable(
                                    actionRunCallback<PlayerActionCallback>(
                                        actionParametersOf(ActionKeys.actionKey to MusicPlaybackService.ACTION_PLAY_PAUSE)
                                    )
                                )
                        ) {
                            val playIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            Image(
                                provider = ImageProvider(playIcon),
                                contentDescription = "Play Pause Button",
                                modifier = GlanceModifier.fillMaxSize().padding(6.dp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(28.dp))

                        // Next Button
                        Box(
                            modifier = GlanceModifier
                                .size(24.dp)
                                .clickable(
                                    actionRunCallback<PlayerActionCallback>(
                                        actionParametersOf(ActionKeys.actionKey to MusicPlaybackService.ACTION_NEXT)
                                    )
                                )
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_skip_next),
                                contentDescription = "Next Button",
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }

    private fun getActiveLyricsLine(lyricsText: String?, progressMs: Long): String {
        if (lyricsText.isNullOrBlank()) {
            return "No lyrics available"
        }
        val lines = LyricsHelper.parseLrc(lyricsText)
        if (lines.isNotEmpty()) {
            val idx = LyricsHelper.getActiveLineIndex(lines, progressMs)
            if (idx in lines.indices) {
                return lines[idx].text
            }
        } else {
            // Plain lyrics estimation
            val rawLines = lyricsText.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
            if (rawLines.isNotEmpty()) {
                val percent = if (WidgetStateStore.duration > 0) {
                    progressMs.toFloat() / WidgetStateStore.duration.toFloat()
                } else 0f
                val idx = (percent * rawLines.size).toInt().coerceIn(0, rawLines.size - 1)
                return rawLines[idx]
            }
        }
        return "Enjoy your music"
    }

    private fun getNextLyricsLine(lyricsText: String?, progressMs: Long): String {
        if (lyricsText.isNullOrBlank()) {
            return ""
        }
        val lines = LyricsHelper.parseLrc(lyricsText)
        if (lines.isNotEmpty()) {
            val idx = LyricsHelper.getActiveLineIndex(lines, progressMs)
            if (idx + 1 in lines.indices) {
                return lines[idx + 1].text
            }
        } else {
            // Plain lyrics estimation
            val rawLines = lyricsText.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
            if (rawLines.isNotEmpty()) {
                val percent = if (WidgetStateStore.duration > 0) {
                    progressMs.toFloat() / WidgetStateStore.duration.toFloat()
                } else 0f
                val idx = (percent * rawLines.size).toInt().coerceIn(0, rawLines.size - 1)
                if (idx + 1 in rawLines.indices) {
                    return rawLines[idx + 1]
                }
            }
        }
        return ""
    }
}

object ActionKeys {
    val actionKey = ActionParameters.Key<String>("action_key")
}

class PlayerActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val action = parameters[ActionKeys.actionKey] ?: return
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
