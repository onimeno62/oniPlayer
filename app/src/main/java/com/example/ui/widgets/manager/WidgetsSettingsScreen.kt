package com.example.ui.widgets.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.screens.SettingSection
import com.example.ui.screens.SettingsSubscreenHeader
import com.example.ui.theme.OniSkin
import com.example.ui.widgets.core.OniWidgetRegistry
import com.example.ui.widgets.defaultpack.DefaultWidgetPack

/**
 * Screen exposing installed and available widget plugins, packs, and supported sizes.
 * Seamlessly integrates with the existing oniPlayer Settings architecture.
 */
@Composable
fun WidgetsSettingsScreen(
    onBack: () -> Unit
) {
    val packs = remember {
        if (OniWidgetRegistry.getAllPacks().isEmpty()) {
            OniWidgetRegistry.registerPack(DefaultWidgetPack)
        }
        OniWidgetRegistry.getAllPacks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Home Screen Widgets",
            subtitle = "Active widget plugins and supported layouts",
            onBack = onBack,
            backButtonTestTag = "widgets_settings_back_button"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            item {
                SettingSection(
                    title = "Widget System Architecture",
                    description = "oniPlayer widgets render through the active skin engine and adapt automatically to your theme."
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "To place a widget, return to your Android home screen launcher, long-press empty space, select Widgets, and choose oniPlayer.",
                            style = OniSkin.typography.bodySmall,
                            color = OniSkin.colors.textSecondary
                        )
                    }
                }
            }

            items(packs) { pack ->
                SettingSection(
                    title = pack.name,
                    description = "${pack.description} • Version ${pack.version}"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pack.widgets.forEach { widget ->
                            OniSurface(
                                variant = OniSurfaceVariant.Soft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(OniSkin.colors.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = OniSkin.colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = widget.name,
                                            style = OniSkin.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OniSkin.colors.textPrimary
                                        )
                                        Text(
                                            text = widget.description,
                                            style = OniSkin.typography.caption,
                                            color = OniSkin.colors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            widget.supportedSizes.forEach { sz ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(OniSkin.colors.surfaceVariant)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = sz.displayName,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = OniSkin.colors.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
