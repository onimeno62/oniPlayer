package com.example.playback

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.ui.viewmodel.MusicPlayerViewModel

class OniStandardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val viewModel = MusicPlayerViewModel.activeInstance
        val song = viewModel?.audioEngine?.currentSong?.value
        val isPlaying = viewModel?.audioEngine?.isPlaying?.value ?: false
        OniWidgetUpdater.updateAllWidgets(context, song, isPlaying, 0)
    }
}
