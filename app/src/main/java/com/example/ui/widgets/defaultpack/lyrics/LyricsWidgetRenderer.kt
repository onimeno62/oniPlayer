package com.example.ui.widgets.defaultpack.lyrics

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.defaultpack.shared.OniAccentTick
import com.example.ui.widgets.defaultpack.shared.OniLyricFrame
import com.example.ui.widgets.defaultpack.shared.OniPlaybackDot
import com.example.ui.widgets.defaultpack.shared.OniPrimaryControl
import com.example.ui.widgets.defaultpack.shared.OniPrimaryStyle
import com.example.ui.widgets.defaultpack.shared.OniProgressRail
import com.example.ui.widgets.defaultpack.shared.OniSecondaryControl
import com.example.ui.widgets.defaultpack.shared.OniTimeRow
import com.example.ui.widgets.defaultpack.shared.OniTransport
import com.example.ui.widgets.defaultpack.shared.OniWidgetArtwork
import com.example.ui.widgets.defaultpack.shared.OniWidgetBackdrop
import com.example.ui.widgets.defaultpack.shared.OniWidgetCanvas
import com.example.ui.widgets.defaultpack.shared.OniWidgetCopy
import com.example.ui.widgets.defaultpack.shared.OniWidgetTextRole
import com.example.ui.widgets.defaultpack.shared.OniWidgetType
import com.example.ui.widgets.skin.OniWidgetVisualSystem
import com.example.ui.widgets.skin.WidgetArtworkAtmosphere

/**
 * Lyrics: CONTENT FIRST.
 *
 * Editorial, start-aligned lyric typography (Start follows each line's own
 * direction, so Persian/RTL lines align correctly). The current line is marked
 * with an accent tick and carries the strongest weight; context lines are
 * subdued and indented to the lyric column. Controls are tonal and secondary.
 * The artwork wash is kept faint so the words stay the subject.
 */
class LyricsWidgetRenderer : OniWidgetRenderer {

    private companion object {
        /** Accent tick (3dp) + gap (10dp): context lines align with the current line's text. */
        const val LYRIC_INDENT = 13f
    }

    @Composable
    override fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        val backdrop = OniWidgetBackdrop.ambient(context, state, skin, intensity = 0.45f)
        val frame = OniWidgetCopy.lyricFrame(state)
        val dims = LocalSize.current
        val w = dims.width.value
        val h = dims.height.value

