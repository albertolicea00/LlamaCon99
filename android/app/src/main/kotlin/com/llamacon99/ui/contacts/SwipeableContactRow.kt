package com.llamacon99.ui.contacts

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Reveals a hidden "call anonymously" action when dragged left, mirroring iOS's
 * `.swipeActions(edge: .leading)` — unlike Material's [androidx.compose.material3.SwipeToDismissBox],
 * this never removes the row, it only slides it to expose the action underneath and springs back.
 */
@Composable
fun SwipeableContactRow(
    enabled: Boolean,
    onRevealAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var rowWidthPx by remember { mutableStateOf(0) }
    val triggerThreshold = remember(rowWidthPx) { rowWidthPx * 0.35f }

    Box(modifier = modifier.onSizeChanged { rowWidthPx = it.width }) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 20.dp),
            )
        }

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(enabled) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val triggered = offsetX.value < -triggerThreshold
                                if (triggered) {
                                    offsetX.animateTo(-rowWidthPx.toFloat())
                                    onRevealAction()
                                }
                                offsetX.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount)
                                    .coerceIn(-rowWidthPx.toFloat(), 0f)
                                offsetX.snapTo(next)
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
