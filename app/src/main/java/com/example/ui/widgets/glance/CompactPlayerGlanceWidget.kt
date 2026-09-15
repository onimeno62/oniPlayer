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
import com.example.ui.widgets.core.OniWidgetRegistry
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.defaultpack.DefaultWidgetPack
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter
import com.example.ui.widgets.skin.WidgetSkinResolver

class CompactPlayerGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val skin = WidgetSkinResolver.resolveActiveSkin(context)
        val widgetState = runCatching {
            WidgetPlaybackStateAdapter.fromPlaybackState(OniAudioEngine.getInstance(context).state.value)
        }.getOrDefault(OniWidgetPlaybackState())
        val plugin = OniWidgetRegistry.getPlugin("oni.compactplayer")
            ?: DefaultWidgetPack.widgets.first { it.id == "oni.compactplayer" }
        val renderer = plugin.createRenderer()

        provideContent {
            val sizeInfo = androidx.glance.LocalSize.current
            val logicalSize = WidgetSize.fromDimensions(sizeInfo.width.value.toInt(), sizeInfo.height.value.toInt())
            Box(modifier = GlanceModifier.fillMaxSize()) {
                renderer.Render(context, logicalSize, widgetState, skin)
            }
        }
    }
}

class CompactPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactPlayerGlanceWidget()
}
