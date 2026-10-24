package com.llamacon99.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** The accent is always BrandGreen. Profiles only change neutral surfaces and structure. */
internal fun profileColors(style: ThemeStyle, dark: Boolean, brand: ColorScheme): ColorScheme {
    return when (style) {
        ThemeStyle.APPLE -> {
            val page = if (dark) Color.Black else Color.White
            brand.copy(
                background = page,
                surface = page,
                surfaceContainerLow = page,
                outlineVariant = if (dark) Color(0xFF38383A) else Color(0xFFD1D1D6),
            )
        }
        ThemeStyle.MATERIAL -> if (dark) {
            brand.copy(
                background = Color(0xFF111117), onBackground = Color(0xFFE5E1E9),
                surface = Color(0xFF111117), onSurface = Color(0xFFE5E1E9),
                surfaceVariant = Color(0xFF2B2B35), onSurfaceVariant = Color(0xFFC6C3CD),
                surfaceContainerLowest = Color(0xFF0C0C12), surfaceContainerLow = Color(0xFF1A1A22),
                surfaceContainer = Color(0xFF22222A), surfaceContainerHigh = Color(0xFF2A2A34),
                surfaceContainerHighest = Color(0xFF34343E), outlineVariant = Color(0xFF474751),
            )
        } else {
            brand.copy(
                background = Color(0xFFF2F0F7), onBackground = Color(0xFF1A1B20),
                surface = Color(0xFFFDFBFF), onSurface = Color(0xFF1A1B20),
                surfaceVariant = Color(0xFFE8E4EC), onSurfaceVariant = Color(0xFF45464F),
                surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9F7FD),
                surfaceContainer = Color(0xFFF3F0F7), surfaceContainerHigh = Color(0xFFECE9F1),
                surfaceContainerHighest = Color(0xFFE6E2EA), outlineVariant = Color(0xFFC7C5D0),
            )
        }
        else -> brand
    }
}
