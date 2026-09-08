package com.example.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

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
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = OniSkin.spacing.screenHorizontal,
    buttonHeight: Dp = 48.dp
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeatureActionButton(Icons.Default.Lyrics, "Lyrics", false, onLyricsClick, "player_lyrics_button", buttonHeight, Modifier.weight(1f))
        FeatureActionButton(Icons.Default.PictureInPicture, "Floating", floatingLyricsEnabled, onToggleFloatingLyrics, "player_floating_lyrics_button", buttonHeight, Modifier.weight(1f))
        FeatureActionButton(Icons.Default.Timer, if (isSleepTimerRunning) "${sleepTimerMinutesLeft}m" else "Timer", isSleepTimerRunning, onSleepTimerClick, "player_sleep_timer_button", buttonHeight, Modifier.weight(1f))
        FeatureActionButton(Icons.Default.EditNote, "Tags", false, onEditTagsClick, "player_edit_tags_button", buttonHeight, Modifier.weight(1f))
        FeatureActionButton(Icons.Default.DeleteOutline, "Delete", false, onDeleteClick, "player_delete_track_button", buttonHeight, Modifier.weight(1f))
    }
}

@Composable
private fun FeatureActionButton(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit, testTag: String, height: Dp = 48.dp, modifier: Modifier = Modifier) {
    val containerVariant = if (isActive) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft
    val contentColor = if (isActive) OniSkin.colors.primary else OniSkin.colors.textSecondary
    OniSurface(
        variant = containerVariant,
        shape = OniSkin.shapes.medium,
        onClick = onClick,
        modifier = modifier.heightIn(min = height).testTag(testTag).semantics(mergeDescendants = true) {
            selected = isActive
            contentDescription = "$label${if (isActive) ", active" else ""}"
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = OniSkin.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
            Text(label, style = OniSkin.typography.caption, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, color = contentColor, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
