package com.example.ui.widgets.defaultpack.dynamicalbum

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.unit.ColorProvider
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize
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
import com.example.ui.widgets.defaultpack.shared.OniWidgetPanel
import com.example.ui.widgets.defaultpack.shared.OniWidgetTextRole
import com.example.ui.widgets.defaultpack.shared.OniWidgetTone
import com.example.ui.widgets.defaultpack.shared.OniWidgetType
import com.example.ui.widgets.skin.OniWidgetVisualSystem
import com.example.ui.widgets.skin.WidgetArtworkAtmosphere

/**
 * Dynamic Album: ARTWORK FIRST.
 *
 * The artwork is the composition, not a thumbnail. 4x1 bleeds artwork off the
 * leading edge into a strong ambient wash; 4x2 and 4x4 render full-bleed hero
 * artwork with a skin-coloured scrim baked in, and overlay minimal text and
 * translucent controls. No shuffle/repeat, no control grid.
 */
class DynamicAlbumWidgetRenderer : OniWidgetRenderer {

    @Composable
    override fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        val dims = LocalSize.current
        val w = dims.width.value
        val h = dims.height.value

        when (size) {
            WidgetSize.SIZE_4X1 -> {
                val art = WidgetArtworkAtmosphere.artwork(context, state.albumArtworkUri)
                val ambient = OniWidgetBackdrop.ambient(context, state, skin, intensity = 1.6f)
                OniWidgetCanvas(skin, ambient) { Ribbon(state, skin, art, h) }
            }
            WidgetSize.SIZE_4X2 -> {
                val hero = OniWidgetBackdrop.hero(context, state, skin, WidgetArtworkAtmosphere.HeroAspect.WIDE)
                OniWidgetCanvas(skin, hero) { Poster(state, skin, hero != null, w, h) }
            }
            WidgetSize.SIZE_4X4 -> {
                val hero = OniWidgetBackdrop.hero(context, state, skin, WidgetArtworkAtmosphere.HeroAspect.SQUARE)
                OniWidgetCanvas(skin, hero) { Cover(state, skin, hero != null, w, h) }
            }
        }
    }

    /** 4x1: edge-bleed artwork, identity floating on the artwork's own colour, one action. */
    @Composable
    private fun Ribbon(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        art: Bitmap?,
        h: Float
    ) {
        val primaryD = (h - 24f).coerceIn(36f, 44f)
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniWidgetArtwork(
                skin = skin,
                bitmap = art,
                modifier = GlanceModifier.width(h.dp).fillMaxHeight(),
                radius = 0.dp,
                description = OniWidgetCopy.artworkDescription(state),
                fallbackIconSize = (h * 0.36f).dp
            )
            Spacer(GlanceModifier.width(14.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (h >= 64f && state.album.isNotBlank() && !state.isBlank) {
                    Text(
                        state.album,
                        style = OniWidgetType.style(skin, OniWidgetTextRole.Label, OniWidgetVisualSystem.primary(skin)),
                        maxLines = 1
                    )
                }
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
            OniPrimaryControl(skin, state.isPlaying, primaryD.dp, OniPrimaryStyle.Glass)
            Spacer(GlanceModifier.width(12.dp))
        }
    }

    /** 4x2: full-bleed poster. Album chip on top, title/artist/rail and one glass action on the scrim. */
    @Composable
    private fun Poster(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        hasArt: Boolean,
        w: Float,
        h: Float
    ) {
        val pad = 16f
        val roomy = h >= 150f
        val primaryD = if (roomy) 48f else 42f
        val textW = (w - pad * 2 - 12f - primaryD).coerceAtLeast(40f)

        Box(modifier = GlanceModifier.fillMaxSize()) {
            if (!hasArt) NoArtWash(skin)
            Column(modifier = GlanceModifier.fillMaxSize().padding(pad.dp)) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hasArt) {
                        OniWidgetArtwork(
                            skin = skin,
                            bitmap = null,
                            modifier = GlanceModifier.size(36.dp),
                            radius = OniWidgetVisualSystem.artworkRadius(skin, 36f),
                            description = OniWidgetCopy.artworkDescription(state),
                            fallbackIconSize = 18.dp
                        )
                    } else if (state.album.isNotBlank()) {
                        AlbumChip(skin, state.album)
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            OniWidgetCopy.title(state),
                            style = OniWidgetType.style(skin, OniWidgetTextRole.Hero, OniWidgetVisualSystem.text(skin)),
                            maxLines = if (roomy) 2 else 1
                        )
                        Text(
                            OniWidgetCopy.artist(state),
                            style = OniWidgetType.style(skin, OniWidgetTextRole.Body, OniWidgetVisualSystem.muted(skin)),
                            maxLines = 1
                        )
                        Spacer(GlanceModifier.height(8.dp))
                        OniProgressRail(skin, state, textW.dp, thickness = 3.dp, onArtwork = hasArt)
                    }
                    Spacer(GlanceModifier.width(12.dp))
                    OniPrimaryControl(
                        skin,
                        state.isPlaying,
                        primaryD.dp,
                        if (hasArt) OniPrimaryStyle.Glass else OniPrimaryStyle.Filled
                    )
                }
            }
        }
    }

    /** 4x4: album cover. Artwork is the whole widget; text and quiet controls sit on its scrim. */
    @Composable
    private fun Cover(
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        hasArt: Boolean,
        w: Float,
        h: Float
    ) {
        val pad = 18f
        val primaryD = 56f
        val showSkip = w >= 300f
        val controlsW = primaryD + if (showSkip) 80f else 0f
        val railW = (w - pad * 2 - 14f - controlsW).coerceAtLeast(48f)
        val fallbackSize = (minOf(w, h) * 0.38f).coerceIn(72f, 140f)

        Box(modifier = GlanceModifier.fillMaxSize()) {
            if (!hasArt) NoArtWash(skin)
            Column(modifier = GlanceModifier.fillMaxSize().padding(pad.dp)) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasArt && state.album.isNotBlank()) AlbumChip(skin, state.album)
                    Spacer(GlanceModifier.defaultWeight())
                    if (!state.isBlank) StatusChip(skin, state.isPlaying)
                }
                if (hasArt) {
                    Spacer(GlanceModifier.defaultWeight())
                } else {
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        OniWidgetArtwork(
                            skin = skin,
                            bitmap = null,
                            modifier = GlanceModifier.size(fallbackSize.dp),
                            radius = OniWidgetVisualSystem.artworkRadius(skin, fallbackSize),
                            description = OniWidgetCopy.artworkDescription(state),
                            fallbackIconSize = (fallbackSize * 0.36f).dp
                        )
                    }
                }
                Text(
                    OniWidgetCopy.title(state),
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Display, OniWidgetVisualSystem.text(skin)),
                    maxLines = 2
                )
                Text(
                    OniWidgetCopy.artist(state),
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Body, OniWidgetVisualSystem.muted(skin)),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(12.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        OniProgressRail(skin, state, railW.dp, thickness = 3.dp, onArtwork = hasArt)
                        Spacer(GlanceModifier.height(4.dp))
                        OniTimeRow(skin, state, railW.dp)
                    }
                    Spacer(GlanceModifier.width(14.dp))
                    if (showSkip) OniSecondaryControl(skin, OniTransport.Previous, 40.dp, 22.dp)
                    OniPrimaryControl(
                        skin,
                        state.isPlaying,
                        primaryD.dp,
                        if (hasArt) OniPrimaryStyle.Glass else OniPrimaryStyle.Filled
                    )
                    if (showSkip) OniSecondaryControl(skin, OniTransport.Next, 40.dp, 22.dp)
                }
            }
        }
    }

    @Composable
    private fun AlbumChip(skin: OniSkinTokens, album: String) {
        OniWidgetPanel(skin = skin, tone = OniWidgetTone.Glass, radius = 12.dp) {
            Text(
                album,
                modifier = GlanceModifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = OniWidgetType.style(skin, OniWidgetTextRole.Label, OniWidgetVisualSystem.text(skin)),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun StatusChip(skin: OniSkinTokens, isPlaying: Boolean) {
        OniWidgetPanel(skin = skin, tone = OniWidgetTone.Glass, radius = 12.dp) {
            Row(
                modifier = GlanceModifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OniPlaybackDot(skin, isPlaying)
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    if (isPlaying) "Playing" else "Paused",
                    style = OniWidgetType.style(skin, OniWidgetTextRole.Label, OniWidgetVisualSystem.text(skin)),
                    maxLines = 1
                )
            }
        }
    }

    /** Deliberate accent wash for the no-artwork state, so the widget never reads as an empty card. */
    @Composable
    private fun NoArtWash(skin: OniSkinTokens) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(OniWidgetVisualSystem.tonal(skin)))
        ) {}
    }
}
