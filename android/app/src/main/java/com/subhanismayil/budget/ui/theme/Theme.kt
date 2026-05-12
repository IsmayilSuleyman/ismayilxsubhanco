package com.subhanismayil.budget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = TextPrimary,
    surface = SurfaceGlass,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGlassStrong,
    onSurfaceVariant = TextSecondary,
    error = Negative,
    onError = Color.White
)

@Composable
fun SharedBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
