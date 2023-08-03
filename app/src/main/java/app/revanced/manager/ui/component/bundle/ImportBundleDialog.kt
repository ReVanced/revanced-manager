package app.revanced.manager.ui.component.bundle

import android.net.Uri
import android.webkit.URLUtil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.revanced.manager.R
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.JAR_MIMETYPE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBundleDialog(
    onDismissRequest: () -> Unit,
    onRemoteSubmit: (String, String, Boolean) -> Unit,
    onLocalSubmit: (String, Uri, Uri?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    var autoUpdate by rememberSaveable { mutableStateOf(true) }
    var isLocal by rememberSaveable { mutableStateOf(false) }
    var patchBundle by rememberSaveable { mutableStateOf<Uri?>(null) }
    var integrations by rememberSaveable { mutableStateOf<Uri?>(null) }

    val patchBundleText = patchBundle?.toString().orEmpty()
    val integrationText = integrations?.toString().orEmpty()

    val inputsAreValid by remember {
        derivedStateOf {
            val nameSize = name.length
            when {
                nameSize !in 1..19 -> false
                isLocal -> patchBundle != null
                else -> remoteUrl.isNotEmpty() && URLUtil.isValidUrl(remoteUrl)
            }
        }
    }

    val patchActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { patchBundle = it }
        }
    val integrationsActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { integrations = it }
        }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Scaffold(
            topBar = {
                BundleTopBar(
                    title = stringResource(R.string.import_bundle),
                    onBackClick = onDismissRequest,
                    onBackIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    },
                    actions = {
                        TextButton(
                            enabled = inputsAreValid,
                            onClick = {
                                if (isLocal) {
                                    onLocalSubmit(name, patchBundle!!, integrations)
                                } else {
                                    onRemoteSubmit(
                                        name,
                                        remoteUrl,
                                        autoUpdate
                                    )
                                }
                            },
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(stringResource(R.string.import_))
                        }
                    }
                )
            },
        ) { paddingValues ->
            BaseBundleDialog(
                modifier = Modifier.padding(paddingValues),
                isDefault = false,
                name = name,
                onNameChange = { name = it },
                remoteUrl = remoteUrl.takeUnless { isLocal },
                onRemoteUrlChange = { remoteUrl = it },
                patchCount = 0,
                version = null,
                autoUpdate = autoUpdate,
                onAutoUpdateChange = { autoUpdate = it },
                onPatchesClick = {},
                onBundleTypeClick = { isLocal = !isLocal },
            ) {
                if (isLocal) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        value = patchBundleText,
                        onValueChange = {},
                        label = {
                            Text("Patches Source File")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    patchActivityLauncher.launch(JAR_MIMETYPE)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Topic,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        value = integrationText,
                        onValueChange = {},
                        label = {
                            Text("Integrations Source File")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    integrationsActivityLauncher.launch(APK_MIMETYPE)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Topic,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}