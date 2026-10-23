package com.llamacon99.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** The accent is always BrandGreen. Profiles only change neutral surfaces and structure. */
internal fun profileColors(style: ThemeStyle, dark: Boolean, brand: ColorScheme): ColorScheme {
    if (style != ThemeStyle.APPLE) return brand
    val page = if (dark) Color.Black else Color.White
    return brand.copy(
        background = page,
        surface = page,
        surfaceContainerLow = page,
        outlineVariant = if (dark) Color(0xFF38383A) else Color(0xFFD1D1D6),
    )
}
