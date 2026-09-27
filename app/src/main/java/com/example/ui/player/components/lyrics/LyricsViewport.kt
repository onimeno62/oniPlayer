package com.example.ui.player.components.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LyricsAlignment
import com.example.data.preferences.LyricsAppearance
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.components.button.OniSecondaryButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.LrcLine
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.theme.OniSkin
import java.util.Locale

/**
 * Synced lyrics list. The lyrics are the surface: no cards, no borders.
 * The active line lands at [LYRICS_FOCUS_FRACTION] via symmetric content padding.
 * Size, colours and alignment come from the user's lyrics appearance settings; lines other than
 * the active one are drawn slightly smaller.
 */
@Composable
fun SyncedLyricsList(
    lines: List<LrcLine>,
    activeIndex: Int,
    followState: LyricsFollowState,
    accentActiveLine: Boolean,
    onLineClick: (LrcLine) -> Unit,
    modifier: Modifier = Modifier
) {
    LyricsAutoFollowEffect(followState, activeIndex, lines.size)
    val appearance by rememberLyricsAppearance()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        LazyColumn(
            state = followState.listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OniSkin.spacing.screenHorizontal,
                end = OniSkin.spacing.screenHorizontal,
                top = viewportHeight * LYRICS_FOCUS_FRACTION,
                bottom = viewportHeight * (1f - LYRICS_FOCUS_FRACTION)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = lines,
                key = { index, line -> "${index}_${line.timestampMs}" }
            ) { index, line ->
                SyncedLyricLine(
                    line = line,
                    emphasis = lyricEmphasisFor(index, activeIndex),
                    accent = accentActiveLine,
                    appearance = appearance,
                    onClick = { onLineClick(line) }
                )
            }
        }

        ResumeLyricsChip(
            visible = !followState.isFollowing,
            onClick = followState::resume,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = OniSkin.spacing.md)
        )
    }
}

@Composable
private fun SyncedLyricLine(
    line: LrcLine,
    emphasis: LyricEmphasis,
    accent: Boolean,
    appearance: LyricsAppearance,
    onClick: () -> Unit
) {
    val isActive = emphasis == LyricEmphasis.Active
    val seekLabel = "Seek to ${formatLyricTime(line.timestampMs)}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(OniSkin.shapes.medium)
            .clickable(onClickLabel = seekLabel, role = Role.Button, onClick = onClick)
            .semantics { if (isActive) stateDescription = "Current line" }
            .padding(horizontal = OniSkin.spacing.xs, vertical = OniSkin.spacing.xs),
        contentAlignment = if (appearance.alignment == LyricsAlignment.CENTER) Alignment.Center else Alignment.CenterStart
    ) {
        LyricLineContent(
            text = line.text,
            emphasis = emphasis,
            appearance = appearance,
            accentActiveLine = accent
        )
    }
}

/**
 * Plain lyrics. Rendered per line so each line resolves its own direction:
 * a Persian line starts on the right even inside an English UI.
 */
@Composable
fun PlainLyricsList(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) { LyricsHelper.stripLrcTags(text).lines().map { it.trim() } }
    val appearance by rememberLyricsAppearance()
    val plainSize = appearance.fontSizeSp * 0.8f
    val plainColor = parseHexColor(appearance.textColorHex) ?: OniSkin.colors.textPrimary
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = OniSkin.spacing.screenHorizontal + OniSkin.spacing.xs,
            vertical = OniSkin.spacing.xl
        )
    ) {
        itemsIndexed(lines) { _, line ->
            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(OniSkin.spacing.md))
            } else {
                Text(
                    text = line,
                    style = OniSkin.typography.titleMedium.copy(
                        fontSize = plainSize.sp,
                        lineHeight = (plainSize * 1.4f).sp,
                        textDirection = TextDirection.Content
                    ),
                    color = plainColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = OniSkin.spacing.xxs)
                )
            }
        }
    }
}

@Composable
fun LyricsEmptyState(
    onSearchOnline: () -> Unit,
    onPasteManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(OniSkin.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lyrics,
            contentDescription = null,
            tint = OniSkin.colors.textTertiary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.md))
        Text(
            text = "No lyrics yet",
            style = OniSkin.typography.titleMedium,
            color = OniSkin.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
        Text(
            text = "Find them online or paste your own.",
            style = OniSkin.typography.bodyMedium,
            color = OniSkin.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.xl))
        OniPrimaryButton(
            text = "Search online",
            onClick = onSearchOnline,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))
        OniSecondaryButton(
            text = "Paste manually",
            onClick = onPasteManually,
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}

@Composable
private fun ResumeLyricsChip(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = OniSkin.motion
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(motion.quickDurationMs)) +
            slideInVertically(animationSpec = tween(motion.componentStateDurationMs)) { it / 2 },
        exit = fadeOut(tween(motion.quickDurationMs)) +
            slideOutVertically(animationSpec = tween(motion.quickDurationMs)) { it / 2 }
    ) {
        OniSurface(
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.pill,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(horizontal = OniSkin.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = OniSkin.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                Text(
                    text = "Resume lyrics",
                    style = OniSkin.typography.labelLarge,
                    color = OniSkin.colors.textPrimary
                )
            }
        }
    }
}

internal fun formatLyricTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
