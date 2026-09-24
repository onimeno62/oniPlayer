package com.example.ui.widgets.defaultpack.shared

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.skin.OniWidgetVisualSystem

/**
 * Artwork tile. Real artwork is centre-cropped; missing artwork renders a
 * deliberate accent-tinted tile instead of an untinted placeholder icon.
 *
 * The caller supplies sizing through [modifier] so the same primitive covers
 * thumbnails, framed album art, and edge-bleed panels.
 */
@Composable
fun OniWidgetArtwork(
    skin: OniSkinTokens,
    bitmap: Bitmap?,
    modifier: GlanceModifier,
    radius: Dp,
    description: String,
    fallbackIconSize: Dp
) {
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = description,
            modifier = modifier.cornerRadius(radius),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .cornerRadius(radius)
                .background(ColorProvider(OniWidgetVisualSystem.tonal(skin))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_music_note),
                contentDescription = description,
                modifier = GlanceModifier.size(fallbackIconSize),
                colorFilter = ColorFilter.tint(ColorProvider(OniWidgetVisualSystem.primary(skin)))
            )
        }
    }
}
