package com.example.ui.widgets.defaultpack.compactplayer

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

class CompactPlayerWidgetPlugin : OniWidgetPlugin {
    override val id: String = "oni.compactplayer"
    override val packId: String = "default.pack"
    override val name: String = "Compact Player"
    override val description: String = "Ultra-responsive and tactile home-screen controller prioritizing fast playback access."
    override val supportedSizes: Set<WidgetSize> = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)

    override fun createRenderer(): OniWidgetRenderer = CompactPlayerWidgetRenderer()
}

class CompactPlayerWidgetRenderer : OniWidgetRenderer {

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
            .padding(8.dp)
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
            Box(
                modifier = GlanceModifier
                    .size(42.dp)
                    .background(ColorProvider(skin.colors.primaryContainer))
                    .cornerRadius(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (artworkBitmap != null) {
                    Image(
                        provider = ImageProvider(artworkBitmap),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = "Music",
                        modifier = GlanceModifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = state.title,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(skin.colors.textPrimary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Prev, Play, Next Cluster
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(30.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(15.dp)
                        .padding(6.dp)
                        .clickable(actionRunCallback<SkipPreviousActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

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

                Spacer(modifier = GlanceModifier.width(4.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(30.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(15.dp)
                        .padding(6.dp)
                        .clickable(actionRunCallback<SkipNextActionCallback>())
                )
            }
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
                Box(
                    modifier = GlanceModifier
                        .size(54.dp)
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
                            modifier = GlanceModifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
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
                        text = state.artist,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(skin.colors.textSecondary),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Centered Controls
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(34.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(17.dp)
                        .padding(8.dp)
                        .clickable(actionRunCallback<SkipPreviousActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(18.dp))

                Image(
                    provider = ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier
                        .size(42.dp)
                        .background(ColorProvider(skin.colors.primary))
                        .cornerRadius(21.dp)
                        .padding(10.dp)
                        .clickable(actionRunCallback<TogglePlayPauseActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(18.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(34.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(17.dp)
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
            Box(
                modifier = GlanceModifier
                    .size(130.dp)
                    .background(ColorProvider(skin.colors.primaryContainer))
                    .cornerRadius(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (artworkBitmap != null) {
                    Image(
                        provider = ImageProvider(artworkBitmap),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(14.dp)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = "Music",
                        modifier = GlanceModifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            Text(
                text = state.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(skin.colors.textPrimary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = state.artist,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(skin.colors.textSecondary),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_shuffle),
                    contentDescription = "Shuffle",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(ColorProvider(if (state.isShuffle) skin.colors.primaryContainer else skin.colors.surfaceVariant))
                        .cornerRadius(16.dp)
                        .padding(7.dp)
                        .clickable(actionRunCallback<ToggleShuffleActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(38.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(19.dp)
                        .padding(9.dp)
                        .clickable(actionRunCallback<SkipPreviousActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                Image(
                    provider = ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier
                        .size(48.dp)
                        .background(ColorProvider(skin.colors.primary))
                        .cornerRadius(24.dp)
                        .padding(11.dp)
                        .clickable(actionRunCallback<TogglePlayPauseActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(38.dp)
                        .background(ColorProvider(skin.colors.surfaceVariant))
                        .cornerRadius(19.dp)
                        .padding(9.dp)
                        .clickable(actionRunCallback<SkipNextActionCallback>())
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_repeat),
                    contentDescription = "Repeat",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(ColorProvider(if (state.isRepeat) skin.colors.primaryContainer else skin.colors.surfaceVariant))
                        .cornerRadius(16.dp)
                        .padding(7.dp)
                        .clickable(actionRunCallback<ToggleRepeatActionCallback>())
                )
            }
        }
    }
}
