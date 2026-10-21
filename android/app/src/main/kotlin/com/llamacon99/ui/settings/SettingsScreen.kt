package com.llamacon99.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.llamacon99.BuildConfig
import com.llamacon99.R
import com.llamacon99.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissionStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissionStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader(stringResource(R.string.settings_section_dialing)) }
            item {
                DialModeRow(
                    label = stringResource(R.string.settings_dial_mode_prefill),
                    selected = uiState.dialMode == SettingsRepository.DialMode.PREFILL,
                    onSelect = { viewModel.setDialMode(SettingsRepository.DialMode.PREFILL) },
                )
            }
            item {
                DialModeRow(
                    label = stringResource(R.string.settings_dial_mode_direct),
                    selected = uiState.dialMode == SettingsRepository.DialMode.DIRECT,
                    onSelect = { viewModel.setDialMode(SettingsRepository.DialMode.DIRECT) },
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.settings_anonymous_swipe_title),
                    subtitle = stringResource(R.string.settings_anonymous_swipe_subtitle),
                    checked = uiState.anonymousSwipeEnabled,
                    onCheckedChange = viewModel::setAnonymousSwipeEnabled,
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_caller_id)) }
            item {
                Text(
                    text = stringResource(R.string.settings_caller_id_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (!uiState.hasCallMonitorPermissions) {
                item {
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG),
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_grant_call_monitor_permission))
                    }
                }
            } else {
                item {
                    ToggleRow(
                        title = stringResource(R.string.settings_call_monitor_title),
                        subtitle = stringResource(R.string.settings_call_monitor_subtitle),
                        checked = uiState.callMonitorEnabled,
                        onCheckedChange = viewModel::setCallMonitorEnabled,
                    )
                }
                item {
                    ToggleRow(
                        title = stringResource(R.string.settings_monitor_from_boot_title),
                        subtitle = stringResource(R.string.settings_monitor_from_boot_subtitle),
                        checked = uiState.monitorFromBootEnabled,
                        onCheckedChange = viewModel::setMonitorFromBootEnabled,
                    )
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_developer)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_app_version)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun DialModeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}
