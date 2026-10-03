package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.music.OniFavoriteAction
import com.example.ui.theme.OniSkin

/**
 * Track title, artist, album, and favorite toggle for the Player screen in Default Skin.
 *
 * Supports Serif typography option for elegant editorial Canvas layout.
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
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = OniSkin.spacing.screenHorizontal,
    useSerifFont: Boolean = false,
    qualityBadge: String? = null,
    onMenuClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
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
                style = if (useSerifFont) {
                    OniSkin.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                } else {
                    OniSkin.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary,
                maxLines = 2,
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

            if (!qualityBadge.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                OniSurface(
                    variant = OniSurfaceVariant.Outlined,
                    shape = OniSkin.shapes.small
                ) {
                    Text(
                        text = qualityBadge,
                        style = OniSkin.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = OniSkin.colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniFavoriteAction(
                isFavorite = isFavorite,
                onToggle = onToggleFavorite,
                modifier = Modifier.testTag("player_favorite_button")
            )
            if (onMenuClick != null) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.testTag("player_overflow_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = OniSkin.colors.textPrimary
                    )
                }
            }
        }
    }
}
