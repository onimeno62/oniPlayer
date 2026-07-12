package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.screens.MainAppContainer
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            MainAppContainer(viewModel = viewModel)
        }

        // Keep the floating overlay service in sync with the user's settings state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.floatingLyricsEnabled.collect { enabled ->
                    if (enabled) {
                        if (Settings.canDrawOverlays(this@MainActivity)) {
                            val intent = Intent(this@MainActivity, com.example.playback.FloatingLyricsService::class.java)
                            startService(intent)
                        }
                    } else {
                        val intent = Intent(this@MainActivity, com.example.playback.FloatingLyricsService::class.java)
                        stopService(intent)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra("com.example.EXTRA_CATEGORY_INDEX")) {
                val categoryIndex = it.getIntExtra("com.example.EXTRA_CATEGORY_INDEX", -1)
                if (categoryIndex != -1) {
                    viewModel.setActiveCategoryIndex(categoryIndex)
                    viewModel.selectTab(0) // Switch to Songs/Library tab
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        moveTaskToBack(true)
    }
}


