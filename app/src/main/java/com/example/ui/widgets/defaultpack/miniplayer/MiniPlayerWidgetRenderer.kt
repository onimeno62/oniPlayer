package com.example.ui.widgets.defaultpack.miniplayer

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
import com.example.ui.widgets.defaultpack.shared.OniInlineProgress
import com.example.ui.widgets.defaultpack.shared.OniProgressRail
import com.example.ui.widgets.defaultpack.shared.OniTransportCapsule
import com.example.ui.widgets.defaultpack.shared.OniWidgetArtwork
import com.example.ui.widgets.defaultpack.shared.OniWidgetBackdrop
import com.example.ui.widgets.defaultpack.shared.OniWidgetCanvas
import com.example.ui.widgets.defaultpack.shared.OniWidgetCopy
import com.example.ui.widgets.defaultpack.shared.OniWidgetPanel
import com.example.ui.widgets.defaultpack.shared.OniWidgetTextRole
import com.example.ui.widgets.defaultpack.shared.OniWidgetTone
import com.example.ui.widgets.defaultpack.shared.OniWidgetType
import com.example.ui.widgets.defaultpack.shared.transportCapsuleWidth
import com.example.ui.widgets.skin.OniWidgetVisualSystem
import com.example.ui.widgets.skin.WidgetArtworkAtmosphere

/**
 * Mini Player: CONTROL FIRST.
 *
 * Signature element is the integrated transport capsule (prev / play / next on
 * one control surface). Artwork and identity stay compact; the capsule gets
 * the space. The ambient artwork wash is kept quiet so controls dominate.
 */
class MiniPlayerWidgetRenderer : OniWidgetRenderer {

    @Composable
    override fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        val art = WidgetArtworkAtmosphere.artwork(context, state.albumArtworkUri)
        val backdrop = OniWidgetBackdrop.ambient(context, state, skin, intensity = 0.7f)
        val dims = LocalSize.current
        val w = dims.width.value
        val h = dims.height.value

        OniWidgetCanvas(skin, backdrop) {
            when (size) {
                WidgetSize.SIZE_4X1 -> Strip(state, skin, art, w, h)
                WidgetSize.SIZE_4X2 -> Deck(state, skin, art, w, h)
                WidgetSize.SIZE_4X4 -> Docked(state, skin, art, w, h)
            }
        }
    }

    /** 4x1: one integrated strip. Art, identity over a hairline rail, transport capsule. */
    @Composable
    private fun Strip(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val pad = 8f
        val artSize = (h - pad * 2).coerceIn(36f, 72f)
        val showPrevious = w >= 300f
        val capsuleH = (h - pad * 2).coerceIn(40f, 52f)
        val secondary = 40f
        val capsuleW = transportCapsuleWidth(skin, capsuleH, secondary, showPrevious)
        val railW = w - pad * 2 - artSize - 12f - 10f - capsuleW
        val showRail = h >= 64f && railW >= 40f

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
                fallbackIconSize = (artSize * 0.42f).dp
            )
            Spacer(GlanceModifier.width(12.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                if (showRail) {
                    Spacer(GlanceModifier.height(6.dp))
                    OniProgressRail(skin, state, railW.dp, thickness = 3.dp)
                }
            }
            Spacer(GlanceModifier.width(10.dp))
            OniTransportCapsule(
                skin = skin,
                isPlaying = state.isPlaying,
                height = capsuleH.dp,
                secondaryTarget = secondary.dp,
                secondaryIcon = 20.dp,
                showPrevious = showPrevious
            )
        }
    }

    /** 4x2: control deck. Compact identity on top, a full-width capsule owns the bottom. */
    @Composable
    private fun Deck(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val pad = if (h < 140f) 10f else 12f
        val idArt = (h * 0.30f).coerceIn(40f, 56f)
        val capsuleH = (h * 0.34f).coerceIn(44f, 60f)
        val showProgress = h >= 124f
        val innerW = w - pad * 2

        Column(modifier = GlanceModifier.fillMaxSize().padding(pad.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OniWidgetArtwork(
                    skin = skin,
                    bitmap = art,
                    modifier = GlanceModifier.size(idArt.dp),
                    radius = OniWidgetVisualSystem.artworkRadius(skin, idArt),
                    description = OniWidgetCopy.artworkDescription(state),
                    fallbackIconSize = (idArt * 0.42f).dp
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        OniWidgetCopy.title(state),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.Title, OniWidgetVisualSystem.text(skin)),
                        maxLines = 1
                    )
                    Text(
                        OniWidgetCopy.artist(state),
                        style = OniWidgetType.style(skin, OniWidgetTextRole.Body, OniWidgetVisualSystem.muted(skin)),
                        maxLines = 1
                    )
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (showProgress) {
                OniInlineProgress(skin, state, innerW.dp)
                Spacer(GlanceModifier.height(6.dp))
            }
            OniTransportCapsule(
                skin = skin,
                isPlaying = state.isPlaying,
                height = capsuleH.dp,
                secondaryTarget = 48.dp,
                secondaryIcon = 26.dp,
                showPrevious = true,
                stretch = true,
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }

    /**
     * 4x4: album mode. Framed artwork on a lifted panel with the mini strip docked
     * beneath it, so the family stays recognisable instead of becoming Now Playing.
     */
    @Composable
    private fun Docked(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        w: Float,
        h: Float
    ) {
        val pad = 12f
        val dockH = 68f
        val capsuleH = 48f
        val secondary = 40f
        val showPrevious = w >= 260f
        val capsuleW = transportCapsuleWidth(skin, capsuleH, secondary, showPrevious)
        val railW = (w - pad * 2 - 24f - 10f - capsuleW).coerceAtLeast(24f)
        val stageH = h - pad * 2 - dockH - 10f
        val artSize = minOf(w - pad * 2 - 32f, stageH - 32f).coerceAtLeast(64f)

        Column(modifier = GlanceModifier.fillMaxSize().padding(pad.dp)) {
            OniWidgetPanel(
                skin = skin,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                tone = OniWidgetTone.Frosted
            ) {
                OniWidgetArtwork(
                    skin = skin,
                    bitmap = art,
                    modifier = GlanceModifier.size(artSize.dp),
                    radius = OniWidgetVisualSystem.artworkRadius(skin, artSize),
                    description = OniWidgetCopy.artworkDescription(state),
                    fallbackIconSize = (artSize * 0.3f).dp
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            OniWidgetPanel(
                skin = skin,
                modifier = GlanceModifier.fillMaxWidth().height(dockH.dp),
                tone = OniWidgetTone.Elevated
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Spacer(GlanceModifier.height(6.dp))
                        OniProgressRail(skin, state, railW.dp, thickness = 3.dp)
                    }
                    Spacer(GlanceModifier.width(10.dp))
                    OniTransportCapsule(
                        skin = skin,
                        isPlaying = state.isPlaying,
                        height = capsuleH.dp,
                        secondaryTarget = secondary.dp,
                        secondaryIcon = 20.dp,
                        showPrevious = showPrevious
                    )
                }
            }
        }
    }
}
