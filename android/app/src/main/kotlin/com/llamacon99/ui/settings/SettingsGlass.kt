package com.llamacon99.ui.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/** Background-only and scrolling-content layers remain separate to avoid capture cycles. */
internal data class SettingsGlassBackdrop(val layer: GraphicsLayer, val origin: Offset)
internal val LocalSettingsGlass = staticCompositionLocalOf<SettingsGlassBackdrop?> { null }

/** Blur only the captured backdrop: labels, icons and touch targets stay sharp. */
@Composable
internal fun SettingsGlass(
    shape: Shape,
    modifier: Modifier = Modifier,
    backdrop: SettingsGlassBackdrop? = LocalSettingsGlass.current,
    content: @Composable BoxScope.() -> Unit,
) {
    val blurred = rememberGraphicsLayer()
    var position by remember { mutableStateOf(Offset.Zero) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backdrop != null
    val tint = if (dark) Color(0xFF1C1C1E) else Color.White
    val highlight = Brush.linearGradient(listOf(
        Color.White.copy(alpha = if (dark) 0.35f else 0.90f),
        Color.White.copy(alpha = if (dark) 0.07f else 0.22f),
        Color.White.copy(alpha = if (dark) 0.18f else 0.65f),
    ))
    Box(
        modifier.onGloballyPositioned { position = it.positionInRoot() }
            .clip(shape)
            .drawWithContent {
                if (supported && !backdrop.layer.isReleased) {
                    val offset = position - backdrop.origin
                    blurred.renderEffect = BlurEffect(20.dp.toPx(), 20.dp.toPx(), TileMode.Clamp)
                    blurred.record {
                        // Offset the scene to show the region directly beneath this control.
                        drawContext.canvas.save()
                        drawContext.canvas.translate(-offset.x, -offset.y)
                        drawLayer(backdrop.layer)
                        drawContext.canvas.restore()
                    }
                    drawLayer(blurred)
                }
                drawRect(tint.copy(alpha = if (supported) { if (dark) 0.68f else 0.62f } else 0.97f))
                drawContent()
            }
            .border(BorderStroke(0.75.dp, highlight), shape),
        content = content,
    )
}
