package com.pawrescue.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// --- THEME COLOR SCHEMES ---

// 1. Glass (Default - Liquid Glass Frosted Style, sincronizado con paw-portal)
private val GlassColorScheme = darkColorScheme(
    primary = GlassPrimary,
    secondary = GlassSecondary,
    background = GlassGradientBottom,
    surface = GlassSurface,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// 2. Premium Dark
private val PremiumDarkColorScheme = darkColorScheme(
    primary = PremiumGradientStart,
    secondary = PremiumGradientEnd,
    background = PremiumDarkBg,
    surface = PremiumDarkSurface,
    onPrimary = Color.White,
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
    themeName: String = "Glass",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeName) {
        "Premium Dark" -> PremiumDarkColorScheme
        "Midnight" -> MidnightColorScheme
        else -> GlassColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
