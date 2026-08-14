package com.example.aikeyboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BlueAccentLight,
    onPrimary = KeyWhite,
    primaryContainer = PaleBlueContainer,
    onPrimaryContainer = BlueAccentLight,
    secondary = BlueAccentLight,
    onSecondary = KeyWhite,
    secondaryContainer = PaleBlueContainer,
    onSecondaryContainer = BlueAccentLight,
    background = PaleBluePage,
    onBackground = NavyText,
    surface = KeyWhite,
    onSurface = NavyText,
    surfaceVariant = PaleBluePage,
    onSurfaceVariant = MutedNavy,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = Color(0xFFE0483E),
    onError = KeyWhite
)

private val DarkColors = darkColorScheme(
    primary = BlueAccentDark,
    onPrimary = Color(0xFF152049),
    primaryContainer = DeepBlueContainer,
    onPrimaryContainer = BlueAccentDark,
    secondary = BlueAccentDark,
    onSecondary = Color(0xFF152049),
    secondaryContainer = DeepBlueContainer,
    onSecondaryContainer = BlueAccentDark,
    background = NearBlackPage,
    onBackground = OffWhiteText,
    surface = KeyDark,
    onSurface = OffWhiteText,
    surfaceVariant = NearBlackPage,
    onSurfaceVariant = MutedLavender,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Color(0xFFFF6B61),
    onError = Color(0xFF152049)
)

/**
 * White (light mode) / near-black (dark mode) as the primary surface, with a
 * single blue accent for interactive elements — matches the two-color design
 * reference. Dynamic (Material You) color is intentionally NOT used here so
 * the keyboard's branding stays consistent across all devices.
 */
@Composable
fun AiKeyboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = if (darkTheme) DarkAppTypography
                else LightAppTypography,
        content = content
    )
}