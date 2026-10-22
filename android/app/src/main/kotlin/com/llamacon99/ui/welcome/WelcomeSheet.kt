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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.llamacon99.R

private const val INTRO_STEP = 0
private const val PHONE_STATE_STEP = 1
private const val CALL_LOG_STEP = 2
private const val NOTIFICATIONS_STEP = 3
private const val OVERLAY_STEP = 4
private const val DONE_STEP = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val shouldShow by viewModel.shouldShow.collectAsState()
    if (!shouldShow) return

    var step by remember { mutableIntStateOf(INTRO_STEP) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val sheetState = rememberModalBottomSheetState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted = Settings.canDrawOverlays(context)
    }

    val permissionForStep = when (step) {
        PHONE_STATE_STEP -> Manifest.permission.READ_PHONE_STATE
        CALL_LOG_STEP -> Manifest.permission.READ_CALL_LOG
        NOTIFICATIONS_STEP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null
        else -> null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) step++
    }
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayGranted = Settings.canDrawOverlays(context)
    }

    val title = when (step) {
        INTRO_STEP -> stringResource(R.string.welcome_title, stringResource(R.string.app_name))
        PHONE_STATE_STEP -> stringResource(R.string.welcome_phone_state_title)
        CALL_LOG_STEP -> stringResource(R.string.welcome_call_log_title)
        NOTIFICATIONS_STEP -> stringResource(R.string.welcome_notifications_title)
        OVERLAY_STEP -> stringResource(R.string.welcome_overlay_title)
        else -> stringResource(R.string.welcome_done_title)
    }
    val description = when (step) {
        INTRO_STEP -> stringResource(R.string.welcome_body)
        PHONE_STATE_STEP -> stringResource(R.string.welcome_phone_state_body)
        CALL_LOG_STEP -> stringResource(R.string.welcome_call_log_body)
        NOTIFICATIONS_STEP -> stringResource(R.string.welcome_notifications_body)
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

    ModalBottomSheet(
        onDismissRequest = viewModel::onDismissed,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (step in PHONE_STATE_STEP..OVERLAY_STEP) {
                Text(
                    text = stringResource(R.string.welcome_setup_progress, step, OVERLAY_STEP),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
            Button(
                onClick = {
                    when (step) {
                        INTRO_STEP -> step = PHONE_STATE_STEP
                        PHONE_STATE_STEP, CALL_LOG_STEP, NOTIFICATIONS_STEP -> {
                            val permission = permissionForStep
                            if (permission == null || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                step++
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        }
                        OVERLAY_STEP -> {
                            if (Settings.canDrawOverlays(context)) {
                                overlayGranted = true
                                step = DONE_STEP
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
        }
    }
}
