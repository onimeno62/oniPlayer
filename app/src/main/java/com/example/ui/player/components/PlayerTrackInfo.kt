package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Track title, artist, album, and favorite toggle for the Player screen in Default Skin.
 *
 * Emphasizes typography hierarchy and primary text contrast.
 */
@Composable
fun PlayerTrackInfo(
    title: String,
    artist: String,
    album: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = OniSkin.spacing.sm)
        ) {
            Text(
                text = title.ifBlank { "Unknown Title" },
                style = OniSkin.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = artist.ifBlank { "Unknown Artist" },
                style = OniSkin.typography.bodyLarge,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!album.isNullOrBlank() && album != "Unknown Album") {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = album,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val favoriteTint = if (isFavorite) Color(0xFFE53935) else OniSkin.colors.textTertiary
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 24.dp),
                    role = Role.Button,
                    onClick = onToggleFavorite
                )
                .testTag("player_favorite_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = favoriteTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
