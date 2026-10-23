package com.llamacon99.ui.contacts

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val haptics = LocalHapticFeedback.current
    var activeLetter by remember { mutableStateOf<String?>(null) }
    val description = stringResource(R.string.contacts_alphabet_index)
    val mono = LocalAppTheme.current.style == ThemeStyle.MONO
    BoxWithConstraints(modifier.width(32.dp)) {
        val railHeight = maxHeight
        val visibleLetters = if (railHeight < 360.dp) targets.map { it.letter } else letters
        Column(
            modifier = Modifier.fillMaxSize()
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
                            val index = (y / size.height.coerceAtLeast(1) * visibleLetters.size).toInt()
                                .coerceIn(visibleLetters.indices)
                            val letter = visibleLetters[index]
                            if (activeLetter == letter) return
                            activeLetter = letter
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (mono) {
                        DotMatrixIndexLetter(letter, MaterialTheme.colorScheme.primary, activeLetter == letter)
                    } else {
                    Text(letter, fontSize = 11.sp, lineHeight = 12.sp,
                        fontWeight = if (activeLetter == letter) FontWeight.Black else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
