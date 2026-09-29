package com.example.ui.library.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Compact inline search bar that sits beside Play All and Shuffle buttons.
 *
 * Expands from an icon into a text field. Filters the current category's content
 * in real-time using debounce in the ViewModel layer.
 *
 * Based on patterns from WAVORA and modern music player UX.
 */
@Composable
fun OniInlineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            fadeIn() + expandHorizontally(expandFrom = Alignment.End) togetherWith
                    fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
        },
        label = "search_expand",
        modifier = modifier
    ) { isExpanded ->
        if (isExpanded) {
            OniSurface(
                variant = OniSurfaceVariant.Soft,
                shape = OniSkin.shapes.button,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.screenHorizontal)
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = OniSkin.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = OniSkin.typography.bodyMedium.copy(
                            color = OniSkin.colors.textPrimary
                        ),
                        cursorBrush = SolidColor(OniSkin.colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = OniSkin.typography.bodyMedium,
                                        color = OniSkin.colors.textTertiary
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = OniSkin.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                onExpandedChange(false)
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = OniSkin.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
        // When collapsed, the search icon is provided by PlayAllShuffleRow
    }
}
