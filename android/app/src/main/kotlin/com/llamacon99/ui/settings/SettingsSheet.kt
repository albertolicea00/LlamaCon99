package com.llamacon99.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        ThemeStyle.HORIZON -> 24.dp
        ThemeStyle.HYPER -> 20.dp
        else -> 28.dp
    }
    val shape = RoundedCornerShape(topStart = corner, topEnd = corner)
    val body: @Composable ColumnScope.() -> Unit = {
        SheetHandle(style)
        content()
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest, sheetState = sheetState,
        containerColor = if (style == ThemeStyle.APPLE) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape, dragHandle = null,
    ) {
        if (style == ThemeStyle.APPLE) {
            SettingsGlass(shape = shape, modifier = Modifier.fillMaxWidth()) { Column(content = body) }
        } else {
            val surface = MaterialTheme.colorScheme.surfaceContainerLow
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            val background = when (style) {
                ThemeStyle.HYPER -> Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), surface, surface,
                ))
                else -> Brush.verticalGradient(listOf(surface, surface))
            }
            Column(
                Modifier.fillMaxWidth()
                    .background(background, shape)
                    .then(if (style == ThemeStyle.MONO || style == ThemeStyle.FLUENT || style == ThemeStyle.HORIZON)
                        Modifier.border(
                            BorderStroke(if (style == ThemeStyle.MONO) 1.dp else 0.7.dp, borderColor.copy(alpha = 0.8f)), shape,
                        ) else Modifier),
                content = body,
            )
        }
    }
}

@Composable
private fun ColumnScope.SheetHandle(style: ThemeStyle) {
    val width = when (style) { ThemeStyle.MONO -> 44.dp; ThemeStyle.FLUENT -> 28.dp; else -> 36.dp }
    val height = when (style) { ThemeStyle.MONO -> 2.dp; ThemeStyle.FLUENT -> 3.dp; else -> 4.dp }
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = when (style) { ThemeStyle.APPLE -> 0.28f; ThemeStyle.MONO -> 0.75f; else -> 0.4f },
    )
    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = if (style == ThemeStyle.MONO) 14.dp else 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(width).height(height).background(color, RoundedCornerShape(if (style == ThemeStyle.MONO) 0.dp else 50.dp)))
    }
}

/** Modal headings use the same typographic language as each theme's settings page. */
@Composable
internal fun SettingsSheetTitle(
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val style = LocalAppTheme.current.style
    val alignment = if (style == ThemeStyle.MATERIAL || style == ThemeStyle.HORIZON) Alignment.CenterHorizontally else Alignment.Start
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalAlignment = alignment,
    ) {
        if (style == ThemeStyle.MONO || style == ThemeStyle.FLUENT) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (style == ThemeStyle.MONO) 1.2.sp else 0.2.sp,
                    fontSize = if (style == ThemeStyle.MONO) 18.sp else 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                title,
                style = when (style) {
                    ThemeStyle.APPLE -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    ThemeStyle.HORIZON -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    ThemeStyle.HYPER -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    else -> MaterialTheme.typography.headlineSmall
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
        }
        if (style == ThemeStyle.FLUENT) {
            Spacer(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

/** A modal row is a theme-specific control, not a Material row with a different outline. */
@Composable
internal fun SettingsModalOption(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int = 0,
    enabled: Boolean = true,
    chevron: Boolean = false,
) {
    val style = LocalAppTheme.current.style
    val shape = when (style) {
        ThemeStyle.MONO -> RoundedCornerShape(0.dp)
        ThemeStyle.FLUENT -> RoundedCornerShape(4.dp)
        ThemeStyle.APPLE -> RoundedCornerShape(12.dp)
        ThemeStyle.HORIZON -> RoundedCornerShape(20.dp)
        ThemeStyle.HYPER -> RoundedCornerShape(18.dp)
        else -> RoundedCornerShape(24.dp)
    }
    val selectedBackground = when (style) {
        ThemeStyle.APPLE -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f)
        ThemeStyle.MATERIAL -> MaterialTheme.colorScheme.secondaryContainer
        ThemeStyle.HORIZON -> MaterialTheme.colorScheme.surfaceContainerHigh
        ThemeStyle.HYPER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ThemeStyle.MONO -> MaterialTheme.colorScheme.onSurface
        ThemeStyle.FLUENT -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val foreground = if (style == ThemeStyle.MONO && selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    val modifier = when (style) {
        ThemeStyle.APPLE -> Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
        ThemeStyle.MATERIAL -> Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
        ThemeStyle.HORIZON -> Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)
        ThemeStyle.HYPER -> Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
        ThemeStyle.MONO -> Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
        ThemeStyle.FLUENT -> Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp)
        ThemeStyle.BRAND -> Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (selected || style == ThemeStyle.MATERIAL && selected) selectedBackground else Color.Transparent,
        border = when {
            style == ThemeStyle.MONO -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            style == ThemeStyle.FLUENT -> BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            else -> null
        },
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth()) {
        when (style) {
            ThemeStyle.MONO -> Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("%02d".format(index + 1), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = foreground)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = foreground)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = 0.72f)) }
                }
                ModalOptionAccessory(selected, chevron, foreground)
            }
            ThemeStyle.FLUENT -> Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(32.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal), color = foreground)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                ModalOptionAccessory(selected, chevron, foreground)
            }
            ThemeStyle.HORIZON -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = foreground, modifier = Modifier.weight(1f))
                    ModalOptionAccessory(selected, chevron, foreground)
                }
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp, end = 28.dp)) }
            }
            ThemeStyle.HYPER -> Row(
                Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(4.dp).height(38.dp).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = foreground)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                ModalOptionAccessory(selected, chevron, foreground)
            }
            ThemeStyle.MATERIAL -> ListItem(
                headlineContent = { Text(title, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal) },
                supportingContent = subtitle?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
                trailingContent = {
                    if (chevron) ModalOptionAccessory(false, true, MaterialTheme.colorScheme.primary)
                    else RadioButton(selected = selected, onClick = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth(),
            )
            ThemeStyle.APPLE -> Row(
                Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = foreground)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp)) }
                }
                ModalOptionAccessory(selected, chevron, MaterialTheme.colorScheme.primary)
            }
            ThemeStyle.BRAND -> Row(
                Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium), color = foreground)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp)) }
                }
                ModalOptionAccessory(selected, chevron, MaterialTheme.colorScheme.primary)
            }
        }
            if (style == ThemeStyle.APPLE) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun ModalOptionAccessory(selected: Boolean, chevron: Boolean, color: Color) {
    when {
        chevron -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        selected -> Icon(Icons.Filled.Check, contentDescription = null, tint = color)
        else -> Unit
    }
}
