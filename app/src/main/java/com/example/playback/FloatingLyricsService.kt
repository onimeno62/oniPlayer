package com.example.playback

import android.app.Service
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
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
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
        val defaultWidthPx = (300 * density).toInt()
        val defaultHeightPx = (180 * density).toInt()

        val minWidthPx = (180 * density).toInt()
        val maxWidthPx = (450 * density).toInt()
        val minHeightPx = (115 * density).toInt()
        val maxHeightPx = (350 * density).toInt()

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
                    FloatingLyricsOverlayContent(
                        songFlow = currentSong,
                        positionFlow = position,
                        isPlayingFlow = isPlaying,
                        onClose = {
                            // Request VM to turn off the floating state if running
                            MusicPlayerViewModel.activeInstance?.setFloatingLyricsEnabled(false)
                            stopSelf()
                        },
                        onPlayPauseToggle = {
                            MusicPlayerViewModel.activeInstance?.togglePlayPause()
                        },
                        onSkipNext = {
                            MusicPlayerViewModel.activeInstance?.skipNext()
                        },
                        onSkipPrevious = {
                            MusicPlayerViewModel.activeInstance?.skipPrevious()
                        },
                        onSeekTo = { timestampMs ->
                            MusicPlayerViewModel.activeInstance?.audioEngine?.seekTo(timestampMs)
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
                            lp.width = (lp.width + dx.toInt()).coerceIn(minWidthPx, maxWidthPx)
                            lp.height = (lp.height + dy.toInt()).coerceIn(minHeightPx, maxHeightPx)
                            try {
                                wm.updateViewLayout(this, lp)
                            } catch (e: Exception) {
                                Log.e("FloatingLyricsService", "Failed to resize floating window: ${e.message}")
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
            while (isActive) {
                val viewModel = MusicPlayerViewModel.activeInstance
                if (viewModel != null) {
                    val engine = viewModel.audioEngine
                    combine(
                        engine.currentSong,
                        engine.position,
                        engine.isPlaying,
                        viewModel.floatingLyricsEnabled
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
                } else {
                    currentSong.value = null
                    position.value = 0L
                    isPlaying.value = false
                    delay(1000)
                }
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
    onResize: (Float, Float) -> Unit
) {
    val currentSong by songFlow.collectAsState()
    val position by positionFlow.collectAsState()
    val isPlaying by isPlayingFlow.collectAsState()

    // Parse LRC lines
    val lrcLines = remember(currentSong?.lyrics) {
        LyricsHelper.parseLrc(currentSong?.lyrics)
    }

    // Active lyric index matching the position
    val activeIndex = remember(lrcLines, position) {
        LyricsHelper.getActiveLineIndex(lrcLines, position)
    }

    val listState = rememberLazyListState()

    // Auto-scroll logic: centers the active line when it changes
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lrcLines.size) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val halfVisible = visibleItems.size / 2
                val targetIndex = (activeIndex - halfVisible).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            } else {
                listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE040FB).copy(alpha = 0.5f), // Modern purple primary
                            Color(0xFF00E5FF).copy(alpha = 0.3f)  // Cyan accent
                        )
                    ),
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF101012).copy(alpha = 0.94f) // Sleek translucent dark backing
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Header Area with Title & Close Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            contentDescription = "Drag",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Floating Lyrics",
                            tint = Color(0xFFE040FB),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentSong?.customTitle ?: currentSong?.title ?: "No Song Playing",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Scrollable Lyrics View Area (Middle segment)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentSong == null) {
                        Text(
                            text = "Play a track to view lyrics",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    } else if (currentSong?.lyrics.isNullOrBlank()) {
                        Text(
                            text = "No lyrics found for this song",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    } else if (lrcLines.isEmpty()) {
                        // Plain text fallback (scrollable scroll view without scrollbar)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = currentSong!!.lyrics!!,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // Synchronized interactive karaoke lyrics
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            itemsIndexed(lrcLines) { index, line ->
                                val isActiveLine = index == activeIndex
                                Text(
                                    text = line.text,
                                    fontSize = if (isActiveLine) 13.sp else 11.sp,
                                    fontWeight = if (isActiveLine) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isActiveLine) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.55f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSeekTo(line.timestampMs) }
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Action Control Deck (Non-overlapping player buttons)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFE040FB), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
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
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Subtly position the Resize Handle on the bottom-right corner of the Card overlay
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
                contentDescription = "Resize",
                tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
