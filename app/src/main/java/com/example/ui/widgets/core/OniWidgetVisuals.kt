package com.example.ui.widgets.core

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.action.actionRunCallback
import com.example.R
import com.example.ui.theme.OniSkinTokens
import com.example.ui.widgets.actions.SkipNextActionCallback
import com.example.ui.widgets.actions.SkipPreviousActionCallback
import com.example.ui.widgets.actions.TogglePlayPauseActionCallback
import com.example.ui.widgets.actions.ToggleRepeatActionCallback
import com.example.ui.widgets.actions.ToggleShuffleActionCallback

object OniWidgetVisuals {
    fun surface(skin: OniSkinTokens, paddingDp: Int = 10): GlanceModifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(skin.colors.surface))
        .cornerRadius(20.dp)
        .padding(paddingDp.dp)

    fun secondarySurface(skin: OniSkinTokens, size: Int): GlanceModifier = GlanceModifier
        .size(size.dp)
        .background(ColorProvider(skin.colors.surfaceVariant))
        .cornerRadius((size / 2).coerceAtLeast(10).dp)

    fun playButton(skin: OniSkinTokens, playing: Boolean, size: Int): GlanceModifier = GlanceModifier
        .size(size.dp)
        .background(ColorProvider(skin.colors.primary))
        .cornerRadius((size / 2).dp)
        .padding((size / 4).dp)
        .clickable(actionRunCallback<TogglePlayPauseActionCallback>())

    fun artwork(size: Int): GlanceModifier = GlanceModifier
        .size(size.dp)
        .cornerRadius((size / 5).dp)

    fun progressTrack(skin: OniSkinTokens): GlanceModifier = GlanceModifier
        .fillMaxWidth()
        .height(4.dp)
        .background(ColorProvider(skin.colors.surfaceVariant))
        .cornerRadius(2.dp)

    fun progressFill(skin: OniSkinTokens, fraction: Float): GlanceModifier = GlanceModifier
        .width((fraction.coerceIn(0f, 1f) * 1000).toInt().coerceAtLeast(1).dp)
        .height(4.dp)
        .background(ColorProvider(skin.colors.primary))
        .cornerRadius(2.dp)

    fun textPrimary(skin: OniSkinTokens, size: Int = 14) = TextStyle(
        color = ColorProvider(skin.colors.textPrimary),
        fontSize = size.sp,
        fontWeight = FontWeight.Medium
    )

    fun textSecondary(skin: OniSkinTokens, size: Int = 11) = TextStyle(
        color = ColorProvider(skin.colors.textSecondary),
        fontSize = size.sp
    )

    fun textAccent(skin: OniSkinTokens, size: Int = 12) = TextStyle(
        color = ColorProvider(skin.colors.primary),
        fontSize = size.sp,
        fontWeight = FontWeight.Medium
    )

    fun ControlRow(skin: OniSkinTokens, state: OniWidgetPlaybackState) {
        // Intentionally empty: visual primitives live here; renderers own layout.
    }

    fun iconButton(
        skin: OniSkinTokens,
        resource: Int,
        description: String,
        size: Int = 36,
        onClick: androidx.glance.action.Action
    ): GlanceModifier = GlanceModifier
        .size(size.dp)
        .background(ColorProvider(skin.colors.surfaceVariant))
        .cornerRadius((size / 2).dp)
        .padding((size / 4).dp)
        .clickable(onClick)
}

fun OniWidgetPlaybackState.progressFraction(): Float =
    if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

fun OniWidgetPlaybackState.timeLabel(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
