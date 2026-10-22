package com.llamacon99.ui.contacts

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ContactComponents
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.llamacon99.R
import com.llamacon99.model.DeviceContact

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsListScreen(
    onOpenSettings: () -> Unit,
    quickDialMode: String? = null,
    viewModel: ContactsListViewModel = hiltViewModel(),
) {
    val appTheme = LocalAppTheme.current
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }
                ?.let { viewModel.onSearchQueryChanged(it.trim()) }
        }
    }
    var hasContactsPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    var permissionBlocked by rememberSaveable { mutableStateOf(false) }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasContactsPermission = granted
        permissionBlocked = !granted && activity?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS)
        } == true
        viewModel.reload()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted != hasContactsPermission) {
            hasContactsPermission = granted
            viewModel.reload()
        }
    }
    if (!hasContactsPermission) {
        ContactsPermissionScreen(
            openSettings = permissionBlocked,
            onOpenInfo = onOpenSettings,
            onGrantAccess = {
                if (permissionBlocked) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                } else {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
        )
        return
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        // Let the list draw behind navigation buttons; inset only the floating controls below.
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + appTheme.search.listTopInset,
                    bottom = appTheme.search.listBottomInset + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            ) {
                item(key = "contacts_title") {
                    Text(
                        text = stringResource(R.string.contacts_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, end = 88.dp, top = 12.dp, bottom = 20.dp),
                    )
                }
                if (uiState.isEmpty || (query.isNotBlank() && uiState.groups.isEmpty())) {
                    item(key = "empty_contacts") {
                        EmptyContactsState(
                            isSearch = query.isNotBlank(),
                            modifier = Modifier.fillParentMaxHeight(0.75f),
                        )
                    }
                }
                uiState.groups.forEach { group ->
                    stickyHeader(key = group.letter) {
                        Text(
                            text = group.letter,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(start = 40.dp, end = 24.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    itemsIndexed(group.contacts, key = { _, entry -> entry.id }) { index, entry ->
                        val first = index == 0
                        val last = index == group.contacts.lastIndex
                        val corner = appTheme.contacts.groupCorner
                        val shape = RoundedCornerShape(
                            topStart = if (first) corner else 0.dp,
                            topEnd = if (first) corner else 0.dp,
                            bottomStart = if (last) corner else 0.dp,
                            bottomEnd = if (last) corner else 0.dp,
                        )
                        SwipeableContactRow(
                            modifier = Modifier
                                .padding(horizontal = appTheme.contacts.groupInset)
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            enabled = uiState.anonymousSwipeEnabled,
                            onRevealAction = { viewModel.onDialAnonymous(entry.phoneNumber.number) },
                        ) {
                            Column {
                                ContactRow(
                                    contact = entry.contact,
                                    phoneNumber = entry.phoneNumber.number,
                                    phoneLabel = entry.phoneNumber.label,
                                    showPhoneLabel = entry.contact.numbers.size > 1,
                                    style = appTheme.contacts,
                                    onClick = {
                                        when (quickDialMode) {
                                            "anonymous" -> viewModel.onDialAnonymous(entry.phoneNumber.number)
                                            else -> viewModel.onDial(entry.phoneNumber.number)
                                        }
                                    },
                                )
                                if (!last) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ContactsScrollbar(
                listState = listState,
                groups = uiState.groups,
                modifier = Modifier.matchParentSize().navigationBarsPadding()
                    .padding(appTheme.scrollbar.railPadding),
            )
            ContactsScrollControls(
                listState = listState,
                groups = uiState.groups,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 16.dp),
                scrollToTopModifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding().padding(bottom = 68.dp),
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 20.dp)
                    .size(48.dp),
            ) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.settings_title))
            }
            Box(
                modifier = Modifier.navigationBarsPadding()
                    .align(appTheme.search.alignment)
                    .padding(appTheme.search.outerPadding)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TextField(
                    value = query,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text(stringResource(R.string.contacts_list_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                    .putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                    )
                                    .putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE,
                                        context.resources.configuration.locales[0].toLanguageTag(),
                                    )
                                    .putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        context.getString(R.string.contacts_list_voice_search),
                                    )
                                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                                try {
                                    voiceSearchLauncher.launch(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        R.string.contacts_list_voice_unavailable,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = stringResource(R.string.contacts_list_voice_search),
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(appTheme.search.corner),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                    .heightIn(min = appTheme.search.minHeight)
                    .shadow(appTheme.search.shadowElevation, RoundedCornerShape(appTheme.search.corner)),
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: DeviceContact,
    phoneNumber: String,
    phoneLabel: String,
    showPhoneLabel: Boolean,
    style: ContactComponents,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = style.rowMinHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = style.contentInset, vertical = 8.dp),
    ) {
        ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = style.avatarSize)
        Column(modifier = Modifier.weight(1f).padding(start = style.avatarGap)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp),
            ) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showPhoneLabel) {
                    Text(
                        text = phoneLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContactsState(isSearch: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSearch) Icons.Filled.SearchOff else Icons.Filled.PersonOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                if (isSearch) R.string.contacts_list_no_results_title else R.string.contacts_list_empty_title,
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(
                if (isSearch) R.string.contacts_list_no_results_subtitle else R.string.contacts_list_empty_subtitle,
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
