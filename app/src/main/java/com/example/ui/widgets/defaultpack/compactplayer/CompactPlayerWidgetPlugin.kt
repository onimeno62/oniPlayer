package com.example.ui.widgets.defaultpack.compactplayer

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
import com.example.ui.widgets.skin.AuroraWidgetStyle

class CompactPlayerWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.compactplayer"
    override val packId = "default.pack"
    override val name = "Compact Player"
    override val description = "Minimal Aurora Glass playback controller."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = CompactPlayerWidgetRenderer()
}

class CompactPlayerWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        Box(GlanceModifier.fillMaxSize().cornerRadius(26.dp).background(ColorProvider(AuroraWidgetStyle.surface(skin))).clickable(actionStartActivity<MainActivity>()).padding(if (size == WidgetSize.SIZE_4X1) 10.dp else 13.dp)) {
            Box(GlanceModifier.size(if (size == WidgetSize.SIZE_4X4) 190.dp else 120.dp).background(ColorProvider(AuroraWidgetStyle.secondary(skin).copy(alpha = .10f))).cornerRadius(100.dp).align(Alignment.TopEnd)) {}
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin).copy(alpha = .28f))).align(Alignment.TopCenter)) {}
            when (size) {
                WidgetSize.SIZE_4X1 -> Small(state, skin, art)
                WidgetSize.SIZE_4X2 -> Medium(state, skin, art)
                WidgetSize.SIZE_4X4 -> Large(state, skin, art)
            }
        }
    }

    @Composable
    private fun Artwork(bitmap: Bitmap?, size: Int, skin: OniSkinTokens) {
        Box(GlanceModifier.size((size + 8).dp).background(ColorProvider(AuroraWidgetStyle.glow(skin).copy(alpha = .34f))).cornerRadius(((size + 8) / 5).dp).padding(4.dp), Alignment.Center) {
            Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.fillMaxSize().cornerRadius((size / 5).dp))
        }
    }

    @Composable
    private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 56, skin)
            Spacer(GlanceModifier.width(11.dp))
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 14.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 10.sp))
                Spacer(GlanceModifier.height(6.dp))
                Progress(state, skin, 145)
                Spacer(GlanceModifier.height(4.dp))
                Controls(state, skin, 27, 38)
            }
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 82, skin)
            Spacer(GlanceModifier.width(13.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 17.sp, FontWeight.Medium))
                Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 11.sp))
                Spacer(GlanceModifier.height(7.dp))
                Progress(state, skin, 215)
                Spacer(GlanceModifier.height(3.dp))
                Text(if (state.isPlaying) "NOW PLAYING" else "PAUSED", style = TextStyle(ColorProvider(AuroraWidgetStyle.secondary(skin)), 8.sp, FontWeight.Medium))
                Spacer(GlanceModifier.height(8.dp))
                Controls(state, skin, 31, 44)
            }
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Artwork(art, 148, skin)
            Spacer(GlanceModifier.height(9.dp))
            Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 18.sp, FontWeight.Medium))
            Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 11.sp))
            Spacer(GlanceModifier.height(8.dp))
            Progress(state, skin, 250)
            Spacer(GlanceModifier.height(8.dp))
            Controls(state, skin, 32, 46)
        }
    }

    @Composable
    private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens, width: Int) {
        val filled = (width * state.progress).coerceAtLeast(if (state.progress > 0f) 3f else 0f)
        Box(GlanceModifier.fillMaxWidth().height(9.dp), Alignment.CenterStart) {
            Box(GlanceModifier.fillMaxWidth().height(3.dp).background(ColorProvider(AuroraWidgetStyle.track(skin).copy(alpha = .82f))).cornerRadius(2.dp)) {}
            if (filled > 0f) Box(GlanceModifier.width(filled.dp).height(3.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin))).cornerRadius(2.dp)) {
                Box(GlanceModifier.size(8.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin))).cornerRadius(4.dp).align(Alignment.CenterEnd)) {}
            }
        }
    }

    @Composable
    private fun Controls(state: OniWidgetPlaybackState, skin: OniSkinTokens, side: Int, center: Int) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Control(R.drawable.ic_skip_previous, "Previous", skin, side, SkipPreviousActionCallback::class.java)
            Spacer(GlanceModifier.width(12.dp))
            Control(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", skin, center, TogglePlayPauseActionCallback::class.java, true)
            Spacer(GlanceModifier.width(12.dp))
            Control(R.drawable.ic_skip_next, "Next", skin, side, SkipNextActionCallback::class.java)
        }
    }

    @Composable
    private fun Control(res: Int, description: String, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, primary: Boolean = false) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(if (primary) AuroraWidgetStyle.primary(skin) else AuroraWidgetStyle.elevated(skin).copy(alpha = .92f))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(callback)))
    }
}
