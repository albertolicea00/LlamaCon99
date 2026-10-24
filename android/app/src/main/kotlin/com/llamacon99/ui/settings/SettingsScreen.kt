package com.llamacon99.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.llamacon99.BuildConfig
import com.llamacon99.R
import com.llamacon99.data.SettingsRepository
import com.llamacon99.monitor.CallMonitorService
import com.llamacon99.service.UpdateCheckResult
import com.llamacon99.service.UpdateChecker
import com.llamacon99.service.UpdateInfo
import com.llamacon99.ui.theme.AppThemeCatalog
import com.llamacon99.ui.theme.AppThemeDefinition
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.LlamaCon99Theme
import com.llamacon99.ui.theme.warningTextColor
import com.llamacon99.ui.theme.ThemeStyle
import com.llamacon99.ui.components.UpdateAvailableDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingDirectCall by rememberSaveable { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var pendingUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val upToDateText = stringResource(R.string.update_up_to_date)
    val failedText = stringResource(R.string.update_check_failed)

    val onCheckUpdate: () -> Unit = {
        isCheckingUpdate = true
        updateStatus = null
        scope.launch {
            when (val result = UpdateChecker.check()) {
                is UpdateCheckResult.Available -> {
                    pendingUpdateInfo = result.info
                }
                is UpdateCheckResult.UpToDate -> {
                    updateStatus = upToDateText
                }
                is UpdateCheckResult.Failed -> {
                    updateStatus = failedText
                }
            }
            isCheckingUpdate = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissionStatus() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissionStatus() }
    val directCallPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        viewModel.refreshPermissionStatus()
        if (pendingDirectCall && grants[android.Manifest.permission.CALL_PHONE] == true &&
            grants[android.Manifest.permission.READ_PHONE_STATE] == true) {
            viewModel.setDialMode(SettingsRepository.DialMode.DIRECT)
        }
        pendingDirectCall = false
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshPermissionStatus() }

    SettingsContent(
        uiState = uiState,
        appTheme = appTheme,
        onBack = onBack,
        isCheckingUpdate = isCheckingUpdate,
        updateStatus = updateStatus,
        actions = SettingsActions(
            onThemePicker = { showThemeSheet = true },
            onSelectDialMode = { mode ->
                if (mode == SettingsRepository.DialMode.DIRECT) {
                    pendingDirectCall = true
                    directCallPermissionLauncher.launch(arrayOf(
                        android.Manifest.permission.CALL_PHONE,
                        android.Manifest.permission.READ_PHONE_STATE,
                    ))
                } else viewModel.setDialMode(mode)
            },
            onAnonymousSwipe = viewModel::setAnonymousSwipeEnabled,
            onCallMonitor = viewModel::setCallMonitorEnabled,
            onMonitorFromBoot = viewModel::setMonitorFromBootEnabled,
            onMonitorPermissions = { permissionLauncher.launch(CallMonitorService.requiredPermissions()) },
            onOverlayPermissions = { overlayPermissionLauncher.launch(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"),
            )) },
            onCheckUpdate = onCheckUpdate,
            onLegalLinks = { showLegalDialog = true },
        ),
        overlays = {
            if (pendingUpdateInfo != null) {
                UpdateAvailableDialog(
                    info = pendingUpdateInfo!!,
                    onDismiss = { pendingUpdateInfo = null },
                )
            }

            if (showLegalDialog) {
                LegalLinksSheet(onDismiss = { showLegalDialog = false })
            }
            if (showThemeSheet) {
                ThemePickerSheet(
                    selected = appTheme,
                    onSelect = { viewModel.setAppTheme(it); showThemeSheet = false },
                    onDismiss = { showThemeSheet = false },
                )
            }

        },
    )
}

