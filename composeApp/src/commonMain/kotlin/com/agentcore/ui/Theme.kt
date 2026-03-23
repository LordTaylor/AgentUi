package com.agentcore.ui

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Midnight Premium Palette - Refined
val PrimaryColor = Color(0xFF0A84FF)       // iOS Style Blue
val SecondaryColor = Color(0xFF5E5CE6)     // Indigo
val AccentColor = Color(0xFF30D158)       // Success Green
val ErrorColor = Color(0xFFFF453A)        // System Red

val BackgroundColor = Color(0xFF000000)    // Pure Obsidian
val SurfaceColor = Color(0xFF121214)      // Deep Surface
val SurfaceVariantColor = Color(0x991C1C1E) // Semi-Transparent for Glassmorphism
val GlassBorderColor = Color(0x33FFFFFF)   // Subtle Light Border for Glass

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = AccentColor,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0x33000000),
    error = ErrorColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray
)

val AgentColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = AccentColor,
    background = BackgroundColor,
    surface = SurfaceColor,
    surfaceVariant = SurfaceVariantColor,
    outline = GlassBorderColor,
    error = ErrorColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE5E5EA), 
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color.White
)

@androidx.compose.runtime.Composable
fun AgentTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) AgentColorScheme else LightColorScheme

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = AgentTypography,
        content = content
    )
}

val AgentTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)
