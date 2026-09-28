package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.service.ShohanVoiceService
import com.example.ui.ShohanMainScreen
import com.example.ui.ShohanViewModel
import com.example.ui.theme.ShohanAITheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShohanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            ShohanAITheme {
                ShohanMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val fromWake = intent.getBooleanExtra(ShohanVoiceService.EXTRA_WAKE_WORD_TRIGGERED, false)
        val fromOverlay = intent.getBooleanExtra("FROM_OVERLAY_TRIGGER", false)
        val spokenCommand = intent.getStringExtra(ShohanVoiceService.EXTRA_SPOKEN_COMMAND)

        if (!spokenCommand.isNullOrBlank()) {
            viewModel.handleUserPrompt(spokenCommand)
        } else if (fromWake || fromOverlay) {
            viewModel.speak("হ্যাঁ বলুন, আমি শুনছি!")
            viewModel.startListening(continuous = false)
        }
    }
}
