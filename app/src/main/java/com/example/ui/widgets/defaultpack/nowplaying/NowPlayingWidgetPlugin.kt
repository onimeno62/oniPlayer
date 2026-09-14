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
import com.example.ui.widgets.skin.OniWidgetVisualSystem

class NowPlayingWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.nowplaying"
    override val packId = "default.pack"
    override val name = "Now Playing"
    override val description = "Flagship playback widget with full transport context."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = NowPlayingWidgetRenderer()
}

class NowPlayingWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(ColorProvider(OniWidgetVisualSystem.surface(skin))).clickable(actionStartActivity<MainActivity>()).padding(12.dp)) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Small(state, skin, art)
                WidgetSize.SIZE_4X2 -> Medium(state, skin, art)
                WidgetSize.SIZE_4X4 -> Large(state, skin, art)
            }
        }
    }

    @Composable
    private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 56)
            Spacer(GlanceModifier.width(11.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 14.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 10.sp))
                Spacer(GlanceModifier.height(6.dp))
                Progress(state, skin, 140)
            }
            Spacer(GlanceModifier.width(9.dp))
            Play(state, skin, 38)
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 104)
            Spacer(GlanceModifier.width(15.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text("NOW PLAYING", style = TextStyle(ColorProvider(OniWidgetVisualSystem.primary(skin)), 9.sp, FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Text(state.title, maxLines = 2, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 18.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp))
                Spacer(GlanceModifier.height(9.dp))
                Waveform(skin)
                Spacer(GlanceModifier.height(5.dp))
                Progress(state, skin, 165)
                Spacer(GlanceModifier.height(8.dp))
                Controls(state, skin)
            }
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(art, 164)
            Spacer(GlanceModifier.height(10.dp))
            Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 19.sp, FontWeight.Medium, textAlign = TextAlign.Center))
            Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp, textAlign = TextAlign.Center))
            Spacer(GlanceModifier.height(8.dp))
            Progress(state, skin, 225)
            Spacer(GlanceModifier.height(3.dp))
            Row(GlanceModifier.width(225.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Time(state.positionMs, skin)
                Time(state.durationMs, skin)
            }
            Spacer(GlanceModifier.height(8.dp))
            Controls(state, skin)
        }
    }

    @Composable
    private fun Artwork(bitmap: Bitmap?, size: Int) = Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.size(size.dp).cornerRadius(20.dp))

    @Composable
    private fun Waveform(skin: OniSkinTokens) {
        val heights = listOf(5, 9, 14, 8, 18, 11, 7, 16, 10, 20, 13, 7, 16, 11, 19, 9, 6, 14)
        Row(verticalAlignment = Alignment.CenterVertically) {
            heights.forEachIndexed { index, height ->
                Box(GlanceModifier.width(2.dp).height(height.dp).background(ColorProvider(if (index < 9) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.muted(skin))).cornerRadius(2.dp)) {}
                if (index < heights.lastIndex) Spacer(GlanceModifier.width(3.dp))
            }
        }
    }

    @Composable
    private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens, width: Int) {
        val fraction = state.progress.coerceIn(0f, 1f)
        val filled = (width * fraction).coerceAtLeast(if (fraction > 0f) 3f else 0f)
        Box(GlanceModifier.width(width.dp).height(7.dp), Alignment.CenterStart) {
            Box(GlanceModifier.fillMaxWidth().height(3.dp).background(ColorProvider(OniWidgetVisualSystem.track(skin))).cornerRadius(2.dp)) {}
            if (filled > 0f) Box(GlanceModifier.width(filled.dp).height(3.dp).background(ColorProvider(OniWidgetVisualSystem.primary(skin))).cornerRadius(2.dp)) {}
        }
    }

    @Composable
    private fun Controls(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Action(R.drawable.ic_skip_previous, "Previous", 34, SkipPreviousActionCallback::class.java, skin, false)
            Spacer(GlanceModifier.width(10.dp))
            Play(state, skin, 46)
            Spacer(GlanceModifier.width(10.dp))
            Action(R.drawable.ic_skip_next, "Next", 34, SkipNextActionCallback::class.java, skin, false)
        }
    }

    @Composable
    private fun Play(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) = Action(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", size, TogglePlayPauseActionCallback::class.java, skin, true)

    @Composable
    private fun Action(res: Int, description: String, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, skin: OniSkinTokens, primary: Boolean) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(if (primary) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.control(skin))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(callback)))
    }

    @Composable
    private fun Time(ms: Long, skin: OniSkinTokens) = Text(time(ms), style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 8.sp))

    private fun time(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1000).toInt()
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
