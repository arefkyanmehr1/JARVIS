package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F59),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = JarvisElectricBlue,
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF004C70),
    onSecondaryContainer = Color(0xFFCCE5FF),
    tertiary = JarvisEmerald,
    onTertiary = Color(0xFF003824),
    background = JarvisNavyDeep,
    onBackground = Color(0xFFE2E8F0),
    surface = JarvisSurfaceDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = JarvisSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = JarvisBorderSubtle,
    outlineVariant = JarvisBorderHighlight,
    error = JarvisCrimson,
    onError = Color.White
)

private val JarvisLightColorScheme = lightColorScheme(
    primary = JarvisLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = JarvisEmerald,
    background = JarvisLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = JarvisLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = JarvisLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = JarvisLightBorder,
    error = JarvisCrimson,
    onError = Color.White
)

@Composable
fun JarvisTheme(
    darkTheme: Boolean = true, // Default to futuristic Dark HUD as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JarvisDarkColorScheme else JarvisLightColorScheme

    // Real RTL layout direction for Persian support
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

