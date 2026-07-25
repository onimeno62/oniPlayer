package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // Whether approved or denied, re-sync so the pending-cleanup filter re-checks
        // whether the leftover duplicate file is finally gone.
        viewModel.onDeleteConsentResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            MainAppContainer(viewModel = viewModel)
        }

        // Launch the system consent dialog whenever a rename needs permission to delete
        // a leftover MediaStore-owned file it couldn't remove on its own.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingDeleteRequest.collect { intentSender ->
                    deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            }
        }

        // Keep the floating overlay service in sync with the user's settings state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.floatingLyricsEnabled.collect { enabled ->
                    if (enabled) {
                        if (Settings.canDrawOverlays(this@MainActivity)) {
                            val intent = Intent(this@MainActivity, com.example.playback.FloatingLyricsService::class.java)
                            startService(intent)
                        } else {
                            viewModel.setFloatingLyricsEnabled(false)
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                startActivity(intent)
                            }
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
        super.onBackPressed()
        moveTaskToBack(true)
    }
}


