package com.example.ui.widgets.defaultpack.lyrics

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.SkipNextActionCallback
import com.example.ui.widgets.actions.SkipPreviousActionCallback
import com.example.ui.widgets.actions.TogglePlayPauseActionCallback
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter

class LyricsWidgetPlugin : OniWidgetPlugin {
    override val id: String = "oni.lyrics"
    override val packId: String = "default.pack"
    override val name: String = "Lyrics & Visualizer"
    override val description: String = "Displays real-time scrolling lyrics and context cues directly on your launcher."
    override val supportedSizes: Set<WidgetSize> = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)

    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}

class LyricsWidgetRenderer : OniWidgetRenderer {

    @Composable
    override fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        val openAppIntent = WidgetPlaybackStateAdapter.createOpenAppIntent(context)

        val bgModifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(skin.colors.surface))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(openAppIntent))

        when (size) {
            WidgetSize.SIZE_4X1 -> Render4x1(state, skin, bgModifier)
            WidgetSize.SIZE_4X2 -> Render4x2(state, skin, bgModifier)
            WidgetSize.SIZE_4X4 -> Render4x4(state, skin, bgModifier)
        }
    }

    @Composable
    private fun Render4x1(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        modifier: GlanceModifier
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_lyrics),
                contentDescription = "Lyrics",
                modifier = GlanceModifier.size(24.dp)
            )

            Spacer(modifier = GlanceModifier.width(10.dp))

            val displayText = when {
                state.isBlank -> "No track playing"
                !state.hasLyrics -> "♪  No lyrics available"
                state.activeLyric.isNullOrBlank() -> "♪  (Intro / Instrumental)"
                else -> state.activeLyric
            }

            Text(
                text = displayText,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(if (state.activeLyric != null) skin.colors.primary else skin.colors.textSecondary),
                    fontSize = 13.sp,
                    fontWeight = if (state.activeLyric != null) FontWeight.Bold else FontWeight.Normal
                )
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Image(
                provider = ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = GlanceModifier
                    .size(34.dp)
                    .background(ColorProvider(skin.colors.primary))
                    .cornerRadius(17.dp)
                    .padding(8.dp)
                    .clickable(actionRunCallback<TogglePlayPauseActionCallback>())
            )
        }
    }

    @Composable
    private fun Render4x2(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!state.hasLyrics || state.isBlank) {
                Image(
                    provider = ImageProvider(R.drawable.ic_lyrics),
                    contentDescription = null,
                    modifier = GlanceModifier.size(32.dp)
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = if (state.isBlank) "Play a track to view lyrics" else "No lyrics available for this track",
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textSecondary),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            } else {
                // Previous context line
                Text(
                    text = state.previousLyric ?: "",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textTertiary),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // CURRENT ACTIVE LYRIC LINE (Hero emphasis)
                Text(
                    text = state.activeLyric ?: "(Instrumental)",
                    maxLines = 2,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.primary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Next upcoming line
                Text(
                    text = state.nextLyric ?: "",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textTertiary),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    @Composable
    private fun Render4x4(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_lyrics),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp)
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (!state.hasLyrics || state.isBlank) {
                Text(
                    text = if (state.isBlank) "No music playing" else "No synchronized lyrics found",
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textPrimary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Tap to open oniPlayer and search lyrics online",
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textSecondary),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
            } else {
                Text(
                    text = state.previousLyric ?: "",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textTertiary),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = state.activeLyric ?: "(Instrumental)",
                    maxLines = 2,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.primary),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = state.nextLyric ?: "",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textTertiary),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            // Track info strip
            Text(
                text = "${state.title} · ${state.artist}",
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(skin.colors.textSecondary),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Playback controls row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(18.dp)
                        .padding(8.dp)
                        .clickable(actionRunCallback<SkipPreviousActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(16.dp))

                Image(
                    provider = ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier
                        .size(46.dp)
                        .background(ColorProvider(skin.colors.primary))
                        .cornerRadius(23.dp)
                        .padding(11.dp)
                        .clickable(actionRunCallback<TogglePlayPauseActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(16.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(18.dp)
                        .padding(8.dp)
                        .clickable(actionRunCallback<SkipNextActionCallback>())
                )
            }
        }
    }
}
