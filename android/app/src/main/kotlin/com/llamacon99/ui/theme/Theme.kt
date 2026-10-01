package com.llamacon99.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SamsungBlue,
    background = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = SamsungBlueDark,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = DangerRed,
)

/**
 * One UI takes its accent color from the device's wallpaper (Material You) on real Samsung
 * phones, so dynamic color is the most "Samsung" choice available on API 31+; below that we fall
 * back to a fixed Samsung-blue palette rather than stock Material purple.
 */
@Composable
fun LlamaCon99Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LlamaTypography,
        shapes = LlamaShapes,
        content = content,
    )
}
