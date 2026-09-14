package com.example.ui.widgets.defaultpack.dynamicalbum

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

/** Artwork-first widget. It intentionally minimizes transport UI. */
class DynamicAlbumWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.dynamicalbum"
    override val packId = "default.pack"
    override val name = "Dynamic Album"
    override val description = "Artwork-first music identity widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = DynamicAlbumWidgetRenderer()
}

class DynamicAlbumWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(ColorProvider(OniWidgetVisualSystem.background(skin))).clickable(actionStartActivity<MainActivity>()).padding(10.dp)) {
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
            Spacer(GlanceModifier.width(11.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 14.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 10.sp))
            }
            Spacer(GlanceModifier.width(8.dp))
            Play(state, skin, 34)
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 92)
            Spacer(GlanceModifier.width(15.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text("NOW", style = TextStyle(ColorProvider(OniWidgetVisualSystem.primary(skin)), 9.sp, FontWeight.Bold))
                Spacer(GlanceModifier.height(5.dp))
                Text(state.title, maxLines = 2, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 18.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp))
                Spacer(GlanceModifier.height(10.dp))
                Progress(state, skin, 145)
                Spacer(GlanceModifier.height(8.dp))
                Play(state, skin, 40)
            }
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(art, 196)
            Spacer(GlanceModifier.height(11.dp))
            Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 18.sp, FontWeight.Medium, textAlign = TextAlign.Center))
            Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp, textAlign = TextAlign.Center))
            Spacer(GlanceModifier.height(10.dp))
            Progress(state, skin, 220)
            Spacer(GlanceModifier.height(9.dp))
            Play(state, skin, 48)
        }
    }

    @Composable
    private fun Artwork(bitmap: Bitmap?, size: Int) {
        Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.size(size.dp).cornerRadius(20.dp))
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
    private fun Play(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) {
        Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(size.dp).background(ColorProvider(OniWidgetVisualSystem.primary(skin))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(TogglePlayPauseActionCallback::class.java)))
    }
}
