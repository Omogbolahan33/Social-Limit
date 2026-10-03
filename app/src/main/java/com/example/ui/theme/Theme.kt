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
    primary = Color(0xFF1E88E5),          // Vibrant Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D47A1),  // Deep Navy Blue
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFFFFB300),        // Gold
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF3E2723), // Deep Chocolate
    onSecondaryContainer = Color(0xFFD7CCC8),
    tertiary = Color(0xFF43A047),         // Green
    onTertiary = Color.White,
    background = Color(0xFF121212),       // Blank/Black
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    outline = Color(0xFF455A64),
    error = Color(0xFFE65100),            // Orange
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1565C0),        // Deep Royal Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD), // Soft Ice Blue container
    onPrimaryContainer = Color(0xFF0D47A1), // Deep Navy Blue text
    secondary = Color(0xFF4E342E),      // Rich Chocolate Brown
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEBE9), // Warm Chocolate Latte helper container
    onSecondaryContainer = Color(0xFF3E2723), // Deep Chocolate text
    tertiary = Color(0xFFFFB300),       // Brilliant Gold
    onTertiary = Color(0xFF121212),
    background = Color(0xFFF8F9FA),     // Crisp clean white/off-white canvas (zero purple)
    onBackground = Color(0xFF121212),   // Dark blank/charcoal text
    surface = Color(0xFFFFFFFF),        // Pure white containers/cards
    onSurface = Color(0xFF121212),      // Charcoal text on containers
    outline = Color(0xFFCFD8DC),        // Geometric border outlines
    error = Color(0xFFE65100),          // Vibrant Orange alerts
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Strict palette enforcement: dynamicColor disabled to eliminate accidental system purple tints
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
