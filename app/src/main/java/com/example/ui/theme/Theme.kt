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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF0A84FF),         // Apple System Blue (Dark)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003060),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF30D158),       // Apple System Green (Dark)
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003810),
    onSecondaryContainer = Color(0xFFD0FFD6),
    tertiary = Color(0xBFBF5AF2),        // Apple System Purple (Dark)
    onTertiary = Color.White,
    background = Color(0xFF000000),      // True Black OLED Background
    onBackground = Color(0xFFFFFFFF),    // Full White primary text
    surface = Color(0xFF2C2C2E),         // Cards/surfaces visual container
    onSurface = Color(0xFFFFFFFF),       // Full White
    surfaceVariant = Color(0xFF1C1C1E),  // Apple Grouped background
    outline = Color(0xFF38383A),         // Apple Hairline Separator
    error = Color(0xFFFF453A),           // Apple Destructive Red (Dark)
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF007AFF),        // Apple System Blue (Light)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1F0FF),
    onPrimaryContainer = Color(0xFF001A40),
    secondary = Color(0xFF34C759),      // Apple System Green (Light)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3FCE9),
    onSecondaryContainer = Color(0xFF00360C),
    tertiary = Color(0xFFAF52DE),       // Apple System Purple (Light)
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),     // Pure White Background
    onBackground = Color(0xFF000000),   // Full Black primary text
    surface = Color(0xFFFFFFFF),        // Cards/elevated surfaces (White)
    onSurface = Color(0xFF000000),      // Full Black text
    surfaceVariant = Color(0xFFF2F2F7), // Apple Grouped Background
    onSurfaceVariant = Color(0xFF3C3C43), // 60% opacity look text
    outline = Color(0x1F3C3C43),        // Apple separator (12% opacity)
    error = Color(0xFFFF3B30),          // Apple Destructive Red (Light)
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
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
