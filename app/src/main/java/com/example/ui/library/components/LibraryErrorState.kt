package com.example.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.components.state.OniErrorState

@Composable
fun LibraryErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ErrorOutline,
    retryLabel: String? = "Retry",
    onRetryClick: (() -> Unit)? = null
) {
    OniErrorState(
        title = title,
        message = message,
        modifier = modifier,
        icon = icon,
        retryLabel = retryLabel,
        onRetryClick = onRetryClick
    )
}

@Preview(showBackground = true)
@Composable
private fun LibraryErrorStatePreview() {
    LibraryErrorState(
        title = "Failed to Load Library",
        message = "An unexpected error occurred while loading your music library.",
        retryLabel = "Retry",
        onRetryClick = {}
    )
}
