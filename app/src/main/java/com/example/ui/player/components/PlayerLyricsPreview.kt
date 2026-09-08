package com.example.ui.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

@Composable
fun PlayerLyricsPreview(
    currentLyricLine: String?,
    hasSynchronizedLyrics: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = OniSkin.spacing.screenHorizontal
) {
    OniSurface(
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.medium,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = horizontalPadding)
            .testTag("player_lyrics_preview")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Lyrics, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
            val displayText = when {
                !currentLyricLine.isNullOrBlank() -> currentLyricLine
                hasSynchronizedLyrics -> "Synchronized lyrics available · Tap to sing"
                else -> "Lyrics & Karaoke · Tap to view or search"
            }
            val motion = OniSkin.motion
            AnimatedContent(
                targetState = displayText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing)) togetherWith
                        fadeOut(animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing))
                },
                label = "lyrics_preview_text_transition",
                modifier = Modifier.weight(1f)
            ) { targetText ->
                Text(
                    text = targetText,
                    style = OniSkin.typography.bodySmall,
                    fontWeight = if (!currentLyricLine.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (!currentLyricLine.isNullOrBlank()) OniSkin.colors.textPrimary else OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
