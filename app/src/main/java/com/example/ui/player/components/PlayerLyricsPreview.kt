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
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.surface.OniSurface
import com.example.ui.player.LyricLineMode
import com.example.ui.player.components.lyrics.LyricEmphasis
import com.example.ui.player.components.lyrics.LyricLineContent
import com.example.ui.player.components.lyrics.lyricEmphasisFor
import com.example.ui.player.components.lyrics.rememberLyricsAppearance
import com.example.ui.theme.OniSkin

/**
 * Compact inline lyrics row used by the Player control card.
 *
 * This is intentionally not a card of its own: the floating Player controller owns the surface.
 * The visual treatment is driven by the same persisted Lyrics Appearance settings as the full
 * lyrics screen, including active-line color, inactive-line color, font size, inactive scale,
 * alignment, and bold-active-line.
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
    if (lyricLineMode == LyricLineMode.HIDDEN) return

    val appearance by rememberLyricsAppearance()
    val visibleLineCount = lineCount.coerceIn(1, 3)
    val lines = listOfNotNull(
        currentLyricLine?.takeIf { it.isNotBlank() },
        nextLyricLine?.takeIf { visibleLineCount >= 2 && it.isNotBlank() },
        thirdLyricLine?.takeIf { visibleLineCount >= 3 && it.isNotBlank() }
    )

    val hasAnyLyrics = lines.isNotEmpty()
    val accessibilityDesc = when {
        isFetchingLyrics -> "Downloading lyrics…"
        hasAnyLyrics && hasSynchronizedLyrics -> "Live lyrics: $currentLyricLine. Tap for karaoke."
        hasAnyLyrics -> "Lyrics preview: $currentLyricLine. Tap to open lyrics."
        else -> "No lyrics found. Tap to search online or add lyrics."
    }

    val compactLineHeight = 20.dp
    val lyricContentHeight = when (visibleLineCount) {
        1 -> 48.dp
        2 -> 64.dp
        else -> 80.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(lyricContentHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding)
            .testTag("player_lyrics_preview")
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDesc
            },
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
            if (hasAnyLyrics) {
                lines.forEachIndexed { index, line ->
                    LyricLineContent(
                        text = line,
                        emphasis = when (index) {
                            0 -> LyricEmphasis.Active
                            1 -> LyricEmphasis.Near
                            else -> LyricEmphasis.Distant
                        },
                        appearance = appearance,
                        accentActiveLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(compactLineHeight),
                        maxLines = 1,
                        fontSizeOverrideSp = if (index == 0) 15f else 13f
                    )
                }
            } else if (isFetchingLyrics) {
                Text(
                    text = "Downloading lyrics…",
                    style = OniSkin.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = OniSkin.colors.primary,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "No lyrics • Tap to search or add",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1
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
