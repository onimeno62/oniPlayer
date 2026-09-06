package com.example.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Top bar for the Player screen in Default Skin.
 *
 * Clean, restrained header with navigation, track context, and queue entry.
 * Replaces the legacy "NOW PLAYING / Aurora Glass" header.
 */
@Composable
fun PlayerTopBar(
    onNavigateBack: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "NOW PLAYING",
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.xs, vertical = OniSkin.spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("player_back_button")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize player",
                tint = OniSkin.colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = OniSkin.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onQueueClick,
            modifier = Modifier.testTag("player_queue_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Playing queue",
                tint = OniSkin.colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
