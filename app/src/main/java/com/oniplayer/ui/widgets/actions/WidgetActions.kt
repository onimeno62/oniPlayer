package com.oniplayer.ui.widgets.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.oniplayer.ui.widgets.playback.WidgetPlaybackStateAdapter
import com.oniplayer.ui.widgets.glance.NowPlayingGlanceWidget
import com.oniplayer.ui.widgets.glance.CompactPlayerGlanceWidget
import com.oniplayer.ui.widgets.glance.LyricsGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TogglePlayPauseActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetPlaybackStateAdapter.togglePlayPause(context)
        triggerWidgetRefresh(context)
    }
}

class SkipNextActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetPlaybackStateAdapter.skipNext(context)
        triggerWidgetRefresh(context)
    }
}

class SkipPreviousActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetPlaybackStateAdapter.skipPrevious(context)
        triggerWidgetRefresh(context)
    }
}

class ToggleShuffleActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetPlaybackStateAdapter.toggleShuffle(context)
        triggerWidgetRefresh(context)
    }
}

class ToggleRepeatActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetPlaybackStateAdapter.toggleRepeat(context)
        triggerWidgetRefresh(context)
    }
}

private fun triggerWidgetRefresh(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            NowPlayingGlanceWidget().updateAll(context)
            CompactPlayerGlanceWidget().updateAll(context)
            LyricsGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Safe fallback
        }
    }
}
