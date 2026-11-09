package com.tau.studymate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = TauBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = LaborGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F397),
    onSecondaryContainer = Color(0xFF042100),
    tertiary = TauCyanAccent,
    background = SurfaceLight,
    surface = CardBackgroundLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = UrgentRed
)

private val DarkColorScheme = darkColorScheme(
    primary = TauBlueLight,
    onPrimary = Color.Black,
    primaryContainer = TauBluePrimary,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = LaborGreenLight,
    onSecondary = Color.Black,
    secondaryContainer = LaborGreen,
    onSecondaryContainer = Color(0xFFB7F397),
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
    dynamicColor: Boolean = true,
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
