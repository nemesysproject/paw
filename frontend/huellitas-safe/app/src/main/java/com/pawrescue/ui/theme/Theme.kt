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

// 2. Premium (Warm Taupe & Amber Glass: #AA907A, #FFDA9F, #FFFFFF, #0F0F0F)
private val PremiumWarmColorScheme = darkColorScheme(
    primary = PremiumGold,
    secondary = PremiumTaupe,
    tertiary = PremiumWhite,
    background = PremiumObsidian,
    surface = PremiumDarkSurface,
    onPrimary = PremiumObsidian,
    onSecondary = PremiumWhite,
    onBackground = PremiumWhite,
    onSurface = PremiumWhite
)

// 3. Ice Clean (Light Glacier Minimalist: #C5DCE7, #FFFFFF, #111827, #4A909E)
private val IceCleanColorScheme = lightColorScheme(
    primary = IceInk,
    secondary = IceTeal,
    tertiary = IceAmber,
    background = IceGradientMid,
    surface = IceSurface,
    surfaceVariant = IceSurfaceMuted,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = IceInk,
    onSurface = IceInk,
    onSurfaceVariant = IceInkSecondary
)

fun isPremiumTheme(themeName: String): Boolean =
    themeName == "Premium" || themeName == "Premium Dark"

fun isIceCleanTheme(themeName: String): Boolean =
    themeName == "Ice Clean" || themeName == "Midnight"

@Composable
fun MyApplicationTheme(
    themeName: String = "Glass",
    content: @Composable () -> Unit,
) {
    val isPremium = isPremiumTheme(themeName)
    val isIce = isIceCleanTheme(themeName)
    val colorScheme = when {
        isPremium -> PremiumWarmColorScheme
        isIce -> IceCleanColorScheme
        else -> GlassColorScheme
    }
    val typography = if (isPremium || isIce) PremiumTypography else Typography

    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
