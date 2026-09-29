package com.example.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

@Composable
fun LibraryCategoryHero(title: String, subtitle: String, artworkUri: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    OniSurface(modifier = modifier.fillMaxWidth().height(196.dp), variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.card) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (artworkUri != null) {
                AsyncImage(model = artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(22.dp), contentScale = ContentScale.Crop, alpha = 0.36f)
                AsyncImage(model = artworkUri, contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).padding(OniSkin.spacing.lg).size(112.dp), contentScale = ContentScale.Crop, alpha = 0.92f)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(OniSkin.colors.primaryContainer))
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(OniSkin.colors.primary.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.44f), Color.Black.copy(alpha = 0.88f)))))
            Row(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(OniSkin.spacing.lg), verticalAlignment = Alignment.Bottom) {
                OniSurface(modifier = Modifier.size(48.dp), variant = OniSurfaceVariant.Flat, shape = OniSkin.shapes.full, containerColor = Color.Black.copy(alpha = 0.30f)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = OniSkin.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                    Text(text = subtitle, style = OniSkin.typography.bodyMedium, color = Color.White.copy(alpha = 0.84f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