        OniWidgetCanvas(skin, backdrop) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Line(state, skin, frame, h)
                WidgetSize.SIZE_4X2 -> Context(state, skin, frame, h)
                WidgetSize.SIZE_4X4 -> {
                    val art = WidgetArtworkAtmosphere.artwork(context, state.albumArtworkUri)
                    Stage(state, skin, frame, art, w, h)
                }
            }
        }
    }

    /** 4x1: the current line, a status/metadata line, and a quiet play toggle. */
    @Composable
    private fun Line(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        frame: OniLyricFrame,
        h: Float
    ) {
        val twoLines = h >= 80f
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniAccentTick(skin, if (twoLines) 34.dp else 20.dp)
            Spacer(GlanceModifier.width(10.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    frame.current,
                    style = OniWidgetType.style(skin, OniWidgetTextRole.LyricCompact, currentColor(skin, frame)),
                    maxLines = if (twoLines) 2 else 1
                )
                Spacer(GlanceModifier.height(3.dp))
                StatusLine(state, skin)
            }
            Spacer(GlanceModifier.width(10.dp))
            OniPrimaryControl(skin, state.isPlaying, 40.dp, OniPrimaryStyle.Tonal)
        }
    }

    /** 4x2: previous / CURRENT / next, with the play toggle beside the current line. */
    @Composable
    private fun Context(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        frame: OniLyricFrame,
        h: Float
    ) {
        val compact = h < 150f
        val previous = frame.previous
        val next = frame.next

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 12.dp)
        ) {
            if (!compact && previous != null) {
                ContextLine(skin, previous, emphasis = false, maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
            }
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = GlanceModifier.padding(top = 4.dp)) { OniAccentTick(skin, 18.dp) }
                Spacer(GlanceModifier.width(10.dp))
                Text(
                    frame.current,
                    modifier = GlanceModifier.defaultWeight(),
                    style = OniWidgetType.style(skin, OniWidgetTextRole.LyricCurrent, currentColor(skin, frame)),
                    maxLines = 2
                )
                Spacer(GlanceModifier.width(8.dp))
                OniPrimaryControl(skin, state.isPlaying, 40.dp, OniPrimaryStyle.Tonal)
            }
            if (next != null) {
                Spacer(GlanceModifier.height(4.dp))
                ContextLine(skin, next, emphasis = true, maxLines = 1)
            }
            Spacer(GlanceModifier.defaultWeight())
            StatusLine(state, skin)
        }
    }

    /** 4x4: lyric stage. Song identity, three-line lyric focus, then progress and controls as a secondary layer. */
    @Composable
    private fun Stage(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        frame: OniLyricFrame,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val pad = 18f
        val roomy = h >= 380f
        val innerW = w - pad * 2
        val previous = frame.previous
        val next = frame.next

        Column(modifier = GlanceModifier.fillMaxSize().padding(pad.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OniWidgetArtwork(
                    skin = skin,
                    bitmap = art,
                    modifier = GlanceModifier.size(40.dp),
                    radius = OniWidgetVisualSystem.artworkRadius(skin, 40f),
                    description = OniWidgetCopy.artworkDescription(state),
                    fallbackIconSize = 18.dp
                )
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        OniWidgetCopy.title(state),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.CompactTitle, OniWidgetVisualSystem.text(skin)),
                        maxLines = 1
                    )
                    Text(
                        OniWidgetCopy.artist(state),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.Meta, OniWidgetVisualSystem.muted(skin)),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    "LYRICS",
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Label, OniWidgetVisualSystem.primary(skin)),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                if (previous != null) {
                    ContextLine(skin, previous, emphasis = false, maxLines = if (roomy) 2 else 1)
                    Spacer(GlanceModifier.height(10.dp))
                }
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = GlanceModifier.padding(top = 6.dp)) { OniAccentTick(skin, 22.dp) }
                    Spacer(GlanceModifier.width(10.dp))
                    Text(
                        frame.current,
                        modifier = GlanceModifier.defaultWeight(),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.LyricHero, currentColor(skin, frame)),
                        maxLines = if (roomy) 4 else 3
                    )
                }
                if (next != null) {
                    Spacer(GlanceModifier.height(10.dp))
                    ContextLine(skin, next, emphasis = true, maxLines = if (roomy) 2 else 1)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                OniProgressRail(skin, state, innerW.dp, thickness = 3.dp)
                Spacer(GlanceModifier.height(4.dp))
                OniTimeRow(skin, state, innerW.dp)
            }
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OniPlaybackDot(skin, state.isPlaying)
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    when {
                        state.isBlank -> "Ready"
                        state.isPlaying -> "Playing"
                        else -> "Paused"
                    },
                    modifier = GlanceModifier.defaultWeight(),
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Label, OniWidgetVisualSystem.muted(skin)),
                    maxLines = 1
                )
                OniSecondaryControl(skin, OniTransport.Previous, 40.dp, 22.dp)
                OniPrimaryControl(skin, state.isPlaying, 44.dp, OniPrimaryStyle.Tonal)
                OniSecondaryControl(skin, OniTransport.Next, 40.dp, 22.dp)
            }
        }
    }

    /** Previous (quietest) or next (slightly stronger, it is what comes up) context line. */
    @Composable
    private fun ContextLine(skin: OniSkinTokens, text: String, emphasis: Boolean, maxLines: Int) {
        Text(
            text,
            modifier = GlanceModifier.fillMaxWidth().padding(start = LYRIC_INDENT.dp),
            style = OniWidgetType.style(
                skin,
                OniWidgetTextRole.LyricContext,
                if (emphasis) OniWidgetVisualSystem.muted(skin) else OniWidgetVisualSystem.faint(skin)
            ),
            maxLines = maxLines
        )
    }

    @Composable
    private fun StatusLine(state: OniWidgetPlaybackState, skin: OniSkinTokens) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OniPlaybackDot(skin, state.isPlaying)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                OniWidgetCopy.statusMeta(state),
                style = OniWidgetType.style(skin, OniWidgetTextRole.Meta, OniWidgetVisualSystem.muted(skin)),
                maxLines = 1
            )
        }
    }

    private fun currentColor(skin: OniSkinTokens, frame: OniLyricFrame) =
        if (frame.isLyric) OniWidgetVisualSystem.text(skin) else OniWidgetVisualSystem.muted(skin)
}
