package com.tau.studymate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = TauPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = TauPurpleLight,
    onPrimaryContainer = TauPurpleDark,
    secondary = TauMagentaAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAE8FF),
    onSecondaryContainer = Color(0xFF701A75),
    tertiary = TauCyanAccent,
    tertiaryContainer = TauTealLight,
    onTertiaryContainer = Color(0xFF115E59),
    background = SurfaceLight,
    surface = CardBackgroundLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = UrgentRed
)

private val DarkColorScheme = darkColorScheme(
    primary = TauPurpleVibrant,
    onPrimary = Color.White,
    primaryContainer = TauPurpleDark,
    onPrimaryContainer = TauPurpleLight,
    secondary = TauMagentaAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A044E),
    onSecondaryContainer = Color(0xFFF5D0FE),
    tertiary = TauCyanAccent,
    background = SurfaceDark,
    surface = CardBackgroundDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = UrgentRed
)

@Composable
fun TAUStudyMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
