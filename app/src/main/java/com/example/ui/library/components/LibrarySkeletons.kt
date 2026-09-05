package com.example.ui.library.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.state.OniSkeletonTrackRow
import com.example.ui.theme.OniSkin

@Composable
fun LibraryRowSkeleton(
    enableAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    OniSkeletonTrackRow(modifier = modifier)
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

    val placeholderColor = OniSkin.colors.outline.copy(alpha = alpha)

    Column(
        modifier = modifier
            .width(160.dp)
            .padding(OniSkin.spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(OniSkin.shapes.card)
                .background(placeholderColor)
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(placeholderColor)
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

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
    Column(modifier = Modifier.padding(16.dp)) {
        LibraryRowSkeleton(enableAnimation = true)
        Spacer(modifier = Modifier.height(8.dp))
        LibraryGridCardSkeleton(enableAnimation = true)
    }
}
