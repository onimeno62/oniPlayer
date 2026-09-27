package com.example.ui.widgets.defaultpack.dynamicalbum

import com.example.ui.widgets.core.OniWidgetPlugin
import com.example.ui.widgets.core.OniWidgetRenderer
import com.example.ui.widgets.core.WidgetSize

/** Artwork-first widget. It intentionally minimizes transport UI. See [DynamicAlbumWidgetRenderer]. */
class DynamicAlbumWidgetPlugin : OniWidgetPlugin {
    override val id = "oni.dynamicalbum"
    override val packId = "default.pack"
    override val name = "Dynamic Album"
    override val description = "Immersive artwork-first surface with minimal controls."
    override val supportedSizes = setOf(WidgetSize.SIZE_4X1, WidgetSize.SIZE_4X2, WidgetSize.SIZE_4X4)
    override fun createRenderer(): OniWidgetRenderer = DynamicAlbumWidgetRenderer()
}
