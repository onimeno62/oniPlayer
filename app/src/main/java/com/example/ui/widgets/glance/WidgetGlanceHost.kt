package com.example.ui.widgets.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRegistry
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize
import com.example.ui.widgets.defaultpack.DefaultWidgetPack
import com.example.ui.widgets.skin.WidgetSkinResolver
import com.example.ui.theme.OniSkinTokens

/**
 * Shared, failure-safe host for the built-in Glance widgets.
 *
 * Widget providers can be instantiated by the launcher in a process where the
 * normal app UI has never initialized the widget registry. Built-in widgets
 * therefore always resolve from DefaultWidgetPack as the authoritative
 * fallback instead of depending on process-local registry state.
 */
object WidgetGlanceHost {
    suspend fun resolvePlugin(id: String): OniWidgetPlugin? =
        OniWidgetRegistry.getPlugin(id)
            ?: DefaultWidgetPack.widgets.firstOrNull { it.id == id }

    suspend fun resolveSkin(context: Context): OniSkinTokens =
        WidgetSkinResolver.resolveActiveSkin(context)

    @Composable
    fun RenderSafely(
        renderer: OniWidgetRenderer,
        context: Context,
        size: WidgetSize,
        state: OniWidgetPlaybackState,
        skin: OniSkinTokens
    ) {
        try {
            renderer.Render(context, size, state, skin)
        } catch (_: Throwable) {
            Fallback(skin)
        }
    }

    @Composable
    private fun Fallback(skin: OniSkinTokens) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "oniPlayer",
                style = androidx.glance.text.TextStyle(
                    color = ColorProvider(skin.colors.textSecondary)
                )
            )
        }
    }
}