internal data class SettingsActions(
    val onThemePicker: () -> Unit,
    val onSelectDialMode: (SettingsRepository.DialMode) -> Unit,
    val onAnonymousSwipe: (Boolean) -> Unit,
    val onCallMonitor: (Boolean) -> Unit,
    val onMonitorFromBoot: (Boolean) -> Unit,
    val onMonitorPermissions: () -> Unit,
    val onOverlayPermissions: () -> Unit,
    val onCheckUpdate: () -> Unit,
    val onLegalLinks: () -> Unit,
)

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    appTheme: AppThemeDefinition,
    actions: SettingsActions,
    onBack: () -> Unit,
    isCheckingUpdate: Boolean = false,
    updateStatus: String? = null,
    overlays: @Composable () -> Unit = {},
) {
    SettingsLayout(onBack = onBack, overlays = overlays) {
        item { SectionHeader(stringResource(R.string.settings_section_preferences)) }
        item {
            SettingsGroup {
                ThemeSelector(selected = appTheme, onClick = { actions.onThemePicker() })
                GroupDivider()
                DialModeSelector(
                    selected = uiState.dialMode,
                    isDualSim = uiState.isDualSim,
                    onSelect = actions.onSelectDialMode,
                )
                GroupDivider()
                ToggleRow(
                    title = stringResource(R.string.settings_anonymous_swipe_title),
                    subtitle = stringResource(R.string.settings_anonymous_swipe_subtitle),
                    checked = uiState.anonymousSwipeEnabled,
                    onCheckedChange = actions.onAnonymousSwipe,
                )
            }
        }
        item { SectionHeader(stringResource(R.string.settings_section_caller_id)) }
        item {
            SettingsGroup {
                if (!uiState.hasCallMonitorPermissions) {
                    SettingsRow(
                        headlineContent = {
                            Text(stringResource(R.string.settings_monitor_permission_title))
                        },
                        supportingContent = { Text(stringResource(R.string.settings_grant_call_monitor_permission)) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        colors = settingsRowColors(),
                        modifier = Modifier.clickable {
                            actions.onMonitorPermissions()
                        },
                    )
                } else {
                    ToggleRow(
                        title = stringResource(R.string.settings_call_monitor_title),
                        subtitle = stringResource(R.string.settings_call_monitor_subtitle),
                        checked = uiState.callMonitorEnabled,
                        onCheckedChange = actions.onCallMonitor,
                    )
                    if (uiState.callMonitorEnabled) {
                        GroupDivider()
                        ToggleRow(
                            title = stringResource(R.string.settings_monitor_from_boot_title),
                            subtitle = stringResource(R.string.settings_monitor_from_boot_subtitle),
                            checked = uiState.monitorFromBootEnabled,
                            onCheckedChange = actions.onMonitorFromBoot,
                        )
                        GroupDivider()
                        SettingsRow(
                            headlineContent = { Text(stringResource(R.string.settings_caller_id_overlay_title)) },
                            leadingContent = {
                                Icon(
                                    imageVector = if (uiState.canShowCallerIdOverlay) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.Warning
                                    },
                                    contentDescription = null,
                                    tint = if (uiState.canShowCallerIdOverlay) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            },
                            supportingContent = if (uiState.canShowCallerIdOverlay) null else {
                                {
                                    Text(stringResource(R.string.settings_caller_id_overlay_permission))
                                }
                            },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            colors = settingsRowColors(),
                            modifier = Modifier.clickable {
                                actions.onOverlayPermissions()
                            },
                        )
                    }
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
        item { SectionHeader(stringResource(R.string.settings_section_about)) }
        item {
            SettingsGroup {
                SettingsRow(
                    headlineContent = { Text(stringResource(R.string.settings_check_updates)) },
                    supportingContent = updateStatus?.let { status -> { Text(status) } },
                    trailingContent = {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                        }
                    },
                    colors = settingsRowColors(),
                    modifier = Modifier.clickable(enabled = !isCheckingUpdate) { actions.onCheckUpdate() },
                )
                GroupDivider()
                SettingsRow(
                    headlineContent = { Text(stringResource(R.string.settings_app_version)) },
                    trailingContent = {
                        Text(
                            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = settingsRowColors(),
                )
                GroupDivider()
                SettingsRow(
                    headlineContent = { Text(stringResource(R.string.settings_legal_links)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    colors = settingsRowColors(),
                    modifier = Modifier.clickable { actions.onLegalLinks() },
                )
            }
        }
    }
}


@Composable
private fun ThemeSelector(selected: AppThemeDefinition, onClick: () -> Unit) {
    val inlineValue = LocalAppTheme.current.style in setOf(ThemeStyle.APPLE, ThemeStyle.HORIZON, ThemeStyle.HYPER, ThemeStyle.FLUENT)
    SettingsRow(
        headlineContent = { Text(stringResource(R.string.settings_theme_title)) },
        supportingContent = if (inlineValue) null else { { Text(stringResource(selected.nameRes), color = MaterialTheme.colorScheme.primary) } },
        trailingContent = { if (inlineValue) PreferenceValue(stringResource(selected.nameRes)) else SelectionAccessory() },
        colors = settingsRowColors(),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePickerSheet(
    selected: AppThemeDefinition,
    onSelect: (AppThemeDefinition) -> Unit,
    onDismiss: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SettingsSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SettingsSheetTitle(
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.settings_theme_description),
                )
            }
            items(AppThemeCatalog.themes, key = { it.id }) { theme ->
                LlamaCon99Theme(darkTheme = dark, appTheme = theme) {
                    SettingsModalOption(
                        title = stringResource(theme.nameRes),
                        subtitle = stringResource(theme.inspirationRes),
                        selected = theme.id == selected.id,
                        onClick = { onSelect(theme) },
                        index = AppThemeCatalog.themes.indexOf(theme),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalAppTheme.current
    val groupModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
    if (theme.style == ThemeStyle.APPLE) {
        SettingsGlass(shape = RoundedCornerShape(26.dp), modifier = groupModifier) {
            Column(Modifier.padding(vertical = 4.dp), content = content)
        }
        return
    }
    if (theme.style == ThemeStyle.MATERIAL) {
        Column(groupModifier.padding(vertical = 4.dp), content = content)
        return
    }
    val corner = when (theme.style) {
        ThemeStyle.MONO -> 0.dp
        ThemeStyle.FLUENT -> 6.dp
        ThemeStyle.HORIZON -> 24.dp
        ThemeStyle.HYPER -> 20.dp
        else -> 36.dp
    }
    Surface(
        modifier = groupModifier,
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (theme.contacts.borderWidth > 0.dp) BorderStroke(theme.contacts.borderWidth, MaterialTheme.colorScheme.outlineVariant) else null,
        shadowElevation = theme.contacts.elevation,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SectionHeader(text: String) {
    val style = LocalAppTheme.current.style
    val apple = style == ThemeStyle.APPLE
    if (style == ThemeStyle.MONO) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 24.dp, bottom = 8.dp))
        return
    }
    Text(
        text = if (apple) text.uppercase() else text,
        style = if (apple) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelLarge,
        color = if (style == ThemeStyle.MATERIAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupDivider() {
    val style = LocalAppTheme.current.style
    if (style == ThemeStyle.MATERIAL) return
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = if (style == ThemeStyle.MONO) 1.dp else 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (style == ThemeStyle.APPLE) 0.45f else 0.6f),
    )
}

@Composable
private fun settingsRowColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialModeSelector(selected: SettingsRepository.DialMode, isDualSim: Boolean, onSelect: (SettingsRepository.DialMode) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val selectedLabel = if (selected == SettingsRepository.DialMode.PREFILL) {
        R.string.settings_dial_mode_prefill_short
    } else {
        R.string.settings_dial_mode_direct_short
    }
    val inlineValue = LocalAppTheme.current.style in setOf(ThemeStyle.APPLE, ThemeStyle.HORIZON, ThemeStyle.HYPER, ThemeStyle.FLUENT)
    SettingsRow(
        headlineContent = { Text(stringResource(R.string.settings_dial_mode_title)) },
        supportingContent = if (inlineValue) null else { { Text(stringResource(selectedLabel), color = MaterialTheme.colorScheme.primary) } },
        trailingContent = { if (inlineValue) PreferenceValue(stringResource(selectedLabel)) else SelectionAccessory() },
        colors = settingsRowColors(),
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true },
    )
    if (showSheet) {
        SettingsSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp)) {
                SettingsSheetTitle(
                    title = stringResource(R.string.settings_dial_mode_title),
                    subtitle = stringResource(if (isDualSim) R.string.settings_dual_sim_body else R.string.settings_dial_mode_sheet_description),
                    subtitleColor = if (isDualSim) warningTextColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf(SettingsRepository.DialMode.PREFILL, SettingsRepository.DialMode.DIRECT).forEachIndexed { index, mode ->
                    val prefill = mode == SettingsRepository.DialMode.PREFILL
                    val enabled = prefill || !isDualSim
                    SettingsModalOption(
                        title = stringResource(if (prefill) R.string.settings_dial_mode_prefill_short else R.string.settings_dial_mode_direct_short),
                        subtitle = stringResource(if (prefill) R.string.settings_dial_mode_prefill else R.string.settings_dial_mode_direct),
                        selected = mode == selected,
                        index = index,
                        enabled = enabled,
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                showSheet = false
                                onSelect(mode)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsRow(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { ProfileToggle(checked) },
        colors = settingsRowColors(),
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun SelectionAccessory() {
    Icon(if (LocalAppTheme.current.style in setOf(ThemeStyle.APPLE, ThemeStyle.HORIZON, ThemeStyle.HYPER)) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
        contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PreferenceValue(value: String) {
    val fluent = LocalAppTheme.current.style == ThemeStyle.FLUENT
    Row(Modifier.then(if (fluent) Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
        .padding(horizontal = 8.dp, vertical = 6.dp) else Modifier), verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        SelectionAccessory()
    }
}

@Composable
private fun ProfileToggle(checked: Boolean) {
    val style = LocalAppTheme.current.style
    if (style == ThemeStyle.MATERIAL) {
        Switch(checked = checked, onCheckedChange = null)
        return
    }
    val fluent = style == ThemeStyle.FLUENT
    val width = when (style) {
        ThemeStyle.APPLE -> 51.dp; ThemeStyle.FLUENT -> 40.dp; ThemeStyle.MONO -> 48.dp
        ThemeStyle.HYPER -> 46.dp; else -> 44.dp
    }
    val height = when (style) { ThemeStyle.APPLE -> 31.dp; ThemeStyle.FLUENT -> 20.dp; ThemeStyle.BRAND -> 24.dp; else -> 26.dp }
    val thumb = when (style) { ThemeStyle.APPLE -> 27.dp; ThemeStyle.FLUENT -> 12.dp; ThemeStyle.HYPER -> 22.dp; else -> 20.dp }
    val inset = (height - thumb) / 2
    val position by animateDpAsState(if (checked) width - thumb - inset else inset, label = "profileSwitch")
    val shape = if (style == ThemeStyle.MONO) RoundedCornerShape(2.dp) else RoundedCornerShape(50)
    Box(Modifier.size(width, height)
        .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest, shape)
        .then(if (fluent || style == ThemeStyle.MONO) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier)) {
        Box(Modifier.offset(x = position, y = inset).size(thumb)
            .background(if (checked || !fluent) Color.White else MaterialTheme.colorScheme.onSurface, shape))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalLinksSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    SettingsSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        SettingsSheetTitle(title = stringResource(R.string.settings_legal_links))
        listOf(
            R.string.settings_privacy_policy to "https://llama99.vercel.app/privacy",
            R.string.settings_terms to "https://llama99.vercel.app/terms",
            R.string.settings_eula to "https://llama99.vercel.app/eula",
        ).forEachIndexed { index, (label, url) ->
            SettingsModalOption(
                title = stringResource(label),
                selected = false,
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    onDismiss()
                },
                index = index,
                chevron = true,
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
