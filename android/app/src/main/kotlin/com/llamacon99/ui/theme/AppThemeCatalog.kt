package com.llamacon99.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.llamacon99.R

data class ContactComponents(
    val groupCorner: androidx.compose.ui.unit.Dp,
    val groupInset: androidx.compose.ui.unit.Dp,
    val contentInset: androidx.compose.ui.unit.Dp,
    val rowMinHeight: androidx.compose.ui.unit.Dp,
    val avatarSize: androidx.compose.ui.unit.Dp,
    val avatarGap: androidx.compose.ui.unit.Dp,
    val rowGap: androidx.compose.ui.unit.Dp = 0.dp,
    val borderWidth: androidx.compose.ui.unit.Dp = 0.dp,
    val elevation: androidx.compose.ui.unit.Dp = 0.dp,
)
data class SearchComponent(
    val alignment: Alignment,
    val outerPadding: PaddingValues,
    val listTopInset: androidx.compose.ui.unit.Dp,
    val listBottomInset: androidx.compose.ui.unit.Dp,
    val corner: androidx.compose.ui.unit.Dp,
    val minHeight: androidx.compose.ui.unit.Dp,
    val shadowElevation: androidx.compose.ui.unit.Dp,
)
data class ScrollbarComponent(
    val width: androidx.compose.ui.unit.Dp,
    val corner: androidx.compose.ui.unit.Dp,
    val showSectionMarkers: Boolean,
    val railPadding: PaddingValues,
    val minThumbHeight: androidx.compose.ui.unit.Dp,
)

enum class ThemeStyle { BRAND, APPLE, MATERIAL, HORIZON, HYPER, MONO, FLUENT }

/** Register a theme here; each UI component reads its own style from this single catalog. */
data class AppThemeDefinition(
    val id: String,
    val nameRes: Int,
    val inspirationRes: Int,
    val shapes: Shapes,
    val contacts: ContactComponents,
    val search: SearchComponent,
    val scrollbar: ScrollbarComponent,
    val style: ThemeStyle = ThemeStyle.BRAND,
    val typography: Typography = LlamaTypography,
    val titleTopInset: androidx.compose.ui.unit.Dp = 12.dp,
    val titleBottomInset: androidx.compose.ui.unit.Dp = 20.dp,
    val settingsInset: androidx.compose.ui.unit.Dp = 24.dp,
)

