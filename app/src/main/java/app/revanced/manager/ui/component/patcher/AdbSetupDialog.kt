package app.revanced.manager.ui.component.patcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.settings.ExpandableSettingsListItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdbSetupDialog(
    isShizukuAuthorized: Boolean,
    isAdbConnected: Boolean,
    isPairing: Boolean,
    adbPort: String,
    adbPairingPort: String,
    adbPairingCode: String,
    onPortChange: (String) -> Unit,
    onPairingPortChange: (String) -> Unit,
    onPairingCodeChange: (String) -> Unit,
    onBootstrapAdb: () -> Unit,
    onConnectAdb: () -> Unit,
    onPairAdb: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(isAdbConnected) {
        if (isAdbConnected) onDismiss()
    }

    FullscreenDialog(onDismissRequest = onDismiss) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.adb_setup),
                    onBackClick = onDismiss
                )
            }
        ) { paddingValues ->
            ColumnWithScrollbar(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.adb_pairing_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isShizukuAuthorized) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.adb_setup_auto_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.adb_setup_auto_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onBootstrapAdb,
                            modifier = Modifier.fillMaxWidth(),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.adb_bootstrap_shizuku))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.adb_setup_manual_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    ExpandableSettingsListItem(
                        headlineContent = stringResource(R.string.adb_pair),
                        supportingContent = stringResource(R.string.adb_pairing_description_code),
                        expandableContent = {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = adbPairingPort,
                                    onValueChange = onPairingPortChange,
                                    label = { Text(stringResource(R.string.adb_port)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = adbPairingCode,
                                    onValueChange = onPairingCodeChange,
                                    label = { Text(stringResource(R.string.adb_pairing_code)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Button(
                                    onClick = onPairAdb,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = adbPairingPort.isNotEmpty() && adbPairingCode.isNotEmpty() && !isPairing,
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(if (isPairing) stringResource(R.string.adb_pairing_in_progress) else stringResource(R.string.adb_pair))
                                }
                            }
                        }
                    )

                    ExpandableSettingsListItem(
                        headlineContent = stringResource(R.string.connect),
                        supportingContent = stringResource(R.string.adb_connect_description),
                        expandableContent = {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = adbPort,
                                    onValueChange = onPortChange,
                                    label = { Text(stringResource(R.string.adb_port)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Button(
                                    onClick = onConnectAdb,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = adbPort.isNotEmpty(),
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(stringResource(R.string.connect))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
