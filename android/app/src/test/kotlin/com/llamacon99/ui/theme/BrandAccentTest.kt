package com.llamacon99.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class BrandAccentTest {
    @Test fun `each style uses its own accent palette in both appearances`() {
        for (dark in listOf(false, true)) {
            for (style in ThemeStyle.entries) {
                val colors = profileColors(style, dark, if (dark) DarkColors else LightColors)
                val expected = when (style) {
                    ThemeStyle.BRAND -> Color(0xFF1A73E8)
                    ThemeStyle.APPLE -> Color(0xFF0A84FF)
                    ThemeStyle.MATERIAL -> Color(0xFF188038)
                    ThemeStyle.HORIZON -> Color(0xFF168C9B)
                    ThemeStyle.HYPER -> if (dark) Color(0xFFB8D5E2) else Color(0xFF668A9B)
                    ThemeStyle.MONO -> if (dark) Color.White else Color(0xFF171717)
                    ThemeStyle.FLUENT -> Color(0xFF0078D4)
                }
                assertEquals("$style dark=$dark primary", expected, colors.primary)
                assertEquals("$style dark=$dark secondary", expected, colors.secondary)
                assertEquals("$style dark=$dark tertiary", expected, colors.tertiary)
                assertEquals("$style dark=$dark tint", expected, colors.surfaceTint)
            }
        }
    }
}
