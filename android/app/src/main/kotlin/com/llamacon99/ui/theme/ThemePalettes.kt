package com.llamacon99.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Keep the brand profile green while giving every visual profile its own accent palette. */
internal fun profileColors(style: ThemeStyle, dark: Boolean, brand: ColorScheme): ColorScheme {
    val surfaces = when (style) {
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

    val (accent, onAccent, accentContainer, onAccentContainer) = when (style) {
        ThemeStyle.BRAND -> listOf(Color(0xFF1A73E8), Color.White, if (dark) Color(0xFF174A83) else Color(0xFFD7E7FF), if (dark) Color(0xFFD7E7FF) else Color(0xFF0B3D91))
        ThemeStyle.APPLE -> listOf(Color(0xFF0A84FF), Color.White, if (dark) Color(0xFF183A5E) else Color(0xFFD9EBFF), if (dark) Color(0xFFD9EBFF) else Color(0xFF003D7A))
        ThemeStyle.MATERIAL -> listOf(Color(0xFF188038), Color.White, if (dark) Color(0xFF174D2A) else Color(0xFFDCEBDD), if (dark) Color(0xFFDCEBDD) else Color(0xFF254A2E))
        ThemeStyle.HORIZON -> listOf(Color(0xFF168C9B), Color.White, if (dark) Color(0xFF17454D) else Color(0xFFD2F1F3), if (dark) Color(0xFFC7F2F5) else Color(0xFF073B42))
        ThemeStyle.HYPER -> listOf(if (dark) Color(0xFFB8D5E2) else Color(0xFF668A9B), if (dark) Color(0xFF152027) else Color.White, if (dark) Color(0xFF344751) else Color(0xFFDCEAF0), if (dark) Color(0xFFE1F2F8) else Color(0xFF263D48))
        ThemeStyle.MONO -> if (dark) listOf(Color.White, Color.Black, Color(0xFF3A3A3A), Color.White) else listOf(Color(0xFF171717), Color.White, Color(0xFFD8D8D8), Color(0xFF171717))
        ThemeStyle.FLUENT -> listOf(Color(0xFF0078D4), Color.White, if (dark) Color(0xFF164A70) else Color(0xFFD9EDFF), if (dark) Color(0xFFD6EDFF) else Color(0xFF003A66))
    }

    return surfaces.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = accentContainer,
        onTertiaryContainer = onAccentContainer,
        inversePrimary = accent,
        surfaceTint = accent,
    )
}
