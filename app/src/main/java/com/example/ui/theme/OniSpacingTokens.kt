package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for oniPlayer skins.
 * Defines both the dimensional scale and semantic spacing roles specified in Section 12:
 *
 * Scale:
 * - xxs: 4dp
 * - xs: 8dp
 * - sm: 12dp
 * - md: 16dp
 * - lg: 20dp
 * - xl: 24dp
 * - xxl: 32dp
 * - xxxl: 40dp
 * - huge: 48dp
 *
 * Semantic Roles:
 * - screenHorizontal: 16dp
 * - screenVertical: 16dp
 * - section: 24dp
 * - listItem: 12dp
 * - card: 16dp
 * - content: 16dp
 * - control: 8dp
 */
data class OniSpacingTokens(
    // Scale
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp,

    // Semantic Roles
    val screenHorizontal: Dp = 16.dp,
    val screenVertical: Dp = 16.dp,
    val section: Dp = 24.dp,
    val listItem: Dp = 12.dp,
    val card: Dp = 16.dp,
    val content: Dp = 16.dp,
    val control: Dp = 8.dp
) {
    companion object {
        fun defaultSpacing(): OniSpacingTokens = OniSpacingTokens()
    }
}
