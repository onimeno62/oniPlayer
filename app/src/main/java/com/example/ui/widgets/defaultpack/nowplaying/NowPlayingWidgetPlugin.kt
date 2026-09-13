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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.*
import com.example.ui.widgets.core.*
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
    @Composable override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        val root = GlanceModifier.fillMaxSize().cornerRadius(24.dp).clickable(actionStartActivity<MainActivity>())
        AuroraSurface(skin, root, size == WidgetSize.SIZE_4X1) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Compact(state, skin, art)
                WidgetSize.SIZE_4X2 -> Medium(state, skin, art)
                WidgetSize.SIZE_4X4 -> Large(state, skin, art)
            }
        }
    }
    @Composable private fun AuroraSurface(skin: OniSkinTokens, modifier: GlanceModifier, compact: Boolean, content: @Composable () -> Unit) { Box(modifier.background(ColorProvider(skin.colors.surface)).padding(if (compact) 8.dp else 12.dp)) { Box(GlanceModifier.size(if (compact) 92.dp else 150.dp).background(ColorProvider(skin.colors.accentGlow.copy(alpha = .20f))).cornerRadius(80.dp).align(Alignment.TopEnd)) {}; Box(GlanceModifier.size(if (compact) 72.dp else 120.dp).background(ColorProvider(skin.colors.accentSecondary.copy(alpha = .12f))).cornerRadius(70.dp).align(Alignment.BottomStart)) {}; content() } }
    @Composable private fun Art(bitmap: Bitmap?, size: Int, skin: OniSkinTokens) { Box(GlanceModifier.size((size + 6).dp).background(ColorProvider(skin.colors.accentGlow.copy(alpha = .28f))).cornerRadius(((size + 6) / 5).dp).padding(3.dp), Alignment.Center) { Image(bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note), "Album artwork", GlanceModifier.fillMaxSize().cornerRadius((size / 5).dp)) } }
    @Composable private fun Compact(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) { Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Art(art, 52, skin); Spacer(GlanceModifier.width(10.dp)); Column(GlanceModifier.defaultWeight()) { Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 14.sp, FontWeight.Medium)); Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textSecondary), 10.sp)); Progress(state, skin, 120) }; Spacer(GlanceModifier.width(8.dp)); Button(state.isPlaying, skin, 40, TogglePlayPauseActionCallback::class.java); Spacer(GlanceModifier.width(5.dp)); IconButton(R.drawable.ic_skip_next, "Next", skin, 34, SkipNextActionCallback::class.java) } }
    @Composable private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) { Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Art(art, 82, skin); Spacer(GlanceModifier.width(12.dp)); Column(GlanceModifier.defaultWeight()) { Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 17.sp, FontWeight.Bold)); Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.accentSecondary), 12.sp, FontWeight.Medium)); if (state.album.isNotBlank()) Text(state.album, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 10.sp)) } }; Spacer(GlanceModifier.height(10.dp)); Progress(state, skin, 240); Row(GlanceModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TimeText(state.positionMs, skin); TimeText(state.durationMs, skin) }; Spacer(GlanceModifier.height(8.dp)); Controls(state, skin, 38, 48) } }
    @Composable private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) { Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) { Art(art, 166, skin); Spacer(GlanceModifier.height(10.dp)); Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 20.sp, FontWeight.Bold)); Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.accentSecondary), 13.sp, FontWeight.Medium)); if (state.album.isNotBlank()) Text(state.album, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 10.sp)); Spacer(GlanceModifier.height(11.dp)); Progress(state, skin, 270); Row(GlanceModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TimeText(state.positionMs, skin); TimeText(state.durationMs, skin) }; Spacer(GlanceModifier.height(9.dp)); Row(verticalAlignment = Alignment.CenterVertically) { IconButton(R.drawable.ic_shuffle, "Shuffle", skin, 34, ToggleShuffleActionCallback::class.java, state.isShuffle); Spacer(GlanceModifier.width(12.dp)); Controls(state, skin, 40, 52); Spacer(GlanceModifier.width(12.dp)); IconButton(R.drawable.ic_repeat, "Repeat", skin, 34, ToggleRepeatActionCallback::class.java, state.isRepeat) } } }
    @Composable private fun Progress(state: OniWidgetPlaybackState, skin: OniSkinTokens, width: Int) { val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f; Box(GlanceModifier.fillMaxWidth().height(5.dp).background(ColorProvider(skin.colors.surfaceVariant.copy(alpha = .75f))).cornerRadius(3.dp)) { if (fraction > 0f) Box(GlanceModifier.width((width * fraction).coerceAtLeast(2f).dp).height(5.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(3.dp)) } }
    @Composable private fun Controls(state: OniWidgetPlaybackState, skin: OniSkinTokens, side: Int, center: Int) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(R.drawable.ic_skip_previous, "Previous", skin, side, SkipPreviousActionCallback::class.java); Spacer(GlanceModifier.width(12.dp)); Button(state.isPlaying, skin, center, TogglePlayPauseActionCallback::class.java); Spacer(GlanceModifier.width(12.dp)); IconButton(R.drawable.ic_skip_next, "Next", skin, side, SkipNextActionCallback::class.java) } }
    @Composable private fun Button(playing: Boolean, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>) { Image(ImageProvider(if (playing) R.drawable.ic_pause else R.drawable.ic_play), if (playing) "Pause" else "Play", GlanceModifier.size(size.dp).background(ColorProvider(skin.colors.primary)).cornerRadius((size / 2).dp).padding((size / 4).dp).clickable(actionRunCallback(callback))) }
    @Composable private fun IconButton(res: Int, description: String, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, active: Boolean = false) { Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(if (active) skin.colors.primaryContainer else skin.colors.surfaceVariant.copy(alpha = .72f))).cornerRadius((size / 2).dp).padding((size / 4).dp).clickable(actionRunCallback(callback))) }
    @Composable private fun TimeText(ms: Long, skin: OniSkinTokens) = Text(time(ms), style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp))
    private fun time(ms: Long): String { val s = (ms.coerceAtLeast(0) / 1000).toInt(); return "%d:%02d".format(s / 60, s % 60) }
}
