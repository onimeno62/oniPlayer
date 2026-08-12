package com.example.playback

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.entity.SongEntity
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.lyrics.LrcLine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Local preference helper for persisting floating lyrics settings
object FloatingLyricsPrefs {
    private const val PREFS_NAME = "floating_lyrics_prefs"
    private const val KEY_OPACITY = "opacity"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_THEME = "theme"
    private const val KEY_IS_COMPACT = "is_compact"

    fun saveSettings(context: Context, opacity: Float, fontSize: Float, theme: String, isCompact: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_OPACITY, opacity)
            .putFloat(KEY_FONT_SIZE, fontSize)
            .putString(KEY_THEME, theme)
            .putBoolean(KEY_IS_COMPACT, isCompact)
            .apply()
    }

    fun getOpacity(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getFloat(KEY_OPACITY, 0.85f)
    }

    fun getFontSize(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getFloat(KEY_FONT_SIZE, 13f)
    }

    fun getTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, "Amethyst Nebula") ?: "Amethyst Nebula"
    }

    fun isCompact(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_COMPACT, false)
    }

    fun setCompact(context: Context, compact: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_COMPACT, compact).apply()
    }
}

// Custom high-fidelity frosted glass themes
data class FloatingTheme(
    val name: String,
    val cardBgColor: Color,
    val borderColors: List<Color>,
    val activeLineColor: Color,
    val inactiveLineColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val isDark: Boolean
)

val FLOATING_THEMES = listOf(
    FloatingTheme(
        name = "Amethyst Nebula",
        cardBgColor = Color(0xFF0D0714),
        borderColors = listOf(Color(0xFFE040FB), Color(0xFF00E5FF)),
        activeLineColor = Color(0xFF00E5FF),
        inactiveLineColor = Color(0xFFB0A0C0),
        accentColor = Color(0xFFE040FB),
        textColor = Color.White,
        isDark = true
    ),
    FloatingTheme(
        name = "Cyberpunk Neon",
        cardBgColor = Color(0xFF0F0E17),
        borderColors = listOf(Color(0xFFFF007F), Color(0xFFFFFF00)),
        activeLineColor = Color(0xFFFF007F),
        inactiveLineColor = Color(0xFFA7A9BE),
        accentColor = Color(0xFFFF007F),
        textColor = Color.White,
        isDark = true
    ),
    FloatingTheme(
        name = "Emerald Forest",
        cardBgColor = Color(0xFF051009),
        borderColors = listOf(Color(0xFF2E7D32), Color(0xFF81C784)),
        activeLineColor = Color(0xFF81C784),
        inactiveLineColor = Color(0xFFA4C2B2),
        accentColor = Color(0xFF2E7D32),
        textColor = Color.White,
        isDark = true
    ),
    FloatingTheme(
        name = "Amber Sun",
        cardBgColor = Color(0xFF140D07),
        borderColors = listOf(Color(0xFFFF9100), Color(0xFFFFD600)),
        activeLineColor = Color(0xFFFFD600),
        inactiveLineColor = Color(0xFFD7CCC8),
        accentColor = Color(0xFFFF9100),
        textColor = Color.White,
        isDark = true
    ),
    FloatingTheme(
        name = "Ice Frost",
        cardBgColor = Color(0xFFF0F4F8),
        borderColors = listOf(Color(0xFF6200EE), Color(0xFF03DAC6)),
        activeLineColor = Color(0xFF6200EE),
        inactiveLineColor = Color(0xFF757575),
        accentColor = Color(0xFF6200EE),
        textColor = Color(0xFF1A1A1A),
        isDark = false
    )
)

