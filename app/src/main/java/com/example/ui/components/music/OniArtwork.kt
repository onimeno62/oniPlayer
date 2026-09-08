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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.OniSkin

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
    val artworkSemantics = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Box(modifier = modifier.then(sizeModifier), contentAlignment = Alignment.Center) {
        if (showGlow && OniSkin.artwork.glowAlpha > 0f) {
            Box(modifier = Modifier.matchParentSize().background(Brush.radialGradient(listOf(glowColor.copy(alpha = OniSkin.artwork.glowAlpha), Color.Transparent), shape = shape)))
        }
        Box(
            modifier = Modifier.matchParentSize().then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier).clip(shape).background(OniSkin.colors.surfaceVariant).border(1.dp, OniSkin.colors.outline.copy(alpha = 0.25f), shape).then(artworkSemantics),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUri.isNullOrBlank()) {
                SubcomposeAsyncImage(model = artworkUri, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = contentScale, loading = { ArtworkPlaceholder(size, placeholder) }, error = { ArtworkPlaceholder(size, placeholder) })
            } else {
                ArtworkPlaceholder(size, placeholder)
            }
        }
    }
}

@Composable
private fun ArtworkPlaceholder(size: Dp?, placeholder: (@Composable () -> Unit)?) {
    if (placeholder != null) placeholder() else {
        val iconSize = if (size != null) size * 0.45f else 36.dp
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = OniSkin.colors.primary.copy(alpha = 0.4f), modifier = Modifier.size(iconSize))
    }
}
