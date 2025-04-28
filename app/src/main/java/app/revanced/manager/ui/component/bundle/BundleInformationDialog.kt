package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.nameState
import app.revanced.manager.ui.component.ExceptionViewerDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleInformationDialog(
    onDismissRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    bundle: PatchBundleSource,
    onUpdate: () -> Unit,
) {
    val networkInfo = koinInject<NetworkInfo>()
    val hasNetwork = remember { networkInfo.isConnected() }
    val composableScope = rememberCoroutineScope()
    var viewCurrentBundlePatches by remember { mutableStateOf(false) }
    val isLocal = bundle is LocalPatchBundle
    val state by bundle.state.collectAsStateWithLifecycle()
    val props by remember(bundle) {
        bundle.propsFlow()
    }.collectAsStateWithLifecycle(null)
    val patchCount = remember(state) {
        state.patchBundleOrNull()?.patches?.size ?: 0
    }

    if (viewCurrentBundlePatches) {
        BundlePatchesDialog(
            onDismissRequest = {
                viewCurrentBundlePatches = false
            },
            bundle = bundle,
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        val bundleName by bundle.nameState

        Scaffold(
            topBar = {
                BundleTopBar(
                    title = stringResource(R.string.patch_bundle_field),
                    onBackClick = onDismissRequest,
                    backIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    },
                    actions = {
                        if (!bundle.isDefault) {
                            IconButton(onClick = onDeleteRequest) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    stringResource(R.string.delete)
                                )
                            }
                        }
                        if (!isLocal && hasNetwork) {
                            IconButton(onClick = onUpdate) {
                                Icon(
                                    Icons.Outlined.Update,
                                    stringResource(R.string.refresh)
                                )
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            BaseBundleDialog(
                modifier = Modifier.padding(paddingValues),
                isDefault = bundle.isDefault,
                name = bundleName,
                remoteUrl = bundle.asRemoteOrNull?.endpoint,
                patchCount = patchCount,
                version = props?.version,
                autoUpdate = props?.autoUpdate ?: false,
                onAutoUpdateChange = {
                    composableScope.launch {
                        bundle.asRemoteOrNull?.setAutoUpdate(it)
                    }
                },
                onPatchesClick = {
                    viewCurrentBundlePatches = true
                },
                extraFields = {
                    (state as? PatchBundleSource.State.Failed)?.throwable?.let {
                        var showDialog by rememberSaveable {
                            mutableStateOf(false)
                        }
                        if (showDialog) ExceptionViewerDialog(
                            onDismiss = { showDialog = false },
                            text = remember(it) { it.stackTraceToString() }
                        )

                        BundleListItem(
                            headlineText = stringResource(R.string.bundle_error),
                            supportingText = stringResource(R.string.bundle_error_description),
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowRight,
                                    null
                                )
                            },
                            modifier = Modifier.clickable { showDialog = true }
                        )
                    }

                    if (state is PatchBundleSource.State.Missing && !isLocal) {
                        BundleListItem(
                            headlineText = stringResource(R.string.bundle_error),
                            supportingText = stringResource(R.string.bundle_not_downloaded),
                            modifier = Modifier.clickable(onClick = onUpdate)
                        )
                    }
                }
            )
        }
    }
}