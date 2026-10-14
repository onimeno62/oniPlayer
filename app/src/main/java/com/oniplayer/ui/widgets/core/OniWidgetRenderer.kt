package com.oniplayer.ui.widgets.core

import android.content.Context
import androidx.compose.runtime.Composable
import com.oniplayer.ui.theme.OniSkinTokens

/**
 * Contract for rendering an oniPlayer Glance widget with the active skin tokens.
 */
interface OniWidgetRenderer {
    @Composable
    fun Render(
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    )
}
