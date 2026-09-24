package com.example.ui.widgets.defaultpack.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.SkipNextActionCallback
import com.example.ui.widgets.actions.SkipPreviousActionCallback
import com.example.ui.widgets.actions.TogglePlayPauseActionCallback
import com.example.ui.widgets.actions.ToggleRepeatActionCallback
import com.example.ui.widgets.actions.ToggleShuffleActionCallback
import com.example.ui.widgets.skin.OniWidgetVisualSystem

/** Existing widget actions, mapped once. No new actions are introduced here. */
enum class OniTransport(val icon: Int, val label: String) {
    Previous(R.drawable.ic_skip_previous, "Previous track"),
    Next(R.drawable.ic_skip_next, "Next track"),
    Shuffle(R.drawable.ic_shuffle, "Shuffle"),
    Repeat(R.drawable.ic_repeat, "Repeat one");

    fun action(): Action = when (this) {
        Previous -> actionRunCallback<SkipPreviousActionCallback>()
        Next -> actionRunCallback<SkipNextActionCallback>()
        Shuffle -> actionRunCallback<ToggleShuffleActionCallback>()
        Repeat -> actionRunCallback<ToggleRepeatActionCallback>()
    }
}

/** Visual weight of the primary play/pause control. */
enum class OniPrimaryStyle {
    /** Solid accent disc: flagship and transport widgets. */
    Filled,
    /** Accent-tinted disc: lyric widgets, where controls stay secondary. */
    Tonal,
    /** Translucent disc: on top of full-bleed artwork. */
    Glass
}

/** Primary control: Play/Pause. Always the heaviest element in a transport group. */
@Composable
fun OniPrimaryControl(
    skin: OniSkinTokens,
    isPlaying: Boolean,
    diameter: Dp,
    style: OniPrimaryStyle = OniPrimaryStyle.Filled
) {
    val container: Color
    val tint: Color
    when (style) {
        OniPrimaryStyle.Filled -> {
            container = OniWidgetVisualSystem.primary(skin)
            tint = OniWidgetVisualSystem.controlOnPrimary(skin)
        }
        OniPrimaryStyle.Tonal -> {
            container = OniWidgetVisualSystem.tonal(skin)
            tint = OniWidgetVisualSystem.primary(skin)
        }
        OniPrimaryStyle.Glass -> {
            container = OniWidgetVisualSystem.glass(skin)
            tint = OniWidgetVisualSystem.text(skin)
        }
    }
    Box(
        modifier = GlanceModifier
            .size(diameter)
            .cornerRadius(diameter / 2)
            .background(ColorProvider(container))
            .clickable(actionRunCallback<TogglePlayPauseActionCallback>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = GlanceModifier.size(diameter * 0.46f),
            colorFilter = ColorFilter.tint(ColorProvider(tint))
        )
    }
}

/** Secondary control: Previous / Next. Bare icon with a full-size touch target. */
@Composable
fun OniSecondaryControl(
    skin: OniSkinTokens,
    kind: OniTransport,
    target: Dp = 40.dp,
    icon: Dp = 22.dp
) {
    Box(
        modifier = GlanceModifier
            .size(maxOf(target, OniWidgetVisualSystem.minTouchTarget(skin)))
            .cornerRadius(target / 2)
            .clickable(kind.action()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(kind.icon),
            contentDescription = kind.label,
            modifier = GlanceModifier.size(icon),
            colorFilter = ColorFilter.tint(ColorProvider(OniWidgetVisualSystem.secondaryControlTint(skin)))
        )
    }
}

/**
 * Tertiary control: Shuffle / Repeat. Small and quiet; an accent dot under the
 * icon marks the active state so it is not communicated by tint alone.
 */
@Composable
fun OniTertiaryControl(
    skin: OniSkinTokens,
    kind: OniTransport,
    active: Boolean,
    target: Dp = 36.dp,
    icon: Dp = 18.dp
) {
    val tint = if (active) {
        OniWidgetVisualSystem.tertiaryActiveTint(skin)
    } else {
        OniWidgetVisualSystem.tertiaryInactiveTint(skin)
    }
    Box(
        modifier = GlanceModifier
            .size(maxOf(target, OniWidgetVisualSystem.minTouchTarget(skin)))
            .clickable(kind.action()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(kind.icon),
                contentDescription = "${kind.label}, ${if (active) "on" else "off"}",
                modifier = GlanceModifier.size(icon),
                colorFilter = ColorFilter.tint(ColorProvider(tint))
            )
            Spacer(GlanceModifier.height(3.dp))
            Box(
                modifier = GlanceModifier
                    .size(4.dp)
                    .cornerRadius(2.dp)
                    .background(ColorProvider(if (active) tint else Color.Transparent))
            ) {}
        }
    }
}

/**
 * Integrated transport capsule: previous / play-pause / next on one control
 * surface. [stretch] spreads the controls across the full width.
 */
@Composable
fun OniTransportCapsule(
    skin: OniSkinTokens,
    isPlaying: Boolean,
    height: Dp,
    secondaryTarget: Dp,
    secondaryIcon: Dp,
    showPrevious: Boolean = true,
    stretch: Boolean = false,
    modifier: GlanceModifier = GlanceModifier
) {
    val primary = height - 8.dp
    OniWidgetPanel(
        skin = skin,
        modifier = modifier.height(height),
        tone = OniWidgetTone.Control,
        radius = height / 2
    ) {
        if (stretch) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(GlanceModifier.defaultWeight())
                if (showPrevious) {
                    OniSecondaryControl(skin, OniTransport.Previous, secondaryTarget, secondaryIcon)
                    Spacer(GlanceModifier.defaultWeight())
                }
                OniPrimaryControl(skin, isPlaying, primary)
                Spacer(GlanceModifier.defaultWeight())
                OniSecondaryControl(skin, OniTransport.Next, secondaryTarget, secondaryIcon)
                Spacer(GlanceModifier.defaultWeight())
            }
        } else {
            Row(
                modifier = GlanceModifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPrevious) {
                    OniSecondaryControl(skin, OniTransport.Previous, secondaryTarget, secondaryIcon)
                }
                OniPrimaryControl(skin, isPlaying, primary)
                OniSecondaryControl(skin, OniTransport.Next, secondaryTarget, secondaryIcon)
            }
        }
    }
}

/** Width a non-stretched [OniTransportCapsule] occupies; used for layout budgeting. */
fun transportCapsuleWidth(skin: OniSkinTokens, height: Float, secondaryTarget: Float, showPrevious: Boolean): Float {
    val target = maxOf(secondaryTarget, OniWidgetVisualSystem.minTouchTarget(skin).value)
    return 8f + (height - 8f) + target * (if (showPrevious) 2 else 1)
}

/** Small playback-state dot. Always paired with text that also states the playback state. */
@Composable
fun OniPlaybackDot(skin: OniSkinTokens, isPlaying: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(6.dp)
            .cornerRadius(3.dp)
            .background(
                ColorProvider(
                    if (isPlaying) OniWidgetVisualSystem.primary(skin) else OniWidgetVisualSystem.faint(skin)
                )
            )
    ) {}
}

/** Short vertical accent used to mark the active lyric line. */
@Composable
fun OniAccentTick(skin: OniSkinTokens, height: Dp) {
    Box(
        modifier = GlanceModifier
            .width(3.dp)
            .height(height)
            .cornerRadius(2.dp)
            .background(ColorProvider(OniWidgetVisualSystem.primary(skin)))
    ) {}
}
