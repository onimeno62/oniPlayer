package com.example.ui.library.hero

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HeroColors(
    val dominant: Color,
    val secondary: Color
)

object HeroColorExtractor {
    private val colorCache = mutableMapOf<String, HeroColors>()

    fun getCachedColors(uri: String?): HeroColors? {
        if (uri == null) return null
        return colorCache[uri]
    }

    fun cacheColors(uri: String, colors: HeroColors) {
        colorCache[uri] = colors
    }

    suspend fun extractColors(context: Context, uri: String): HeroColors? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false) // needed to extract palette from software bitmap
                .build()
            
            val result = Coil.imageLoader(context).execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getVibrantColor(
                        palette.getDominantColor(
                            palette.getMutedColor(0)
                        )
                    )
                    val secondary = palette.getMutedColor(
                        palette.getLightMutedColor(
                            palette.getDarkMutedColor(0)
                        )
                    )
                    
                    if (dominant != 0) {
                        return@withContext HeroColors(
                            dominant = Color(dominant),
                            secondary = if (secondary != 0) Color(secondary) else Color(dominant).copy(alpha = 0.5f)
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun rememberHeroColors(artworkUri: String?, fallbackAccent: Color): HeroColors {
    val context = LocalContext.current
    val fallbackColors = remember(fallbackAccent) {
        HeroColors(dominant = fallbackAccent, secondary = fallbackAccent.copy(alpha = 0.5f))
    }
    
    var extractedColors by remember(artworkUri) {
        mutableStateOf(HeroColorExtractor.getCachedColors(artworkUri) ?: fallbackColors)
    }

    LaunchedEffect(artworkUri) {
        if (artworkUri != null && HeroColorExtractor.getCachedColors(artworkUri) == null) {
            val colors = HeroColorExtractor.extractColors(context, artworkUri)
            if (colors != null) {
                HeroColorExtractor.cacheColors(artworkUri, colors)
                extractedColors = colors
            }
        }
    }

    return extractedColors
}
