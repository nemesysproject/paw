package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkWarmPrimary,
    secondary = DarkWarmSecondary,
    tertiary = DarkWarmTertiary,
    background = DarkWarmBackground,
    surface = DarkWarmSurface,
    onBackground = DarkWarmOnBackground,
    onSurface = DarkWarmOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = WarmPrimary,
    secondary = WarmSecondary,
    tertiary = WarmTertiary,
    background = WarmBackground,
    surface = WarmSurface,
    onPrimary = WarmOnPrimary,
    onBackground = WarmOnBackground,
    onSurface = WarmOnSurface
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Forzamos el uso de nuestra paleta cálida personalizada para garantizar la consistencia visual solicitada
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
