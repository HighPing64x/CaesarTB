package com.caesar.toolbox.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// CaesarTB 主题 — QQNT 白/黑双色调
// ============================================================

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = PrimaryLight,
    tertiary = PrimaryDark,
    background = Dark_Background,
    surface = Dark_Surface,
    surfaceVariant = Dark_SurfaceVariant,
    onBackground = Dark_OnBackground,
    onSurface = Dark_OnSurface,
    onSurfaceVariant = Dark_OnSurfaceVariant,
    outline = Dark_Outline,
    outlineVariant = Dark_Outline,
    error = Error,
    scrim = Color.Black.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = PrimaryLight,
    tertiary = PrimaryDark,
    background = Light_Background,
    surface = Light_Surface,
    surfaceVariant = Light_SurfaceVariant,
    onBackground = Light_OnBackground,
    onSurface = Light_OnSurface,
    onSurfaceVariant = Light_OnSurfaceVariant,
    outline = Light_Outline,
    outlineVariant = Light_Outline,
    error = Error
)

@Composable
fun CaesarTBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // 沉浸式状态栏
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CaesarTypography,
        shapes = CaesarShapes,
        content = content
    )
}
