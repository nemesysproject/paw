package com.pawrescue.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
  )

// Tipografía limpia estilo SF Pro Display para el tema Premium (jerarquía Light/Regular/Medium/Bold)
val PremiumTypography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Light,
      fontSize = 36.sp,
      lineHeight = 42.sp,
      letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 28.sp,
      lineHeight = 34.sp,
      letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 22.sp,
      lineHeight = 28.sp,
      letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 20.sp,
      lineHeight = 26.sp,
      letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 19.sp,
      letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 14.sp,
      letterSpacing = 0.2.sp
    )
  )
