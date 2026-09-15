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

class NowPlayingWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.nowplaying"
    override val packId = "default.pack"
    override val name = "Now Playing"
    override val description = "Artwork-led playback widget with full transport context."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = NowPlayingWidgetRenderer()
}

class NowPlayingWidgetRenderer : OniWidgetRenderer {
    @Composable
    override fun Render(context: Context, size: WidgetSize, state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val art = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, state.albumArtworkUri)
        Box(
            GlanceModifier.fillMaxSize()
                .cornerRadius(22.dp)
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
            ArtworkPanel(art, 92, 22)
            Column(GlanceModifier.defaultWeight().padding(horizontal = 14.dp, vertical = 9.dp)) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 15.sp, FontWeight.Bold))
                        Text(meta(state), maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 10.sp))
                    }
                    Text("ONI", style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 8.sp, FontWeight.Bold))
                }
                Spacer(GlanceModifier.height(7.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Progress(state, skin, 150)
                    Spacer(GlanceModifier.width(8.dp))
                    Time(state.positionMs, skin)
                }
                Spacer(GlanceModifier.height(5.dp))
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                    BareAction(R.drawable.ic_repeat, "Repeat", 23, ToggleRepeatActionCallback::class.java, skin, state.isRepeat)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_skip_previous, "Previous", 25, SkipPreviousActionCallback::class.java, skin, false)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (state.isPlaying) "Pause" else "Play", 28, TogglePlayPauseActionCallback::class.java, skin, true)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_skip_next, "Next", 25, SkipNextActionCallback::class.java, skin, false)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_shuffle, "Shuffle", 23, ToggleShuffleActionCallback::class.java, skin, state.isShuffle)
                }
            }
        }
    }

    @Composable
    private fun Medium(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Row(GlanceModifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkPanel(art, 112, 18)
            Spacer(GlanceModifier.width(18.dp))
            Column(GlanceModifier.defaultWeight()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("NOW PLAYING", modifier = GlanceModifier.defaultWeight(), style = TextStyle(ColorProvider(OniWidgetVisualSystem.primary(skin)), 9.sp, FontWeight.Bold))
                    BareAction(R.drawable.ic_repeat, "Repeat", 26, ToggleRepeatActionCallback::class.java, skin, state.isRepeat)
                }
                Spacer(GlanceModifier.height(5.dp))
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 20.sp, FontWeight.Bold))
                Text(meta(state), maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp))
                Spacer(GlanceModifier.height(10.dp))
                Waveform(state, skin)
                Spacer(GlanceModifier.height(4.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Time(state.positionMs, skin)
                    Spacer(GlanceModifier.defaultWeight())
                    Time(state.durationMs, skin)
                }
                Spacer(GlanceModifier.height(7.dp))
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                    BareAction(R.drawable.ic_shuffle, "Shuffle", 27, ToggleShuffleActionCallback::class.java, skin, state.isShuffle)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_skip_previous, "Previous", 29, SkipPreviousActionCallback::class.java, skin, false)
                    Spacer(GlanceModifier.width(17.dp))
                    PrimaryAction(state, skin, 48)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_skip_next, "Next", 29, SkipNextActionCallback::class.java, skin, false)
                }
            }
        }
    }

    @Composable
    private fun Large(state: OniWidgetPlaybackState, skin: OniSkinTokens, art: Bitmap?) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            ArtworkPanel(art, 252, 22)
            Column(GlanceModifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.title, maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.text(skin)), 19.sp, FontWeight.Bold, textAlign = TextAlign.Center))
                Text(meta(state), maxLines = 1, style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 11.sp, textAlign = TextAlign.Center))
                Spacer(GlanceModifier.height(10.dp))
                Progress(state, skin, 230)
                Spacer(GlanceModifier.height(4.dp))
                Row(GlanceModifier.width(230.dp)) {
                    Time(state.positionMs, skin)
                    Spacer(GlanceModifier.defaultWeight())
                    Time(state.durationMs, skin)
                }
                Spacer(GlanceModifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BareAction(R.drawable.ic_shuffle, "Shuffle", 28, ToggleShuffleActionCallback::class.java, skin, state.isShuffle)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_skip_previous, "Previous", 30, SkipPreviousActionCallback::class.java, skin, false)
                    Spacer(GlanceModifier.width(17.dp))
                    PrimaryAction(state, skin, 50)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_skip_next, "Next", 30, SkipNextActionCallback::class.java, skin, false)
                    Spacer(GlanceModifier.width(17.dp))
                    BareAction(R.drawable.ic_repeat, "Repeat", 28, ToggleRepeatActionCallback::class.java, skin, state.isRepeat)
                }
            }
        }
    }

    @Composable
    private fun ArtworkPanel(bitmap: Bitmap?, size: Int, radius: Int) {
        Image(
            bitmap?.let(::ImageProvider) ?: ImageProvider(R.drawable.ic_music_note),
            "Album artwork",
            GlanceModifier.size(size.dp).cornerRadius(radius.dp)
        )
    }

    @Composable
    private fun Waveform(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        val heights = listOf(5, 9, 14, 8, 18, 11, 7, 16, 10, 20, 13, 7, 16, 11, 19, 9, 6, 14, 10, 7, 13, 9)
        val activeBars = (heights.size * state.progress).toInt().coerceIn(0, heights.size)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            heights.forEachIndexed { index, height ->
                Box(GlanceModifier.width(2.dp).height(height.dp).background(ColorProvider(if (index < activeBars) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.track(skin))).cornerRadius(2.dp)) {}
                if (index < heights.lastIndex) Spacer(GlanceModifier.width(3.dp))
            }
        }
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

    @Composable
    private fun PrimaryAction(state: OniWidgetPlaybackState, skin: OniSkinTokens, size: Int) {
        Image(
            ImageProvider(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            if (state.isPlaying) "Pause" else "Play",
            GlanceModifier.size(size.dp).background(ColorProvider(OniWidgetVisualSystem.primary(skin))).cornerRadius((size / 2).dp).padding((size / 3).dp).clickable(actionRunCallback<TogglePlayPauseActionCallback>())
        )
    }

    @Composable
    private fun BareAction(res: Int, description: String, size: Int, callback: Class<out androidx.glance.appwidget.action.ActionCallback>, skin: OniSkinTokens, selected: Boolean) {
        Image(
            ImageProvider(res),
            description,
            GlanceModifier.size(size.dp).padding(5.dp).clickable(actionRunCallback(callback))
        )
    }

    @Composable
    private fun Time(ms: Long, skin: OniSkinTokens) = Text(time(ms), style = TextStyle(ColorProvider(OniWidgetVisualSystem.muted(skin)), 8.sp))

    private fun meta(state: OniWidgetPlaybackState): String = if (state.album.isBlank()) state.artist else "${state.artist} · ${state.album}"

    private fun time(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1000).toInt()
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
