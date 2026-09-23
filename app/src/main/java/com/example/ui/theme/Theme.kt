package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrainCardPrimaryLight,
    onPrimary = BrainCardSurfaceLight,
    primaryContainer = BrainCardPrimarySoftLight,
    onPrimaryContainer = BrainCardPrimaryLight,
    secondary = BrainCardMutedLight,
    onSecondary = BrainCardSurfaceLight,
    background = BrainCardBgLight,
    onBackground = BrainCardTextLight,
    surface = BrainCardSurfaceLight,
    onSurface = BrainCardTextLight,
    surfaceVariant = BrainCardSurface2Light,
    onSurfaceVariant = BrainCardMutedLight,
    outline = BrainCardBorderLight,
    error = BrainCardDangerLight,
    onError = BrainCardSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrainCardPrimaryDark,
    onPrimary = BrainCardBgDark,
    primaryContainer = BrainCardPrimarySoftDark,
    onPrimaryContainer = BrainCardPrimaryDark,
    secondary = BrainCardMutedDark,
    onSecondary = BrainCardBgDark,
    background = BrainCardBgDark,
    onBackground = BrainCardTextDark,
    surface = BrainCardSurfaceDark,
    onSurface = BrainCardTextDark,
    surfaceVariant = BrainCardSurface2Dark,
    onSurfaceVariant = BrainCardMutedDark,
    outline = BrainCardBorderDark,
    error = BrainCardDangerDark,
    onError = BrainCardBgDark
)

@Composable
fun BrainCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
