package com.llamacon99.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.llamacon99.R
import com.llamacon99.model.DeviceContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    onOpenSettings: () -> Unit,
    viewModel: ContactsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    var searchActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasContactsPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasContactsPermission = granted
        if (granted) viewModel.reload()
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasContactsPermission) contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.contacts_list_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onSearch = { searchActive = false },
                        expanded = searchActive,
                        onExpandedChange = { searchActive = it },
                        placeholder = { Text(stringResource(R.string.contacts_list_search_hint)) },
                    )
                },
                expanded = searchActive,
                onExpandedChange = { searchActive = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {}

            if (uiState.isEmpty) {
                EmptyContactsState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    uiState.groups.forEach { group ->
                        stickyHeader(key = group.letter) {
                            Text(
                                text = group.letter,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(group.contacts, key = { it.id }) { contact ->
                            SwipeableContactRow(
                                enabled = uiState.anonymousSwipeEnabled,
                                onRevealAction = { viewModel.onDialAnonymous(contact.phoneNumber) },
                            ) {
                                ContactRow(
                                    contact = contact,
                                    onClick = { viewModel.onDial(contact.phoneNumber) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: DeviceContact, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(contact.name) },
        supportingContent = { Text(contact.phoneNumber) },
        leadingContent = { ContactAvatar(name = contact.name, photoUri = contact.photoUri) },
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun EmptyContactsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.contacts_list_empty_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.contacts_list_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
