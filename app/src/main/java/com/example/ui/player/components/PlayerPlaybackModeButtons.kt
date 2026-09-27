package com.example.ui.player.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.RepeatMode
import com.example.playback.ShuffleType
import com.example.ui.theme.OniSkin

/**
 * Poweramp-style shuffle button.
 *
 * Tap cycles Off -> Songs -> Albums -> Songs & albums -> All songs -> Off and toasts the new mode.
 * Long-press opens the full list so any mode can be picked directly.
 */
@Composable
fun ShuffleModeButton(
    isShuffle: Boolean,
    shuffleType: ShuffleType,
    onToggle: () -> Unit,
    onSelect: (ShuffleType?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    val badge = if (!isShuffle) null else when (shuffleType) {
        ShuffleType.SONGS -> null
        ShuffleType.CATEGORIES -> "ALB"
        ShuffleType.SONGS_AND_CATEGORIES -> "MIX"
        ShuffleType.ALL -> "ALL"
    }
    val description = if (isShuffle) shuffleType.label else "Shuffle off"

    Box(modifier = modifier) {
        PlaybackModeIconButton(
            icon = Icons.Default.Shuffle,
            isActive = isShuffle,
            badge = badge,
            description = description,
            enabled = enabled,
            onClick = {
                val next = ShuffleType.nextAfter(isShuffle, shuffleType)
                onToggle()
                Toast.makeText(context, next?.label ?: "Shuffle off", Toast.LENGTH_SHORT).show()
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menuOpen = true
            }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            PlaybackModeMenuItem(
                title = "Shuffle off",
                subtitle = "Play in list order",
                selected = !isShuffle,
                onClick = { menuOpen = false; onSelect(null) }
            )
            ShuffleType.entries.forEach { type ->
                PlaybackModeMenuItem(
                    title = type.label,
                    subtitle = type.description,
                    selected = isShuffle && shuffleType == type,
                    onClick = { menuOpen = false; onSelect(type) }
                )
            }
        }
    }
}

/**
 * Poweramp-style repeat button.
 *
 * Tap cycles Off -> Repeat list -> Repeat song -> Play single song -> Off and toasts the new mode.
 * Long-press opens the full list.
 */
@Composable
fun RepeatModeButton(
    repeatMode: RepeatMode,
    onToggle: () -> Unit,
    onSelect: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    val icon = when (repeatMode) {
        RepeatMode.OFF, RepeatMode.ALL -> Icons.Default.Repeat
        RepeatMode.ONE -> Icons.Default.RepeatOne
        RepeatMode.SINGLE -> Icons.Default.LooksOne
    }

    Box(modifier = modifier) {
        PlaybackModeIconButton(
            icon = icon,
            isActive = repeatMode != RepeatMode.OFF,
            badge = null,
            description = repeatMode.label,
            enabled = enabled,
            onClick = {
                val next = repeatMode.next()
                onToggle()
                Toast.makeText(context, next.label, Toast.LENGTH_SHORT).show()
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menuOpen = true
            }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            listOf(RepeatMode.OFF, RepeatMode.ALL, RepeatMode.ONE, RepeatMode.SINGLE).forEach { mode ->
                PlaybackModeMenuItem(
                    title = mode.label,
                    subtitle = mode.description,
                    selected = repeatMode == mode,
                    onClick = { menuOpen = false; onSelect(mode) }
                )
            }
        }
    }
}

@Composable
private fun PlaybackModeMenuItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = title,
                    style = OniSkin.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) OniSkin.colors.primary else OniSkin.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary
                )
            }
        },
        trailingIcon = {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = OniSkin.colors.primary)
            }
        },
        onClick = onClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaybackModeIconButton(
    icon: ImageVector,
    isActive: Boolean,
    badge: String?,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val size = OniSkin.playbackControls.tertiaryControlSize

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "mode_press_scale"
    )
    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> OniSkin.colors.disabled
            isActive -> OniSkin.playbackControls.tertiaryActiveTint
            else -> OniSkin.playbackControls.tertiaryInactiveTint
        },
        label = "mode_tint"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = "Show all modes",
                onLongClick = onLongClick,
                onClick = onClick
            )
            .semantics {
                selected = isActive
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        if (badge != null) {
            Text(
                text = badge,
                style = OniSkin.typography.caption,
                fontSize = 7.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
            )
        }
    }
}
