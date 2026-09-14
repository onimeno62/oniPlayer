package com.example.ui.widgets.defaultpack.miniplayer

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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.*
import com.example.ui.widgets.core.*
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter
import com.example.ui.widgets.skin.OniWidgetVisualSystem

class MiniPlayerWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.miniplayer"
    override val packId = "default.pack"
    override val name = "Mini Player"
    override val description = "Transport-first compact music controller."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = MiniPlayerWidgetRenderer()
}

class MiniPlayerWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        Box(GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(ColorProvider(OniWidgetVisualSystem.surface(skin))).clickable(actionStartActivity<MainActivity>()).padding(12.dp)) {
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
            Artwork(art, 54)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 14.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 10.sp))
                Spacer(GlanceModifier.height(6.dp))
                Progress(state, skin, 132)
            }
            Spacer(GlanceModifier.width(8.dp))
            Transport(state, skin, 28)
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 76)
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 16.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp))
                Spacer(GlanceModifier.height(9.dp))
                Progress(state, skin, 180)
                Spacer(GlanceModifier.height(8.dp))
                Transport(state, skin, 30)
            }
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 142)
            Spacer(GlanceModifier.width(18.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text("PLAYING", style = TextStyle(ColorProvider(OniWidgetVisualSystem.primary(skin)), 9.sp, FontWeight.Bold))
                Spacer(GlanceModifier.height(5.dp))
                Text(state.title, maxLines = 2, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 20.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 12.sp))
                Spacer(GlanceModifier.height(14.dp))
                Progress(state, skin, 175)
                Spacer(GlanceModifier.height(10.dp))
                Transport(state, skin, 38)
            }
        }
    }

    @Composable
    private fun Artwork(bitmap: Bitmap?, size: Int) = Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.size(size.dp).cornerRadius(16.dp))

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
    private fun Transport(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionIcon(R.drawable.ic_skip_previous, "Previous", size, SkipPreviousActionCallback::class.java, skin, false)
            Spacer(GlanceModifier.width(7.dp))
            ActionIcon(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", size + 8, TogglePlayPauseActionCallback::class.java, skin, true)
            Spacer(GlanceModifier.width(7.dp))
            ActionIcon(R.drawable.ic_skip_next, "Next", size, SkipNextActionCallback::class.java, skin, false)
        }
    }

    @Composable
    private fun ActionIcon(res: Int, description: String, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, skin: OniSkinTokens, primary: Boolean) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(if (primary) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.control(skin))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(callback)))
    }
}
