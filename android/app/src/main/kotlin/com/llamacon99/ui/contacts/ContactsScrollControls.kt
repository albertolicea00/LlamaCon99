package com.llamacon99.ui.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.llamacon99.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ContactsScrollControls(
    listState: LazyListState,
    groups: List<AlphabetGroup>,
    modifier: Modifier = Modifier,
    scrollToTopModifier: Modifier = Modifier,
) {
    val sections = remember(groups) {
        var index = 1 // The title is the first list item.
        groups.map { group ->
            (index to group.letter).also { index += group.contacts.size + 1 }
        }
    }
    val hasScrolled by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val currentLetter by remember(listState, sections) {
        derivedStateOf {
            sections.lastOrNull { it.first <= listState.firstVisibleItemIndex }?.second.orEmpty()
        }
    }
    val isScrolling = listState.isScrollInProgress
    var showControls by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(hasScrolled, isScrolling) {
        showControls = hasScrolled
        if (hasScrolled && !isScrolling) {
            delay(2_000)
            showControls = false
        }
    }

    val visible = hasScrolled && (showControls || isScrolling)
    val background = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val description = stringResource(R.string.contacts_list_current_section, currentLetter)
        Box(
            modifier = Modifier.size(48.dp).background(background, MaterialTheme.shapes.large)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Text(currentLetter, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = scrollToTopModifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.size(48.dp).background(background, MaterialTheme.shapes.large),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_md_keyboard_double_arrow_up),
                contentDescription = stringResource(R.string.contacts_list_scroll_to_top),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
