package com.example.ui.library.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.screens.dashboardRadiusMedium
import com.example.ui.screens.dashboardRadiusSmall

@Composable
fun LibraryRowSkeleton(
    enableAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alpha = if (enableAnimation) {
        val infiniteTransition = rememberInfiniteTransition(label = "skeleton_row_transition")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_row_alpha"
        )
        animatedAlpha
    } else {
        0.3f
    }

    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(dashboardRadiusSmall()))
                .background(placeholderColor)
        )

        Spacer(modifier = Modifier.width(LibrarySpacing.md))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )

            Spacer(modifier = Modifier.height(LibrarySpacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
        }
    }
}

@Composable
fun LibraryGridCardSkeleton(
    enableAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alpha = if (enableAnimation) {
        val infiniteTransition = rememberInfiniteTransition(label = "skeleton_card_transition")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_card_alpha"
        )
        animatedAlpha
    } else {
        0.3f
    }

    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Column(
        modifier = modifier
            .width(160.dp)
            .padding(LibrarySpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(dashboardRadiusMedium()))
                .background(placeholderColor)
        )

        Spacer(modifier = Modifier.height(LibrarySpacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(placeholderColor)
        )

        Spacer(modifier = Modifier.height(LibrarySpacing.xs))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(placeholderColor)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibrarySkeletonsPreview() {
    Column(modifier = Modifier.padding(LibrarySpacing.md)) {
        LibraryRowSkeleton(enableAnimation = true)
        Spacer(modifier = Modifier.height(LibrarySpacing.sm))
        LibraryGridCardSkeleton(enableAnimation = true)
    }
}
