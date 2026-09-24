package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import com.example.data.preferences.PlayerSettingsStore
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REPO_URL = "https://github.com/onimeno62/oniPlayer"

// region Storage

@Composable
fun StorageSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()

    val totalDurationMs = remember(songs) { songs.sumOf { it.duration } }
    val formattedDuration = remember(totalDurationMs) { formatDuration(totalDurationMs) }

    var refreshKey by remember { mutableIntStateOf(0) }
    var cacheBytes by remember { mutableStateOf(-1L) }
    var clearing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshKey) {
        cacheBytes = withContext(Dispatchers.IO) {
            settingsDirSize(context.cacheDir) + settingsDirSize(context.externalCacheDir)
        }
    }

    SettingsPage(
        title = "Storage & Cache",
        subtitle = "Library stats and temporary files",
        onBack = onBack,
        backButtonTestTag = "storage_back_button"
    ) {
        item {
            SettingSection(title = "Library", description = "What oniPlayer has indexed from your device.") {
                SettingsInfoRow("Songs", "${songs.size}")
                SettingDivider()
                SettingsInfoRow("Total listening time", formattedDuration)
                SettingDivider()
                SettingsInfoRow("Playlists", "${playlists.size}")
                SettingDivider()
                SettingsInfoRow("Equalizer presets", "${presets.size}")
            }
        }
        item {
            SettingSection(
                title = "Cache",
                description = "Artwork thumbnails and temporary files. Safe to clear; they are rebuilt when needed."
            ) {
                SettingsInfoRow("Cache size", if (cacheBytes < 0) "Calculating…" else formatSettingsBytes(cacheBytes))
                SettingDivider()
                SettingsActionRow(
                    title = if (clearing) "Clearing…" else "Clear cache",
                    description = "Frees space without touching your music, playlists or settings.",
                    icon = Icons.Default.DeleteSweep,
                    showChevron = false,
                    enabled = !clearing,
                    onClick = {
                        clearing = true
                        scope.launch {
                            runCatching { context.imageLoader.memoryCache?.clear() }
                            withContext(Dispatchers.IO) {
                                runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                                runCatching { context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() } }
                            }
                            clearing = false
                            refreshKey++
                            context.settingsToast("Cache cleared")
                        }
                    },
                    testTag = "setting_clear_cache"
                )
            }
        }
        item {
            SettingSection(title = "Rebuild", description = "Re-read every audio file and refresh the library index.") {
                SettingsActionRow(
                    title = "Rescan library",
                    description = "Use after adding, moving or retagging files.",
                    icon = Icons.Default.Refresh,
                    showChevron = false,
                    onClick = {
                        viewModel.rescanLibrary()
                        context.settingsToast("Scanning your music…")
                    },
                    testTag = "setting_storage_rescan"
                )
            }
        }
    }
}

private fun settingsDirSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    return runCatching { dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)
}

private fun formatSettingsBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

// endregion

// region Backup & reset

@Composable
fun BackupSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val root = JSONObject()
                root.put("format", "oniPlayer-backup")
                root.put("version", 1)
                root.put("createdAt", System.currentTimeMillis())
                root.put("playerSettings", PlayerSettingsStore.exportJson(context))
                val playlistArray = JSONArray()
                viewModel.allPlaylists.value.forEach { playlist ->
                    playlistArray.put(
                        JSONObject()
                            .put("name", playlist.name)
                            .put("songIds", JSONArray(parseBackupSongIds(playlist.songIdsJson)))
                    )
                }
                root.put("playlists", playlistArray)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) }
                        ?: error("Cannot open file")
                }
            }
            context.settingsToast(if (result.isSuccess) "Backup saved" else "Backup failed")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Cannot open file")
                }
                val root = JSONObject(text)
                require(root.optString("format") == "oniPlayer-backup") { "Not an oniPlayer backup" }
                root.optJSONObject("playerSettings")?.let { PlayerSettingsStore.importJson(context, it) }
                val existing = viewModel.allPlaylists.value.map { it.name }.toSet()
                var restored = 0
                val array = root.optJSONArray("playlists") ?: JSONArray()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    if (name in existing) continue
                    val idsArray = item.optJSONArray("songIds") ?: JSONArray()
                    val ids = List(idsArray.length()) { idsArray.get(it).toString() }
                    viewModel.createPlaylist(name, ids)
                    restored++
                }
                restored
            }
            context.settingsToast(
                result.fold(
                    onSuccess = { "Restored settings and $it playlist(s)" },
                    onFailure = { "Restore failed: ${it.message ?: "invalid file"}" }
                )
            )
        }
    }

    SettingsPage(
        title = "Backup & Reset",
        subtitle = "Move your setup to a new phone or start fresh",
        onBack = onBack,
        backButtonTestTag = "backup_back_button"
    ) {
        item {
            SettingSection(
                title = "Backup",
                description = "Saves player settings and playlists to a JSON file you choose."
            ) {
                SettingsActionRow(
                    title = "Create backup",
                    description = "Export to Downloads, Drive or any folder.",
                    icon = Icons.Default.Backup,
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportLauncher.launch("oniPlayer-backup-$stamp.json")
                    },
                    testTag = "setting_backup_export"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Restore from backup",
                    description = "Existing playlists with the same name are kept.",
                    icon = Icons.Default.Restore,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    testTag = "setting_backup_import"
                )
            }
        }
        item {
            SettingSection(title = "Reset", description = "Your music, playlists and lyrics are never deleted.") {
                SettingsActionRow(
                    title = "Reset appearance",
                    description = "Theme, accent, glass and corner settings back to default.",
                    icon = Icons.Default.Palette,
                    showChevron = false,
                    onClick = {
                        viewModel.resetAppearancePreferences()
                        context.settingsToast("Appearance reset")
                    },
                    testTag = "setting_reset_appearance"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Reset all settings",
                    description = "Playback, audio, interaction and appearance.",
                    icon = Icons.Default.RestartAlt,
                    destructive = true,
                    showChevron = false,
                    onClick = { confirmReset = true },
                    testTag = "setting_reset_all"
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = OniSkin.colors.surface,
            title = { Text("Reset all settings?", color = OniSkin.colors.textPrimary) },
            text = { Text("Playback, audio, interaction and appearance settings go back to default. Your library stays untouched.", color = OniSkin.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        runCatching { PlayerSettingsStore.resetAll(context) }
                        viewModel.resetAppearancePreferences()
                        context.settingsToast("All settings reset")
                    }
                }) { Text("Reset", color = OniSkin.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = OniSkin.colors.textPrimary) }
            }
        )
    }
}

