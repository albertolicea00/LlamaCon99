package com.llamacon99.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.llamacon99.BuildConfig
import com.llamacon99.R
import com.llamacon99.data.SettingsRepository
import com.llamacon99.monitor.CallMonitorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissionStatus() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissionStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader(stringResource(R.string.settings_section_dialing)) }
            item {
                SettingsGroup {
                    DialModeSelector(
                        selected = uiState.dialMode,
                        onSelect = { mode ->
                            if (mode == SettingsRepository.DialMode.DIRECT) {
                                viewModel.setDialMode(mode)
                                permissionLauncher.launch(arrayOf(android.Manifest.permission.CALL_PHONE))
                            } else {
                                viewModel.setDialMode(mode)
                            }
                        },
                    )
                    GroupDivider()
                    ToggleRow(
                        title = stringResource(R.string.settings_anonymous_swipe_title),
                        subtitle = stringResource(R.string.settings_anonymous_swipe_subtitle),
                        checked = uiState.anonymousSwipeEnabled,
                        onCheckedChange = viewModel::setAnonymousSwipeEnabled,
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.settings_section_caller_id)) }
            item {
                SettingsGroup {
                    if (!uiState.hasCallMonitorPermissions) {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.settings_monitor_permission_title))
                            },
                            supportingContent = { Text(stringResource(R.string.settings_grant_call_monitor_permission)) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            colors = settingsRowColors(),
                            modifier = Modifier.clickable {
                                permissionLauncher.launch(
                                    CallMonitorService.requiredPermissions(),
                                )
                            },
                        )
                    } else {
                        ToggleRow(
                            title = stringResource(R.string.settings_call_monitor_title),
                            subtitle = stringResource(R.string.settings_call_monitor_subtitle),
                            checked = uiState.callMonitorEnabled,
                            onCheckedChange = viewModel::setCallMonitorEnabled,
                        )
                        GroupDivider()
                        ToggleRow(
                            title = stringResource(R.string.settings_monitor_from_boot_title),
                            subtitle = stringResource(R.string.settings_monitor_from_boot_subtitle),
                            checked = uiState.monitorFromBootEnabled,
                            onCheckedChange = viewModel::setMonitorFromBootEnabled,
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.settings_caller_id_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item { SectionHeader(stringResource(R.string.settings_section_developer)) }
            item {
                SettingsGroup {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_app_version)) },
                        trailingContent = {
                            Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = settingsRowColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun settingsRowColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun DialModeSelector(selected: SettingsRepository.DialMode, onSelect: (SettingsRepository.DialMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (selected == SettingsRepository.DialMode.PREFILL) {
        R.string.settings_dial_mode_prefill_short
    } else {
        R.string.settings_dial_mode_direct_short
    }
    Box {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_dial_mode_title)) },
            supportingContent = { Text(stringResource(selectedLabel), color = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
            colors = settingsRowColors(),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            listOf(SettingsRepository.DialMode.PREFILL, SettingsRepository.DialMode.DIRECT).forEach { mode ->
                val prefill = mode == SettingsRepository.DialMode.PREFILL
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(if (prefill) R.string.settings_dial_mode_prefill_short else R.string.settings_dial_mode_direct_short))
                            Text(
                                stringResource(if (prefill) R.string.settings_dial_mode_prefill else R.string.settings_dial_mode_direct),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    },
                    trailingIcon = {
                        if (mode == selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = settingsRowColors(),
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}
