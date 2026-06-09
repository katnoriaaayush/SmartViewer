package com.smartai.explorer.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary           = Primary,
    secondary         = Secondary,
    tertiary          = Tertiary,
    background        = Background,
    surface           = Surface,
    surfaceVariant    = SurfaceVariant,
    outline           = Outline,
    onPrimary         = Color.White,
    onSecondary       = Color.White,
    onBackground      = OnSurface,
    onSurface         = OnSurface,
    onSurfaceVariant  = OnSurfaceVariant,
)

@Composable
fun SmartAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = SmartAITypography,
        content     = content,
    )
}