object AppThemeCatalog {
    val themes = listOf(
        AppThemeDefinition(
            id = "llama", nameRes = R.string.settings_theme_llama, inspirationRes = R.string.settings_theme_inspired_samsung,
            shapes = Shapes(extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(30.dp), extraLarge = RoundedCornerShape(36.dp)),
            contacts = ContactComponents(groupCorner = 36.dp, groupInset = 24.dp, contentInset = 16.dp, rowMinHeight = 64.dp, avatarSize = 38.dp, avatarGap = 12.dp),
            search = SearchComponent(Alignment.BottomCenter, PaddingValues(horizontal = 24.dp, vertical = 4.dp), 8.dp, 104.dp, 50.dp, 56.dp, 8.dp),
            scrollbar = ScrollbarComponent(12.dp, 6.dp, true, PaddingValues(top = 64.dp, end = 6.dp, bottom = 80.dp), 32.dp),
            titleTopInset = 64.dp,
            titleBottomInset = 32.dp,
        ),
        AppThemeDefinition(
            id = "manzana", nameRes = R.string.settings_theme_manzana, inspirationRes = R.string.settings_theme_inspired_apple,
            shapes = themeShapes(12),
            contacts = ContactComponents(0.dp, 0.dp, 20.dp, 48.dp, 0.dp, 0.dp),
            search = bottomSearch(inset = 16, corner = 28, elevation = 0),
            scrollbar = compactScrollbar(),
            style = ThemeStyle.APPLE, typography = ManzanaTypography,
            titleTopInset = 24.dp, settingsInset = 16.dp,
        ),
        AppThemeDefinition(
            id = "material", nameRes = R.string.settings_theme_material, inspirationRes = R.string.settings_theme_inspired_google,
            shapes = themeShapes(24),
            contacts = ContactComponents(0.dp, 0.dp, 16.dp, 72.dp, 40.dp, 16.dp),
            search = topSearch(inset = 16, corner = 32, height = 56),
            scrollbar = compactScrollbar(),
            style = ThemeStyle.MATERIAL, typography = MaterialTypography,
            titleTopInset = 32.dp, titleBottomInset = 28.dp, settingsInset = 16.dp,
        ),
        AppThemeDefinition(
            id = "horizon", nameRes = R.string.settings_theme_horizon, inspirationRes = R.string.settings_theme_inspired_huawei,
            shapes = themeShapes(20),
            contacts = ContactComponents(24.dp, 16.dp, 16.dp, 64.dp, 40.dp, 12.dp),
            search = topSearch(inset = 16, corner = 24),
            scrollbar = compactScrollbar(),
            style = ThemeStyle.HORIZON, typography = HorizonTypography,
            titleTopInset = 24.dp, settingsInset = 20.dp,
        ),
        AppThemeDefinition(
            id = "hyper", nameRes = R.string.settings_theme_hyper, inspirationRes = R.string.settings_theme_inspired_xiaomi,
            shapes = themeShapes(20),
            contacts = ContactComponents(20.dp, 16.dp, 12.dp, 56.dp, 34.dp, 12.dp),
            search = topSearch(inset = 16, corner = 14),
            scrollbar = compactScrollbar(),
            style = ThemeStyle.HYPER, typography = HyperTypography,
            titleTopInset = 20.dp, settingsInset = 16.dp,
        ),
        AppThemeDefinition(
            id = "mono", nameRes = R.string.settings_theme_mono, inspirationRes = R.string.settings_theme_inspired_nothing,
            shapes = themeShapes(4),
            contacts = ContactComponents(4.dp, 20.dp, 16.dp, 64.dp, 36.dp, 12.dp, rowGap = 6.dp, borderWidth = 1.dp),
            search = topSearch(inset = 20, corner = 0),
            scrollbar = ScrollbarComponent(8.dp, 0.dp, true, PaddingValues(top = 64.dp, end = 4.dp, bottom = 80.dp), 32.dp),
            style = ThemeStyle.MONO, typography = MonoTypography,
            titleTopInset = 28.dp, settingsInset = 20.dp,
        ),
        AppThemeDefinition(
            id = "fluent", nameRes = R.string.settings_theme_fluent, inspirationRes = R.string.settings_theme_inspired_microsoft,
            shapes = themeShapes(8),
            contacts = ContactComponents(8.dp, 16.dp, 16.dp, 60.dp, 34.dp, 12.dp, rowGap = 4.dp, borderWidth = 0.5.dp, elevation = 2.dp),
            search = topSearch(inset = 16, corner = 4),
            scrollbar = ScrollbarComponent(3.dp, 2.dp, false, PaddingValues(4.dp), 32.dp),
            style = ThemeStyle.FLUENT, typography = FluentTypography,
            titleTopInset = 20.dp, settingsInset = 16.dp,
        ),
    )

    val default: AppThemeDefinition get() = themes.first()
    fun find(id: String?): AppThemeDefinition = themes.firstOrNull { it.id == id } ?: default
}

private fun themeShapes(radius: Int) = Shapes(
    extraSmall = RoundedCornerShape((radius / 2).dp),
    small = RoundedCornerShape(radius.dp),
    medium = RoundedCornerShape(radius.dp),
    large = RoundedCornerShape((radius + 4).dp),
    extraLarge = RoundedCornerShape((radius + 8).dp),
)

private fun bottomSearch(inset: Int, corner: Int, height: Int = 56, elevation: Int) = SearchComponent(
    Alignment.BottomCenter, PaddingValues(horizontal = inset.dp, vertical = 8.dp),
    8.dp, (height + 48).dp, corner.dp, height.dp, elevation.dp,
)

private fun compactScrollbar() = ScrollbarComponent(
    8.dp, 4.dp, false, PaddingValues(top = 64.dp, end = 4.dp, bottom = 88.dp), 32.dp,
)

private fun topSearch(inset: Int, corner: Int, height: Int = 48) = SearchComponent(
    Alignment.TopCenter, PaddingValues(horizontal = inset.dp, vertical = 8.dp),
    8.dp, 24.dp, corner.dp, height.dp, 0.dp,
)
