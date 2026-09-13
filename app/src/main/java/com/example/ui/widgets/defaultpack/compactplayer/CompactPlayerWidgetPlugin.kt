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

class CompactPlayerWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.compactplayer"
    override val packId = "default.pack"
    override val name = "Compact Player"
    override val description = "Minimal premium playback controller."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = CompactPlayerWidgetRenderer()
}

class CompactPlayerWidgetRenderer : OniWidgetRenderer {
    @Composable override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        val root = GlanceModifier.fillMaxSize().cornerRadius(24.dp).clickable(actionStartActivity<MainActivity>())
        Box(root.background(ColorProvider(skin.colors.surface)).padding(if (size == WidgetSize.SIZE_4X1) 8.dp else 12.dp)) {
            Box(GlanceModifier.size(if (size == WidgetSize.SIZE_4X4) 170.dp else 110.dp).background(ColorProvider(skin.colors.accentGlow.copy(alpha = .18f))).cornerRadius(90.dp).align(Alignment.TopEnd)) {}
            Box(GlanceModifier.fillMaxSize()) { when (size) { WidgetSize.SIZE_4X1 -> Small(state, skin, art); WidgetSize.SIZE_4X2 -> Medium(state, skin, art); WidgetSize.SIZE_4X4 -> Large(state, skin, art) } }
        }
    }
    @Composable private fun Art(bitmap: Bitmap?, size: Int, skin: OniSkinTokens) { Box(GlanceModifier.size((size + 6).dp).background(ColorProvider(skin.colors.accentGlow.copy(alpha = .25f))).cornerRadius(((size + 6) / 5).dp).padding(3.dp), Alignment.Center) { Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.fillMaxSize().cornerRadius((size / 5).dp)) } }
    @Composable private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) { Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Art(art, 50, skin); Spacer(GlanceModifier.width(9.dp)); Column(GlanceModifier.defaultWeight()) { Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 13.sp, FontWeight.Medium)); Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textSecondary), 10.sp)) }; Spacer(GlanceModifier.width(7.dp)); Control(R.drawable.ic_skip_previous, "Previous", skin, 30, SkipPreviousActionCallback::class.java); Spacer(GlanceModifier.width(4.dp)); Control(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", skin, 40, TogglePlayPauseActionCallback::class.java, true); Spacer(GlanceModifier.width(4.dp)); Control(R.drawable.ic_skip_next, "Next", skin, 30, SkipNextActionCallback::class.java) } }
    @Composable private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) { Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Art(art, 68, skin); Spacer(GlanceModifier.width(11.dp)); Column(GlanceModifier.defaultWeight()) { Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 16.sp, FontWeight.Bold)); Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.accentSecondary), 11.sp, FontWeight.Medium)); Text(if (state.isPlaying) "NOW PLAYING" else "PAUSED", style = TextStyle(ColorProvider(skin.colors.textTertiary), 8.sp, FontWeight.Medium)) } }; Spacer(GlanceModifier.height(9.dp)); Progress(state, skin); Spacer(GlanceModifier.height(8.dp)); Controls(state, skin, 34, 44) } }
    @Composable private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) { Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) { Art(art, 142, skin); Spacer(GlanceModifier.height(10.dp)); Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 18.sp, FontWeight.Bold)); Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.accentSecondary), 12.sp, FontWeight.Medium)); Spacer(GlanceModifier.height(10.dp)); Progress(state, skin); Spacer(GlanceModifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Control(R.drawable.ic_shuffle, "Shuffle", skin, 34, ToggleShuffleActionCallback::class.java, state.isShuffle); Spacer(GlanceModifier.width(10.dp)); Controls(state, skin, 38, 48); Spacer(GlanceModifier.width(10.dp)); Control(R.drawable.ic_repeat, "Repeat", skin, 34, ToggleRepeatActionCallback::class.java, state.isRepeat) } } }
    @Composable private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens) { val f = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f; Box(GlanceModifier.fillMaxWidth().height(5.dp).background(ColorProvider(skin.colors.surfaceVariant.copy(alpha = .7f))).cornerRadius(3.dp)) { if (f > 0) Box(GlanceModifier.fillMaxWidth(f).height(5.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(3.dp)) } }
    @Composable private fun Controls(state: OniWidgetPlaybackState, skin: OniSkinTokens, side: Int, center: Int) { Row(verticalAlignment = Alignment.CenterVertically) { Control(R.drawable.ic_skip_previous, "Previous", skin, side, SkipPreviousActionCallback::class.java); Spacer(GlanceModifier.width(11.dp)); Control(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", skin, center, TogglePlayPauseActionCallback::class.java, true); Spacer(GlanceModifier.width(11.dp)); Control(R.drawable.ic_skip_next, "Next", skin, side, SkipNextActionCallback::class.java) } }
    @Composable private fun Control(res: Int, desc: String, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, primary: Boolean = false) { Image(ImageProvider(res), desc, GlanceModifier.size(size.dp).background(ColorProvider(if (primary) skin.colors.primary else skin.colors.surfaceVariant.copy(alpha = .72f))).cornerRadius((size / 2).dp).padding((size / 4).dp).clickable(actionRunCallback(callback))) }
}
