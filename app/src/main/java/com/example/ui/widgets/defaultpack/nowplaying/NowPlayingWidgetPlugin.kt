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
import com.example.MainActivity
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
    override val id = "oni.nowplaying"
    override val packId = "default.pack"
    override val name = "Now Playing"
    override val description = "Flagship skin-aware now playing widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = NowPlayingWidgetRenderer()
}

class NowPlayingWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val artwork = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        val root = GlanceModifier.fillMaxSize()
            .background(ColorProvider(skin.colors.surface))
            .cornerRadius(22.dp)
            .padding(if (size == WidgetSize.SIZE_4X1) 8.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>())
        when (size) {
            WidgetSize.SIZE_4X1 -> Small(state, skin, artwork, root)
            WidgetSize.SIZE_4X2 -> Medium(state, skin, artwork, root)
            WidgetSize.SIZE_4X4 -> Large(state, skin, artwork, root)
        }
    }

    @Composable private fun Art(state: OniWidgetPlaybackState, artwork: android.graphics.Bitmap?, size: Int, skin: OniSkinTokens) {
        Box(GlanceModifier.size(size.dp).background(ColorProvider(skin.colors.primaryContainer)).cornerRadius((size / 5).dp), Alignment.Center) {
            Image(
                provider = artwork?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.ic_music_note),
                contentDescription = "Album artwork",
                modifier = if (artwork != null) GlanceModifier.fillMaxSize().cornerRadius((size / 5).dp) else GlanceModifier.size((size / 3).dp)
            )
        }
    }

    @Composable private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens, artwork: android.graphics.Bitmap?, modifier: GlanceModifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Art(state, artwork, 50, skin)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 14.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textSecondary), 11.sp))
            }
            Spacer(GlanceModifier.width(8.dp))
            Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(38.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(19.dp).padding(9.dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
            Spacer(GlanceModifier.width(5.dp))
            Image(ImageProvider(R.drawable.ic_skip_next), "Next", GlanceModifier.size(34.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius(17.dp).padding(8.dp).clickable(actionRunCallback<SkipNextActionCallback>()))
        }
    }

    @Composable private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, artwork: android.graphics.Bitmap?, modifier: GlanceModifier) {
        Column(modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Art(state, artwork, 76, skin)
                Spacer(GlanceModifier.width(12.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 17.sp, FontWeight.Bold))
                    Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.primary), 12.sp, FontWeight.Medium))
                    if (state.album.isNotBlank()) Text(state.album, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 10.sp))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Progress(state, skin, 220)
            Spacer(GlanceModifier.height(3.dp))
            Row(GlanceModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(time(state.positionMs), style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp))
                Text(time(state.durationMs), style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp))
            }
            Spacer(GlanceModifier.height(7.dp))
            Controls(state, skin, 38, 46)
        }
    }

    @Composable private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, artwork: android.graphics.Bitmap?, modifier: GlanceModifier) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.fillMaxWidth().height(4.dp), Alignment.Center) {}
            Art(state, artwork, 154, skin)
            Spacer(GlanceModifier.height(10.dp))
            Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 19.sp, FontWeight.Bold))
            Spacer(GlanceModifier.height(2.dp))
            Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.primary), 13.sp, FontWeight.Medium))
            if (state.album.isNotBlank()) Text(state.album, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 10.sp))
            Spacer(GlanceModifier.height(10.dp))
            Progress(state, skin, 260)
            Spacer(GlanceModifier.height(3.dp))
            Row(GlanceModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(time(state.positionMs), style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp))
                Text(time(state.durationMs), style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp))
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_shuffle), "Shuffle", GlanceModifier.size(34.dp).background(ColorProvider(if (state.isShuffle) skin.colors.primaryContainer else skin.colors.surfaceVariant)).cornerRadius(17.dp).padding(8.dp).clickable(actionRunCallback<ToggleShuffleActionCallback>()))
                Spacer(GlanceModifier.width(12.dp))
                Controls(state, skin, 40, 52)
                Spacer(GlanceModifier.width(12.dp))
                Image(ImageProvider(R.drawable.ic_repeat), "Repeat", GlanceModifier.size(34.dp).background(ColorProvider(if (state.isRepeat) skin.colors.primaryContainer else skin.colors.surfaceVariant)).cornerRadius(17.dp).padding(8.dp).clickable(actionRunCallback<ToggleRepeatActionCallback>()))
            }
        }
    }

    @Composable private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens, width: Int) {
        val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
        Box(GlanceModifier.fillMaxWidth().height(4.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius(2.dp)) {
            Box(GlanceModifier.width((width * fraction).coerceAtLeast(2f).dp).height(4.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(2.dp)) {}
        }
    }

    @Composable private fun Controls(state: OniWidgetPlaybackState, skin: OniSkinTokens, side: Int, center: Int) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_skip_previous), "Previous", GlanceModifier.size(side.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius((side / 2).dp).padding(8.dp).clickable(actionRunCallback<SkipPreviousActionCallback>()))
            Spacer(GlanceModifier.width(14.dp))
            Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(center.dp).background(ColorProvider(skin.colors.primary)).cornerRadius((center / 2).dp).padding((center / 4).dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
            Spacer(GlanceModifier.width(14.dp))
            Image(ImageProvider(R.drawable.ic_skip_next), "Next", GlanceModifier.size(side.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius((side / 2).dp).padding(8.dp).clickable(actionRunCallback<SkipNextActionCallback>()))
        }
    }

    private fun time(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1000).toInt()
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
