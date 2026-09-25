package com.example.ui.player.components.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.button.OniIconButton
import com.example.ui.components.button.OniIconButtonStyle
import com.example.ui.components.playback.OniNextButton
import com.example.ui.components.playback.OniPlayPauseButton
import com.example.ui.components.playback.OniPlaybackProgress
import com.example.ui.components.playback.OniPreviousButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.PitchStatus
import com.example.ui.theme.OniSkin
import kotlin.math.roundToInt

/** A passive status shown next to the mode selector (e.g. "Vocal cut"). Not a control. */
data class LyricsStatus(val icon: ImageVector, val label: String)

@Composable
fun LyricsHeader(
    title: String,
    artist: String,
    onClose: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.xs, vertical = OniSkin.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OniIconButton(
            icon = Icons.Default.Close,
            contentDescription = "Close lyrics",
            onClick = onClose,
            style = OniIconButtonStyle.Ghost
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OniSkin.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = OniSkin.typography.titleSmall.copy(textDirection = TextDirection.Content),
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = OniSkin.typography.bodySmall.copy(textDirection = TextDirection.Content),
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        OniIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "Lyrics tools",
            onClick = onOpenTools,
            style = OniIconButtonStyle.Ghost
        )
    }
}

/**
 * Mode selector + passive statuses. Renders nothing when there is neither a mode choice
 * nor any status to show, so it costs zero vertical space in the common case of plain lyrics.
 */
@Composable
fun LyricsModeRow(
    showModeSelector: Boolean,
    plainSelected: Boolean,
    onSelectPlain: (Boolean) -> Unit,
    statuses: List<LyricsStatus>,
    modifier: Modifier = Modifier
) {
    if (!showModeSelector && statuses.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
    ) {
        if (showModeSelector) {
            LyricsModeSelector(plainSelected = plainSelected, onSelectPlain = onSelectPlain)
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            statuses.forEach { LyricsStatusLabel(it) }
        }
    }
}

@Composable
private fun LyricsModeSelector(
    plainSelected: Boolean,
    onSelectPlain: (Boolean) -> Unit
) {
    OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.pill) {
        Row(modifier = Modifier.selectableGroup()) {
            ModeSegment(label = "Synced", selected = !plainSelected, onClick = { onSelectPlain(false) })
            ModeSegment(label = "Plain", selected = plainSelected, onClick = { onSelectPlain(true) })
        }
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = OniSkin.colors
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(OniSkin.shapes.pill)
            .background(if (selected) colors.primary.copy(alpha = 0.14f) else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = OniSkin.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Check mark is the non-color cue for the selected segment.
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
        }
        Text(
            text = label,
            style = OniSkin.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.primary else colors.textSecondary
        )
    }
}

@Composable
private fun LyricsStatusLabel(status: LyricsStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = status.icon,
            contentDescription = null,
            tint = OniSkin.colors.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
        Text(
            text = status.label,
            style = OniSkin.typography.labelMedium,
            color = OniSkin.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact transport for the lyrics screen: progress + time, previous / play-pause / next.
 * Built only from shared playback primitives; playback remains owned by the ViewModel/engine.
 */
@Composable
fun CompactLyricsPlayer(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
            .padding(bottom = OniSkin.spacing.xs)
    ) {
        OniPlaybackProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = onSeek,
            showTimeLabels = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxl, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniPreviousButton(onClick = onPrevious)
            OniPlayPauseButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
                size = OniSkin.playbackControls.compactPrimaryControlSize,
                elevation = OniSkin.elevation.flat
            )
            OniNextButton(onClick = onNext)
        }
    }
}

/**
 * Compact Sing Along status strip, shown only while the mic is on.
 * Gain and ear-monitor controls are tucked behind an expand action.
 */
@Composable
fun SingAlongStrip(
    pitchStatus: PitchStatus,
    amplitudeFraction: Float,
    micGain: Float,
    passThroughEnabled: Boolean,
    onGainChange: (Float) -> Unit,
    onPassThroughChange: (Boolean) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val statusText = when (pitchStatus) {
        is PitchStatus.Detected -> "Singing ${pitchStatus.note}"
        is PitchStatus.Detecting -> "Detecting pitch…"
        is PitchStatus.NoClearPitch -> "No clear pitch"
        is PitchStatus.Listening -> "Listening"
        is PitchStatus.Idle -> "Idle"
        else -> "Listening"
    }

    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Column(modifier = Modifier.padding(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xxs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = OniSkin.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sing along",
                        style = OniSkin.typography.labelLarge,
                        color = OniSkin.colors.textPrimary
                    )
                    Text(
                        text = statusText,
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LevelMeter(fraction = amplitudeFraction, modifier = Modifier.width(56.dp))
                OniIconButton(
                    icon = if (expanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                    contentDescription = if (expanded) "Hide sing along settings" else "Sing along settings",
                    onClick = { expanded = !expanded },
                    style = OniIconButtonStyle.Ghost,
                    iconSize = 20.dp
                )
                OniIconButton(
                    icon = Icons.Default.MicOff,
                    contentDescription = "Stop sing along",
                    onClick = onStop,
                    style = OniIconButtonStyle.Ghost,
                    iconSize = 20.dp
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Text(
                        text = "Mic gain ${(micGain * 100).roundToInt()}%",
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.textSecondary
                    )
                    Slider(
                        value = micGain,
                        onValueChange = onGainChange,
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = OniSkin.colors.primary,
                            activeTrackColor = OniSkin.colors.primary,
                            inactiveTrackColor = OniSkin.colors.outline.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = passThroughEnabled,
                                role = Role.Switch,
                                onValueChange = onPassThroughChange
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ear monitor",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textPrimary
                            )
                            Text(
                                text = "Use headphones to avoid feedback",
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary
                            )
                        }
                        Switch(
                            checked = passThroughEnabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OniSkin.colors.primary,
                                checkedTrackColor = OniSkin.colors.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelMeter(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(OniSkin.shapes.pill)
            .background(OniSkin.colors.outline.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(OniSkin.colors.primary)
        )
    }
}
