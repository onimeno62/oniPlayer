package com.example.ui.player.components.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.LyricsAlignment
import com.example.data.preferences.LyricsAppearance
import com.example.data.preferences.LyricsSettingsStore
import com.example.ui.theme.OniSkin
import kotlinx.coroutines.delay

/** Observes the persisted lyrics appearance. */
@Composable
fun rememberLyricsAppearance(): State<LyricsAppearance> {
    val context = LocalContext.current.applicationContext
    val flow = remember(context) { LyricsSettingsStore.appearance(context) }
    return flow.collectAsStateWithLifecycle(initialValue = LyricsAppearance())
}

/** Parses `#RRGGBB` / `#AARRGGBB`; returns null for blank or invalid input. */
fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * One synced lyric line styled by [appearance].
 *
 * Non-active lines are drawn smaller with a graphicsLayer scale (not a smaller font), so the
 * measured height of every row stays constant and the auto-follow scroll never jumps.
 */
@Composable
fun LyricLineContent(
    text: String,
    emphasis: LyricEmphasis,
    appearance: LyricsAppearance,
    accentActiveLine: Boolean,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val colors = OniSkin.colors
    val motion = OniSkin.motion
    val isActive = emphasis == LyricEmphasis.Active
    val customText = parseHexColor(appearance.textColorHex)
    val highlight = parseHexColor(appearance.highlightColorHex)

    val targetColor = when (emphasis) {
        LyricEmphasis.Active -> highlight ?: if (accentActiveLine) colors.primary else (customText ?: colors.textPrimary)
        LyricEmphasis.Near -> customText?.copy(alpha = 0.72f) ?: colors.textSecondary
        LyricEmphasis.Distant -> customText?.copy(alpha = 0.42f) ?: colors.textTertiary
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = motion.componentStateDurationMs, easing = motion.standardEasing),
        label = "lyric_line_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else appearance.inactiveScale,
        animationSpec = tween(durationMillis = motion.componentStateDurationMs, easing = motion.standardEasing),
        label = "lyric_line_scale"
    )

    val base = if (isActive) OniSkin.typography.lyricActive else OniSkin.typography.lyricInactive
    val weight = if (isActive && appearance.boldActiveLine) FontWeight.Bold else OniSkin.typography.lyricInactive.fontWeight
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val textAlign = if (appearance.alignment == LyricsAlignment.CENTER) TextAlign.Center else TextAlign.Start
    val originX = when {
        appearance.alignment == LyricsAlignment.CENTER -> 0.5f
        isRtl -> 1f
        else -> 0f
    }

    Text(
        text = text,
        style = base.copy(
            fontSize = appearance.fontSizeSp.sp,
            lineHeight = (appearance.fontSizeSp * 1.33f).sp,
            fontWeight = weight,
            textDirection = TextDirection.Content
        ),
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(originX, 0.5f)
            }
    )
}

/** Live, self-advancing preview used by the lyrics settings screen. */
@Composable
fun LyricsAppearancePreview(
    appearance: LyricsAppearance,
    modifier: Modifier = Modifier,
    accentActiveLine: Boolean = true
) {
    val sample = remember {
        listOf(
            "City lights are fading slow",
            "I still hear you in the night",
            "Every word becomes a song",
            "Singing softly till the dawn"
        )
    }
    var activeIndex by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            activeIndex = (activeIndex + 1) % sample.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(OniSkin.shapes.medium)
            .background(OniSkin.colors.background)
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = "Lyrics style preview" }
            .testTag("lyrics_appearance_preview"),
        horizontalAlignment = if (appearance.alignment == LyricsAlignment.CENTER) Alignment.CenterHorizontally else Alignment.Start
    ) {
        sample.forEachIndexed { index, line ->
            LyricLineContent(
                text = line,
                emphasis = lyricEmphasisFor(index, activeIndex),
                appearance = appearance,
                accentActiveLine = accentActiveLine,
                modifier = Modifier.padding(vertical = OniSkin.spacing.xxs)
            )
        }
    }
}
