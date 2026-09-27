package com.example.ui.widgets.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter

/**
 * Cached artwork derivations for widget rendering.
 *
 * Every asset is derived from the single decoded bitmap held by
 * [WidgetPlaybackStateAdapter]'s artwork cache. Nothing here opens streams,
 * touches the network, or runs continuously: each derivation is computed once
 * per (artwork, skin colour, variant) and reused across recompositions and
 * widget instances. Output bitmaps are small so RemoteViews stays well inside
 * its bitmap memory budget.
 */
object WidgetArtworkAtmosphere {

    /** Hero crops, each with a scrim baked toward the text edge. */
    enum class HeroAspect(
        val width: Int,
        val height: Int,
        val scrimStart: Float,
        val scrimTopAlpha: Float
    ) {
        /** Near-square 4x4 surfaces. Scrim covers the lower 62%. */
        SQUARE(420, 420, 0.38f, 0f),
        /** Wide 4x2 surfaces. Scrim spans the full height, strongest at the bottom. */
        WIDE(520, 220, 0f, 0.22f)
    }

    private const val FOREGROUND_MAX_PX = 480
    private const val AMBIENT_PX = 96
    private const val CACHE_BYTES = 8 * 1024 * 1024

    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /** Foreground artwork, capped so large covers are not shipped to the launcher at full size. */
    fun artwork(context: Context, uri: String?): Bitmap? {
        if (uri.isNullOrBlank()) return null
        val source = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, uri) ?: return null
        val longest = maxOf(source.width, source.height)
        if (longest <= FOREGROUND_MAX_PX) return source
        return cached("fg|$uri") {
            val scale = FOREGROUND_MAX_PX.toFloat() / longest
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }

    /**
     * Softened artwork washed toward [wash]. [strength] is how much artwork
     * colour survives (0 = pure wash, 1 = raw blur).
     */
    fun ambient(context: Context, uri: String?, wash: Color, strength: Float): Bitmap? {
        if (uri.isNullOrBlank() || strength <= 0f) return null
        val source = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, uri) ?: return null
        val solid = wash.copy(alpha = 1f)
        val key = "amb|$uri|${solid.toArgb()}|${(strength * 100).toInt()}"
        return cached(key) {
            val soft = soften(source, AMBIENT_PX, AMBIENT_PX)
            Canvas(soft).drawColor(solid.copy(alpha = (1f - strength).coerceIn(0f, 1f)).toArgb())
            soft
        }
    }

    /** Full-bleed artwork with a scrim toward [scrim] baked in for readable overlaid text. */
    fun hero(
        context: Context,
        uri: String?,
        scrim: Color,
        strength: Float,
        aspect: HeroAspect
    ): Bitmap? {
        if (uri.isNullOrBlank()) return null
        val source = WidgetPlaybackStateAdapter.loadArtworkBitmap(context, uri) ?: return null
        val solid = scrim.copy(alpha = 1f)
        val s = strength.coerceIn(0f, 1f)
        val key = "hero|${aspect.name}|$uri|${solid.toArgb()}|${(s * 100).toInt()}"
        return cached(key) {
            val w = aspect.width
            val h = aspect.height
            val out = draw(source, centerCrop(source.width, source.height, w, h), w, h)
            val canvas = Canvas(out)
            val start = h * aspect.scrimStart
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, start, 0f, h.toFloat(),
                    intArrayOf(
                        solid.copy(alpha = aspect.scrimTopAlpha * s).toArgb(),
                        solid.copy(alpha = 0.62f * s).toArgb(),
                        solid.copy(alpha = s).toArgb()
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, start, w.toFloat(), h.toFloat(), paint)
            // A faint overall wash seats the artwork in the skin and keeps top chips legible.
            canvas.drawColor(solid.copy(alpha = 0.08f * s).toArgb())
            out
        }
    }

    /** Cheap, allocation-bounded blur: progressive downscale to a tiny seed, then bilinear upscale. */
    private fun soften(source: Bitmap, outW: Int, outH: Int): Bitmap {
        val crop = centerCrop(source.width, source.height, outW, outH)
        val step = draw(source, crop, 48, (48 * outH / outW).coerceAtLeast(1))
        val seed = draw(step, null, 8, (8 * outH / outW).coerceAtLeast(1))
        val mid = draw(seed, null, (outW / 3).coerceAtLeast(1), (outH / 3).coerceAtLeast(1))
        val out = draw(mid, null, outW, outH)
        step.recycle()
        seed.recycle()
        mid.recycle()
        return out
    }

    private fun draw(src: Bitmap, srcRect: Rect?, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, srcRect, Rect(0, 0, out.width, out.height), filterPaint)
        return out
    }

    private fun centerCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcRatio = srcW.toFloat() / srcH.coerceAtLeast(1)
        val dstRatio = dstW.toFloat() / dstH.coerceAtLeast(1)
        return if (srcRatio > dstRatio) {
            val w = (srcH * dstRatio).toInt().coerceIn(1, srcW)
            val left = (srcW - w) / 2
            Rect(left, 0, left + w, srcH)
        } else {
            val h = (srcW / dstRatio).toInt().coerceIn(1, srcH)
            val top = (srcH - h) / 2
            Rect(0, top, srcW, top + h)
        }
    }

    private inline fun cached(key: String, build: () -> Bitmap?): Bitmap? {
        cache.get(key)?.let { return it }
        val built = try {
            build()
        } catch (_: Throwable) {
            null
        } ?: return null
        cache.put(key, built)
        return built
    }
}
