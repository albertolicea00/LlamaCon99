package com.llamacon99.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle

private val avatarPalette = listOf(
    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784),
    Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC),
)

private fun colorForName(name: String): Color {
    if (name.isEmpty()) return avatarPalette.first()
    val index = (name.sumOf { it.code } % avatarPalette.size + avatarPalette.size) % avatarPalette.size
    return avatarPalette[index]
}

private fun initialsForName(name: String): String =
    name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

/** Circular avatar with profile-aware initials and grayscale photos for MonoUI. */
@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val palette = LocalAppTheme.current.style
    val monochrome = palette == ThemeStyle.MONO
    val material = palette == ThemeStyle.MATERIAL
    val apple = palette == ThemeStyle.APPLE
    val hyper = palette == ThemeStyle.HYPER
    val horizon = palette == ThemeStyle.HORIZON
    val colors = MaterialTheme.colorScheme
    val avatarColors = when {
        monochrome -> listOf(colors.onSurface, colors.onSurface)
        material -> listOf(Color(0xFFD5F5D9), Color(0xFFB9EFC0))
        apple -> listOf(Color(0xFF777986), Color(0xFF777986))
        hyper -> listOf(
            colors.surfaceContainerLow.copy(alpha = 0.96f),
            colors.surfaceContainerHigh.copy(alpha = 0.92f),
        )
        horizon -> listOf(Color(0xFFDCE8EF), Color(0xFFC9DCE7))
        palette == ThemeStyle.BRAND -> listOf(colorForName(name).copy(alpha = 0.65f), colorForName(name))
        else -> listOf(colors.primaryContainer, colors.secondaryContainer)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    avatarColors,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                colorFilter = if (monochrome) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text(
                text = if (material) name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty() else initialsForName(name),
                color = when {
                    monochrome -> colors.surface
                    material -> Color(0xFF38543C)
                    apple -> Color(0xFFFFF8F0)
                    hyper -> colors.onSurface
                    horizon -> Color(0xFF294454)
                    palette == ThemeStyle.BRAND -> Color.White
                    else -> colors.onPrimaryContainer
                },
                fontSize = (size.value / 2.4).sp,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            )
        }
    }
}
