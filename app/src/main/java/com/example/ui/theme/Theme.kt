package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GlassIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = GlassIndigoLight,
    secondary = GlassCyanAccent,
    onSecondary = Color(0xFF080910),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = GlassCyanLight,
    tertiary = GlassVioletAccent,
    onTertiary = Color.White,
    background = GlassObsidianBackground,
    onBackground = GlassTextSlate100,
    surface = GlassDarkSurface,
    onSurface = GlassTextSlate100,
    surfaceVariant = Color(0xFF151928),
    onSurfaceVariant = GlassTextSlate400,
    outline = Color.White.copy(alpha = 0.12f),
    error = GlassRoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GlassIndigoDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = GlassCyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = GlassVioletAccent,
    onTertiary = Color.White,
    background = GlassTextSlate100,
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = GlassRoseError,
    onError = Color.White
)

@Composable
fun NovaTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
