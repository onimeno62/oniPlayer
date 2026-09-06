package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Secondary feature action row for the Player screen in Default Skin.
 *
 * Provides access to Lyrics/Karaoke, Floating Lyrics, Sleep Timer, Tag Editor,
 * and Track Deletion in a quiet, organized row.
 *
 * Replaces the legacy Aurora Glass floating dock.
 */
@Composable
fun PlayerFeatureActions(
    onLyricsClick: () -> Unit,
    floatingLyricsEnabled: Boolean,
    onToggleFloatingLyrics: () -> Unit,
    isSleepTimerRunning: Boolean,
    sleepTimerMinutesLeft: Int,
    onSleepTimerClick: () -> Unit,
    onEditTagsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeatureActionButton(
            icon = Icons.Default.Lyrics,
            label = "Lyrics",
            isActive = false,
            onClick = onLyricsClick,
            testTag = "player_lyrics_button",
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        FeatureActionButton(
            icon = Icons.Default.PictureInPicture,
            label = "Floating",
            isActive = floatingLyricsEnabled,
            onClick = onToggleFloatingLyrics,
            testTag = "player_floating_lyrics_button",
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        FeatureActionButton(
            icon = Icons.Default.Timer,
            label = if (isSleepTimerRunning) "${sleepTimerMinutesLeft}m" else "Timer",
            isActive = isSleepTimerRunning,
            onClick = onSleepTimerClick,
            testTag = "player_sleep_timer_button",
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        FeatureActionButton(
            icon = Icons.Default.EditNote,
            label = "Tags",
            isActive = false,
            onClick = onEditTagsClick,
            testTag = "player_edit_tags_button",
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        FeatureActionButton(
            icon = Icons.Default.DeleteOutline,
            label = "Delete",
            isActive = false,
            onClick = onDeleteClick,
            testTag = "player_delete_track_button",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val containerVariant = if (isActive) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft
    val contentColor = if (isActive) OniSkin.colors.primary else OniSkin.colors.textSecondary

    OniSurface(
        variant = containerVariant,
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = OniSkin.typography.caption,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
