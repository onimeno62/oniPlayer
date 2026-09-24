package com.example.ui.widgets.glance

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.ui.widgets.core.WidgetSize

class CompactPlayerGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val skin = WidgetGlanceHost.resolveSkin(context)
        val plugin = WidgetGlanceHost.resolvePlugin("oni.miniplayer") ?: return
        val renderer = plugin.createRenderer()

        provideContent {
            val widgetState = currentState<Preferences>().toPlaybackState()
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        com.example.ui.widgets.updater.WidgetUpdateManager.requestWithFollowUp(context)
    }
}
