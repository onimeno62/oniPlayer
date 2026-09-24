package com.example.ui.widgets.defaultpack.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.skin.OniWidgetVisualSystem

/**
 * Progress rail. Width is explicit because Glance has no fractional weights;
 * callers compute it from LocalSize. Tiny fractions keep a visible cap and an
 * optional knob marks position on flagship surfaces.
 */
@Composable
fun OniProgressRail(
    skin: OniSkinTokens,
    state: OniWidgetPlaybackState,
    width: Dp,
    thickness: Dp = skin.widgets.progressThickness,
    knob: Boolean = false,
    onArtwork: Boolean = false
) {
    val w = width.value.coerceAtLeast(0f)
    val t = thickness.value.coerceAtLeast(1f)
    val hasDuration = state.durationMs > 0L
    val showKnob = knob && skin.widgets.progressKnob && hasDuration
    val knobD = if (showKnob) t + 6f else 0f
    val fill = OniWidgetProgressMath.fillWidth(state.progress, w - knobD, t, hasDuration)
    val track = if (onArtwork) OniWidgetVisualSystem.trackOnArtwork(skin) else OniWidgetVisualSystem.track(skin)
    val active = OniWidgetVisualSystem.primary(skin)

    Box(
        modifier = GlanceModifier
            .width(w.dp)
            .height(maxOf(t, knobD).dp)
            .semantics { contentDescription = OniWidgetCopy.progressDescription(state) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = GlanceModifier
                .width(w.dp)
                .height(t.dp)
                .cornerRadius((t / 2f).dp)
                .background(ColorProvider(track))
        ) {}
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (fill > 0f) {
                Box(
                    modifier = GlanceModifier
                        .width(fill.dp)
                        .height(t.dp)
                        .cornerRadius((t / 2f).dp)
                        .background(ColorProvider(active))
                ) {}
            }
            if (showKnob) {
                Box(
                    modifier = GlanceModifier
                        .size(knobD.dp)
                        .cornerRadius((knobD / 2f).dp)
                        .background(ColorProvider(active))
                ) {}
            }
        }
    }
}

/** Elapsed / total labels spanning [width]. */
@Composable
fun OniTimeRow(skin: OniSkinTokens, state: OniWidgetPlaybackState, width: Dp) {
    val style = OniWidgetType.style(skin, OniWidgetTextRole.Time, OniWidgetVisualSystem.muted(skin))
    Row(modifier = GlanceModifier.width(width)) {
        Text(OniWidgetCopy.time(state.positionMs), style = style, maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        Text(OniWidgetCopy.time(state.durationMs), style = style, maxLines = 1)
    }
}

/** Compact inline variant: 0:42 ━━━━━ 3:51, for rows that cannot afford a second line. */
@Composable
fun OniInlineProgress(
    skin: OniSkinTokens,
    state: OniWidgetPlaybackState,
    width: Dp,
    thickness: Dp = 3.dp
) {
    val labelW = 34f
    val gap = 6f
    val railW = (width.value - labelW * 2 - gap * 2).coerceAtLeast(16f)
    val muted = OniWidgetVisualSystem.muted(skin)
    Row(modifier = GlanceModifier.width(width), verticalAlignment = Alignment.CenterVertically) {
        Text(
            OniWidgetCopy.time(state.positionMs),
            modifier = GlanceModifier.width(labelW.dp),
            style = OniWidgetType.style(skin, OniWidgetTextRole.Time, muted, TextAlign.Start),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(gap.dp))
        OniProgressRail(skin, state, railW.dp, thickness)
        Spacer(GlanceModifier.width(gap.dp))
        Text(
            OniWidgetCopy.time(state.durationMs),
            modifier = GlanceModifier.width(labelW.dp),
            style = OniWidgetType.style(skin, OniWidgetTextRole.Time, muted, TextAlign.End),
            maxLines = 1
        )
    }
}
