package com.nedaye.rollapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val accent: Color, val accentSoft: Color, val bg: Color, val surface: Color,
    val ink: Color, val inkSoft: Color, val border: Color,
    val warn: Color, val warnSoft: Color, val error: Color, val errorSoft: Color
)

private val LightColors = AppColors(
    accent = Color(0xFF2F6845), accentSoft = Color(0xFFE7F1EA),
    bg = Color(0xFFFAFAF8), surface = Color(0xFFFFFFFF),
    ink = Color(0xFF1F2937), inkSoft = Color(0xFF6B7280), border = Color(0xFFE3E1DA),
    warn = Color(0xFFB8860B), warnSoft = Color(0xFFFBF2DC),
    error = Color(0xFFB3261E), errorSoft = Color(0xFFFBEAE8)
)

private val DarkColors = AppColors(
    accent = Color(0xFF4CAF7D), accentSoft = Color(0xFF1E3226),
    bg = Color(0xFF14171A), surface = Color(0xFF1C2023),
    ink = Color(0xFFEDEEEC), inkSoft = Color(0xFF9AA0A6), border = Color(0xFF2E3338),
    warn = Color(0xFFE0B94F), warnSoft = Color(0xFF332A11),
    error = Color(0xFFE5847C), errorSoft = Color(0xFF3A1E1C)
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun MadrasaRollTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark)
        darkColorScheme(primary = colors.accent, background = colors.bg, surface = colors.surface)
    else
        lightColorScheme(primary = colors.accent, background = colors.bg, surface = colors.surface)

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
