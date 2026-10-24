@file:OptIn(ExperimentalLayoutApi::class)

package com.llamacon99.ui.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llamacon99.R
import com.llamacon99.ui.theme.AppThemeCatalog
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle
import kotlinx.coroutines.launch

/** Separate screen structures, sharing only contact data and actions. */
@Composable
internal fun PlatformContactsScreen(
    uiState: ContactsListUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onDial: (String) -> Unit,
    onDialAnonymous: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = { scope.launch { listState.animateScrollToItem(0) } }
    val style = LocalAppTheme.current.style
    val search: @Composable (Modifier) -> Unit = { modifier ->
        ProfileSearchField(query, onQueryChange, onVoiceSearch, modifier)
    }
    val list: @Composable (Modifier) -> Unit = { modifier ->
        ProfileContactList(uiState, query, listState, modifier, onDial, onDialAnonymous)
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    when (style) {
        ThemeStyle.APPLE -> AppleContactsLayout(listState, uiState.groups, search, list, onOpenSettings, scrollToTop)
        ThemeStyle.MATERIAL -> PixelContactsLayout(listState, search, list, onOpenSettings, scrollToTop)
        ThemeStyle.HORIZON -> HorizonContactsLayout(search, list, onOpenSettings, scrollToTop)
        ThemeStyle.HYPER -> HyperContactsLayout(listState, search, list, onOpenSettings, scrollToTop)
        ThemeStyle.MONO -> MonoContactsLayout(search, list, onOpenSettings, scrollToTop)
        ThemeStyle.FLUENT -> FluentContactsLayout(listState, search, list, onOpenSettings, scrollToTop)
        ThemeStyle.BRAND -> error("SamUI uses its one-handed contacts layout")
    }
    }
}

