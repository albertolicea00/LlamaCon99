package com.llamacon99.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppTheme = staticCompositionLocalOf { AppThemeCatalog.default }

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = OnBrandGreen,
    primaryContainer = LightGreenContainer,
    onPrimaryContainer = OnBrandGreen,
    secondary = BrandGreen,
    onSecondary = OnBrandGreen,
    secondaryContainer = LightGreenContainer,
    onSecondaryContainer = OnBrandGreen,
    tertiary = BrandGreen,
    onTertiary = OnBrandGreen,
    tertiaryContainer = LightGreenContainer,
    onTertiaryContainer = OnBrandGreen,
    inversePrimary = BrandGreen,
    surfaceTint = BrandGreen,
    background = LightSurface,
    onBackground = DarkSurface,
    surface = LightSurface,
    onSurface = DarkSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF444746),
    surfaceDim = Color(0xFFDADADC),
    surfaceBright = LightSurface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE3E3E6),
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = OnBrandGreen,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = LightGreenContainer,
    secondary = BrandGreen,
    onSecondary = OnBrandGreen,
    secondaryContainer = DarkGreenContainer,
    onSecondaryContainer = LightGreenContainer,
    tertiary = BrandGreen,
    onTertiary = OnBrandGreen,
    tertiaryContainer = DarkGreenContainer,
    onTertiaryContainer = LightGreenContainer,
    inversePrimary = BrandGreen,
    surfaceTint = BrandGreen,
    background = DarkSurface,
    onBackground = LightSurface,
    surface = DarkSurface,
    onSurface = LightSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C7C5),
    surfaceDim = DarkSurface,
    surfaceBright = Color(0xFF383838),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF333333),
    error = DangerRed,
)

/** Keep the brand colors consistent; app profiles customize component shapes and layout. */
@Composable
fun LlamaCon99Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppThemeDefinition = AppThemeCatalog.default,
    content: @Composable () -> Unit,
) {
    val source = if (darkTheme) DarkColors else LightColors

    androidx.compose.runtime.CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(colorScheme = source, typography = LlamaTypography, shapes = appTheme.shapes, content = content)
    }
}
