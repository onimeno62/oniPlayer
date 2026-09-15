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
    override val description = "Editorial synchronized lyric view with quiet playback controls."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}

class LyricsWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Box(
            GlanceModifier.fillMaxSize().cornerRadius(22.dp)
                .background(ColorProvider(OniWidgetVisualSystem.surface(skin)))
                .clickable(actionStartActivity<MainActivity>())
        ) {
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
        state.activeLyric.isNullOrBlank() -> "♪  Instrumental"
        else -> state.activeLyric.orEmpty()
    }

    @Composable
    private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Row(GlanceModifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(42.dp).cornerRadius(21.dp).background(ColorProvider(OniWidgetVisualSystem.primary(skin))), Alignment.Center) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(22.dp).padding(2.dp))
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Text(current(state), maxLines = 2, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 14.sp, FontWeight.Bold))
                if (!state.isBlank) Text("${state.title} · ${state.artist}", maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 9.sp))
            }
            Spacer(GlanceModifier.width(9.dp))
            Play(state, skin, 36)
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Column(GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 13.dp)) {
            Header(state, skin, 34)
            Spacer(GlanceModifier.height(8.dp))
            LyricLine(state.previousLyric, skin, false, 10, 1)
            Spacer(GlanceModifier.height(5.dp))
            LyricLine(current(state), skin, true, 17, 2)
            Spacer(GlanceModifier.height(5.dp))
            LyricLine(state.nextLyric, skin, false, 10, 1)
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Column(GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(24.dp).padding(2.dp))
                Spacer(GlanceModifier.width(8.dp))
                Text("LYRICS", modifier = GlanceModifier.defaultWeight(), style = TextStyle(ColorProvider(OniWidgetVisualSystem.primary(skin)), 9.sp, FontWeight.Bold))
                Text(if (state.isBlank) "oniPlayer" else state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 9.sp))
            }
            Spacer(GlanceModifier.height(30.dp))
            LyricLine(state.previousLyric, skin, false, 12, 1)
            Spacer(GlanceModifier.height(14.dp))
            LyricLine(current(state), skin, true, 22, 3)
            Spacer(GlanceModifier.height(14.dp))
            LyricLine(state.nextLyric, skin, false, 12, 1)
            Spacer(GlanceModifier.defaultWeight())
            Text(
                if (state.isBlank) "Tap to open oniPlayer" else "${state.title} · ${state.artist}",
                maxLines = 1,
                style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 10.sp, FontWeight.Medium, textAlign = TextAlign.Center)
            )
            Spacer(GlanceModifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bare(R.drawable.ic_skip_previous, "Previous", 30, SkipPreviousActionCallback::class.java)
                Spacer(GlanceModifier.width(18.dp))
                Play(state, skin, 48)
                Spacer(GlanceModifier.width(18.dp))
                Bare(R.drawable.ic_skip_next, "Next", 30, SkipNextActionCallback::class.java)
            }
        }
    }

    @Composable
    private fun Header(state: OniWidgetPlaybackState, skin: OniSkinTokens, playSize: Int) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(22.dp).padding(2.dp))
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 12.sp, FontWeight.Bold))
                if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 9.sp))
            }
            Play(state, skin, playSize)
        }
    }

    @Composable
    private fun LyricLine(text: String?, skin: OniSkinTokens, active: Boolean, size: Int, maxLines: Int) {
        Text(
            text.orEmpty().ifBlank { " " },
            maxLines = maxLines,
            style = TextStyle(
                ColorProvider(if (active) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.muted(skin)),
                size.sp,
                if (active) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
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
}
