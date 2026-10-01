package com.llamacon99.ui.welcome

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.llamacon99.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    onOpenSettings: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val shouldShow by viewModel.shouldShow.collectAsState()
    if (!shouldShow) return

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = viewModel::onDismissed,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
            Button(
                onClick = {
                    viewModel.onDismissed()
                    onOpenSettings()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.welcome_cta))
            }
        }
    }
}
