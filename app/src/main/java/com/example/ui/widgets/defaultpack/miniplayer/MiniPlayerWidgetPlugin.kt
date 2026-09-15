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

class MiniPlayerWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.miniplayer"
    override val packId = "default.pack"
    override val name = "Mini Player"
    override val description = "Minimal artwork and transport controller."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = MiniPlayerWidgetRenderer()
}

class MiniPlayerWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        Box(
            GlanceModifier.fillMaxSize().cornerRadius(22.dp)
                .background(ColorProvider(OniWidgetVisualSystem.surface(skin)))
                .clickable(actionStartActivity<MainActivity>())
        ) {
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
            Artwork(art, 82, 20)
            Spacer(GlanceModifier.width(13.dp))
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 15.sp, FontWeight.Bold))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 10.sp))
            }
            Spacer(GlanceModifier.width(8.dp))
            Transport(state, skin, 25, 37)
            Spacer(GlanceModifier.width(11.dp))
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 96, 17)
            Spacer(GlanceModifier.width(16.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 18.sp, FontWeight.Bold))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp))
                Spacer(GlanceModifier.height(11.dp))
                Progress(state, skin, 170)
                Spacer(GlanceModifier.height(10.dp))
                Transport(state, skin, 29, 45)
            }
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(art, 224, 22)
            Spacer(GlanceModifier.height(12.dp))
            Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 19.sp, FontWeight.Bold, textAlign = TextAlign.Center))
            Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp, textAlign = TextAlign.Center))
            Spacer(GlanceModifier.height(12.dp))
            Progress(state, skin, 220)
            Spacer(GlanceModifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bare(R.drawable.ic_shuffle, "Shuffle", 28, ToggleShuffleActionCallback::class.java)
                Spacer(GlanceModifier.width(17.dp))
                Bare(R.drawable.ic_skip_previous, "Previous", 31, SkipPreviousActionCallback::class.java)
                Spacer(GlanceModifier.width(17.dp))
                Play(state, skin, 50)
                Spacer(GlanceModifier.width(17.dp))
                Bare(R.drawable.ic_skip_next, "Next", 31, SkipNextActionCallback::class.java)
                Spacer(GlanceModifier.width(17.dp))
                Bare(R.drawable.ic_repeat, "Repeat", 28, ToggleRepeatActionCallback::class.java)
            }
        }
    }

    @Composable
    private fun Artwork(bitmap: Bitmap?, size: Int, radius: Int) {
        Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.size(size.dp).cornerRadius(radius.dp))
    }

    @Composable
    private fun Transport(state: OniWidgetPlaybackState, skin: OniSkinTokens, side: Int, center: Int) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Bare(R.drawable.ic_skip_previous, "Previous", side, SkipPreviousActionCallback::class.java)
            Spacer(GlanceModifier.width(8.dp))
            Play(state, skin, center)
            Spacer(GlanceModifier.width(8.dp))
            Bare(R.drawable.ic_skip_next, "Next", side, SkipNextActionCallback::class.java)
        }
    }

    @Composable
    private fun Play(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) {
        Image(
            ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            if (state.isPlaying) "Pause" else "Play",
            GlanceModifier.size(size.dp).background(ColorProvider(OniWidgetVisualSystem.primary(skin))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>())
        )
    }

    @Composable
    private fun Bare(res: Int, description: String, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).padding(5.dp).clickable(actionRunCallback(callback)))
    }

    @Composable
    private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens, width: Int) {
        val fraction = state.progress.coerceIn(0f, 1f)
        val filled = (width * fraction).coerceAtLeast(if (fraction > 0f) 3f else 0f)
        Box(GlanceModifier.width(width.dp).height(5.dp), Alignment.CenterStart) {
            Box(GlanceModifier.fillMaxWidth().height(2.dp).background(ColorProvider(OniWidgetVisualSystem.track(skin))).cornerRadius(2.dp)) {}
            if (filled > 0f) Box(GlanceModifier.width(filled.dp).height(2.dp).background(ColorProvider(OniWidgetVisualSystem.primary(skin))).cornerRadius(2.dp)) {}
        }
    }
}
