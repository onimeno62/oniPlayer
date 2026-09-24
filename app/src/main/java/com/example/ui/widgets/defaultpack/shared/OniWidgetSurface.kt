package com.example.ui.widgets.defaultpack.shared

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.skin.OniWidgetVisualSystem
import com.example.ui.widgets.skin.WidgetArtworkAtmosphere

/** Surface treatments available to widget compositions. */
enum class OniWidgetTone { Elevated, Frosted, Control, Glass, Tonal }

/**
 * Root widget canvas: rounded skin surface, optional artwork backdrop
 * (ambient wash or hero), and tap-to-open-player.
 */
@Composable
fun OniWidgetCanvas(
    skin: OniSkinTokens,
    backdrop: Bitmap? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(OniWidgetVisualSystem.cornerRadius(skin))
            .background(ColorProvider(OniWidgetVisualSystem.surface(skin)))
            .clickable(actionStartActivity<MainActivity>())
    ) {
        if (backdrop != null) {
            Image(
                provider = ImageProvider(backdrop),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}

/** Nested rounded layer used for grouping (capsules, docks, chips). */
@Composable
fun OniWidgetPanel(
    skin: OniSkinTokens,
    modifier: GlanceModifier = GlanceModifier,
    tone: OniWidgetTone = OniWidgetTone.Frosted,
    radius: Dp = OniWidgetVisualSystem.innerRadius(skin),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .cornerRadius(radius)
            .background(ColorProvider(toneColor(skin, tone))),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

fun toneColor(skin: OniSkinTokens, tone: OniWidgetTone): Color = when (tone) {
    OniWidgetTone.Elevated -> OniWidgetVisualSystem.elevated(skin)
    OniWidgetTone.Frosted -> OniWidgetVisualSystem.frosted(skin)
    OniWidgetTone.Control -> OniWidgetVisualSystem.control(skin)
    OniWidgetTone.Glass -> OniWidgetVisualSystem.glass(skin)
    OniWidgetTone.Tonal -> OniWidgetVisualSystem.tonal(skin)
}

/** Resolves skin-controlled artwork backdrops. Returns null when atmosphere is disabled or art is missing. */
object OniWidgetBackdrop {
    /** Softened artwork wash. [intensity] scales the skin's atmosphere strength per family. */
    fun ambient(
        context: Context,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        intensity: Float = 1f
    ): Bitmap? {
        val tokens = skin.widgets
        if (!tokens.artworkAtmosphere) return null
        val strength = (tokens.atmosphereStrength * intensity).coerceIn(0f, 0.8f)
        return WidgetArtworkAtmosphere.ambient(
            context,
            state.albumArtworkUri,
            OniWidgetVisualSystem.surfaceOpaque(skin),
            strength
        )
    }

    /** Full-bleed artwork with a baked, skin-coloured scrim. */
    fun hero(
        context: Context,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens,
        aspect: WidgetArtworkAtmosphere.HeroAspect
    ): Bitmap? = WidgetArtworkAtmosphere.hero(
        context,
        state.albumArtworkUri,
        OniWidgetVisualSystem.surfaceOpaque(skin),
        skin.widgets.heroScrimStrength,
        aspect
    )
}
