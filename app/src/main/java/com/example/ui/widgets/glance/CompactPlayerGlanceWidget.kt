package com.example.ui.widgets.glance

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.example.playback.OniAudioEngine
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter

class CompactPlayerGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val skin = WidgetGlanceHost.resolveSkin(context)
        val widgetState = runCatching {
            WidgetPlaybackStateAdapter.fromPlaybackState(OniAudioEngine.getInstance(context).state.value)
        }.getOrDefault(OniWidgetPlaybackState())
        // CompactPlayer is the legacy receiver name; the rebuilt plugin is Mini Player.
        val plugin = WidgetGlanceHost.resolvePlugin("oni.miniplayer")
            ?: return
        val renderer = plugin.createRenderer()

        provideContent {
            val sizeInfo = androidx.glance.LocalSize.current
            val logicalSize = WidgetSize.fromDimensions(
                sizeInfo.width.value.toInt(),
                sizeInfo.height.value.toInt()
            )
            Box(modifier = GlanceModifier.fillMaxSize()) {
                WidgetGlanceHost.RenderSafely(renderer, context, logicalSize, widgetState, skin)
            }
        }
    }
}

class CompactPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactPlayerGlanceWidget()
}
