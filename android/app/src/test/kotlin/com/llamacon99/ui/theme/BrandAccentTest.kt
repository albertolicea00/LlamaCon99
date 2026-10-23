package com.llamacon99.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandAccentTest {
    @Test fun `every style preserves the exact brand green in both appearances`() {
        for (dark in listOf(false, true)) {
            for (style in ThemeStyle.entries) {
                val colors = profileColors(style, dark, if (dark) DarkColors else LightColors)
                assertEquals("$style dark=$dark primary", BrandGreen, colors.primary)
                assertEquals("$style dark=$dark secondary", BrandGreen, colors.secondary)
                assertEquals("$style dark=$dark tertiary", BrandGreen, colors.tertiary)
                assertEquals("$style dark=$dark tint", BrandGreen, colors.surfaceTint)
                assertEquals("$style dark=$dark button text", OnBrandGreen, colors.onPrimary)
            }
        }
    }
}
