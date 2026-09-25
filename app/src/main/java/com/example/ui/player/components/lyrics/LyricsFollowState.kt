package com.example.ui.player.components.lyrics

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Where the active synced lyric settles inside the viewport, as a fraction of the viewport
 * height measured from the top. Upper-middle keeps the previous line visible for context
 * without burying the active line near the bottom.
 */
const val LYRICS_FOCUS_FRACTION = 0.35f

/** Whether the lyrics viewport follows playback or the user is browsing freely. */
enum class LyricsFollowMode { Following, Browsing }

/** Visual emphasis tier of a synced lyric line relative to the active line. */
enum class LyricEmphasis { Active, Near, Distant }

/**
 * Pure emphasis calculation so it can be unit tested.
 * Before the first timestamp (activeIndex < 0) the first two lines are treated as upcoming.
 */
fun lyricEmphasisFor(index: Int, activeIndex: Int): LyricEmphasis = when {
    activeIndex < 0 -> if (index <= 1) LyricEmphasis.Near else LyricEmphasis.Distant
    index == activeIndex -> LyricEmphasis.Active
    index == activeIndex - 1 -> LyricEmphasis.Near
    index in (activeIndex + 1)..(activeIndex + 2) -> LyricEmphasis.Near
    else -> LyricEmphasis.Distant
}

/**
 * UI-only state holder for lyrics follow behaviour. It is bound to a [LazyListState], which is
 * UI state, so it deliberately lives in the composition rather than the ViewModel. There is no
 * duplicate copy of this state anywhere else.
 */
@Stable
class LyricsFollowState internal constructor(val listState: LazyListState) {
    var mode by mutableStateOf(LyricsFollowMode.Following)
        private set

    val isFollowing: Boolean
        get() = mode == LyricsFollowMode.Following

    /** Only real user drags call this; programmatic scrolls never emit a DragInteraction. */
    fun onUserDrag() {
        mode = LyricsFollowMode.Browsing
    }

    fun resume() {
        mode = LyricsFollowMode.Following
    }
}

/**
 * Creates a follow state that resets to [LyricsFollowMode.Following] whenever [key] changes
 * (track change, lyrics text change, synced/plain switch).
 */
@Composable
fun rememberLyricsFollowState(key: Any?): LyricsFollowState {
    val state = remember(key) { LyricsFollowState(LazyListState()) }
    LaunchedEffect(state) {
        state.listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) state.onUserDrag()
        }
    }
    return state
}

/**
 * Keeps the active line at the focus position while following. The first positioning after a
 * reset jumps instantly; later changes animate. Never scrolls while the user is browsing.
 */
@Composable
internal fun LyricsAutoFollowEffect(state: LyricsFollowState, activeIndex: Int, lineCount: Int) {
    var hasPositioned by remember(state) { mutableStateOf(false) }
    LaunchedEffect(state, activeIndex, state.mode, lineCount) {
        if (!state.isFollowing || lineCount == 0) return@LaunchedEffect
        val target = activeIndex.coerceIn(0, lineCount - 1)
        if (!hasPositioned) {
            state.listState.scrollToItem(target)
            hasPositioned = true
        } else {
            state.listState.animateScrollToItem(target)
        }
    }
}
