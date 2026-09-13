package com.example.ui.widgets.defaultpack.lyrics

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import com.example.ui.widgets.actions.SkipNextActionCallback
import com.example.ui.widgets.actions.SkipPreviousActionCallback
import com.example.ui.widgets.actions.TogglePlayPauseActionCallback
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize

class LyricsWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.lyrics"
    override val packId = "default.pack"
    override val name = "Lyrics & Visualizer"
    override val description = "Premium skin-aware synchronized lyrics widget."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = LyricsWidgetRenderer()
}

class LyricsWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val root = GlanceModifier.fillMaxSize()
            .background(ColorProvider(skin.colors.surface))
            .cornerRadius(22.dp)
            .padding(if (size == WidgetSize.SIZE_4X1) 9.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>())
        when (size) {
            WidgetSize.SIZE_4X1 -> Small(state, skin, root)
            WidgetSize.SIZE_4X2 -> Medium(state, skin, root)
            WidgetSize.SIZE_4X4 -> Large(state, skin, root)
        }
    }

    private fun lyric(state: OniWidgetPlaybackState): String = when {
        state.isBlank -> "Nothing playing"
        !state.hasLyrics -> "No lyrics available"
        state.activeLyric.isNullOrBlank() -> "♪  Instrumental"
        else -> state.activeLyric
    }

    @Composable private fun Small(state: OniWidgetPlaybackState, skin: OniSkinTokens, modifier: GlanceModifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(38.dp).background(ColorProvider(skin.colors.primaryContainer)).cornerRadius(19.dp), Alignment.Center) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(21.dp))
            }
            Spacer(GlanceModifier.width(9.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(lyric(state), maxLines = 1, style = TextStyle(color = ColorProvider(if (state.activeLyric != null) skin.colors.primary else skin.colors.textSecondary), fontSize = 12.sp, fontWeight = if (state.activeLyric != null) FontWeight.Medium else FontWeight.Normal))
                if (!state.isBlank) Text(state.title, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 9.sp))
            }
            Spacer(GlanceModifier.width(8.dp))
            Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(36.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(18.dp).padding(9.dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
        }
    }

    @Composable private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, modifier: GlanceModifier) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(20.dp))
                Spacer(GlanceModifier.width(7.dp))
                Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = ColorProvider(skin.colors.textSecondary), fontSize = 10.sp, fontWeight = FontWeight.Medium))
                if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 9.sp))
            }
            Spacer(GlanceModifier.height(7.dp))
            if (!state.hasLyrics || state.isBlank) {
                Text(lyric(state), maxLines = 2, style = TextStyle(color = ColorProvider(skin.colors.textSecondary), fontSize = 14.sp, textAlign = TextAlign.Center))
            } else {
                Text(state.previousLyric ?: "", maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 10.sp, textAlign = TextAlign.Center))
                Spacer(GlanceModifier.height(5.dp))
                Box(GlanceModifier.fillMaxWidth().background(ColorProvider(skin.colors.primaryContainer)).cornerRadius(14.dp).padding(horizontal = 10.dp, vertical = 7.dp), Alignment.Center) {
                    Text(state.activeLyric ?: "♪", maxLines = 2, style = TextStyle(color = ColorProvider(skin.colors.textPrimary), fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                }
                Spacer(GlanceModifier.height(5.dp))
                Text(state.nextLyric ?: "", maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 10.sp, textAlign = TextAlign.Center))
            }
        }
    }

    @Composable private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, modifier: GlanceModifier) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_lyrics), "Lyrics", GlanceModifier.size(24.dp))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(if (state.isBlank) "Lyrics" else state.title, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textPrimary), fontSize = 13.sp, fontWeight = FontWeight.Medium))
                    if (!state.isBlank) Text(state.artist, maxLines = 1, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 9.sp))
                }
                Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(36.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(18.dp).padding(9.dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
            }
            Spacer(GlanceModifier.height(12.dp))
            if (!state.hasLyrics || state.isBlank) {
                Box(GlanceModifier.fillMaxWidth().defaultWeight().background(ColorProvider(skin.colors.primaryContainer)).cornerRadius(18.dp).padding(14.dp), Alignment.Center) {
                    Text(lyric(state), maxLines = 4, style = TextStyle(color = ColorProvider(skin.colors.textSecondary), fontSize = 15.sp, textAlign = TextAlign.Center))
                }
            } else {
                Text(state.previousLyric ?: "", maxLines = 2, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 12.sp, textAlign = TextAlign.Center))
                Spacer(GlanceModifier.height(9.dp))
                Box(GlanceModifier.fillMaxWidth().background(ColorProvider(skin.colors.primaryContainer)).cornerRadius(18.dp).padding(horizontal = 16.dp, vertical = 14.dp), Alignment.Center) {
                    Text(state.activeLyric ?: "♪", maxLines = 4, style = TextStyle(color = ColorProvider(skin.colors.textPrimary), fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                }
                Spacer(GlanceModifier.height(9.dp))
                Text(state.nextLyric ?: "", maxLines = 2, style = TextStyle(color = ColorProvider(skin.colors.textTertiary), fontSize = 12.sp, textAlign = TextAlign.Center))
            }
            Spacer(GlanceModifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_skip_previous), "Previous", GlanceModifier.size(34.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius(17.dp).padding(8.dp).clickable(actionRunCallback<SkipPreviousActionCallback>()))
                Spacer(GlanceModifier.width(14.dp))
                Image(ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), if (state.isPlaying) "Pause" else "Play", GlanceModifier.size(46.dp).background(ColorProvider(skin.colors.primary)).cornerRadius(23.dp).padding(11.dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>()))
                Spacer(GlanceModifier.width(14.dp))
                Image(ImageProvider(R.drawable.ic_skip_next), "Next", GlanceModifier.size(34.dp).background(ColorProvider(skin.colors.surfaceVariant)).cornerRadius(17.dp).padding(8.dp).clickable(actionRunCallback<SkipNextActionCallback>()))
            }
        }
    }
}
