package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.theme.OniSkin

@Composable
private fun modalShape(): CornerBasedShape =
    OniSkin.shapes.bottomSheet.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

/**
 * Unified Contextual Menu Modal for the Player Screen.
 *
 * Consolidates secondary player and track actions (karaoke/lyrics, floating lyrics,
 * sleep timer, tag editor, audio analyzer, artwork effects, and track deletion) into a
 * clean Material 3 bottom sheet without cluttering primary playback controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenuModal(
    song: SongEntity?,
    floatingLyricsEnabled: Boolean,
    isSleepTimerRunning: Boolean,
    sleepTimerMinutesLeft: Int,
    audioAnalyzerEnabled: Boolean,
    artworkEffectsEnabled: Boolean,
    onToggleFloatingLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenKaraoke: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleAudioAnalyzer: () -> Unit,
    onToggleArtworkEffects: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surfaceElevated,
        contentColor = OniSkin.colors.textPrimary,
        shape = modalShape(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = OniSkin.spacing.xl)
        ) {
            // Header
            Text(
                text = song?.displayTitle ?: "Now Playing Actions",
                style = OniSkin.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary,
                modifier = Modifier
                    .padding(horizontal = OniSkin.spacing.xl, vertical = OniSkin.spacing.xs)
                    .semantics { heading() }
            )
            if (song != null) {
                Text(
                    text = "${song.displayArtist} • ${song.displayAlbum}",
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = OniSkin.spacing.xl)
                )
            }

            Spacer(modifier = Modifier.height(OniSkin.spacing.md))

            // Playback & Features Section
            SectionHeader(title = "Playback & Features")
            MenuActionRow(
                icon = Icons.Default.Lyrics,
                title = "Karaoke & Full Lyrics",
                subtitle = "Synced lyrics display, sing along, and tools",
                onClick = {
                    onDismiss()
                    onOpenKaraoke()
                }
            )
            MenuToggleRow(
                icon = Icons.Default.PictureInPicture,
                title = "Floating Lyrics",
                subtitle = "Overlay synced lyrics on top of other apps",
                checked = floatingLyricsEnabled,
                onToggle = onToggleFloatingLyrics
            )
            MenuActionRow(
                icon = Icons.Default.Timer,
                title = if (isSleepTimerRunning) "Sleep Timer (${sleepTimerMinutesLeft}m remaining)" else "Sleep Timer",
                subtitle = if (isSleepTimerRunning) "Tap to adjust or cancel timer" else "Automatically pause playback after time",
                onClick = {
                    onDismiss()
                    onOpenSleepTimer()
                }
            )

            // Visuals & Effects Section
            SectionHeader(title = "Visuals & Audio Effects")
            MenuToggleRow(
                icon = Icons.Default.GraphicEq,
                title = "Audio Analyzer Pulse",
                subtitle = "Real-time frequency & beat responsive player glow",
                checked = audioAnalyzerEnabled,
                onToggle = onToggleAudioAnalyzer
            )
            MenuToggleRow(
                icon = Icons.Default.AutoAwesome,
                title = "Artwork Motion & Glow",
                subtitle = "Ken Burns slow pan and adaptive color radiance",
                checked = artworkEffectsEnabled,
                onToggle = onToggleArtworkEffects
            )

            // Track Management Section
            SectionHeader(title = "Track Options")
            MenuActionRow(
                icon = Icons.Default.EditNote,
                title = "Edit Tags & Metadata",
                subtitle = "Modify title, artist, album, and lyrics tags",
                onClick = {
                    onDismiss()
                    onOpenTagEditor()
                }
            )
            MenuActionRow(
                icon = Icons.Default.DeleteOutline,
                title = "Delete Track",
                subtitle = "Remove track from library or permanently from storage",
                destructive = true,
                onClick = {
                    onDismiss()
                    onDeleteClick()
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = OniSkin.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = OniSkin.colors.textTertiary,
        modifier = Modifier
            .padding(
                start = OniSkin.spacing.xl,
                end = OniSkin.spacing.xl,
                top = OniSkin.spacing.md,
                bottom = OniSkin.spacing.xxs
            )
            .semantics { heading() }
    )
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val iconColor = if (destructive) OniSkin.colors.error else OniSkin.colors.primary
    val textColor = if (destructive) OniSkin.colors.error else OniSkin.colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = OniSkin.spacing.xl, vertical = OniSkin.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(OniSkin.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OniSkin.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MenuToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = OniSkin.spacing.xl, vertical = OniSkin.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) OniSkin.colors.primary else OniSkin.colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(OniSkin.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OniSkin.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OniSkin.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OniSkin.colors.onPrimary,
                checkedTrackColor = OniSkin.colors.primary,
                uncheckedThumbColor = OniSkin.colors.outline,
                uncheckedTrackColor = OniSkin.colors.surfaceVariant
            )
        )
    }
}
