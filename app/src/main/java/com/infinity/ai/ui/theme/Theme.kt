package com.infinity.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightBlueScheme = lightColorScheme(
    primary          = BluePrimary,
    onPrimary        = TextOnPrimary,
    background       = LightBlueBgEnd,
    onBackground     = TextPrimary,
    surface          = CardWhite,
    onSurface        = TextPrimary,
    surfaceVariant   = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = TextSecondary,
    outline          = BorderLight,
    secondary        = BluePrimary,
    onSecondary      = TextOnPrimary,
    error            = ErrorRed,
    onError          = TextOnPrimary
)

@Composable
fun InfinityTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightBlueScheme,
        typography  = Typography,
        content     = content
    )
}
