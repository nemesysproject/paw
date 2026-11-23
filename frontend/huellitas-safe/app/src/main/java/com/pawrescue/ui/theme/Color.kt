package com.pawrescue.ui.theme

import androidx.compose.ui.graphics.Color

// Liquid Glass Palette (frosted periwinkle-lavender -> midnight navy, alineado con paw-portal)
val GlassGradientTop = Color(0xFFB9C3EC)
val GlassGradientMid1 = Color(0xFF8A95CC)
val GlassGradientMid2 = Color(0xFF4A558C)
val GlassGradientBottom = Color(0xFF0C1229)
val GlassPrimary = Color(0xFF818CF8)
val GlassSecondary = Color(0xFFA5B4FC)
val GlassSurface = Color(0xFF28315A)
val GlassCardFill = Color(0x29FFFFFF)      // ~16% white frosted fill
val GlassCardBorder = Color(0x52FFFFFF)    // ~32% white specular border

// Premium Dark Palette (based on the provided image)
val PremiumDarkBg = Color(0xFF1B1C26)      // Very dark bluish-gray background
val PremiumDarkSurface = Color(0xFF232533) // Slightly lighter for cards/surfaces
val PremiumGradientStart = Color(0xFFEC6A7C) // Pink/Magenta
val PremiumGradientEnd = Color(0xFFF39C6B)   // Orange/Peach

val PremiumOnPrimary = Color(0xFFFFFFFF)
val PremiumOnBackground = Color(0xFFFFFFFF)
val PremiumOnSurface = Color(0xFFFFFFFF)
val PremiumSecondaryText = Color(0xFF8E8E93)

// Legacy mapping to maintain theme functions
val WarmPrimary = PremiumGradientStart
val WarmSecondary = PremiumGradientEnd
val WarmTertiary = Color(0xFF9E77ED) 
val WarmBackground = PremiumDarkBg
val WarmSurface = PremiumDarkSurface
val WarmOnPrimary = PremiumOnPrimary
val WarmOnBackground = PremiumOnBackground
val WarmOnSurface = PremiumOnSurface

val WarmDialogBackground = PremiumDarkSurface

// Dark Theme Variants
val DarkWarmPrimary = PremiumGradientStart
val DarkWarmSecondary = PremiumGradientEnd
val DarkWarmTertiary = Color(0xFF9E77ED)
val DarkWarmBackground = PremiumDarkBg
val DarkWarmSurface = PremiumDarkSurface
val DarkWarmOnBackground = PremiumOnBackground
val DarkWarmOnSurface = PremiumOnSurface
