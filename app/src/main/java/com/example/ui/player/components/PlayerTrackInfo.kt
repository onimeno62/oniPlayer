package com.example.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.music.OniFavoriteAction
import com.example.ui.theme.OniSkin

/**
 * Track title, artist, album, and favorite toggle for the Player screen in Default Skin.
 *
 * Emphasizes typography hierarchy and primary text contrast.
 * Uses shared [OniFavoriteAction] for consistent favorite toggling behavior and tokens.
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

        OniFavoriteAction(
            isFavorite = isFavorite,
            onToggle = onToggleFavorite,
            modifier = Modifier.testTag("player_favorite_button")
        )
    }
}
