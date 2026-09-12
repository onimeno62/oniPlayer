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
    nextLyricLine: String? = null,
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
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = horizontalPadding)
            .testTag("player_lyrics_preview")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics and Karaoke",
                tint = OniSkin.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))

            val motion = OniSkin.motion
            AnimatedContent(
                targetState = Pair(currentLyricLine, nextLyricLine),
                transitionSpec = {
                    fadeIn(animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing)) togetherWith
                        fadeOut(animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing))
                },
                label = "lyrics_preview_text_transition",
                modifier = Modifier.weight(1f)
            ) { (current, next) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!current.isNullOrBlank()) {
                        // Two-line layout: Line 1 (current singing line), Line 2 (upcoming line or wrapped line)
                        Text(
                            text = current,
                            style = OniSkin.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary,
                            maxLines = if (next.isNullOrBlank()) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        if (!next.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = next,
                                style = OniSkin.typography.bodySmall,
                                fontWeight = FontWeight.Normal,
                                color = OniSkin.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                        }
                    } else {
                        // Only shown when song has no lyrics at all
                        Text(
                            text = "Lyrics & Karaoke",
                            style = OniSkin.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OniSkin.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Tap to view or search online",
                            style = OniSkin.typography.bodySmall,
                            color = OniSkin.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
