package com.llamacon99.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle

/** Preference rows retain their semantics/actions while adopting the profile's own anatomy. */
@Composable
internal fun SettingsRow(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
) {
    val style = LocalAppTheme.current.style
    if (style == ThemeStyle.MATERIAL || style == ThemeStyle.HORIZON) {
        ListItem(headlineContent = headlineContent, supportingContent = supportingContent,
            leadingContent = leadingContent, trailingContent = trailingContent, colors = colors, modifier = modifier)
        return
    }
    val compact = style == ThemeStyle.APPLE || style == ThemeStyle.FLUENT || style == ThemeStyle.HYPER
    val titleStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = when (style) { ThemeStyle.APPLE -> 17.sp; ThemeStyle.FLUENT -> 14.sp; else -> 16.sp },
        fontWeight = when (style) { ThemeStyle.HYPER, ThemeStyle.HORIZON -> FontWeight.Medium; else -> FontWeight.Normal },
    )
    val detailStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = if (style == ThemeStyle.FLUENT) 12.sp else 13.sp, lineHeight = 18.sp,
    )
    Row(modifier.fillMaxWidth().heightIn(min = if (compact) 56.dp else 68.dp)
        .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (leadingContent != null) {
            Box(Modifier.padding(end = 12.dp), contentAlignment = Alignment.Center) { leadingContent() }
        }
        Column(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                ProvideTextStyle(titleStyle) { headlineContent() }
            }
            if (supportingContent != null) {
                Spacer(Modifier.height(3.dp))
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(detailStyle) { supportingContent() }
                }
            }
        }
        if (trailingContent != null) {
            Box(Modifier.padding(start = 16.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    trailingContent()
                }
            }
        }
    }
}
