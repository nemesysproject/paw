package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

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
