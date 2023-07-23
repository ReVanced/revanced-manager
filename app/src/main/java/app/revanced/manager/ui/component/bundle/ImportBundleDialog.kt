package app.revanced.manager.ui.component.bundle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import app.revanced.manager.util.parseUrlOrNull
import io.ktor.http.Url

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBundleDialog(
    onDismissRequest: () -> Unit,
    onRemoteSubmit: (String, Url) -> Unit,
    onLocalSubmit: (String, Uri, Uri?) -> Unit,
    patchCount: Int = 0,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    var checked by remember { mutableStateOf(true) }
    var isLocal by rememberSaveable { mutableStateOf(false) }
    var patchBundle by rememberSaveable { mutableStateOf<Uri?>(null) }
    var integrations by rememberSaveable { mutableStateOf<Uri?>(null) }

    val patchBundleText = patchBundle?.toString().orEmpty()
    val integrationText = integrations?.toString().orEmpty()

    val inputsAreValid by remember {
        derivedStateOf {
            val nameSize = name.length
            nameSize in 4..19 && if (isLocal) patchBundle != null else {
                remoteUrl.isNotEmpty() && remoteUrl.parseUrlOrNull() != null
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

    val onPatchLauncherClick = {
        patchActivityLauncher.launch(JAR_MIMETYPE)
    }

    val onIntegrationLauncherClick = {
        integrationsActivityLauncher.launch(APK_MIMETYPE)
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
                        Text(
                            text = stringResource(R.string.import_),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable {
                                    if (inputsAreValid) {
                                        if (isLocal) {
                                            onLocalSubmit(name, patchBundle!!, integrations)
                                        } else {
                                            onRemoteSubmit(name, remoteUrl.parseUrlOrNull()!!)
                                        }
                                    }
                                }
                        )
                    }
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 16.dp,
                        end = 24.dp,
                    )
                ) {
                    BundleTextContent(
                        name = name,
                        onNameChange = { name = it },
                        isLocal = isLocal,
                        remoteUrl = remoteUrl,
                        onRemoteUrlChange = { remoteUrl = it },
                    )

                    if(isLocal) {
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
                                    onClick = onPatchLauncherClick
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
                                IconButton(onClick = onIntegrationLauncherClick) {
                                    Icon(
                                        imageVector = Icons.Default.Topic,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }

                Column(
                    Modifier.padding(
                        start = 8.dp,
                        top = 8.dp,
                        end = 4.dp,
                    )
                ) {
                    BundleInfoContent(
                        switchChecked = checked,
                        onCheckedChange = { checked = it },
                        patchInfoText = stringResource(R.string.no_patches),
                        patchCount = patchCount,
                        onArrowClick = {},
                        tonalButtonContent = {
                            if (isLocal) {
                                Text(stringResource(R.string.local))
                            } else {
                                Text(stringResource(R.string.remote))
                            }
                        },
                        tonalButtonOnClick = { isLocal = !isLocal },
                        isLocal = isLocal,
                    )
                }
            }
        }
    }
}
