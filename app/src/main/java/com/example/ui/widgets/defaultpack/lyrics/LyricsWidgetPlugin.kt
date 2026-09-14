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
import com.example.ui.widgets.skin.OniWidgetVisualSystem

class LyricsWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.lyrics"
    override val packId = "default.pack"
    override val name = "Lyrics"
    override val description = "Lyric-first synchronized home-screen widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}

class LyricsWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(ColorProvider(OniWidgetVisualSystem.surface(skin))).clickable(actionStartActivity<MainActivity>()).padding(12.dp)) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Small(state, skin)
                WidgetSize.SIZE_4X2 -> Medium(state, skin)
                WidgetSize.SIZE_4X4 -> Large(state, skin)
            }
        }
    }

    private fun current(state: OniWidgetPlaybackState): String = when {
        state.isBlank -> "Nothing playing"
        !state.hasLyrics -> "No lyrics available"
        state.activeLyric.isNullOrBlank() -> "♪ Instrumental"
        else -> state.activeLyric.orEmpty()
    }

    @Composable
    private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(28.dp).padding(2.dp))
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Text(current(state), maxLines = 2, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 13.sp, FontWeight.Medium))
                if (!state.isBlank) Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 9.sp))
            }
            Spacer(GlanceModifier.width(8.dp))
            Play(state, skin, 38)
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(state, skin, 34)
            Spacer(GlanceModifier.height(8.dp))
            LyricLine(state.previousLyric, skin, false, 10)
            Spacer(GlanceModifier.height(6.dp))
            LyricLine(current(state), skin, true, 15)
            Spacer(GlanceModifier.height(6.dp))
            LyricLine(state.nextLyric, skin, false, 10)
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Header(state, skin, 38)
            Spacer(GlanceModifier.height(11.dp))
            LyricLine(state.previousLyric, skin, false, 12)
            Spacer(GlanceModifier.height(8.dp))
            Box(GlanceModifier.fillMaxWidth().background(ColorProvider(OniWidgetVisualSystem.control(skin))).cornerRadius(16.dp).padding(horizontal = 14.dp, vertical = 12.dp), Alignment.Center) {
                Text(current(state), maxLines = 3, style = TextStyle(ColorProvider(OniWidgetVisualSystem.primary(skin)), 18.sp, FontWeight.Medium, textAlign = TextAlign.Center))
            }
            Spacer(GlanceModifier.height(8.dp))
            LyricLine(state.nextLyric, skin, false, 12)
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Action(R.drawable.ic_skip_previous, "Previous", 32, SkipPreviousActionCallback::class.java, skin, false)
                Spacer(GlanceModifier.width(12.dp))
                Play(state, skin, 44)
                Spacer(GlanceModifier.width(12.dp))
                Action(R.drawable.ic_skip_next, "Next", 32, SkipNextActionCallback::class.java, skin, false)
            }
        }
    }

    @Composable
    private fun Header(state: OniWidgetPlaybackState, skin: OniSkinTokens, playSize: Int) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(24.dp).padding(2.dp))
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 12.sp, FontWeight.Medium))
                if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 9.sp))
            }
            Play(state, skin, playSize)
        }
    }

    @Composable
    private fun LyricLine(text: String?, skin: OniSkinTokens, active: Boolean, size: Int) {
        Text(text.orEmpty().ifBlank { " " }, maxLines = if (active) 3 else 1, style = TextStyle(ColorProvider(if (active) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.muted(skin)), size.sp, if (active) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center), modifier = GlanceModifier.fillMaxWidth())
    }

    @Composable
    private fun Play(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) = Action(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", size, TogglePlayPauseActionCallback::class.java, skin, true)

    @Composable
    private fun Action(res: Int, description: String, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, skin: OniSkinTokens, primary: Boolean) {
        Image(ImageProvider(res), description, GlanceModifier.size(size.dp).background(ColorProvider(if (primary) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.control(skin))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback(callback)))
    }
}
