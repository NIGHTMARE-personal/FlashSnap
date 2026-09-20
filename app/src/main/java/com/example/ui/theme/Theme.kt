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

private val DarkColorScheme = darkColorScheme(
    primary = DarkCreamPrimary,
    onPrimary = DarkCreamOnPrimary,
    primaryContainer = DarkCreamPrimaryContainer,
    onPrimaryContainer = DarkCreamOnPrimaryContainer,
    secondary = DarkCreamSecondary,
    onSecondary = DarkCreamOnSecondary,
    secondaryContainer = DarkCreamSecondaryContainer,
    onSecondaryContainer = DarkCreamOnSecondaryContainer,
    tertiary = DarkCreamTertiary,
    onTertiary = DarkCreamOnTertiary,
    tertiaryContainer = DarkCreamTertiaryContainer,
    onTertiaryContainer = DarkCreamOnTertiaryContainer,
    background = DarkCreamBackground,
    onBackground = DarkCreamOnBackground,
    surface = DarkCreamSurface,
    onSurface = DarkCreamOnSurface,
    surfaceVariant = DarkCreamSurfaceVariant,
    onSurfaceVariant = DarkCreamOnSurfaceVariant,
    outline = DarkCreamOutline,
    error = PenaltyWarmCocoa,
    errorContainer = PenaltyWarmCocoaContainer
)

private val LightColorScheme = lightColorScheme(
    primary = CreamPrimary,
    onPrimary = CreamOnPrimary,
    primaryContainer = CreamPrimaryContainer,
    onPrimaryContainer = CreamOnPrimaryContainer,
    secondary = CreamSecondary,
    onSecondary = CreamOnSecondary,
    secondaryContainer = CreamSecondaryContainer,
    onSecondaryContainer = CreamOnSecondaryContainer,
    tertiary = CreamTertiary,
    onTertiary = CreamOnTertiary,
    tertiaryContainer = CreamTertiaryContainer,
    onTertiaryContainer = CreamOnTertiaryContainer,
    background = CreamBackground,
    onBackground = CreamOnBackground,
    surface = CreamSurface,
    onSurface = CreamOnSurface,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = CreamOnSurfaceVariant,
    outline = CreamOutline,
    error = PenaltyWarmCocoa,
    errorContainer = PenaltyWarmCocoaContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve our custom vibrant educational gamification palette
    content: @Composable () -> Unit,
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
