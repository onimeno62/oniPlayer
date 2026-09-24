package com.example.ui.widgets.defaultpack.nowplaying

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.defaultpack.shared.OniPrimaryControl
import com.example.ui.widgets.defaultpack.shared.OniProgressRail
import com.example.ui.widgets.defaultpack.shared.OniSecondaryControl
import com.example.ui.widgets.defaultpack.shared.OniTertiaryControl
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
 * Now Playing: PLAYER EXPERIENCE.
 *
 * The flagship. Strongest hierarchy: artwork, title, artist, a knob-marked
 * progress rail with times, then a weighted transport row where play/pause is
 * the only filled control. Full-strength artwork atmosphere.
 */
class NowPlayingWidgetRenderer : OniWidgetRenderer {

    @Composable
    override fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        val art = WidgetArtworkAtmosphere.artwork(context, state.albumArtworkUri)
        val backdrop = OniWidgetBackdrop.ambient(context, state, skin, intensity = 1f)
        val dims = LocalSize.current
        val w = dims.width.value
        val h = dims.height.value

        OniWidgetCanvas(skin, backdrop) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Compact(state, skin, art, w, h)
                WidgetSize.SIZE_4X2 -> Flagship(state, skin, art, w, h)
                WidgetSize.SIZE_4X4 -> Stage(state, skin, art, w, h)
            }
        }
    }

    /** 4x1: compact flagship summary with an edge-to-edge progress rail along the bottom. */
    @Composable
    private fun Compact(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val artSize = (h - 22f).coerceIn(36f, 64f)
        val primaryD = (h - 26f).coerceIn(36f, 46f)
        val showNext = w >= 260f
        val railW = w - 18f

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 7.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OniWidgetArtwork(
                    skin = skin,
                    bitmap = art,
                    modifier = GlanceModifier.size(artSize.dp),
                    radius = OniWidgetVisualSystem.artworkRadius(skin, artSize),
                    description = OniWidgetCopy.artworkDescription(state),
                    fallbackIconSize = (artSize * 0.42f).dp
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        OniWidgetCopy.title(state),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.CompactTitle, OniWidgetVisualSystem.text(skin)),
                        maxLines = 1
                    )
                    Text(
                        OniWidgetCopy.artistAlbum(state),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.Meta, OniWidgetVisualSystem.muted(skin)),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                OniPrimaryControl(skin, state.isPlaying, primaryD.dp)
                if (showNext) {
                    OniSecondaryControl(skin, OniTransport.Next, 40.dp, 22.dp)
                }
            }
            Spacer(GlanceModifier.height(4.dp))
            OniProgressRail(skin, state, railW.dp, thickness = 3.dp)
        }
    }

    /** 4x2: the flagship composition. Large art, identity, knob rail, weighted transport. */
    @Composable
    private fun Flagship(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val compact = h < 168f
        val pad = if (compact) 12f else 14f
        val artSize = minOf(h - pad * 2, w * 0.40f).coerceAtLeast(64f)
        val gap = 14f
        val colW = w - pad * 2 - artSize - gap
        val showTertiary = colW >= 214f
        val primaryD = if (compact) 40f else 46f

        Row(
            modifier = GlanceModifier.fillMaxSize().padding(pad.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniWidgetArtwork(
                skin = skin,
                bitmap = art,
                modifier = GlanceModifier.size(artSize.dp),
                radius = OniWidgetVisualSystem.artworkRadius(skin, artSize),
                description = OniWidgetCopy.artworkDescription(state),
                fallbackIconSize = (artSize * 0.32f).dp
            )
            Spacer(GlanceModifier.width(gap.dp))
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                if (!compact) {
                    Text(
                        "NOW PLAYING",
                        style = OniWidgetType.style(skin, OniWidgetTextRole.Label, OniWidgetVisualSystem.primary(skin)),
                        maxLines = 1
                    )
                }
                Text(
                    OniWidgetCopy.title(state),
                    style = OniWidgetType.style(
                        skin,
                        if (compact) OniWidgetTextRole.CompactTitle else OniWidgetTextRole.Title,
                        OniWidgetVisualSystem.text(skin)
                    ),
                    maxLines = 1
                )
                Text(
                    OniWidgetCopy.artistAlbum(state),
                    style = OniWidgetType.style(
                        skin,
                        if (compact) OniWidgetTextRole.Meta else OniWidgetTextRole.Body,
                        OniWidgetVisualSystem.muted(skin)
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                OniProgressRail(skin, state, colW.dp, knob = true)
                if (!compact) {
                    Spacer(GlanceModifier.height(3.dp))
                    OniTimeRow(skin, state, colW.dp)
                }
                Spacer(GlanceModifier.height(if (compact) 4.dp else 6.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTertiary) {
                        OniTertiaryControl(skin, OniTransport.Shuffle, state.isShuffle)
                        Spacer(GlanceModifier.defaultWeight())
                    }
                    OniSecondaryControl(skin, OniTransport.Previous, 40.dp, 24.dp)
                    Spacer(GlanceModifier.defaultWeight())
                    OniPrimaryControl(skin, state.isPlaying, primaryD.dp)
                    Spacer(GlanceModifier.defaultWeight())
                    OniSecondaryControl(skin, OniTransport.Next, 40.dp, 24.dp)
                    if (showTertiary) {
                        Spacer(GlanceModifier.defaultWeight())
                        OniTertiaryControl(skin, OniTransport.Repeat, state.isRepeat)
                    }
                }
            }
        }
    }

    /** 4x4: artwork-led stage. Centred art and identity above the full transport. */
    @Composable
    private fun Stage(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val pad = 18f
        val innerW = w - pad * 2
        val primaryD = 56f
        val block = 24f + 18f + 12f + 10f + 4f + 13f + 10f + primaryD
        val artSize = minOf(innerW, h - pad * 2 - block - 14f).coerceAtLeast(96f)
        val showTertiary = innerW >= 240f

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(pad.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OniWidgetArtwork(
                skin = skin,
                bitmap = art,
                modifier = GlanceModifier.size(artSize.dp),
                radius = OniWidgetVisualSystem.artworkRadius(skin, artSize),
                description = OniWidgetCopy.artworkDescription(state),
                fallbackIconSize = (artSize * 0.28f).dp
            )
            Spacer(GlanceModifier.height(14.dp))
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    OniWidgetCopy.title(state),
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Hero, OniWidgetVisualSystem.text(skin), TextAlign.Center),
                    maxLines = 1
                )
                Text(
                    OniWidgetCopy.artistAlbum(state),
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Body, OniWidgetVisualSystem.muted(skin), TextAlign.Center),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.height(12.dp))
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                OniProgressRail(skin, state, innerW.dp, knob = true)
                Spacer(GlanceModifier.height(4.dp))
                OniTimeRow(skin, state, innerW.dp)
            }
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTertiary) {
                    OniTertiaryControl(skin, OniTransport.Shuffle, state.isShuffle, 40.dp, 20.dp)
                    Spacer(GlanceModifier.defaultWeight())
                }
                OniSecondaryControl(skin, OniTransport.Previous, 48.dp, 28.dp)
                Spacer(GlanceModifier.defaultWeight())
                OniPrimaryControl(skin, state.isPlaying, primaryD.dp)
                Spacer(GlanceModifier.defaultWeight())
                OniSecondaryControl(skin, OniTransport.Next, 48.dp, 28.dp)
                if (showTertiary) {
                    Spacer(GlanceModifier.defaultWeight())
                    OniTertiaryControl(skin, OniTransport.Repeat, state.isRepeat, 40.dp, 20.dp)
                }
            }
        }
    }
}
