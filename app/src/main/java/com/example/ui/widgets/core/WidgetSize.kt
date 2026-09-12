package com.example.ui.widgets.core

/**
 * Logical Android home screen launcher widget sizes supported by oniPlayer.
 * These map to standard grid cells in Android launchers.
 */
enum class WidgetSize(val columns: Int, val rows: Int, val displayName: String) {
    SIZE_4X1(4, 1, "4 × 1"),
    SIZE_4X2(4, 2, "4 × 2"),
    SIZE_4X4(4, 4, "4 × 4");

    companion object {
        fun fromDimensions(widthDp: Int, heightDp: Int): WidgetSize {
            return when {
                heightDp >= 260 -> SIZE_4X4
                heightDp >= 110 -> SIZE_4X2
                else -> SIZE_4X1
            }
        }
    }
}
