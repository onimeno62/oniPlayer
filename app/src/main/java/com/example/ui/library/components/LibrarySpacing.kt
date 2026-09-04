package com.example.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSpacingTokens

object LibrarySpacing {
    val xs = OniSpacingTokens.defaultSpacing().xxs  // 4.dp
    val sm = OniSpacingTokens.defaultSpacing().xs   // 8.dp
    val md = OniSpacingTokens.defaultSpacing().sm   // 12.dp
    val lg = OniSpacingTokens.defaultSpacing().md   // 16.dp
    val xl = OniSpacingTokens.defaultSpacing().lg   // 20.dp
    val xxl = OniSpacingTokens.defaultSpacing().xl  // 24.dp
    val xxxl = OniSpacingTokens.defaultSpacing().xxl // 32.dp
}

@Preview(showBackground = true)
@Composable
private fun LibrarySpacingPreview() {
    Column(modifier = Modifier.padding(LibrarySpacing.md)) {
        Text("Library Spacing Tokens Preview", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(LibrarySpacing.sm))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(LibrarySpacing.lg)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
