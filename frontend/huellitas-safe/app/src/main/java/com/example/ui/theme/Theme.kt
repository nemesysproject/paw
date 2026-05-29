package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// --- THEME COLOR SCHEMES ---

// 1. Premium Dark (Default)
private val PremiumDarkColorScheme = darkColorScheme(
    primary = PremiumGradientStart,
    secondary = PremiumGradientEnd,
    background = PremiumDarkBg,
    surface = PremiumDarkSurface,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// 2. Glass (New - Frosted Glass Style)
private val GlassColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color(0xFFB0B0B0),
    background = Color(0xFF121417), // Very dark base for glass contrast
    surface = Color(0xFF2A2D35),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// 3. Midnight (Blue/Purple)
private val MidnightColorScheme = darkColorScheme(
    primary = Color(0xFF9E77ED),
    secondary = Color(0xFF7694FF),
    background = Color(0xFF0F101A),
    surface = Color(0xFF1B1D2E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Premium Dark",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeName) {
        "Glass" -> GlassColorScheme
        "Midnight" -> MidnightColorScheme
        else -> PremiumDarkColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