@Composable
private fun AppleContactsLayout(
    listState: LazyListState,
    groups: List<AlphabetGroup>,
    search: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: () -> Unit,
) {
    val titleInteraction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 48.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.contacts_list_title),
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp).clickable(
                    interactionSource = titleInteraction, indication = null, onClick = scrollToTop,
                ).semantics { heading() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Info, stringResource(R.string.settings_title), tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
            }
        }
        search(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            list(Modifier.fillMaxSize())
            val imeVisible = WindowInsets.isImeVisible
            androidx.compose.animation.AnimatedVisibility(!imeVisible,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = fadeIn(), exit = fadeOut()) {
                ContactsAlphabetIndex(listState, groups,
                    Modifier.padding(vertical = 16.dp).heightIn(max = 360.dp).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun PixelContactsLayout(
    listState: LazyListState,
    search: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            search(Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.settings_title))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            list(Modifier.fillMaxSize())
            PlatformGoTopButton(listState, scrollToTop, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun HorizonContactsLayout(
    search: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeading(onSettings = onSettings, onTitleClick = scrollToTop)
        search(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        list(Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun HyperContactsLayout(
    listState: LazyListState,
    search: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeading(onSettings = onSettings, overflow = true, onTitleClick = scrollToTop)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            list(Modifier.fillMaxSize())
            PlatformGoTopButton(
                listState,
                scrollToTop,
                Modifier.align(Alignment.BottomCenter).padding(bottom = LocalAppTheme.current.search.minHeight + 24.dp),
            )
            search(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MonoContactsLayout(
    search: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: () -> Unit,
) {
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                val gap = 8.dp.toPx()
                var x = size.width * 0.62f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) { drawCircle(dotColor, 1.dp.toPx(), Offset(x, y)); y += gap }
                    x += gap
                }
            }
            ScreenHeading(onSettings = onSettings, onTitleClick = scrollToTop)
        }
        search(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
        HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface)
        list(Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun FluentContactsLayout(
    listState: LazyListState,
    search: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Column(Modifier.width(216.dp).fillMaxHeight().padding(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 24.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable(onClick = scrollToTop)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(20.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Text(stringResource(R.string.contacts_list_title), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, null); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.settings_title))
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp)) {
                ScreenHeading(onSettings = onSettings, horizontalInset = 0, onTitleClick = scrollToTop)
                search(Modifier.fillMaxWidth().padding(bottom = 16.dp))
                Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 16.dp)) {
                    Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        list(Modifier.fillMaxSize())
                    }
                    PlatformGoTopButton(listState, scrollToTop, Modifier.align(Alignment.BottomEnd).padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ScreenHeading(
    onSettings: () -> Unit,
    overflow: Boolean = false,
    horizontalInset: Int = 20,
    onTitleClick: (() -> Unit)? = null,
) {
    val titleInteraction = remember { MutableInteractionSource() }
    Row(Modifier.fillMaxWidth().padding(start = horizontalInset.dp, end = horizontalInset.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (LocalAppTheme.current.style == ThemeStyle.MONO) {
            DotMatrixHeading(stringResource(R.string.contacts_list_title), Modifier.weight(1f).clickable(
                enabled = onTitleClick != null, interactionSource = titleInteraction, indication = null,
            ) { onTitleClick?.invoke() })
        } else {
        Text(stringResource(R.string.contacts_list_title), style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).semantics { heading() }
                .clickable(enabled = onTitleClick != null, interactionSource = titleInteraction, indication = null) {
                    onTitleClick?.invoke()
                }, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onSettings) {
            Icon(if (overflow) Icons.Filled.MoreHoriz else Icons.Outlined.Settings, stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun PlatformGoTopButton(listState: LazyListState, scrollToTop: () -> Unit, modifier: Modifier = Modifier) {
    val style = LocalAppTheme.current.style
    val view = LocalView.current
    val hapticScrollToTop = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); scrollToTop() }
    val visible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        if (style == ThemeStyle.FLUENT) {
            OutlinedButton(onClick = hapticScrollToTop, shape = RoundedCornerShape(4.dp)) {
                Icon(painterResource(R.drawable.ic_md_keyboard_double_arrow_up), null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.contacts_list_go_top))
            }
        } else if (style == ThemeStyle.HYPER) {
            FloatingActionButton(
                onClick = hapticScrollToTop,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 5.dp),
            ) {
                Icon(painterResource(R.drawable.ic_md_keyboard_double_arrow_up), stringResource(R.string.contacts_list_go_top))
            }
        } else {
            ExtendedFloatingActionButton(
                onClick = hapticScrollToTop,
                icon = { Icon(painterResource(R.drawable.ic_md_keyboard_double_arrow_up), null) },
                text = { Text(stringResource(R.string.contacts_list_go_top)) },
            )
        }
    }
}

@Composable
private fun ProfileSearchField(query: String, onQuery: (String) -> Unit, onVoice: () -> Unit, modifier: Modifier) {
    val theme = LocalAppTheme.current
    val focus = LocalFocusManager.current
    val fluent = theme.style == ThemeStyle.FLUENT
    val mono = theme.style == ThemeStyle.MONO
    val horizon = theme.style == ThemeStyle.HORIZON
    val hyper = theme.style == ThemeStyle.HYPER
    var focused by remember { mutableStateOf(false) }
    val fieldColor = if (hyper) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f)
        else MaterialTheme.colorScheme.surfaceVariant
    val underlineColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val underlineStroke = if (focused) 2.dp else 1.dp
    BasicTextField(
        value = query, onValueChange = onQuery, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = modifier.heightIn(min = theme.search.minHeight)
            .then(if (horizon) {
                Modifier.onFocusChanged { focused = it.isFocused }
                    .drawBehind {
                        val stroke = underlineStroke.toPx()
                        drawLine(
                            color = underlineColor,
                            start = Offset(0f, size.height - stroke / 2f),
                            end = Offset(size.width, size.height - stroke / 2f),
                            strokeWidth = stroke,
                        )
                    }
            } else Modifier.clip(RoundedCornerShape(theme.search.corner)).background(fieldColor))
            .then(if ((fluent || mono) && !horizon) Modifier.border(
                if (mono) 1.dp else 0.5.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(theme.search.corner),
            ) else Modifier),
        decorationBox = { field ->
            Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 10.dp)) {
                    if (query.isEmpty()) Text(stringResource(R.string.contacts_list_search_hint),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    field()
                }
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.contacts_clear_search), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onVoice) {
                    Icon(Icons.Filled.Mic, stringResource(R.string.contacts_list_voice_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        },
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProfileContactList(
    state: ContactsListUiState, query: String, listState: LazyListState, modifier: Modifier,
    onDial: (String) -> Unit, onAnonymous: (String) -> Unit,
) {
    val theme = LocalAppTheme.current
    val apple = theme.style == ThemeStyle.APPLE
    val indexed = theme.style in setOf(ThemeStyle.APPLE, ThemeStyle.HORIZON, ThemeStyle.HYPER, ThemeStyle.MONO)
    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (theme.style == ThemeStyle.HYPER) theme.search.minHeight + 32.dp else 24.dp),
        ) {
            item(key = "contacts_title") {
                when (theme.style) {
                    ThemeStyle.APPLE -> Spacer(Modifier.height(8.dp))
                    ThemeStyle.MATERIAL -> Text(stringResource(R.string.contacts_list_title),
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                    else -> Spacer(Modifier.height(4.dp))
                }
            }
            if (state.isEmpty || (query.isNotBlank() && state.groups.isEmpty())) {
                item { EmptyContactsState(query.isNotBlank(), Modifier.fillParentMaxHeight(0.75f)) }
            }
            state.groups.forEach { group ->
                stickyHeader(key = "section:${group.letter}") {
                    Row(Modifier.fillMaxWidth().background(if (theme.style == ThemeStyle.FLUENT)
                        MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.background)
                        .padding(start = if (apple) 20.dp else 24.dp, end = 36.dp, top = if (apple) 4.dp else 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(group.letter, style = if (theme.style == ThemeStyle.MATERIAL)
                            MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
                        if (apple || theme.style == ThemeStyle.FLUENT) HorizontalDivider(
                            Modifier.weight(1f).padding(start = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                itemsIndexed(group.contacts, key = { _, row -> row.id }) { index, entry ->
                    val materialRows = theme.style == ThemeStyle.MATERIAL
                    val horizon = theme.style == ThemeStyle.HORIZON
                    val shape = RoundedCornerShape(if (materialRows) 6.dp else 0.dp)
                    val cardInset = if (materialRows) 14.dp else 0.dp
                    val rowBackground = when {
                        materialRows -> MaterialTheme.colorScheme.surfaceContainerLow
                        theme.style == ThemeStyle.FLUENT -> MaterialTheme.colorScheme.surfaceContainerLow
                        else -> MaterialTheme.colorScheme.background
                    }
                    SwipeableContactRow(
                        state.anonymousSwipeEnabled, { onAnonymous(entry.phoneNumber.number) },
                        Modifier.padding(start = cardInset, end = if (indexed) 28.dp else cardInset)
                            .padding(vertical = if (materialRows) 2.dp else 0.dp)
                            .fillMaxWidth().clip(shape).background(rowBackground),
                    ) {
                        Column(Modifier.background(rowBackground)) {
                            PlatformContactRow(entry, { onDial(entry.phoneNumber.number) })
                            if (index != group.contacts.lastIndex && !apple && (horizon || (!materialRows && theme.style != ThemeStyle.MONO))) {
                                val dividerStart = when {
                                    apple -> 20.dp
                                    horizon -> 60.dp
                                    else -> 72.dp
                                }
                                HorizontalDivider(Modifier.padding(start = dividerStart, end = 12.dp),
                                    thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
        val imeVisible = WindowInsets.isImeVisible
        if (indexed && !apple) {
            androidx.compose.animation.AnimatedVisibility(!imeVisible,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = fadeIn(), exit = fadeOut()) {
                ContactsAlphabetIndex(listState, state.groups, Modifier.padding(vertical = 12.dp).fillMaxHeight(0.88f))
            }
        } else if (!indexed) {
            ContactsScrollbar(listState, state.groups, Modifier.matchParentSize().padding(end = 4.dp, top = 48.dp, bottom = 16.dp))
        }
    }
}

@Composable
private fun PlatformContactRow(entry: ContactNumberEntry, onDial: () -> Unit) {
    val theme = LocalAppTheme.current
    val apple = theme.style == ThemeStyle.APPLE
    val components = if (theme.style == ThemeStyle.HORIZON) AppThemeCatalog.find("material").contacts else theme.contacts
    Row(Modifier.fillMaxWidth().heightIn(min = components.rowMinHeight).clickable(onClick = onDial)
        .padding(horizontal = components.contentInset, vertical = if (apple) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (components.avatarSize > 0.dp) ContactAvatar(entry.contact.name, entry.contact.photoUri, size = components.avatarSize)
        Column(Modifier.weight(1f).padding(start = if (components.avatarSize > 0.dp) components.avatarGap else 0.dp)) {
            Text(entry.contact.name, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (entry.contact.numbers.size > 1) "${entry.phoneNumber.label} · ${entry.phoneNumber.number}" else entry.phoneNumber.number,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
