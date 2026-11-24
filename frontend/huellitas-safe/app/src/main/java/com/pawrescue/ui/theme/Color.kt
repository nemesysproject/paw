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

// Premium Warm Glass Palette (inspirado en referencia: #AA907A, #FFDA9F, #FFFFFF, #0F0F0F)
val PremiumTaupe = Color(0xFFAA907A)         // Warm Taupe / Champagne Bronze
val PremiumGold = Color(0xFFFFDA9F)          // Soft Warm Gold / Amber Glow
val PremiumObsidian = Color(0xFF0F0F0F)      // Deep Obsidian Black
val PremiumWhite = Color(0xFFFFFFFF)         // Crisp Pure White

val PremiumGradientTop = Color(0xFF8F7763)   // Ambient taupe top glow
val PremiumGradientMid = Color(0xFF3E3127)   // Warm smoked mocha midtone
val PremiumGradientBottom = Color(0xFF0F0F0F)// Deep obsidian bottom

val PremiumDarkBg = PremiumObsidian
val PremiumDarkSurface = Color(0xFF241D17)   // Translucent warm dark surface
val PremiumCardBorder = Color(0x38FFFFFF)    // Subtle luminous glass rim
val PremiumGradientStart = PremiumTaupe
val PremiumGradientEnd = PremiumGold

val PremiumOnPrimary = PremiumObsidian
val PremiumOnBackground = PremiumWhite
val PremiumOnSurface = PremiumWhite
val PremiumSecondaryText = Color(0xFFD1C4B8)

// Ice Clean Light Palette (inspirado en CleanPro: azul hielo -> blanco puro -> tinta oscura)
val IceGradientTop = Color(0xFFC5DCE7)       // Soft icy sky blue top
val IceGradientMid = Color(0xFFDCEAF1)       // Pale glacier mist
val IceGradientBottom = Color(0xFFEEF5F8)    // Clean airy frost bottom
val IceSurface = Color(0xFFFFFFFF)           // Crisp white card surface
val IceSurfaceMuted = Color(0xFFF3F7FA)      // Soft inner card surface
val IceInk = Color(0xFF111827)               // Deep ink primary text & CTA
val IceInkSecondary = Color(0xFF5D7482)      // Slate blue-gray secondary text
val IceTeal = Color(0xFF4A909E)              // Cool teal status accent
val IceAmber = Color(0xFFF59E0B)             // Warm amber status accent

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
