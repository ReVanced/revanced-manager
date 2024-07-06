package app.revanced.manager.ui.component.bundle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.revanced.manager.ui.model.BundleType
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.JAR_MIMETYPE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBundleDialog(
    onDismissRequest: () -> Unit,
    onRemoteSubmit: (String, Boolean) -> Unit,
    onLocalSubmit: (Uri, Uri?) -> Unit,
    initialBundleType: BundleType
) {
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    var autoUpdate by rememberSaveable { mutableStateOf(true) }
    var bundleType by rememberSaveable { mutableStateOf(initialBundleType) }
    var patchBundle by rememberSaveable { mutableStateOf<Uri?>(null) }
    var integrations by rememberSaveable { mutableStateOf<Uri?>(null) }

    val inputsAreValid by remember {
        derivedStateOf {
            if (bundleType == BundleType.Local) patchBundle != null else remoteUrl.isNotEmpty()
        }
    }

    val patchActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { patchBundle = it }
        }

    fun launchPatchActivity() {
        patchActivityLauncher.launch(JAR_MIMETYPE)
    }

    val integrationsActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { integrations = it }
        }

    fun launchIntegrationsActivity() {
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
                    backIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    },
                    actions = {
                        TextButton(
                            enabled = inputsAreValid,
                            onClick = {
                                when (bundleType) {
                                    BundleType.Local -> onLocalSubmit(patchBundle!!, integrations)
                                    BundleType.Remote -> onRemoteSubmit(remoteUrl, autoUpdate)
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
                name = null,
                remoteUrl = remoteUrl.takeUnless { bundleType == BundleType.Local },
                onRemoteUrlChange = { remoteUrl = it },
                patchCount = 0,
                version = null,
                autoUpdate = autoUpdate,
                onAutoUpdateChange = { autoUpdate = it },
                onPatchesClick = {},
                onBundleTypeClick = {
                    bundleType = when (bundleType) {
                        BundleType.Local -> BundleType.Remote
                        BundleType.Remote -> BundleType.Local
                    }
                },
            ) {
                if (bundleType == BundleType.Remote) return@BaseBundleDialog

                BundleListItem(
                    headlineText = stringResource(R.string.patch_bundle_field),
                    supportingText = stringResource(if (patchBundle != null) R.string.file_field_set else R.string.file_field_not_set),
                    trailingContent = {
                        IconButton(
                            onClick = ::launchPatchActivity
                        ) {
                            Icon(
                                imageVector = Icons.Default.Topic,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        launchPatchActivity()
                    }
                )

                BundleListItem(
                    headlineText = stringResource(R.string.integrations_field),
                    supportingText = stringResource(if (integrations != null) R.string.file_field_set else R.string.file_field_not_set),
                    trailingContent = {
                        IconButton(
                            onClick = ::launchIntegrationsActivity
                        ) {
                            Icon(
                                imageVector = Icons.Default.Topic,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        launchIntegrationsActivity()
                    }
                )
            }
        }
    }
}
