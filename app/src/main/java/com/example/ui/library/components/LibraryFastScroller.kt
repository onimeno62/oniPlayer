package com.example.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Draggable fast scroller overlay for a [LazyListState]-backed list.
 *
 * - Appears while the list scrolls (and ~1.2s after), fades out when idle.
 * - Only the thumb (28dp x 56dp) captures touches, so row taps/swipes near the edge keep working.
 * - While dragging, a bubble shows the first letter of the item under the thumb ([letterAt]).
 * - Hidden for short lists (< [minItems]).
 *
 * Place it as a sibling on top of the list inside a Box: `Box { LazyColumn(...); LibraryFastScroller(...) }`.
 */
@Composable
fun BoxScope.LibraryFastScroller(
    listState: LazyListState,
    itemCount: Int,
    letterAt: (Int) -> String,
    modifier: Modifier = Modifier,
    minItems: Int = 30,
    bottomInset: androidx.compose.ui.unit.Dp = 96.dp
) {
    if (itemCount < minItems) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { 56.dp.toPx() }

    var heightPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var recentlyScrolled by remember { mutableStateOf(false) }

    val scrolling = listState.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            recentlyScrolled = true
        } else {
            delay(1200)
            recentlyScrolled = false
        }
    }
    val visible = recentlyScrolled || dragging
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "fastScrollerAlpha")

    val listFraction = if (itemCount <= 1) 0f
    else (listState.firstVisibleItemIndex.toFloat() / (itemCount - 1)).coerceIn(0f, 1f)
    val fraction = if (dragging) dragFraction else listFraction
    val travelPx = (heightPx - thumbHeightPx).coerceAtLeast(1f)
    val currentIndex = (fraction * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)

    Box(
        modifier = modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .padding(bottom = bottomInset)
            .width(28.dp)
            .onSizeChanged { heightPx = it.height }
    ) {
        val thumbModifier = Modifier
            .align(Alignment.TopEnd)
            .offset { IntOffset(0, (fraction * travelPx).roundToInt()) }
            .width(28.dp)
            .height(56.dp)
            .graphicsLayer { this.alpha = alpha }
            .semantics { contentDescription = "Fast scroll" }
            .then(
                if (visible) {
                    Modifier.pointerInput(itemCount, heightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                dragFraction = listFraction
                            },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                dragFraction = (dragFraction + dy / travelPx).coerceIn(0f, 1f)
                                val target = (dragFraction * (itemCount - 1)).roundToInt()
                                scope.launch { listState.scrollToItem(target) }
                            }
                        )
                    }
                } else Modifier
            )

        Box(thumbModifier) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .width(if (dragging) 8.dp else 6.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(OniSkin.colors.primary.copy(alpha = 0.85f))
            )
        }

        if (dragging) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(with(density) { (-44).dp.roundToPx() }, (fraction * travelPx).roundToInt()) }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(OniSkin.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letterAt(currentIndex),
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.onPrimary
                )
            }
        }
    }
}