class FloatingLyricsService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Direct flows to push state updates into the Compose view
    private val currentSong = MutableStateFlow<SongEntity?>(null)
    private val position = MutableStateFlow(0L)
    private val isPlaying = MutableStateFlow(false)

    // Tracks cached expanded window height to restore dynamically when unfolding
    private var expandedHeight: Int = 0

    companion object {
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlay()
        observePlayback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupOverlay() {
        val wm = windowManager ?: return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        
        // Dynamic initial height configuration based on persisted state
        val isCompactInit = FloatingLyricsPrefs.isCompact(this)
        val defaultWidthPx = (300 * density).toInt()
        val defaultHeightPx = if (isCompactInit) (88 * density).toInt() else (180 * density).toInt()

        val minWidthPx = (200 * density).toInt()
        val maxWidthPx = (480 * density).toInt()
        val minHeightPx = (120 * density).toInt()
        val maxHeightPx = (380 * density).toInt()

        // Cache expected default height for expansion transition
        expandedHeight = (180 * density).toInt()

        // Window position and layout params setup
        val lp = WindowManager.LayoutParams(
            defaultWidthPx,
            defaultHeightPx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }
        params = lp

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingLyricsService)
            setViewTreeSavedStateRegistryOwner(this@FloatingLyricsService)
            setViewTreeViewModelStoreOwner(this@FloatingLyricsService)
            
            setContent {
                MaterialTheme {
                    val engine = OniAudioEngine.getInstance(this@FloatingLyricsService)
                    FloatingLyricsOverlayContent(
                        songFlow = currentSong,
                        positionFlow = position,
                        isPlayingFlow = isPlaying,
                        onClose = {
                            engine.setFloatingLyricsEnabled(false)
                            stopSelf()
                        },
                        onPlayPauseToggle = {
                            if (engine.isPlaying.value) {
                                engine.pause()
                            } else {
                                engine.resume()
                            }
                        },
                        onSkipNext = {
                            engine.skipNext()
                        },
                        onSkipPrevious = {
                            engine.skipPrevious()
                        },
                        onSeekTo = { timestampMs ->
                            engine.seekTo(timestampMs)
                        },
                        onDrag = { dx, dy ->
                            lp.x += dx.toInt()
                            lp.y += dy.toInt()
                            try {
                                wm.updateViewLayout(this, lp)
                            } catch (e: Exception) {
                                Log.e("FloatingLyricsService", "Failed to drag floating window: ${e.message}")
                            }
                        },
                        onResize = { dx, dy ->
                            val isCompactNow = FloatingLyricsPrefs.isCompact(this@FloatingLyricsService)
                            lp.width = (lp.width + dx.toInt()).coerceIn(minWidthPx, maxWidthPx)
                            if (!isCompactNow) {
                                lp.height = (lp.height + dy.toInt()).coerceIn(minHeightPx, maxHeightPx)
                                expandedHeight = lp.height
                            }
                            try {
                                wm.updateViewLayout(this, lp)
                            } catch (e: Exception) {
                                Log.e("FloatingLyricsService", "Failed to resize floating window: ${e.message}")
                            }
                        },
                        onToggleCompactMode = { compact ->
                            FloatingLyricsPrefs.setCompact(this@FloatingLyricsService, compact)
                            if (compact) {
                                expandedHeight = lp.height
                                lp.height = (88 * density).toInt()
                            } else {
                                lp.height = expandedHeight.coerceIn(minHeightPx, maxHeightPx)
                            }
                            try {
                                wm.updateViewLayout(this, lp)
                            } catch (e: Exception) {
                                Log.e("FloatingLyricsService", "Failed to resize for compact mode: ${e.message}")
                            }
                        }
                    )
                }
            }
        }
        composeView = view
        try {
            wm.addView(view, lp)
        } catch (e: Exception) {
            Log.e("FloatingLyricsService", "Failed to add overlay view: ${e.message}", e)
        }
    }

    private fun observePlayback() {
        serviceScope.launch {
            val engine = OniAudioEngine.getInstance(this@FloatingLyricsService)
            combine(
                engine.currentSong,
                engine.position,
                engine.isPlaying,
                engine.floatingLyricsEnabled
            ) { s, pos, playing, enabled ->
                Quadruple(s, pos, playing, enabled)
            }.collectLatest { (s, pos, playing, enabled) ->
                if (!enabled) {
                    stopSelf()
                    return@collectLatest
                }
                currentSong.value = s
                position.value = pos
                isPlaying.value = playing
            }
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        composeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("FloatingLyricsService", "Failed to remove overlay view: ${e.message}")
            }
        }
        isServiceRunning = false
        super.onDestroy()
    }
}

