package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BrainCardViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.BrainCardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrainCardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsStateWithLifecycle()
            val isDark = isDarkThemeOverride ?: isSystemInDarkTheme()

            BrainCardTheme(darkTheme = isDark) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
