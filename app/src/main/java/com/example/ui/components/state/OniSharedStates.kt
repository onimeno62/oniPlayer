package com.example.ui.components.state

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.theme.OniSkin

/**
 * Reusable loading state component.
 * Consumes [OniSkin.colors], [OniSkin.typography], and [OniSkin.spacing] tokens.
 */
@Composable
fun OniLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
    indicatorSize: Dp = 40.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OniSkin.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(indicatorSize)
                .semantics {
                    contentDescription = message ?: "Loading"
                },
            strokeWidth = 3.dp,
            color = OniSkin.colors.primary
        )

        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(OniSkin.spacing.md))
            Text(
                text = message,
                style = OniSkin.typography.bodyMedium,
                color = OniSkin.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reusable empty state component.
 * Consumes [OniSkin.colors], [OniSkin.typography], and [OniSkin.spacing] tokens.
 */
@Composable
fun OniEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.HourglassEmpty,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OniSkin.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OniSkin.colors.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.lg))

        Text(
            text = title,
            style = OniSkin.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OniSkin.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        Text(
            text = message,
            style = OniSkin.typography.bodyMedium,
            color = OniSkin.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(OniSkin.spacing.xl))
            OniPrimaryButton(
                text = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

/**
 * Reusable error state component.
 * Consumes [OniSkin.colors], [OniSkin.typography], and [OniSkin.spacing] tokens.
 */
@Composable
fun OniErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ErrorOutline,
    retryLabel: String? = "Retry",
    onRetryClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OniSkin.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OniSkin.colors.error,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.lg))

        Text(
            text = title,
            style = OniSkin.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OniSkin.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        Text(
            text = message,
            style = OniSkin.typography.bodyMedium,
            color = OniSkin.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (retryLabel != null && onRetryClick != null) {
            Spacer(modifier = Modifier.height(OniSkin.spacing.xl))
            OniPrimaryButton(
                text = retryLabel,
                onClick = onRetryClick
            )
        }
    }
}

/**
 * Reusable skeleton placeholder row for loading track lists.
 */
@Composable
fun OniSkeletonTrackRow(
    modifier: Modifier = Modifier,
    enableAnimation: Boolean = true
) {
    val alpha = if (enableAnimation) {
        val infiniteTransition = rememberInfiniteTransition(label = "skeleton_track_transition")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_track_alpha"
        )
        animatedAlpha
    } else {
        0.25f
    }

    val placeholderColor = OniSkin.colors.onSurface.copy(alpha = alpha)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(OniSkin.shapes.small)
                .background(placeholderColor)
        )

        Spacer(modifier = Modifier.width(OniSkin.spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(16.dp)
                    .clip(OniSkin.shapes.xs)
                    .background(placeholderColor)
            )

            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.40f)
                    .height(12.dp)
                    .clip(OniSkin.shapes.xs)
                    .background(placeholderColor)
            )
        }
    }
}
