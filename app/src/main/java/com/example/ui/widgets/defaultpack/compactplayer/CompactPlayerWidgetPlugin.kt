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

class CompactPlayerWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.compactplayer"
    override val packId = "default.pack"
    override val name = "Compact Player"
    override val description = "Minimal premium playback controller."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = CompactPlayerWidgetRenderer()
}

class CompactPlayerWidgetRenderer : OniWidgetRenderer {
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

    @Composable private fun Art(artwork: android.graphics.Bitmap?, size: Int, skin: OniSkinTokens) {
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
            Art(artwork, 48, skin)
            Spacer(GlanceModifier.width(9.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textPrimary), fontSize = 13.sp, fontWeight = FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textSecondary), fontSize = 10.sp))
            }
            Spacer(GlanceModifier.width(7.dp))
            Image(ImageProvider(R.drawable.ic_skip_previous), "Previous", GlanceModifier.size(30.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius(15.dp).padding(7.dp).clickable(actionRunCallback<SkipPreviousActionCallback>()))
            Spacer(GlanceModifier.width(4.dp))
            Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(38.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(19.dp).padding(9.dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
            Spacer(GlanceModifier.width(4.dp))
            Image(ImageProvider(R.drawable.ic_skip_next), "Next", GlanceModifier.size(30.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius(15.dp).padding(7.dp).clickable(actionRunCallback<SkipNextActionCallback>()))
        }
    }

    @Composable private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, artwork: android.graphics.Bitmap?, modifier: GlanceModifier) {
        Column(modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Art(artwork, 62, skin)
                Spacer(GlanceModifier.width(11.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(state.title, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textPrimary), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(state.artist, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.primary), fontSize = 11.sp, fontWeight = FontWeight.Medium))
                    Text(if (state.isPlaying) "Playing now" else "Paused", maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 9.sp))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Progress(state, skin, 210)
            Spacer(GlanceModifier.height(7.dp))
            Controls(state, skin, 34, 44)
        }
    }

    @Composable private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, artwork: android.graphics.Bitmap?, modifier: GlanceModifier) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Art(artwork, 136, skin)
            Spacer(GlanceModifier.height(9.dp))
            Text(state.title, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textPrimary), fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Text(state.artist, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.primary), fontSize = 12.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(9.dp))
            Progress(state, skin, 250)
            Spacer(GlanceModifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_shuffle), "Shuffle", GlanceModifier.size(32.dp).background(ColorProvider(if (state.isShuffle) skin.colors.primaryContainer else skin.colors.surfaceVariant)).cornerRadius(16.dp).padding(7.dp).clickable(actionRunCallback<ToggleShuffleActionCallback>()))
                Spacer(GlanceModifier.width(10.dp))
                Controls(state, skin, 38, 48)
                Spacer(GlanceModifier.width(10.dp))
                Image(ImageProvider(R.drawable.ic_repeat), "Repeat", GlanceModifier.size(32.dp).background(ColorProvider(if (state.isRepeat) skin.colors.primaryContainer else skin.colors.surfaceVariant)).cornerRadius(16.dp).padding(7.dp).clickable(actionRunCallback<ToggleRepeatActionCallback>()))
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
            Spacer(GlanceModifier.width(12.dp))
            Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(center.dp).background(ColorProvider(skin.colors.primary)).cornerRadius((center / 2).dp).padding((center / 4).dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
            Spacer(GlanceModifier.width(12.dp))
            Image(ImageProvider(R.drawable.ic_skip_next), "Next", GlanceModifier.size(side.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius((side / 2).dp).padding(8.dp).clickable(actionRunCallback<SkipNextActionCallback>()))
        }
    }
}
