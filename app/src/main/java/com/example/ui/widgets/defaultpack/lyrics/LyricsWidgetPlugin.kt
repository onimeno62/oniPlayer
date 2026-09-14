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
import com.example.ui.widgets.skin.AuroraWidgetStyle

class LyricsWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.lyrics"
    override val packId = "default.pack"
    override val name = "Lyrics & Visualizer"
    override val description = "Premium Aurora Glass synchronized lyrics widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}

class LyricsWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Box(
            GlanceModifier.fillMaxSize().cornerRadius(26.dp)
                .background(ColorProvider(AuroraWidgetStyle.surface(skin)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(if (size == WidgetSize.SIZE_4X1) 10.dp else 13.dp)
        ) {
            Box(GlanceModifier.size(if (size == WidgetSize.SIZE_4X4) 190.dp else 120.dp).background(ColorProvider(AuroraWidgetStyle.glow(skin).copy(alpha = .11f))).cornerRadius(100.dp).align(Alignment.TopEnd)) {}
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(AuroraWidgetStyle.secondary(skin).copy(alpha = .28f))).align(Alignment.TopCenter)) {}
            when (size) {
                WidgetSize.SIZE_4X1 -> Small(state, skin)
                WidgetSize.SIZE_4X2 -> Medium(state, skin)
                WidgetSize.SIZE_4X4 -> Large(state, skin)
            }
        }
    }

    private fun lyric(state: OniWidgetPlaybackState): String = when {
        state.isBlank -> "Nothing playing"
        !state.hasLyrics -> "No lyrics available"
        state.activeLyric.isNullOrBlank() -> "♪  Instrumental"
        else -> state.activeLyric!!
    }

    @Composable
    private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(44.dp).background(ColorProvider(AuroraWidgetStyle.elevated(skin).copy(alpha = .92f))).cornerRadius(22.dp), Alignment.Center) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(22.dp).padding(2.dp))
            }
            Spacer(GlanceModifier.width(11.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(lyric(state), maxLines = 1, style = TextStyle(ColorProvider(if (state.activeLyric != null) AuroraWidgetStyle.primary(skin) else AuroraWidgetStyle.textSecondary(skin)), 13.sp, FontWeight.Medium))
                if (!state.isBlank) Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 9.sp))
                Spacer(GlanceModifier.height(6.dp))
                LyricPulse(skin)
            }
            Spacer(GlanceModifier.width(10.dp))
            PlayButton(state, skin, 42)
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(22.dp).padding(2.dp))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 11.sp, FontWeight.Medium))
                    if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 9.sp))
                }
                PlayButton(state, skin, 36)
            }
            Spacer(GlanceModifier.height(9.dp))
            LyricsStack(state, skin, 11, 16)
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(25.dp).padding(2.dp))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textPrimary(skin)), 13.sp, FontWeight.Medium))
                    if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), 9.sp))
                }
                PlayButton(state, skin, 40)
            }
            Spacer(GlanceModifier.height(10.dp))
            LyricsStack(state, skin, 13, 20)
            Spacer(GlanceModifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(R.drawable.ic_skip_previous, "Previous", skin, 32, SkipPreviousActionCallback::class.java)
                Spacer(GlanceModifier.width(13.dp))
                PlayButton(state, skin, 46)
                Spacer(GlanceModifier.width(13.dp))
                IconButton(R.drawable.ic_skip_next, "Next", skin, 32, SkipNextActionCallback::class.java)
            }
        }
    }

    @Composable
    private fun LyricsStack(state: OniWidgetPlaybackState, skin: OniSkinTokens, sideSize: Int, activeSize: Int) {
        if (!state.hasLyrics || state.isBlank) {
            Empty(state, skin, activeSize)
        } else {
            Line(state.previousLyric, skin, sideSize, false)
            Spacer(GlanceModifier.height(7.dp))
            Active(state.activeLyric, skin, activeSize)
            Spacer(GlanceModifier.height(7.dp))
            Line(state.nextLyric, skin, sideSize, false)
        }
    }

    @Composable
    private fun Empty(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) {
        Box(GlanceModifier.fillMaxWidth().background(ColorProvider(AuroraWidgetStyle.elevated(skin).copy(alpha = .72f))).cornerRadius(18.dp).padding(12.dp), Alignment.Center) {
            Text(lyric(state), maxLines = 3, style = TextStyle(ColorProvider(AuroraWidgetStyle.textSecondary(skin)), size.sp, textAlign = TextAlign.Center))
        }
    }

    @Composable
    private fun Line(text: String?, skin: OniSkinTokens, size: Int, active: Boolean) {
        Text(text.orEmpty().ifBlank { " " }, maxLines = if (active) 3 else 1, style = TextStyle(ColorProvider(if (active) AuroraWidgetStyle.textPrimary(skin) else AuroraWidgetStyle.textSecondary(skin).copy(alpha = .62f)), size.sp, if (active) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center))
    }

    @Composable
    private fun Active(text: String?, skin: OniSkinTokens, size: Int) {
        Box(GlanceModifier.fillMaxWidth().background(ColorProvider(AuroraWidgetStyle.elevated(skin).copy(alpha = .90f))).cornerRadius(18.dp).padding(horizontal = 15.dp, vertical = 10.dp), Alignment.Center) {
            Line(text, skin, size, true)
        }
    }

    @Composable
    private fun LyricPulse(skin: OniSkinTokens) {
        val heights = listOf(3, 6, 10, 5, 8, 4, 9, 6, 11, 5, 8, 3, 7, 5, 9, 4)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            heights.forEachIndexed { index, height ->
                Box(GlanceModifier.width(2.dp).height(height.dp).background(ColorProvider(if (index % 3 == 0) AuroraWidgetStyle.primary(skin) else AuroraWidgetStyle.secondary(skin).copy(alpha = .65f))).cornerRadius(2.dp)) {}
                if (index < heights.lastIndex) Spacer(GlanceModifier.width(3.dp))
            }
        }
    }

    @Composable
    private fun PlayButton(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) {
        Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(size.dp).background(ColorProvider(AuroraWidgetStyle.primary(skin))).cornerRadius((size / 2).dp).padding((size / 4).dp).clickable(actionRunCallback(TogglePlayPauseActionCallback::class.java)))
    }

    @Composable
    private fun IconButton(res: Int, description: String, skin: OniSkinTokens, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(AuroraWidgetStyle.elevated(skin).copy(alpha = .92f))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(callback)))
    }
}
