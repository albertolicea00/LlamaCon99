package com.llamacon99.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Shapes
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

/** Register a theme here; each UI component reads its own style from this single catalog. */
data class AppThemeDefinition(
    val id: String,
    val nameRes: Int,
    val inspirationRes: Int,
    val shapes: Shapes,
    val contacts: ContactComponents,
    val search: SearchComponent,
    val scrollbar: ScrollbarComponent,
)

object AppThemeCatalog {
    val themes = listOf(
        AppThemeDefinition(
            id = "llama", nameRes = R.string.settings_theme_llama, inspirationRes = R.string.settings_theme_inspired_samsung,
            shapes = Shapes(extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(30.dp), extraLarge = RoundedCornerShape(36.dp)),
            contacts = ContactComponents(groupCorner = 36.dp, groupInset = 24.dp, contentInset = 16.dp, rowMinHeight = 64.dp, avatarSize = 38.dp, avatarGap = 12.dp),
            search = SearchComponent(Alignment.BottomCenter, PaddingValues(horizontal = 24.dp, vertical = 4.dp), 8.dp, 104.dp, 50.dp, 56.dp, 8.dp),
            scrollbar = ScrollbarComponent(12.dp, 6.dp, true, PaddingValues(top = 64.dp, end = 6.dp, bottom = 80.dp), 32.dp),
        ),
    )

    val default: AppThemeDefinition get() = themes.first()
    fun find(id: String?): AppThemeDefinition = themes.firstOrNull { it.id == id } ?: default
}
