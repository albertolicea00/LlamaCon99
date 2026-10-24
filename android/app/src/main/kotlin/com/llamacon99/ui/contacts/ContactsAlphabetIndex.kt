package com.llamacon99.ui.contacts

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.llamacon99.R
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A persistent iOS-style A–Z rail. Tapping or scrubbing jumps to section headers. */
@Composable
internal fun ContactsAlphabetIndex(
    listState: LazyListState,
    groups: List<AlphabetGroup>,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) return
    val letters = remember { ('A'..'Z').map(Char::toString) + "#" }
    val targets = remember(groups) { contactSectionTargets(groups) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var activeLetterY by remember { mutableFloatStateOf(0f) }
    val description = stringResource(R.string.contacts_alphabet_index)
    val style = LocalAppTheme.current.style
    val mono = style == ThemeStyle.MONO
    val hyper = style == ThemeStyle.HYPER
    val density = LocalDensity.current
    BoxWithConstraints(modifier.width(if (hyper) 104.dp else 32.dp)) {
        val railHeight = maxHeight
        val visibleLetters = if (railHeight < 360.dp) targets.map { it.letter } else letters
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).width(32.dp).fillMaxHeight()
                .padding(vertical = if (style == ThemeStyle.HORIZON) 16.dp else 0.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    customActions = targets.map { target ->
                        CustomAccessibilityAction(target.letter) {
                            scope.launch { listState.scrollToItem(target.itemIndex) }
                            true
                        }
                    }
                }
                .pointerInput(visibleLetters, targets) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var job: Job? = null
                        fun jump(y: Float) {
                            activeLetterY = y
                            val index = (y / size.height.coerceAtLeast(1) * visibleLetters.size).toInt()
                                .coerceIn(visibleLetters.indices)
                            val letter = visibleLetters[index]
                            if (activeLetter == letter) return
                            activeLetter = letter
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            val target = sectionTargetForLetter(letter, targets) ?: return
                            job?.cancel()
                            job = scope.launch { listState.scrollToItem(target) }
                        }
                        try {
                            jump(down.position.y)
                            drag(down.id) { change -> change.consume(); jump(change.position.y) }
                        } finally {
                            activeLetter = null
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            visibleLetters.forEach { letter ->
                val letterDistance = if (style == ThemeStyle.HORIZON && activeLetter != null) {
                    kotlin.math.abs(visibleLetters.indexOf(letter) - visibleLetters.indexOf(activeLetter))
                } else Int.MAX_VALUE
                val horizonZoom by animateFloatAsState(
                    targetValue = when {
                        letterDistance == Int.MAX_VALUE || letterDistance > 3 -> 1f
                        railHeight < 360.dp -> when (letterDistance) {
                            0 -> 2.8f
                            1 -> 2f
                            2 -> 1.4f
                            else -> 1f
                        }
                        else -> when (letterDistance) {
                            0 -> 3.3f
                            1 -> 2.4f
                            2 -> 1.7f
                            else -> 1.1f
                        }
                    },
                    label = "horizonAlphabetLetterSize",
                )
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .zIndex(if (letterDistance <= 3) (4 - letterDistance).toFloat() else 0f)
                        .graphicsLayer {
                            if (style == ThemeStyle.HORIZON) {
                                scaleX = horizonZoom
                                scaleY = horizonZoom
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (mono) {
                        DotMatrixIndexLetter(letter, MaterialTheme.colorScheme.primary, activeLetter == letter)
                    } else {
                        Text(
                            letter,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (activeLetter == letter) FontWeight.Black else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (hyper) {
            activeLetter?.let { letter ->
                val bubbleY = with(density) {
                    (activeLetterY - 36.dp.toPx()).coerceIn(0f, (railHeight - 72.dp).toPx().coerceAtLeast(0f)).toDp()
                }
                Box(
                    Modifier.align(Alignment.TopStart).offset(y = bubbleY).size(72.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, fontSize = 36.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
