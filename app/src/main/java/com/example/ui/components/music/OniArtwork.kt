package com.example.ui.components.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.OniSkin

/**
 * Reusable album-artwork component.
 * Consumes [OniSkin.artwork], [OniSkin.shapes], and [OniSkin.colors] tokens.
 * Pure presentation component accepting visual data.
 */
@Composable
fun OniArtwork(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = OniSkin.artwork.shape,
    showGlow: Boolean = false,
    glowColor: Color = OniSkin.colors.primary,
    elevation: Dp = OniSkin.artwork.shadowElevation,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: (@Composable () -> Unit)? = null
) {
    val sizeModifier = if (size != null) Modifier.size(size) else Modifier

    Box(
        modifier = modifier.then(sizeModifier),
        contentAlignment = Alignment.Center
    ) {
        if (showGlow && OniSkin.artwork.glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                glowColor.copy(alpha = OniSkin.artwork.glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = shape
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (elevation > 0.dp) Modifier.shadow(elevation, shape)
                    else Modifier
                )
                .clip(shape)
                .background(OniSkin.colors.surfaceVariant)
                .border(1.dp, OniSkin.colors.outline.copy(alpha = 0.25f), shape)
                .semantics {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (artworkUri != null) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale,
                    error = painterResource(id = android.R.drawable.ic_media_play)
                )
            } else if (placeholder != null) {
                placeholder()
            } else {
                val iconSize = if (size != null) size * 0.45f else 36.dp
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = contentDescription,
                    tint = OniSkin.colors.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
