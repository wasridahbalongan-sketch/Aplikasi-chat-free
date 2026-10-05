package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = Color(0xFF003828),
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = ZallCardDark,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF38BDF8),
    background = ZallBgDark,
    onBackground = Color(0xFFE9EDEF),
    surface = ZallSurfaceDark,
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = ZallCardDark,
    onSurfaceVariant = Color(0xFF8696A0),
    error = CallRed
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = EmeraldDark,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = MintAccent,
    background = ZallBgLight,
    onBackground = Color(0xFF111B21),
    surface = ZallSurfaceLight,
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFE9EDEF),
    onSurfaceVariant = Color(0xFF54656F),
    error = CallRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