@Composable
fun FloatingLyricsOverlayContent(
    songFlow: StateFlow<SongEntity?>,
    positionFlow: StateFlow<Long>,
    isPlayingFlow: StateFlow<Boolean>,
    onClose: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val currentSong by songFlow.collectAsState()
    val position by positionFlow.collectAsState()
    val isPlaying by isPlayingFlow.collectAsState()

    // Preferences & Customization States
    var opacity by remember { mutableStateOf(FloatingLyricsPrefs.getOpacity(context)) }
    var fontSize by remember { mutableStateOf(FloatingLyricsPrefs.getFontSize(context)) }
    var selectedThemeName by remember { mutableStateOf(FloatingLyricsPrefs.getTheme(context)) }
    var isCompact by remember { mutableStateOf(FloatingLyricsPrefs.isCompact(context)) }
    var showSettings by remember { mutableStateOf(false) }

    val activeTheme = remember(selectedThemeName) {
        FLOATING_THEMES.find { it.name == selectedThemeName } ?: FLOATING_THEMES[0]
    }

    // Parse LRC lines
    val lrcLines = remember(currentSong?.lyrics) {
        LyricsHelper.parseLrc(currentSong?.lyrics)
    }

    // Active lyric index matching the position
    val activeIndex = remember(lrcLines, position) {
        LyricsHelper.getActiveLineIndex(lrcLines, position)
    }

    val listState = rememberLazyListState()

    // Auto-scroll logic: always scroll the active highlighted lyric line to the top of the visible list
    LaunchedEffect(activeIndex, showSettings, isCompact, lrcLines) {
        if (!isCompact && !showSettings && activeIndex >= 0 && activeIndex < lrcLines.size) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Frosted Glassmorphism Card Overlay Container
        Card(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = activeTheme.cardBgColor.copy(alpha = opacity)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // ---------- Header Bar (Title, Compact Toggle, Settings, Close) ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Draggable Area for repositioning window
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.x, dragAmount.y)
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag Window",
                            tint = activeTheme.textColor.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Floating App Icon",
                            tint = activeTheme.accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentSong != null) {
                                currentSong!!.customTitle ?: currentSong!!.title
                            } else "Oni Player",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = activeTheme.textColor.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }

                    // Mode Controls & Utilities Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        // Toggle Compact vs Expanded Mode
                        IconButton(
                            onClick = {
                                val nextCompact = !isCompact
                                isCompact = nextCompact
                                onToggleCompactMode(nextCompact)
                                showSettings = false // Close settings panel if transitioning
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompact) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                contentDescription = if (isCompact) "Expand Window" else "Compact Window",
                                tint = activeTheme.textColor.copy(alpha = 0.65f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Toggle Settings Customization Panel
                        IconButton(
                            onClick = { showSettings = !showSettings },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showSettings) Icons.Default.ArrowBack else Icons.Default.Settings,
                                contentDescription = "Customize Settings",
                                tint = if (showSettings) activeTheme.accentColor else activeTheme.textColor.copy(alpha = 0.65f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Close overlay service
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Window",
                                tint = activeTheme.textColor.copy(alpha = 0.65f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // ---------- Middle Frame (Settings vs Scrollable Lyrics) ----------
                if (showSettings) {
                    // Settings control dashboard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Section 1: Font Size (Segmented touch selectors to avoid drag conflicts)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "Size",
                                    tint = activeTheme.textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Font Size",
                                    fontSize = 11.sp,
                                    color = activeTheme.textColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(65.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                               ) {
                                    listOf(10f to "S", 13f to "M", 16f to "L", 19f to "XL").forEach { (sz, label) ->
                                        val isSelected = fontSize == sz
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) activeTheme.accentColor else Color.White.copy(
                                                        alpha = 0.08f
                                                    )
                                                )
                                                .clickable {
                                                    fontSize = sz
                                                    FloatingLyricsPrefs.saveSettings(context, opacity, sz, selectedThemeName, isCompact)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else activeTheme.textColor.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 2: Opacity (Touch-friendly presets)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = "Opacity",
                                    tint = activeTheme.textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Opacity",
                                    fontSize = 11.sp,
                                    color = activeTheme.textColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(65.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(0.45f to "45%", 0.65f to "65%", 0.85f to "85%", 1.00f to "100%").forEach { (op, label) ->
                                        val isSelected = opacity == op
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) activeTheme.accentColor else Color.White.copy(
                                                        alpha = 0.08f
                                                    )
                                                )
                                                .clickable {
                                                    opacity = op
                                                    FloatingLyricsPrefs.saveSettings(context, op, fontSize, selectedThemeName, isCompact)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else activeTheme.textColor.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 3: Frosted Theme Palette Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = activeTheme.textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Theme Preset",
                                    fontSize = 11.sp,
                                    color = activeTheme.textColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(65.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                LazyColumn(
                                    modifier = Modifier.weight(1f).height(45.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    itemsIndexed(FLOATING_THEMES) { _, th ->
                                        val isSelected = th.name == selectedThemeName
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) th.accentColor.copy(alpha = 0.25f) else Color.White.copy(
                                                        alpha = 0.04f
                                                    )
                                                )
                                                .border(
                                                    width = if (isSelected) 1.dp else 0.dp,
                                                    color = if (isSelected) th.accentColor else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    selectedThemeName = th.name
                                                    FloatingLyricsPrefs.saveSettings(context, opacity, fontSize, th.name, isCompact)
                                                }
                                                .padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = th.name,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = activeTheme.textColor.copy(alpha = 0.9f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected Theme",
                                                    tint = th.accentColor,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (isCompact) {
                    // COMPACT MODE: Minimized screen real-estate (shows active line with fallback)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentSong == null) {
                            Text(
                                text = "Play a track to view lyrics",
                                fontSize = fontSize.sp * 0.85f,
                                color = activeTheme.textColor.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        } else if (currentSong?.lyrics.isNullOrBlank()) {
                            Text(
                                text = "No lyrics found for this song",
                                fontSize = fontSize.sp * 0.85f,
                                color = activeTheme.textColor.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        } else if (lrcLines.isEmpty()) {
                            // Snippet plain lyrics fallback
                            val lines = remember(currentSong?.lyrics) {
                                currentSong!!.lyrics!!.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
                            }
                            val displayLine = lines.getOrNull(0) ?: "Enjoy your music"
                            Text(
                                text = displayLine,
                                fontSize = fontSize.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.textColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        } else {
                            // Synchronized line highlights
                            val activeLine = if (activeIndex >= 0 && activeIndex < lrcLines.size) lrcLines[activeIndex] else null
                            val nextLine = if (activeIndex + 1 >= 0 && activeIndex + 1 < lrcLines.size) lrcLines[activeIndex + 1] else null

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (activeLine != null) {
                                    Text(
                                        text = activeLine.text,
                                        fontSize = fontSize.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeTheme.activeLineColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "♩ ♪ ♫ ♬ (Intro / Instrumental)",
                                        fontSize = fontSize.sp * 0.85f,
                                        color = activeTheme.inactiveLineColor.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (nextLine != null) {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = nextLine.text,
                                        fontSize = fontSize.sp * 0.75f,
                                        fontWeight = FontWeight.Medium,
                                        color = activeTheme.inactiveLineColor.copy(alpha = 0.45f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // EXPANDED MODE: Beautiful scrollable lyrics with interactive seeking
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentSong == null) {
                            Text(
                                text = "Play a track to view lyrics",
                                fontSize = fontSize.sp,
                                color = activeTheme.textColor.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        } else if (currentSong?.lyrics.isNullOrBlank()) {
                            Text(
                                text = "No lyrics found for this song",
                                fontSize = fontSize.sp,
                                color = activeTheme.textColor.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        } else if (lrcLines.isEmpty()) {
                            // Plain text scrollable fallback
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    Text(
                                        text = currentSong!!.lyrics!!,
                                        fontSize = fontSize.sp * 0.9f,
                                        color = activeTheme.textColor.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = (fontSize.sp.value + 4).sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // Karaoke lyrics with smooth automatic centered scrolling
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(lrcLines) { index, line ->
                                    val isActiveLine = index == activeIndex
                                    Text(
                                        text = line.text,
                                        fontSize = if (isActiveLine) fontSize.sp else (fontSize.sp.value - 2).sp,
                                        fontWeight = if (isActiveLine) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isActiveLine) activeTheme.activeLineColor else activeTheme.inactiveLineColor.copy(
                                            alpha = 0.55f
                                        ),
                                        textAlign = TextAlign.Center,
                                        lineHeight = (fontSize.sp.value + 4).sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSeekTo(line.timestampMs) }
                                            .padding(horizontal = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------- Playback Controls (Always visible in expanded mode except in settings) ----------
                if (!isCompact && !showSettings) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = activeTheme.textColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                        IconButton(
                            onClick = onPlayPauseToggle,
                            modifier = Modifier
                                .size(28.dp)
                                .background(activeTheme.accentColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play / Pause",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = activeTheme.textColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---------- Bottom-Right Resize Handle (Hidden in compact/settings modes) ----------
        if (!isCompact && !showSettings) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Drag to Resize",
                    tint = activeTheme.textColor.copy(alpha = 0.45f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
