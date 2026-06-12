package com.smartai.explorer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary              = Indigo600,
    onPrimary            = Color.White,
    primaryContainer     = Indigo100,
    onPrimaryContainer   = OnIndigoContainer,
    secondary            = IndexSlate,
    onSecondary          = Color.White,
    secondaryContainer   = SurfaceSubtle,
    onSecondaryContainer = InkPrimary,
    tertiary             = InsightsGreen,
    onTertiary           = Color.White,
    background           = Canvas,
    onBackground         = InkPrimary,
    surface              = CardWhite,
    onSurface            = InkPrimary,
    surfaceVariant       = SurfaceSubtle,
    onSurfaceVariant     = InkSecondary,
    outline              = HairlineBorder,
    outlineVariant       = HairlineBorder,
    error                = ErrorRed,
    onError              = Color.White,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    scrim                = Color(0xFF101319),
)

private val SmartAIShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(14.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmartAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = SmartAITypography,
        shapes      = SmartAIShapes,
        content     = content,
    )
}
