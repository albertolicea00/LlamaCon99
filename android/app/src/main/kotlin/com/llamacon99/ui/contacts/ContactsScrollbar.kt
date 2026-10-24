package com.llamacon99.ui.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ContactsScrollbar(
    listState: LazyListState,
    groups: List<AlphabetGroup>,
    modifier: Modifier = Modifier,
) {
    val labels = remember(groups) {
        buildList {
            add("#") // The title occupies the first list item.
            groups.forEach { group ->
                add(group.letter)
                repeat(group.contacts.size) { add(group.letter) }
            }
        }
    }
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }
    if (!canScroll || groups.isEmpty()) return

    val scope = rememberCoroutineScope()
    var draggedProgress by remember(groups) { mutableStateOf<Float?>(null) }
    var bubbleProgress by remember(groups) { mutableStateOf(0f) }
    var selectedLetter by remember(groups) { mutableStateOf("") }
    var showScrollbar by remember(groups) { mutableStateOf(false) }
    val isDragging = draggedProgress != null
    val isScrolling = listState.isScrollInProgress
    LaunchedEffect(isScrolling, isDragging) {
        if (isScrolling || isDragging) {
            showScrollbar = true
        } else {
            delay(2_000)
            showScrollbar = false
        }
    }
    val visibility by animateFloatAsState(
        targetValue = if (showScrollbar || isScrolling || isDragging) 1f else 0f,
        animationSpec = tween(250),
        label = "scrollbarVisibility",
    )
    val listProgress = scrollbarProgress(listState)
    val thumbProgress by animateFloatAsState(
        targetValue = draggedProgress ?: listProgress,
        animationSpec = if (isDragging) snap() else spring(dampingRatio = 0.9f, stiffness = 320f),
        label = "scrollbarThumbPosition",
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val activeColor = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    val bubbleColor = if (theme.style == ThemeStyle.MATERIAL) MaterialTheme.colorScheme.primaryContainer else activeColor
    val bubbleContentColor = if (theme.style == ThemeStyle.MATERIAL) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    val density = LocalDensity.current

    Box(modifier) {
        BoxWithConstraints(Modifier.align(Alignment.CenterEnd).fillMaxWidth().fillMaxHeight(0.72f)) {
            val heightPx = with(density) { maxHeight.toPx() }
            val minThumbPx = with(density) { theme.scrollbar.minThumbHeight.toPx() }
            val bubblePx = with(density) { 72.dp.toPx() }

            Canvas(Modifier.matchParentSize().alpha(visibility)) {
                val (_, thumbHeight) = scrollbarPosition(listState, size.height, minThumbPx)
                val width = theme.scrollbar.width.toPx()
                val left = size.width - width
                val centerX = left + width / 2
                val corner = with(density) { theme.scrollbar.corner.toPx() }.coerceAtMost(width / 2)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(corner),
                )
                drawRoundRect(
                    color = activeColor.copy(alpha = if (isDragging) 0.55f else 0.18f),
                    topLeft = Offset(left, (size.height - thumbHeight) * thumbProgress),
                    size = Size(width, thumbHeight),
                    cornerRadius = CornerRadius(corner),
                )
                if (theme.scrollbar.showSectionMarkers) {
                    val inset = width / 2
                    val markerCount = groups.size.coerceIn(2, 26)
                    repeat(markerCount) { index ->
                        drawCircle(
                            color = markerColor,
                            radius = 0.65.dp.toPx(),
                            center = Offset(centerX, inset + (size.height - width).coerceAtLeast(0f) * index / (markerCount - 1)),
                        )
                    }
                }
            }
            Box(
                Modifier.align(Alignment.CenterEnd).width(32.dp).fillMaxHeight()
                    .pointerInput(listState, labels, heightPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            val (initialProgress, thumbHeight) = scrollbarPosition(listState, heightPx, minThumbPx)
                            val track = (heightPx - thumbHeight).coerceAtLeast(1f)
                            val thumbTop = initialProgress * track
                            val grip = if (down.position.y in thumbTop..(thumbTop + thumbHeight)) {
                                down.position.y - thumbTop
                            } else {
                                thumbHeight / 2
                            }
                            var scrollJob: Job? = null
                            fun moveTo(y: Float) {
                                val progress = ((y - grip) / track).coerceIn(0f, 1f)
                                val index = (progress * labels.lastIndex).roundToInt()
                                draggedProgress = progress
                                bubbleProgress = progress
                                selectedLetter = labels[index]
                                scrollJob?.cancel()
                                scrollJob = scope.launch { listState.scrollToItem(index) }
                            }
                            try {
                                moveTo(down.position.y)
                                drag(down.id) { change ->
                                    change.consume()
                                    moveTo(change.position.y)
                                }
                            } finally {
                                draggedProgress = null
                            }
                        }
                    },
            )
            AnimatedVisibility(
                visible = isDragging && theme.style != ThemeStyle.FLUENT,
                modifier = Modifier.align(Alignment.TopEnd),
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(250)),
            ) {
                val (_, thumbHeight) = scrollbarPosition(listState, heightPx, minThumbPx)
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                x = with(density) { -40.dp.roundToPx() },
                                y = ((heightPx - thumbHeight) * bubbleProgress + thumbHeight / 2 - bubblePx / 2)
                                    .coerceIn(0f, (heightPx - bubblePx).coerceAtLeast(0f)).roundToInt(),
                            )
                        }
                        .size(72.dp)
                        .background(
                            bubbleColor,
                            if (theme.style == ThemeStyle.MATERIAL) CircleShape else RoundedCornerShape(36.dp, 36.dp, 8.dp, 36.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selectedLetter,
                        color = bubbleContentColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun scrollbarPosition(state: LazyListState, height: Float, minimumThumb: Float): Pair<Float, Float> {
    val progress = scrollbarProgress(state)
    val layout = state.layoutInfo
    val first = layout.visibleItemsInfo.firstOrNull { it.index == state.firstVisibleItemIndex }
        ?: return 0f to height
    val last = layout.visibleItemsInfo.maxByOrNull { it.index } ?: return 0f to height
    val start = first.index + state.firstVisibleItemScrollOffset.toFloat() / first.size.coerceAtLeast(1)
    val end = last.index +
        ((layout.viewportEndOffset - last.offset).toFloat() / last.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    val visible = (end - start).coerceAtLeast(1f)
    val total = layout.totalItemsCount.coerceAtLeast(1)
    val thumbHeight = (height * visible / total)
        .coerceIn(minimumThumb.coerceAtMost(height), (minimumThumb * 1.5f).coerceAtMost(height))
    return progress to thumbHeight
}

private fun scrollbarProgress(state: LazyListState): Float {
    val layout = state.layoutInfo
    val first = layout.visibleItemsInfo.firstOrNull { it.index == state.firstVisibleItemIndex }
        ?: return 0f
    val last = layout.visibleItemsInfo.maxByOrNull { it.index } ?: return 0f
    val start = first.index + state.firstVisibleItemScrollOffset.toFloat() / first.size.coerceAtLeast(1)
    val end = last.index +
        ((layout.viewportEndOffset - last.offset).toFloat() / last.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    val visible = (end - start).coerceAtLeast(1f)
    val total = layout.totalItemsCount.coerceAtLeast(1)
    return (start / (total - visible).coerceAtLeast(1f)).coerceIn(0f, 1f)
}
