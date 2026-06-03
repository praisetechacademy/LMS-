package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    secondary = InteractiveBlue,
    tertiary = StarGold,
    background = BackgroundDark,
    surface = CardDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextLightHeader,
    onSurface = TextLightHeader
)

private val LightColorScheme = lightColorScheme(
    primary = NavalBlue,
    secondary = InteractiveBlue,
    tertiary = StarGold,
    background = BackgroundLight,
    surface = CardLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDarkHeader,
    onSurface = TextDarkHeader
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
