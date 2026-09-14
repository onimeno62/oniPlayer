package com.example.ui.widgets.defaultpack.nowplaying

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
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
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.*
import com.example.ui.widgets.core.*
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter
import com.example.ui.widgets.skin.AuroraWidgetStyle

class NowPlayingWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.nowplaying"
    override val packId = "default.pack"
    override val name = "Now Playing"
    override val description = "Flagship Aurora Glass now playing widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = NowPlayingWidgetRenderer()
}

class NowPlayingWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        AuroraCard(skin, size) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Mini(state, skin, art)
                WidgetSize.SIZE_4X2 -> Medium(state, skin, art)
                WidgetSize.SIZE_4X4 -> Album(state, skin, art)
            }
        }
    }

    @Composable
    private fun AuroraCard(skin: OniSkinTokens, size: WidgetSize, content: @Composable () -> Unit) {
        val compact = size == WidgetSize.SIZE_4X1
        Box(GlanceModifier.fillMaxSize().cornerRadius(26.dp).background(ColorProvider(AuroraWidgetStyle.surface(skin))).clickable(actionStartActivity<MainActivity>()).padding(if (compact) 10.dp else 13.dp)) {
            Box(GlanceModifier.size(if (size == WidgetSize.SIZE_4X4) 190.dp else 118.dp).background(ColorProvider(AuroraWidgetStyle.glow(skin).copy(alpha = .16f))).cornerRadius(100.dp).align(Alignment.TopEnd)) {}
            Box(GlanceModifier.size(if (compact) 74.dp else 130.dp).background(ColorProvider(AuroraWidgetStyle.secondary(skin).copy(alpha = .08f))).cornerRadius(80.dp).align(Alignment.BottomStart)) {}
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(AuroraWidgetStyle.secondary(skin).copy(alpha = .30f))).align(Alignment.TopCenter)) {}
            content()
        }
    }

    @Composable
    private fun Artwork(bitmap: Bitmap?, size: Int, skin: OniSkinTokens) {
        Box(GlanceModifier.size((size + 8).dp).background(ColorProvider(AuroraWidgetStyle.glow(skin).copy(alpha = .38f))).cornerRadius(((size + 8) / 5).dp).padding(4.dp), Alignment.Center) {
            Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.fillMaxSize().cornerRadius((size / 5).dp))
        }
    }

    @Composable
    private fun Mini(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 56, skin)
            Spacer(GlanceModifier.width(11.dp))
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 14.sp, FontWeight.Medium))
                        Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 10.sp))
                    }
                    Image(ImageProvider(R.drawable.ic_favorite_border), "Favorite", GlanceModifier.size(21.dp).padding(2.dp))
                }
                Spacer(GlanceModifier.height(6.dp))
                Progress(state, skin, 150)
                Spacer(GlanceModifier.height(4.dp))
                Controls(state, skin, 27, 38)
            }
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 92, skin)
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 18.sp, FontWeight.Medium))
                        Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 11.sp))
                    }
                    Image(ImageProvider(R.drawable.ic_favorite_border), "Favorite", GlanceModifier.size(24.dp).padding(3.dp))
                }
                Spacer(GlanceModifier.height(10.dp))
                Waveform(skin)
                Spacer(GlanceModifier.height(5.dp))
                Progress(state, skin, 230)
                Spacer(GlanceModifier.height(3.dp))
                Row(GlanceModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TimeText(state.positionMs, skin)
                    TimeText(state.durationMs, skin)
                }
                Spacer(GlanceModifier.height(7.dp))
                Controls(state, skin, 32, 46)
            }
        }
    }

    @Composable
    private fun Album(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 158, skin)
            Spacer(GlanceModifier.height(10.dp))
            Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 19.sp, FontWeight.Medium, textAlign = TextAlign.Center))
            Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 11.sp, textAlign = TextAlign.Center))
            Spacer(GlanceModifier.height(10.dp))
            PlayButton(state.isPlaying, skin, 46)
        }
    }

    @Composable
    private fun Waveform(skin: OniSkinTokens) {
        val heights = listOf(5, 9, 14, 8, 18, 11, 7, 16, 10, 20, 13, 7, 16, 11, 19, 9, 6, 14, 10, 18, 8, 13, 6, 11)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            heights.forEachIndexed { index, height ->
                Box(GlanceModifier.width(2.dp).height(height.dp).background(ColorProvider(if (index < 12) AuroraWidgetStyle.primary(skin) else AuroraWidgetStyle.textSecondary(skin).copy(alpha = .55f))).cornerRadius(2.dp)) {}
                if (index < heights.lastIndex) Spacer(GlanceModifier.width(3.dp))
            }
        }
    }

    @Composable
    private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens, width: Int) {
        val fraction = state.progress
        val filled = (width * fraction).coerceAtLeast(if (fraction > 0f) 3f else 0f)
        Box(GlanceModifier.fillMaxWidth().height(9.dp), Alignment.CenterStart) {
            Box(GlanceModifier.fillMaxWidth().height(3.dp).background(ColorProvider(AuroraWidgetStyle.track(skin).copy(alpha = .80f))).cornerRadius(2.dp)) {}
            if (filled > 0f) {
                Box(GlanceModifier.width(filled.dp).height(3.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin))).cornerRadius(2.dp)) {
                    Box(GlanceModifier.size(8.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin))).cornerRadius(4.dp).align(Alignment.CenterEnd)) {}
                }
            }
        }
    }

    @Composable
    private fun Controls(state: OniWidgetPlaybackState, skin: OniSkinTokens, side: Int, center: Int) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(R.drawable.ic_skip_previous, "Previous", skin, side, SkipPreviousActionCallback::class.java)
            Spacer(GlanceModifier.width(12.dp))
            PlayButton(state.isPlaying, skin, center)
            Spacer(GlanceModifier.width(12.dp))
            IconButton(R.drawable.ic_skip_next, "Next", skin, side, SkipNextActionCallback::class.java)
        }
    }

    @Composable
    private fun PlayButton(playing: Boolean, skin: OniSkinTokens, size: Int) {
        Image(ImageProvider(if (playing) R.drawable.ic_pause else R.drawable.ic_play), if (playing) "Pause" else "Play", GlanceModifier.size(size.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin))).cornerRadius((size / 2).dp).padding((size / 4).dp).clickable(actionRunCallback(TogglePlayPauseActionCallback::class.java)))
    }

    @Composable
    private fun IconButton(res: Int, description: String, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(AuroraWidgetStyle.elevated(skin).copy(alpha = .92f))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(callback)))
    }

    @Composable
    private fun TimeText(ms: Long, skin: OniSkinTokens) = Text(time(ms), style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 8.sp))

    private fun time(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1000).toInt()
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