private fun parseBackupSongIds(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { array.get(it).toString() }
    }.getOrElse {
        raw.trim('[', ']').split(',').map { it.trim().trim('"') }.filter { it.isNotEmpty() }
    }
}

// endregion

// region Interaction

@Composable
fun InteractionSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsStateWithLifecycle()
    val settings by rememberPlayerSettings()

    SettingsPage(
        title = "Interaction & Accessibility",
        subtitle = "Start screen, motion, display and gestures",
        onBack = onBack,
        backButtonTestTag = "interaction_back_button"
    ) {
        item {
            SettingSection(title = "General", description = "How oniPlayer opens and behaves on screen.") {
                SettingsChoiceRow(
                    title = "Start screen",
                    description = "The tab shown when you open the app.",
                    options = listOf("Library", "Player", "Equalizer"),
                    selectedIndex = settings.startTab,
                    onSelect = { PlayerSettingsStore.update(context, PlayerSettingsStore.START_TAB, it) },
                    testTag = "setting_start_tab"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Keep screen on",
                    description = "Prevent the display from sleeping while oniPlayer is open.",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.KEEP_SCREEN_ON, it) },
                    testTag = "setting_keep_screen_on"
                )
            }
        }
        item {
            SettingSection(title = "Motion", description = "Animation intensity across the app.") {
                SwitchSettingRow(
                    title = "Reduce motion",
                    description = "Turns off sliding transitions and ambient animations.",
                    checked = reduceMotion,
                    onCheckedChange = { viewModel.setReduceMotionEnabled(it) },
                    testTag = "setting_reduce_motion"
                )
            }
        }
        item {
            SettingSection(title = "Gestures", description = "Shortcuts available in the player and library.") {
                SettingsInfoRow("Swipe artwork left", "Next song")
                SettingDivider()
                SettingsInfoRow("Swipe artwork right", "Previous song")
                SettingDivider()
                SettingsInfoRow("Tap artwork", "Play / pause")
                SettingDivider()
                SettingsInfoRow("Long-press a track", "Tags, playlist, delete")
            }
        }
    }
}

// endregion

// region About

