package com.llamacon99.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llamacon99.BuildConfig
import com.llamacon99.R
import com.llamacon99.service.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val downloadUrl = info.apkUrl ?: info.htmlUrl
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Icon(
            imageVector = Icons.Filled.SystemUpdate,
            contentDescription = null,
            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.update_available_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).align(Alignment.CenterHorizontally),
        )
        Text(
            text = stringResource(R.string.update_available_desc, BuildConfig.VERSION_NAME, info.versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Button(
            onClick = {
                onDismiss()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.update_download))
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp),
        ) {
            Text(stringResource(R.string.update_not_now))
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
