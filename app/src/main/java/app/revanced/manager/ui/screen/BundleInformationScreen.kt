package app.revanced.manager.ui.screen

import android.webkit.URLUtil.isValidUrl
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.TextInputDialog
import app.revanced.manager.ui.component.bundle.BundleListItem
import app.revanced.manager.ui.component.bundle.BundlePatchesDialog
import app.revanced.manager.ui.component.haptics.HapticSwitch
import app.revanced.manager.ui.viewmodel.BundleInformationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleInformationScreen(
    onBackClick: () -> Unit,
    viewModel: BundleInformationViewModel
) {
    val src = viewModel.bundle ?: return
    val patchCount = viewModel.patchCount

    var viewCurrentBundlePatches by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val isLocal = src is LocalPatchBundle
    val bundleManifestAttributes = src.patchBundle?.manifestAttributes
    val (autoUpdate, endpoint) = src.asRemoteOrNull?.let { it.autoUpdate to it.endpoint }
        ?: (null to null)

    if (viewCurrentBundlePatches) {
        BundlePatchesDialog(
            src = src,
            onDismissRequest = { viewCurrentBundlePatches = false }
        )
    }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                viewModel.delete()
                onBackClick()
            },
            title = stringResource(R.string.delete),
            description = stringResource(R.string.patches_delete_single_dialog_description, src.name),
            icon = Icons.Outlined.Delete
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = src.name,
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
                actions = {
                    if (!src.isDefault) {
                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                stringResource(R.string.delete)
                            )
                        }
                    }
                    val hasNetwork = remember { viewModel.networkInfo.isConnected() }
                    if (!isLocal && hasNetwork) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(
                                Icons.Outlined.Update,
                                stringResource(R.string.refresh)
                            )
                        }
                    }
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Tag(Icons.Outlined.Sell, src.name)
                bundleManifestAttributes?.description?.let {
                    Tag(Icons.Outlined.Description, it)
                }
                bundleManifestAttributes?.source?.let {
                    Tag(Icons.Outlined.Commit, it)
                }
                bundleManifestAttributes?.author?.let {
                    Tag(Icons.Outlined.Person, it)
                }
                bundleManifestAttributes?.contact?.let {
                    Tag(Icons.AutoMirrored.Outlined.Send, it)
                }
                bundleManifestAttributes?.website?.let {
                    Tag(Icons.Outlined.Language, it, isUrl = true)
                }
                bundleManifestAttributes?.license?.let {
                    Tag(Icons.Outlined.Gavel, it)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            ListSection {
                if (autoUpdate != null) {
                    BundleListItem(
                        headlineText = stringResource(R.string.auto_update),
                        supportingText = stringResource(R.string.auto_update_description),
                        trailingContent = {
                            HapticSwitch(
                                checked = autoUpdate,
                                onCheckedChange = viewModel::setAutoUpdate
                            )
                        },
                        onClick = { viewModel.setAutoUpdate(!autoUpdate) }
                    )
                }

                if (src.isDefault) {
                    val useBundlePrerelease by viewModel.prefs.usePatchesPrereleases.getAsState()

                    BundleListItem(
                        headlineText = stringResource(R.string.patches_prereleases),
                        supportingText = stringResource(R.string.patches_prereleases_description, src.name),
                        trailingContent = {
                            HapticSwitch(
                                checked = useBundlePrerelease,
                                onCheckedChange = viewModel::updateUsePrereleases
                            )
                        },
                        onClick = { viewModel.updateUsePrereleases(!useBundlePrerelease) }
                    )
                }

                endpoint?.takeUnless { src.isDefault }?.let { url ->
                    var showUrlInputDialog by rememberSaveable { mutableStateOf(false) }

                    if (showUrlInputDialog) {
                        TextInputDialog(
                            initial = url,
                            title = stringResource(R.string.patches_url),
                            onDismissRequest = { showUrlInputDialog = false },
                            onConfirm = {
                                showUrlInputDialog = false
                                // TODO: Not implemented
                            },
                            validator = {
                                if (it.isEmpty()) return@TextInputDialog false
                                isValidUrl(it)
                            }
                        )
                    }

                    BundleListItem(
                        headlineText = stringResource(R.string.patches_url),
                        supportingText = url.ifEmpty {
                            stringResource(R.string.field_not_set)
                        },
                        onClick = null
                    )
                }

                val patchesClickable = patchCount > 0
                BundleListItem(
                    headlineText = stringResource(R.string.patches),
                    supportingText = stringResource(R.string.view_patches),
                    onClick = if (patchesClickable) {
                        { viewCurrentBundlePatches = true }
                    } else null,
                    trailingContent = if (patchesClickable) {
                        {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowRight,
                                stringResource(R.string.patches)
                            )
                        }
                    } else null
                )

                src.error?.let {
                    var showDialog by rememberSaveable { mutableStateOf(false) }

                    if (showDialog) {
                        ExceptionViewerDialog(
                            onDismiss = { showDialog = false },
                            text = remember(it) { it.stackTraceToString() }
                        )
                    }

                    BundleListItem(
                        headlineText = stringResource(R.string.patches_error),
                        supportingText = stringResource(R.string.patches_error_description),
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowRight,
                                null
                            )
                        },
                        onClick = { showDialog = true }
                    )
                }

                if (src.state is PatchBundleSource.State.Missing && !isLocal) {
                    BundleListItem(
                        headlineText = stringResource(R.string.patches_error),
                        supportingText = stringResource(R.string.patches_not_downloaded),
                        onClick = viewModel::refresh
                    )
                }
            }
        }
    }
}

@Composable
private fun Tag(
    icon: ImageVector,
    text: String,
    isUrl: Boolean = false
) {
    val uriHandler = LocalUriHandler.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isUrl) {
            Modifier.clickable {
                try {
                    uriHandler.openUri(text)
                } catch (_: Exception) {
                }
            }
        } else Modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}
