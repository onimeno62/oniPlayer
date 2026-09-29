package com.example.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable row with Play All, Shuffle, and optional inline Search buttons.
 *
 * Placed under the hero section in the dashboard and at the top of every
 * library category (Songs, Albums, Artists, Playlists, Folders, Genres).
 *
 * Follows Poweramp/WAVORA pattern of surfacing primary actions directly.
 */
@Composable
fun PlayAllShuffleRow(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSearchClick: (() -> Unit)? = null,
    songCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = OniSkin.spacing.screenHorizontal,
                vertical = OniSkin.spacing.xs
            ),
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play All button
        Button(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
            shape = OniSkin.shapes.button,
            contentPadding = PaddingValues(
                horizontal = OniSkin.spacing.md,
                vertical = OniSkin.spacing.sm
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = OniSkin.colors.primary,
                contentColor = OniSkin.colors.onPrimary
            ),
            enabled = songCount > 0
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
            Text(
                "Play All",
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Shuffle button
        FilledTonalButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
            shape = OniSkin.shapes.button,
            contentPadding = PaddingValues(
                horizontal = OniSkin.spacing.md,
                vertical = OniSkin.spacing.sm
            ),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = OniSkin.colors.surfaceVariant,
                contentColor = OniSkin.colors.textPrimary
            ),
            enabled = songCount > 0
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
            Text(
                "Shuffle",
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Inline Search icon button
        if (onSearchClick != null) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = OniSkin.colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
