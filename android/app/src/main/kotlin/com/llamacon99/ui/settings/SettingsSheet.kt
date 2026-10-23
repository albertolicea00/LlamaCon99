package com.llamacon99.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle

/** Selection and legal sheets follow the same material and corners as their settings page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalAppTheme.current.style
    val corner = when (style) {
        ThemeStyle.MONO -> 0.dp
        ThemeStyle.FLUENT -> 8.dp
        ThemeStyle.APPLE -> 32.dp
        else -> 28.dp
    }
    val shape = RoundedCornerShape(topStart = corner, topEnd = corner)
    val body: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(if (style == ThemeStyle.MONO) 2.dp else 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(corner)))
        }
        content()
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest, sheetState = sheetState,
        containerColor = if (style == ThemeStyle.APPLE) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape, dragHandle = null,
    ) {
        if (style == ThemeStyle.APPLE) {
            SettingsGlass(shape = shape, modifier = Modifier.fillMaxWidth()) { Column(content = body) }
        } else Column(content = body)
    }
}
