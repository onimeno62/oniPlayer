package com.example.ui.widgets.defaultpack.lyrics

import android.content.Context
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

class LyricsWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.lyrics"
    override val packId = "default.pack"
    override val name = "Lyrics & Visualizer"
    override val description = "Premium skin-aware synchronized lyrics widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}

class LyricsWidgetRenderer : OniWidgetRenderer {
    @Composable override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val root = GlanceModifier.fillMaxSize().cornerRadius(24.dp).clickable(actionStartActivity<MainActivity>())
        Box(root.background(ColorProvider(skin.colors.surface)).padding(if (size == WidgetSize.SIZE_4X1) 9.dp else 12.dp)) {
            Box(GlanceModifier.size(if (size == WidgetSize.SIZE_4X4) 190.dp else 100.dp).background(ColorProvider(skin.colors.accentGlow.copy(alpha = .18f))).cornerRadius(100.dp).align(Alignment.TopEnd)) {}
            when (size) { WidgetSize.SIZE_4X1 -> Small(state, skin); WidgetSize.SIZE_4X2 -> Medium(state, skin); WidgetSize.SIZE_4X4 -> Large(state, skin) }
        }
    }
    private fun lyric(state: OniWidgetPlaybackState) = when { state.isBlank -> "Nothing playing"; !state.hasLyrics -> "No lyrics available"; state.activeLyric.isNullOrBlank() -> "♪  Instrumental"; else -> state.activeLyric!! }
    @Composable private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens) { Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { Box(GlanceModifier.size(38.dp).background(ColorProvider(skin.colors.primaryContainer)).cornerRadius(19.dp), Alignment.Center) { Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(20.dp)) }; Spacer(GlanceModifier.width(9.dp)); Column(GlanceModifier.defaultWeight()) { Text(lyric(state), maxLines = 1, style = TextStyle(ColorProvider(if (state.activeLyric != null) skin.colors.primary else skin.colors.textSecondary), 12.sp, if (state.activeLyric != null) FontWeight.Medium else FontWeight.Normal)); if (!state.isBlank) Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp)) }; Spacer(GlanceModifier.width(7.dp)); Control(state, skin, 38, true) } }
    @Composable private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens) { Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) { Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(21.dp)); Spacer(GlanceModifier.width(7.dp)); Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, modifier = GlanceModifier.defaultWeight(), style = TextStyle(ColorProvider(skin.colors.textSecondary), 10.sp, FontWeight.Medium)); if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp)) }; Spacer(GlanceModifier.height(7.dp)); if (!state.hasLyrics || state.isBlank) { Empty(state, skin, 14) } else { Line(state.previousLyric, skin, 10, false); Spacer(GlanceModifier.height(5.dp)); Active(state.activeLyric, skin, 15); Spacer(GlanceModifier.height(5.dp)); Line(state.nextLyric, skin, 10, false) } } }
    @Composable private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens) { Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) { Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(24.dp)); Spacer(GlanceModifier.width(8.dp)); Column(GlanceModifier.defaultWeight()) { Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textPrimary), 13.sp, FontWeight.Medium)); if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(skin.colors.textTertiary), 9.sp)) }; Control(state, skin, 36, true) }; Spacer(GlanceModifier.height(11.dp)); if (!state.hasLyrics || state.isBlank) { Empty(state, skin, 16) } else { Line(state.previousLyric, skin, 12, false); Spacer(GlanceModifier.height(8.dp)); Active(state.activeLyric, skin, 19); Spacer(GlanceModifier.height(8.dp)); Line(state.nextLyric, skin, 12, false) }; Spacer(GlanceModifier.height(10.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(R.drawable.ic_skip_previous, "Previous", skin, 34, SkipPreviousActionCallback::class.java); Spacer(GlanceModifier.width(12.dp)); Control(state, skin, 46, true); Spacer(GlanceModifier.width(12.dp)); Icon(R.drawable.ic_skip_next, "Next", skin, 34, SkipNextActionCallback::class.java) } } }
    @Composable private fun Empty(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) { Box(GlanceModifier.fillMaxWidth().defaultWeight().background(ColorProvider(skin.colors.primaryContainer.copy(alpha = .72f))).cornerRadius(18.dp).padding(14.dp), Alignment.Center) { Text(lyric(state), maxLines = 4, style = TextStyle(ColorProvider(skin.colors.textSecondary), size.sp, textAlign = TextAlign.Center)) } }
    @Composable private fun Line(text: String?, skin: OniSkinTokens, size: Int, active: Boolean) { Text(text.orEmpty().ifBlank { " " }, maxLines = if (active) 3 else 1, style = TextStyle(ColorProvider(if (active) skin.colors.textPrimary else skin.colors.textTertiary), size.sp, if (active) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)) }
    @Composable private fun Active(text: String?, skin: OniSkinTokens, size: Int) { Box(GlanceModifier.fillMaxWidth().background(ColorProvider(skin.colors.primaryContainer.copy(alpha = .82f))).cornerRadius(18.dp).padding(horizontal = 15.dp, vertical = 12.dp), Alignment.Center) { Line(text, skin, size, true) } }
    @Composable private fun Control(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int, primary: Boolean) { Icon(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", skin, size, TogglePlayPauseActionCallback::class.java, primary) }
    @Composable private fun Icon(res: Int, desc: String, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, primary: Boolean = false) { Image(ImageProvider(res), desc, GlanceModifier.size(size.dp).background(ColorProvider(if (primary) skin.colors.primary else skin.colors.surfaceVariant.copy(alpha = .72f))).cornerRadius((size / 2).dp).padding((size / 4).dp).clickable(actionRunCallback(callback))) }
}
