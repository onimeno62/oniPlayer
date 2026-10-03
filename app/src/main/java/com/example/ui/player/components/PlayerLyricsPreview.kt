package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.player.LyricLineMode
import com.example.ui.theme.OniSkin

/**
 * Compact inline lyrics preview and karaoke prompt for the Player screen in Default Skin.
 *
 * Supports LyricLineMode (UNDERNEATH, OVER_BOTTOM, HIDDEN) and configurable line count (1, 2, 3).
 * When lyrics are missing, invites the user to search online or paste manual lyrics.
 */
@Composable
fun PlayerLyricsPreview(
    currentLyricLine: String?,
    nextLyricLine: String? = null,
    thirdLyricLine: String? = null,
    hasSynchronizedLyrics: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = OniSkin.spacing.screenHorizontal,
    isFetchingLyrics: Boolean = false,
    lyricLineMode: LyricLineMode = LyricLineMode.UNDERNEATH,
    lineCount: Int = 2
) {
    if (lyricLineMode == LyricLineMode.HIDDEN) {
        return
    }

    val hasAnyLyrics = !currentLyricLine.isNullOrBlank()

    val combinedLyrics = buildString {
        if (!currentLyricLine.isNullOrBlank()) append(currentLyricLine)
        if (lineCount >= 2 && !nextLyricLine.isNullOrBlank()) {
            if (isNotEmpty()) append("\n")
            append(nextLyricLine)
        }
        if (lineCount >= 3 && !thirdLyricLine.isNullOrBlank()) {
            if (isNotEmpty()) append("\n")
            append(thirdLyricLine)
        }
    }

    val accessibilityDesc = when {
        isFetchingLyrics -> "Downloading lyrics…"
        hasAnyLyrics && hasSynchronizedLyrics -> "Live lyrics: $currentLyricLine. Tap for karaoke."
        hasAnyLyrics -> "Lyrics preview: $currentLyricLine. Tap to open lyrics."
        else -> "No lyrics found. Tap to search online or add lyrics."
    }

    val containerVariant = if (hasAnyLyrics && hasSynchronizedLyrics) {
        OniSurfaceVariant.Soft
    } else {
        OniSurfaceVariant.Flat
    }

    val visibleLineCount = lineCount.coerceIn(1, 3)
    // Reserve the exact same vertical space for the selected 1/2/3-line mode.
    // This prevents the card from jumping when the active lyric changes length.
    val lyricContentHeight = when (visibleLineCount) {
        1 -> 48.dp
        2 -> 68.dp
        else -> 88.dp
    }

    OniSurface(
        variant = containerVariant,
        shape = OniSkin.shapes.card,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(lyricContentHeight)
            .padding(horizontal = horizontalPadding)
            .testTag("player_lyrics_preview")
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDesc
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasSynchronizedLyrics) Icons.Default.Sync else Icons.Default.Lyrics,
                contentDescription = null,
                tint = if (hasAnyLyrics) OniSkin.colors.primary else OniSkin.colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (isFetchingLyrics) {
                    Text(
                        text = "Downloading lyrics…",
                        style = OniSkin.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = OniSkin.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (hasAnyLyrics) {
                    combinedLyrics
                        .lines()
                        .take(visibleLineCount)
                        .forEach { line ->
                            Text(
                                text = line,
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textPrimary.copy(alpha = 0.95f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                } else {
                    Text(
                        text = "No lyrics • Tap to search or add",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open lyrics",
                tint = OniSkin.colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
