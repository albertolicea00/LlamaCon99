package com.llamacon99.ui.welcome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.llamacon99.R

private const val INTRO_STEP = 0
private const val CONTACTS_STEP = 1
private const val PHONE_STATE_STEP = 2
private const val CALL_LOG_STEP = 3
private const val NOTIFICATIONS_STEP = 4
private const val OVERLAY_STEP = 5
private const val DONE_STEP = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val shouldShow by viewModel.shouldShow.collectAsState()
    if (!shouldShow) return

    val step by viewModel.setupStep.collectAsState()
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != androidx.compose.material3.SheetValue.Hidden },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted = Settings.canDrawOverlays(context)
        viewModel.refreshPermissionStatus()
    }

    val permissionForStep = when (step) {
        CONTACTS_STEP -> Manifest.permission.READ_CONTACTS
        PHONE_STATE_STEP -> Manifest.permission.READ_PHONE_STATE
        CALL_LOG_STEP -> Manifest.permission.READ_CALL_LOG
        NOTIFICATIONS_STEP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null
        else -> null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.refreshPermissionStatus()
    }
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayGranted = Settings.canDrawOverlays(context)
    }

    val title = when (step) {
        INTRO_STEP -> stringResource(R.string.welcome_title, stringResource(R.string.app_name))
        CONTACTS_STEP -> stringResource(R.string.welcome_contacts_title)
        PHONE_STATE_STEP -> stringResource(R.string.welcome_phone_state_title)
        CALL_LOG_STEP -> stringResource(R.string.welcome_call_log_title)
        NOTIFICATIONS_STEP -> stringResource(R.string.welcome_notifications_title)
        OVERLAY_STEP -> stringResource(R.string.welcome_overlay_title)
        else -> stringResource(R.string.welcome_done_title)
    }
    val description = when (step) {
        INTRO_STEP -> stringResource(R.string.welcome_body)
        CONTACTS_STEP -> stringResource(R.string.welcome_contacts_body)
        PHONE_STATE_STEP -> stringResource(R.string.welcome_phone_state_body)
        CALL_LOG_STEP -> stringResource(R.string.welcome_call_log_body)
        NOTIFICATIONS_STEP -> stringResource(R.string.welcome_notifications_body)
        CONTACTS_STEP, PHONE_STATE_STEP, CALL_LOG_STEP, NOTIFICATIONS_STEP -> stringResource(R.string.welcome_grant_permission)
        OVERLAY_STEP -> stringResource(
            if (overlayGranted) R.string.welcome_overlay_granted else R.string.welcome_overlay_body,
        )
        else -> stringResource(R.string.welcome_done_body)
    }
    val buttonLabel = when (step) {
        INTRO_STEP -> stringResource(R.string.welcome_start_setup)
        OVERLAY_STEP -> stringResource(
            if (overlayGranted) R.string.welcome_continue else R.string.welcome_grant_overlay,
        )
        DONE_STEP -> stringResource(R.string.welcome_enable_caller_id)
        else -> stringResource(R.string.welcome_grant_permission)
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.55f)
                .navigationBarsPadding()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WelcomeStepIcon(step, Modifier.align(Alignment.Start))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                textAlign = TextAlign.Start,
            )
            if (step in CONTACTS_STEP..OVERLAY_STEP) {
                Text(
                    text = stringResource(R.string.welcome_setup_progress, step, OVERLAY_STEP),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    when (step) {
                        INTRO_STEP -> viewModel.startSetup()
                        CONTACTS_STEP, PHONE_STATE_STEP, CALL_LOG_STEP, NOTIFICATIONS_STEP -> {
                            val permission = permissionForStep
                            if (permission == null || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.refreshPermissionStatus()
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        }
                        OVERLAY_STEP -> {
                            if (Settings.canDrawOverlays(context)) {
                                overlayGranted = true
                                viewModel.refreshPermissionStatus()
                            } else {
                                overlayLauncher.launch(Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ))
                            }
                        }
                        DONE_STEP -> {
                            viewModel.completeSetup()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(buttonLabel)
            }
            if (step == OVERLAY_STEP && !overlayGranted) {
                TextButton(
                    onClick = { viewModel.skipOverlay() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.welcome_setup_later))
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepIcon(step: Int, modifier: Modifier = Modifier) {
    if (step == INTRO_STEP) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(56.dp),
        )
        return
    }

    val image = when (step) {
        CONTACTS_STEP -> Icons.Filled.Contacts
        PHONE_STATE_STEP -> Icons.Filled.Call
        CALL_LOG_STEP -> Icons.Filled.History
        NOTIFICATIONS_STEP -> Icons.Filled.NotificationsActive
        OVERLAY_STEP -> Icons.Filled.Layers
        else -> Icons.Filled.CheckCircle
    }
    Icon(
        imageVector = image,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(30.dp),
    )
}
