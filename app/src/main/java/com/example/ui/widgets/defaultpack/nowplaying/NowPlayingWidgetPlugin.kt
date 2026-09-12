package com.example.ui.widgets.defaultpack.nowplaying

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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.SkipNextActionCallback
import com.example.ui.widgets.actions.SkipPreviousActionCallback
import com.example.ui.widgets.actions.TogglePlayPauseActionCallback
import com.example.ui.widgets.actions.ToggleRepeatActionCallback
import com.example.ui.widgets.actions.ToggleShuffleActionCallback
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter

class NowPlayingWidgetPlugin : OniWidgetPlugin {
    override val id: String = "oni.nowplaying"
    override val packId: String = "default.pack"
    override val name: String = "Now Playing"
    override val description: String = "Flagship oniPlayer music identity and control widget with skin-aware artwork and playback controls."
    override val supportedSizes: Set<WidgetSize> = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)

    override fun createRenderer(): OniWidgetRenderer = NowPlayingWidgetRenderer()
}

class NowPlayingWidgetRenderer : OniWidgetRenderer {

    @Composable
    override fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        val openAppIntent = WidgetPlaybackStateAdapter.createOpenAppIntent(context)
        val artworkBitmap = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)

        val bgModifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(skin.colors.surface))
            .cornerRadius(16.dp)
            .padding(10.dp)
            .clickable(actionStartActivity(openAppIntent))

        when (size) {
            WidgetSize.SIZE_4X1 -> Render4x1(state, skin, artworkBitmap, bgModifier)
            WidgetSize.SIZE_4X2 -> Render4x2(state, skin, artworkBitmap, bgModifier)
            WidgetSize.SIZE_4X4 -> Render4x4(state, skin, artworkBitmap, bgModifier)
        }
    }

    @Composable
    private fun Render4x1(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        artworkBitmap: android.graphics.Bitmap?,
        modifier: GlanceModifier
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            Box(
                modifier = GlanceModifier
                    .size(46.dp)
                    .background(ColorProvider(skin.colors.primaryContainer))
                    .cornerRadius(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (artworkBitmap != null) {
                    Image(
                        provider = ImageProvider(artworkBitmap),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(10.dp)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = "Music",
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Title & Artist
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textPrimary),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (state.album.isNotBlank()) "${state.artist} · ${state.album}" else state.artist,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(skin.colors.textSecondary),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Play / Pause
            Image(
                provider = ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(ColorProvider(skin.colors.primary))
                    .cornerRadius(18.dp)
                    .padding(8.dp)
                    .clickable(actionRunCallback<TogglePlayPauseActionCallback>())
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Next
            Image(
                provider = ImageProvider(R.drawable.ic_skip_next),
                contentDescription = "Next",
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(ColorProvider(skin.colors.surfaceVariant))
                    .cornerRadius(16.dp)
                    .padding(6.dp)
                    .clickable(actionRunCallback<SkipNextActionCallback>())
            )
        }
    }

    @Composable
    private fun Render4x2(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        artworkBitmap: android.graphics.Bitmap?,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Artwork
                Box(
                    modifier = GlanceModifier
                        .size(68.dp)
                        .background(ColorProvider(skin.colors.primaryContainer))
                        .cornerRadius(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            provider = ImageProvider(artworkBitmap),
                            contentDescription = "Album Art",
                            modifier = GlanceModifier.fillMaxSize().cornerRadius(12.dp)
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_note),
                            contentDescription = "Music",
                            modifier = GlanceModifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(skin.colors.textPrimary),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = state.artist,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(skin.colors.primary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (state.album.isNotBlank()) {
                        Text(
                            text = state.album,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(skin.colors.textTertiary),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Controls row
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
                        .size(44.dp)
                        .background(ColorProvider(skin.colors.primary))
                        .cornerRadius(22.dp)
                        .padding(10.dp)
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

    @Composable
    private fun Render4x4(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        artworkBitmap: android.graphics.Bitmap?,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immersive Top Artwork
            Box(
                modifier = GlanceModifier
                    .size(140.dp)
                    .background(ColorProvider(skin.colors.primaryContainer))
                    .cornerRadius(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (artworkBitmap != null) {
                    Image(
                        provider = ImageProvider(artworkBitmap),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = "Music",
                        modifier = GlanceModifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Title & Artist
            Text(
                text = state.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(skin.colors.textPrimary),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = state.artist,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(skin.colors.primary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(14.dp))

            // Primary Controls Row (Prev, Play/Pause, Next)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_shuffle),
                    contentDescription = "Shuffle",
                    modifier = GlanceModifier
                        .size(34.dp)
                        .background(ColorProvider(if (state.isShuffle) skin.colors.primaryContainer else skin.colors.surfaceVariant))
                        .cornerRadius(17.dp)
                        .padding(8.dp)
                        .clickable(actionRunCallback<ToggleShuffleActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(14.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(40.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(20.dp)
                        .padding(10.dp)
                        .clickable(actionRunCallback<SkipPreviousActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(14.dp))

                Image(
                    provider = ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(ColorProvider(skin.colors.primary))
                        .cornerRadius(26.dp)
                        .padding(12.dp)
                        .clickable(actionRunCallback<TogglePlayPauseActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(14.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(40.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(20.dp)
                        .padding(10.dp)
                        .clickable(actionRunCallback<SkipNextActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(14.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_repeat),
                    contentDescription = "Repeat",
                    modifier = GlanceModifier
                        .size(34.dp)
                        .background(ColorProvider(if (state.isRepeat) skin.colors.primaryContainer else skin.colors.surfaceVariant))
                        .cornerRadius(17.dp)
                        .padding(8.dp)
                        .clickable(actionRunCallback<ToggleRepeatActionCallback>())
                )
            }
        }
    }
}
