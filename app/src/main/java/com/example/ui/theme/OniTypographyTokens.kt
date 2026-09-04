package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens for oniPlayer skins.
 * Defines the complete scale specified in Section 10 of the design system specification:
 * - Display Large: 30sp, SemiBold
 * - Display Medium: 26sp, SemiBold
 * - Title Large: 22sp, SemiBold
 * - Title Medium: 18sp, Medium
 * - Title Small: 16sp, Medium
 * - Body Large: 16sp, Regular
 * - Body Medium: 14sp, Regular
 * - Body Small: 13sp, Regular
 * - Label Large: 14sp, Medium
 * - Label Medium: 12sp, Medium
 * - Caption: 12sp, Regular
 */
data class OniTypographyTokens(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val caption: TextStyle
) {
    /**
     * Maps these tokens to Material 3 [Typography]
     * for seamless compatibility with standard Compose text components.
     */
    fun toMaterialTypography(): Typography {
        return Typography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displayMedium.copy(fontSize = 24.sp, lineHeight = 30.sp),
            headlineLarge = displayLarge,
            headlineMedium = displayMedium,
            headlineSmall = titleLarge,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = caption
        )
    }

    companion object {
        fun defaultTypography(): OniTypographyTokens = OniTypographyTokens(
            displayLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.5).sp
            ),
            displayMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.25).sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp
            ),
            titleSmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.25.sp
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            ),
            caption = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp
            )
        )
    }
}
