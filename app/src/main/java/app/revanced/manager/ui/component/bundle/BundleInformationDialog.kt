package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.asRemoteOrNull
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.isDefault
import app.revanced.manager.domain.bundles.PatchBundleSource.Companion.propsOrNullFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleInformationDialog(
    onDismissRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    bundle: PatchBundleSource,
    onRefreshButton: () -> Unit,
) {
    val composableScope = rememberCoroutineScope()
    var viewCurrentBundlePatches by remember { mutableStateOf(false) }
    val isLocal = bundle is LocalPatchBundle
    val patchCount by remember(bundle) {
        bundle.state.map { it.patchBundleOrNull()?.patches?.size ?: 0 }
    }.collectAsStateWithLifecycle(0)
    val props by remember(bundle) {
        bundle.propsOrNullFlow()
    }.collectAsStateWithLifecycle(null)

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
        Scaffold(
            topBar = {
                BundleTopBar(
                    title = stringResource(R.string.bundle_information),
                    onBackClick = onDismissRequest,
                    onBackIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                        if (!isLocal) {
                            IconButton(onClick = onRefreshButton) {
                                Icon(
                                    Icons.Outlined.Refresh,
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
                name = bundle.name,
                remoteUrl = bundle.asRemoteOrNull?.endpoint,
                patchCount = patchCount,
                version = props?.versionInfo?.patches,
                autoUpdate = props?.autoUpdate ?: false,
                onAutoUpdateChange = {
                    composableScope.launch {
                        bundle.asRemoteOrNull?.setAutoUpdate(it)
                    }
                },
                onPatchesClick = {
                    viewCurrentBundlePatches = true
                },
            )
        }
    }
}