@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val packageName = context.packageName
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "Unknown"
    }
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    val batteryUnrestricted = runCatching { powerManager?.isIgnoringBatteryOptimizations(packageName) == true }.getOrDefault(false)
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

    SettingsPage(
        title = "About",
        subtitle = "Version, system integration and licenses",
        onBack = onBack,
        backButtonTestTag = "about_back_button"
    ) {
        item {
            SettingSection(title = "oniPlayer", description = "A local-first music player for Android.") {
                SettingsInfoRow("Version", versionName)
                SettingDivider()
                SettingsInfoRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                SettingDivider()
                SettingsInfoRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
            }
        }
        item {
            SettingSection(
                title = "Keep playing in the background",
                description = "Some phones kill music apps aggressively. Allow unrestricted battery use if playback stops on its own."
            ) {
                SettingsActionRow(
                    title = "Battery optimization",
                    icon = Icons.Default.BatteryChargingFull,
                    trailingText = if (batteryUnrestricted) "Unrestricted" else "Optimized",
                    onClick = { context.launchFirstAvailable(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appDetails) },
                    testTag = "setting_battery_optimization"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Notifications",
                    description = "The playback notification and lock screen controls.",
                    icon = Icons.Default.Notifications,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.launchFirstAvailable(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                                appDetails
                            )
                        } else {
                            context.launchFirstAvailable(appDetails)
                        }
                    },
                    testTag = "setting_notifications"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "App info & permissions",
                    icon = Icons.Default.Apps,
                    onClick = { context.launchFirstAvailable(appDetails) },
                    testTag = "setting_app_info"
                )
            }
        }
        item {
            SettingSection(title = "Project", description = "oniPlayer is open source.") {
                SettingsActionRow(
                    title = "Source code",
                    description = "github.com/onimeno62/oniPlayer",
                    icon = Icons.Default.Code,
                    onClick = { context.launchFirstAvailable(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) },
                    testTag = "setting_source_code"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Report a bug or request a feature",
                    icon = Icons.Default.BugReport,
                    onClick = { context.launchFirstAvailable(Intent(Intent.ACTION_VIEW, Uri.parse("$REPO_URL/issues"))) },
                    testTag = "setting_report_issue"
                )
            }
        }
        item {
            SettingSection(title = "Open-source libraries", description = "Built with these great projects (Apache 2.0).") {
                SettingsInfoRow("Jetpack Compose & Material 3", "Google")
                SettingDivider()
                SettingsInfoRow("Media3 ExoPlayer & Session", "Google")
                SettingDivider()
                SettingsInfoRow("Glance app widgets", "Google")
                SettingDivider()
                SettingsInfoRow("Room, DataStore, Palette", "Google")
                SettingDivider()
                SettingsInfoRow("Coil image loading", "Coil contributors")
            }
        }
    }
}

// endregion

// region Help

private val HelpFaq = listOf(
    "Some songs are missing from my library" to "Open Library & Metadata and tap Rescan. Make sure oniPlayer has the Music & audio permission (About > App info & permissions). Files in hidden folders or with a .nomedia file are skipped by Android.",
    "Playback stops when the screen is off" to "Your phone is probably killing background apps. Go to About > Battery optimization and set oniPlayer to Unrestricted. On Xiaomi, Samsung and Huawei also allow autostart.",
    "Lyrics are not found" to "Turn on Auto-download lyrics and check your connection. Correct title and artist tags give much better matches; you can edit tags with a long-press on a track.",
    "Floating lyrics don't appear" to "The floating window needs the Display over other apps permission. Open Lyrics > Display over other apps and allow it.",
    "Widgets look outdated or don't update" to "Remove the widget and add it again from the home screen. Widget styles can be changed in Settings > Widgets.",
    "Music is too quiet" to "Use Audio & Equalizer > Volume boost for a few dB of extra gain. Keep it moderate to avoid distortion on loud tracks.",
    "Audio keeps playing over calls or other apps" to "Enable Playback > Pause for other audio so oniPlayer respects audio focus.",
    "Moving to a new phone" to "Use Backup & Reset > Create backup, copy the file, then Restore from backup on the new device after scanning your music."
)

@Composable
fun HelpSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    SettingsPage(
        title = "Help & FAQ",
        subtitle = "Quick fixes for common problems",
        onBack = onBack,
        backButtonTestTag = "help_back_button"
    ) {
        item {
            SettingSection(title = "Frequently asked", description = "Tap a question to see the answer.") {
                HelpFaq.forEachIndexed { index, (question, answer) ->
                    SettingsFaqRow(question, answer)
                    if (index < HelpFaq.lastIndex) SettingDivider()
                }
            }
        }
        item {
            SettingSection(title = "Audio tuning tips", description = "Get the most out of the equalizer.") {
                SettingsNote("Start from a preset, then make small moves: cutting a band usually sounds cleaner than boosting others. Use the spatializer for headphones only, and save your tweaks as a custom preset.")
            }
        }
        item {
            SettingSection(title = "Still stuck?", description = "Open an issue with your device model and steps to reproduce.") {
                SettingsActionRow(
                    title = "Contact via GitHub issues",
                    icon = Icons.Default.BugReport,
                    onClick = { context.launchFirstAvailable(Intent(Intent.ACTION_VIEW, Uri.parse("$REPO_URL/issues"))) },
                    testTag = "setting_help_issues"
                )
            }
        }
    }
}

@Composable
private fun SettingsFaqRow(question: String, answer: String) {
    var expanded by rememberSaveable(question) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question,
                style = OniSkin.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OniSkin.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = OniSkin.colors.textTertiary
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                modifier = Modifier.padding(top = OniSkin.spacing.xs)
            )
        }
    }
}

// endregion
